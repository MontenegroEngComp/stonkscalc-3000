package com.stonkscalc.engine;

import com.stonkscalc.engine.Node.Bin;
import com.stonkscalc.engine.Node.Call;
import com.stonkscalc.engine.Node.Neg;
import com.stonkscalc.engine.Node.Num;
import com.stonkscalc.engine.Node.Var;

import java.math.BigDecimal;
import java.math.MathContext;
import java.math.RoundingMode;
import java.util.List;
import java.util.stream.Collectors;

/** Converte números e ASTs de volta para texto (que o próprio parser consegue ler de novo). */
public final class Formatter {

    private Formatter() {
    }

    /** Número com até 12 algarismos significativos (adeus, 0.30000000000000004). */
    public static String number(double v) {
        if (Double.isNaN(v)) return "NaN";
        if (Double.isInfinite(v)) return v > 0 ? "∞" : "-∞";
        if (v == 0) return "0";
        BigDecimal bd = new BigDecimal(v).round(new MathContext(12, RoundingMode.HALF_EVEN)).stripTrailingZeros();
        double abs = Math.abs(bd.doubleValue());
        if (abs >= 1e15 || abs < 1e-9) {
            int exp = bd.precision() - bd.scale() - 1;
            BigDecimal mantissa = bd.movePointLeft(exp).stripTrailingZeros();
            return mantissa.toPlainString() + "E" + exp;
        }
        return bd.toPlainString();
    }

    public static String expression(Node n) {
        return switch (n) {
            case Num(double v) -> number(v).replace("∞", "inf");
            case Var(String name) -> name;
            case Neg(Node o) -> "-" + wrap(o, prec(o) < 2 || isNegative(o));
            case Call(String name, List<Node> args) ->
                    name + "(" + args.stream().map(Formatter::expression).collect(Collectors.joining(", ")) + ")";
            case Bin b -> binary(b);
        };
    }

    private static String binary(Bin b) {
        int p = prec(b);
        Node l = b.left();
        Node r = b.right();
        boolean wrapL = prec(l) < p || (b.op() == '^' && prec(l) <= p);
        boolean wrapR = prec(r) < p || isNegative(r)
                || (prec(r) == p && (b.op() == '-' || b.op() == '/'));
        String ls = wrap(l, wrapL);
        String rs = wrap(r, wrapR);
        return switch (b.op()) {
            case '+' -> ls + " + " + rs;
            case '-' -> ls + " - " + rs;
            case '/' -> ls + "/" + rs;
            case '^' -> ls + "^" + rs;
            case '*' -> implicit(l, r, wrapR) ? ls + rs : ls + "*" + rs;
            default -> throw new IllegalStateException();
        };
    }

    /** 2x, 3sin(x), 4x^2, 2(x + 1) */
    private static boolean implicit(Node l, Node r, boolean rightWrapped) {
        if (!(l instanceof Num)) return false;
        if (rightWrapped) return true;
        Node first = r;
        while (first instanceof Bin(char op, Node left, Node right) && (op == '*' || op == '^')) first = left;
        return first instanceof Var || first instanceof Call;
    }

    private static boolean isNegative(Node n) {
        return n instanceof Neg || (n instanceof Num(double v) && v < 0);
    }

    private static String wrap(Node n, boolean parens) {
        String s = expression(n);
        return parens ? "(" + s + ")" : s;
    }

    private static int prec(Node n) {
        return switch (n) {
            case Num(double v) -> v < 0 ? 3 : 5;
            case Var v -> 5;
            case Call c -> 5;
            case Neg x -> 3;
            case Bin(char op, Node l, Node r) -> switch (op) {
                case '+', '-' -> 1;
                case '*', '/' -> 2;
                default -> 4;
            };
        };
    }
}
