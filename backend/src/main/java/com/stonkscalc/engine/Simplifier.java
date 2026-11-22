package com.stonkscalc.engine;

import com.stonkscalc.engine.Node.Bin;
import com.stonkscalc.engine.Node.Call;
import com.stonkscalc.engine.Node.Neg;
import com.stonkscalc.engine.Node.Num;
import com.stonkscalc.engine.Node.Var;

import java.util.ArrayList;
import java.util.List;

import static com.stonkscalc.engine.Node.ONE;
import static com.stonkscalc.engine.Node.ZERO;
import static com.stonkscalc.engine.Node.isNum;
import static com.stonkscalc.engine.Node.num;

/** Simplificação algébrica básica: dobra constantes e remove identidades (x+0, x*1, x^1...). */
public final class Simplifier {

    private static final Evaluator CONST = new Evaluator(AngleMode.RAD);

    private Simplifier() {
    }

    public static Node simplify(Node n) {
        Node prev;
        int guard = 0;
        do {
            prev = n;
            n = step(n);
        } while (!n.equals(prev) && ++guard < 50);
        return n;
    }

    private static boolean isInt(double v) {
        return Double.isFinite(v) && v == Math.rint(v) && Math.abs(v) < 1e15;
    }

    private static Node step(Node n) {
        return switch (n) {
            case Num x -> x;
            case Var x -> x;
            case Neg(Node o) -> neg(step(o));
            case Call(String name, List<Node> args) -> call(name, args);
            case Bin(char op, Node l, Node r) -> bin(op, step(l), step(r));
        };
    }

    private static Node neg(Node o) {
        if (o instanceof Num(double v)) return num(-v);
        if (o instanceof Neg(Node inner)) return inner;
        if (o instanceof Bin(char op, Node l, Node r) && op == '-') return new Bin('-', r, l);
        return new Neg(o);
    }

    private static Node call(String name, List<Node> args) {
        List<Node> s = new ArrayList<>(args.size());
        boolean allNum = true;
        for (Node a : args) {
            Node x = step(a);
            s.add(x);
            allNum &= x instanceof Num;
        }
        if (name.equals("ln") && s.size() == 1 && s.get(0) instanceof Var(String v) && v.equals("e")) return ONE;
        if (name.equals("exp") && s.size() == 1 && isNum(s.get(0), 0)) return ONE;
        Call c = new Call(name, s);
        if (allNum && !Functions.LAZY.contains(name) && !name.startsWith("rand")) {
            double v = CONST.eval(c);
            if (isInt(v)) return num(v); // só dobra quando o resultado é "bonito" (sqrt(4) → 2, mas sqrt(2) fica)
        }
        return c;
    }

    private static Node bin(char op, Node l, Node r) {
        if (l instanceof Num(double a) && r instanceof Num(double b)) {
            Double folded = switch (op) {
                case '+' -> a + b;
                case '-' -> a - b;
                case '*' -> a * b;
                case '/' -> isInt(a / b) ? a / b : null;
                case '^' -> isInt(Math.pow(a, b)) ? Math.pow(a, b) : null;
                default -> null;
            };
            if (folded != null && Double.isFinite(folded)) return num(folded);
        }
        return switch (op) {
            case '+' -> addition(l, r);
            case '-' -> subtraction(l, r);
            case '*' -> multiplication(l, r);
            case '/' -> division(l, r);
            case '^' -> power(l, r);
            default -> new Bin(op, l, r);
        };
    }

    private static Node addition(Node l, Node r) {
        if (isNum(l, 0)) return r;
        if (isNum(r, 0)) return l;
        if (r instanceof Neg(Node x)) return new Bin('-', l, x);
        if (r instanceof Num(double b) && b < 0) return new Bin('-', l, num(-b));
        if (l instanceof Neg(Node x)) return new Bin('-', r, x);
        if (l.equals(r)) return new Bin('*', num(2), l);
        if (l instanceof Num && !(r instanceof Num)) return new Bin('+', r, l); // constantes no final: x + 1
        return new Bin('+', l, r);
    }

    private static Node subtraction(Node l, Node r) {
        if (isNum(r, 0)) return l;
        if (isNum(l, 0)) return new Neg(r);
        if (l.equals(r)) return ZERO;
        if (r instanceof Neg(Node x)) return new Bin('+', l, x);
        if (r instanceof Num(double b) && b < 0) return new Bin('+', l, num(-b));
        return new Bin('-', l, r);
    }

    private static Node multiplication(Node l, Node r) {
        if (isNum(l, 0) || isNum(r, 0)) return ZERO;
        if (isNum(l, 1)) return r;
        if (isNum(r, 1)) return l;
        if (isNum(l, -1)) return new Neg(r);
        if (isNum(r, -1)) return new Neg(l);
        if (r instanceof Num && !(l instanceof Num)) return new Bin('*', r, l); // números na frente: 3x
        if (l instanceof Neg(Node a)) return new Neg(new Bin('*', a, r));
        if (r instanceof Neg(Node b)) return new Neg(new Bin('*', l, b));
        if (l instanceof Num(double a) && r instanceof Bin(char op, Node rl, Node rr)
                && op == '*' && rl instanceof Num(double b)) {
            return new Bin('*', num(a * b), rr);
        }
        if (l instanceof Bin(char op, Node ll, Node lr) && op == '*' && ll instanceof Num c && !(r instanceof Num)) {
            return new Bin('*', c, new Bin('*', lr, r)); // (2x)·y → 2(x·y)
        }
        if (r instanceof Bin(char op, Node rl, Node rr) && op == '/' && isNum(rl, 1)) return new Bin('/', l, rr);
        if (l instanceof Bin(char op, Node ll, Node lr) && op == '/' && isNum(ll, 1)) return new Bin('/', r, lr);
        if (l.equals(r)) return new Bin('^', l, num(2));
        if (r instanceof Bin(char op, Node base, Node e) && op == '^' && base.equals(l) && e instanceof Num(double k)) {
            return new Bin('^', l, num(k + 1));
        }
        return new Bin('*', l, r);
    }

    private static Node division(Node l, Node r) {
        if (isNum(l, 0)) return ZERO;
        if (isNum(r, 1)) return l;
        if (l.equals(r)) return ONE;
        if (l instanceof Neg(Node a)) return new Neg(new Bin('/', a, r));
        if (r instanceof Neg(Node b)) return new Neg(new Bin('/', l, b));
        if (l instanceof Bin(char op, Node a, Node b) && op == '/') return new Bin('/', a, new Bin('*', b, r));
        return new Bin('/', l, r);
    }

    private static Node power(Node l, Node r) {
        if (isNum(r, 0)) return ONE;
        if (isNum(r, 1)) return l;
        if (isNum(l, 1)) return ONE;
        if (isNum(l, 0) && r instanceof Num(double b) && b > 0) return ZERO;
        if (l instanceof Bin(char op, Node base, Node e) && op == '^' && e instanceof Num(double m)
                && r instanceof Num(double k)) {
            return new Bin('^', base, num(m * k));
        }
        return new Bin('^', l, r);
    }
}
