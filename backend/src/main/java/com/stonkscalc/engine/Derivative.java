package com.stonkscalc.engine;

import com.stonkscalc.engine.Node.Bin;
import com.stonkscalc.engine.Node.Call;
import com.stonkscalc.engine.Node.Neg;
import com.stonkscalc.engine.Node.Num;
import com.stonkscalc.engine.Node.Var;

import java.util.List;

import static com.stonkscalc.engine.Node.ONE;
import static com.stonkscalc.engine.Node.ZERO;
import static com.stonkscalc.engine.Node.add;
import static com.stonkscalc.engine.Node.call;
import static com.stonkscalc.engine.Node.contains;
import static com.stonkscalc.engine.Node.div;
import static com.stonkscalc.engine.Node.mul;
import static com.stonkscalc.engine.Node.neg;
import static com.stonkscalc.engine.Node.num;
import static com.stonkscalc.engine.Node.pow;
import static com.stonkscalc.engine.Node.sub;

/** Derivação simbólica (regras do produto, quociente, potência e cadeia). Assume ângulos em radianos. */
public final class Derivative {

    private Derivative() {
    }

    public static Node of(Node n, String var) {
        return Simplifier.simplify(d(Simplifier.simplify(n), var));
    }

    static Node d(Node n, String v) {
        if (!contains(n, v)) return ZERO;
        return switch (n) {
            case Num x -> ZERO;
            case Var x -> ONE;
            case Neg(Node u) -> neg(d(u, v));
            case Bin(char op, Node u, Node w) -> binary(n, op, u, w, v);
            case Call c -> function(c, v);
        };
    }

    private static Node binary(Node n, char op, Node u, Node w, String v) {
        return switch (op) {
            case '+' -> add(d(u, v), d(w, v));
            case '-' -> sub(d(u, v), d(w, v));
            case '*' -> add(mul(d(u, v), w), mul(u, d(w, v)));
            case '/' -> {
                if (!contains(w, v)) yield div(d(u, v), w);
                if (!contains(u, v)) yield neg(div(mul(u, d(w, v)), pow(w, num(2))));
                yield div(sub(mul(d(u, v), w), mul(u, d(w, v))), pow(w, num(2)));
            }
            case '^' -> {
                boolean uv = contains(u, v);
                boolean wv = contains(w, v);
                if (!wv) yield mul(mul(w, pow(u, sub(w, ONE))), d(u, v));
                if (!uv) yield mul(mul(n, call("ln", u)), d(w, v));
                yield mul(n, add(mul(d(w, v), call("ln", u)), div(mul(w, d(u, v)), u)));
            }
            default -> throw new IllegalStateException("Operador " + op);
        };
    }

    private static Node function(Call c, String v) {
        String f = c.name();
        List<Node> args = c.args();
        if (f.equals("pow") && args.size() == 2) return d(pow(args.get(0), args.get(1)), v);
        if (f.equals("root") && args.size() == 2) return d(pow(args.get(0), div(ONE, args.get(1))), v);
        if (f.equals("log") && args.size() == 2) return d(div(call("ln", args.get(0)), call("ln", args.get(1))), v);
        if (args.size() != 1) throw unsupported(f);

        Node u = args.get(0);
        Node du = d(u, v);
        Node outer = switch (f) {
            case "sin" -> call("cos", u);
            case "cos" -> neg(call("sin", u));
            case "tan" -> pow(call("sec", u), num(2));
            case "sec" -> mul(call("sec", u), call("tan", u));
            case "csc" -> neg(mul(call("csc", u), call("cot", u)));
            case "cot" -> neg(pow(call("csc", u), num(2)));
            case "asin" -> div(ONE, call("sqrt", sub(ONE, pow(u, num(2)))));
            case "acos" -> neg(div(ONE, call("sqrt", sub(ONE, pow(u, num(2))))));
            case "atan" -> div(ONE, add(ONE, pow(u, num(2))));
            case "acot" -> neg(div(ONE, add(ONE, pow(u, num(2)))));
            case "asec" -> div(ONE, mul(call("abs", u), call("sqrt", sub(pow(u, num(2)), ONE))));
            case "acsc" -> neg(div(ONE, mul(call("abs", u), call("sqrt", sub(pow(u, num(2)), ONE)))));
            case "sinh" -> call("cosh", u);
            case "cosh" -> call("sinh", u);
            case "tanh" -> pow(call("sech", u), num(2));
            case "sech" -> neg(mul(call("sech", u), call("tanh", u)));
            case "csch" -> neg(mul(call("csch", u), call("coth", u)));
            case "coth" -> neg(pow(call("csch", u), num(2)));
            case "asinh" -> div(ONE, call("sqrt", add(pow(u, num(2)), ONE)));
            case "acosh" -> div(ONE, call("sqrt", sub(pow(u, num(2)), ONE)));
            case "atanh" -> div(ONE, sub(ONE, pow(u, num(2))));
            case "ln" -> div(ONE, u);
            case "log" -> div(ONE, mul(u, call("ln", num(10))));
            case "log2" -> div(ONE, mul(u, call("ln", num(2))));
            case "exp" -> call("exp", u);
            case "sqrt" -> div(ONE, mul(num(2), call("sqrt", u)));
            case "cbrt" -> div(ONE, mul(num(3), pow(call("cbrt", u), num(2))));
            case "abs" -> call("sign", u);
            default -> throw unsupported(f);
        };
        return mul(outer, du);
    }

    private static CalcException unsupported(String f) {
        return new CalcException("Não sei derivar '" + f + "' simbolicamente. Nem o Leibniz saberia (ok, talvez ele soubesse).");
    }
}
