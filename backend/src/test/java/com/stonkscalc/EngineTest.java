package com.stonkscalc;

import com.stonkscalc.api.Json;
import com.stonkscalc.engine.AngleMode;
import com.stonkscalc.engine.CalcException;
import com.stonkscalc.engine.Calculus;
import com.stonkscalc.engine.Derivative;
import com.stonkscalc.engine.Evaluator;
import com.stonkscalc.engine.Formatter;
import com.stonkscalc.engine.Parser;

import java.util.List;
import java.util.Map;

/** Testes do motor sem dependências (roda com: java -cp ... com.stonkscalc.EngineTest). */
public final class EngineTest {

    private static int passed;
    private static int failed;

    public static void main(String[] args) {
        arithmetic();
        functions();
        trigDegrees();
        calculus();
        symbolic();
        formatting();
        errors();
        json();
        System.out.printf("%n%d passaram, %d falharam%n", passed, failed);
        if (failed > 0) {
            System.out.println("NOT STONKS 📉");
            System.exit(1);
        }
        System.out.println("STONKS 📈");
    }

    private static void arithmetic() {
        eq("1 + 2 * 3", 7);
        eq("(1 + 2) * 3", 9);
        eq("2^3^2", 512);
        eq("-2^2", -4);
        eq("2^-1", 0.5);
        eq("10 / 4", 2.5);
        eq("0.1 + 0.2", 0.3);
        eq("2pi", 2 * Math.PI);
        eq("3(4 + 1)", 15);
        eq("(2)(3)", 6);
        eq("5!", 120);
        eq("3!!", 720);
        eq("50%", 0.5);
        eq("200 * 10%", 20);
        eq("1.5E3", 1500);
        eq("2e", 2 * Math.E);
        eq("√16", 4);
        eq("2 × 3 ÷ 4 − 1", 0.5);
        eq("(1 + 2", 3);
        eq("(-8)^(1/3)", -2);
        eq("ans * 2", 84, Map.of("ans", 42.0));
    }

    private static void functions() {
        eq("sqrt(2)^2", 2);
        eq("cbrt(27)", 3);
        eq("root(32, 5)", 2);
        eq("ln(e)", 1);
        eq("log(1000)", 3);
        eq("log(8, 2)", 3);
        eq("log2(1024)", 10);
        eq("exp(0)", 1);
        eq("abs(-3)", 3);
        eq("floor(-2.5)", -3);
        eq("ceil(2.1)", 3);
        eq("round(2.567, 2)", 2.57);
        eq("mod(-7, 3)", 2);
        eq("gcd(12, 18, 24)", 6);
        eq("lcm(4, 6)", 12);
        eq("nCr(5, 2)", 10);
        eq("nPr(5, 2)", 20);
        eq("gamma(5)", 24);
        eq("gamma(0.5)", Math.sqrt(Math.PI));
        eq("fact(0.5)", Math.sqrt(Math.PI) / 2);
        eq("mean(1, 2, 3, 4)", 2.5);
        eq("median(5, 1, 3)", 3);
        eq("std(2, 4, 4, 4, 5, 5, 7, 9)", 2.1380899352993950);
        eq("stdp(2, 4, 4, 4, 5, 5, 7, 9)", 2);
        eq("max(1, 9, 3) - min(4, -2)", 11);
        eq("hypot(3, 4)", 5);
        eq("sinh(0) + cosh(0)", 1);
        eq("atanh(tanh(0.5))", 0.5);
        eq("sen(pi/2)", 1);
        eq("sin(pi)", 0);
    }

    private static void trigDegrees() {
        eqDeg("sin(30)", 0.5);
        eqDeg("cos(60)", 0.5);
        eqDeg("sin(180)", 0);
        eqDeg("tan(45)", 1);
        eqDeg("asin(1)", 90);
        eqDeg("atan2(1, 1)", 45);
        isNaN("tan(90)", AngleMode.DEG);
    }

    private static void calculus() {
        eq("integral(x^2, x, 0, 3)", 9);
        eq("integral(sin(x), x, 0, pi)", 2);
        eq("integral(e^(-x^2), x, -inf, inf)", Math.sqrt(Math.PI));
        eq("integral(1/x^2, x, 1, inf)", 1);
        eq("integral(1/sqrt(x), x, 0, 1)", 2, 1e-6);
        eq("deriv(x^3, x, 2)", 12);
        eq("deriv(sin(x), x, 0)", 1);
        eq("deriv(x^4, x, 1, 2)", 12);
        eq("limit(sin(x)/x, x, 0)", 1);
        eq("limit((1 + 1/x)^x, x, inf)", Math.E, 1e-6);
        eq("limit((x^2 - 1)/(x - 1), x, 1)", 2);
        eq("limit(1/x^2, x, 0)", Double.POSITIVE_INFINITY);
        eq("limit(abs(x)/x, x, 0, 1)", 1);
        throwsErr("limit(abs(x)/x, x, 0)");
        eq("sum(k, k, 1, 100)", 5050);
        eq("sum(1/k^2, k, 1, 10000)", 1.6448340718480652);
        eq("prod(k, k, 1, 5)", 120);
        eq("solve(x^2 - 2, x, 1)", Math.sqrt(2));
        eq("solve(cos(x) - x, x)", 0.7390851332151607);
        eq("integral(integral(x*y, y, 0, 1), x, 0, 2)", 1);

        var f = new Evaluator(AngleMode.RAD).function(Parser.parse("x^2 - 4"), "x");
        List<Double> roots = Calculus.findRoots(f, -10, 10);
        check("raízes de x²-4", roots.equals(List.of(-2.0, 2.0)), roots.toString());
        var g = new Evaluator(AngleMode.RAD).function(Parser.parse("(x - 1)^2"), "x");
        check("raiz dupla de (x-1)²", Calculus.findRoots(g, -5, 5).size() == 1, Calculus.findRoots(g, -5, 5).toString());
        var t = new Evaluator(AngleMode.RAD).function(Parser.parse("tan(x)"), "x");
        check("tan não tem raiz nos polos", Calculus.findRoots(t, 1, 2).isEmpty(), Calculus.findRoots(t, 1, 2).toString());
    }

    private static void symbolic() {
        deriv("x^2", "2x");
        deriv("3x^2 + 2x + 1", "6x + 2");
        deriv("sin(x)", "cos(x)");
        deriv("cos(x)", "-sin(x)");
        deriv("e^x", "e^x");
        deriv("ln(x)", "1/x");
        deriv("x*sin(x)", "sin(x) + x*cos(x)");
        deriv("sin(x)^2", "2sin(x)*cos(x)");
        deriv("sqrt(x)", "1/(2sqrt(x))");
        deriv("x^x", "x^x*(ln(x) + 1)");
        deriv("5", "0");
        deriv("sin(2x)", "2cos(2x)");
        // o resultado precisa ser re-analisável pelo parser
        String d = Formatter.expression(Derivative.of(Parser.parse("tan(x)/x"), "x"));
        double v = new Evaluator(AngleMode.RAD, Map.of("x", 1.0)).eval(Parser.parse(d));
        double expected = (1 / Math.pow(Math.cos(1), 2) * 1 - Math.tan(1)) / 1;
        check("derivada re-analisável: " + d, Math.abs(v - expected) < 1e-9, String.valueOf(v));
    }

    private static void formatting() {
        fmt(0.1 + 0.2, "0.3");
        fmt(1e20, "1E20");
        fmt(-1.5e-12, "-1.5E-12");
        fmt(123456.789, "123456.789");
        fmt(1.0 / 3, "0.333333333333");
        fmt(Double.POSITIVE_INFINITY, "∞");
    }

    private static void errors() {
        throwsErr("");
        throwsErr("2 +");
        throwsErr("foo + 1");
        throwsErr("sin()");
        throwsErr("2 $ 3");
        throwsErr("integral(x, 2, 0, 1)");
        isNaN("sqrt(-1)", AngleMode.RAD);
        isNaN("0/0", AngleMode.RAD);
        eq("1/0", Double.POSITIVE_INFINITY);
    }

    private static void json() {
        Object o = Json.parse("{\"a\": [1, 2.5, \"x\\n\"], \"b\": true, \"c\": null}");
        check("json parse", o instanceof Map<?, ?> m && m.get("b").equals(true)
                && ((List<?>) m.get("a")).get(1).equals(2.5), String.valueOf(o));
        String s = Json.stringify(Map.of("v", new double[]{1, Double.NaN, 0.5}));
        check("json stringify", s.equals("{\"v\":[1,null,0.5]}"), s);
    }

    // ------------------------------------------------------------------ mini framework

    private static void eq(String expr, double expected) {
        eq(expr, expected, 1e-9);
    }

    private static void eq(String expr, double expected, Map<String, Double> vars) {
        run(expr, expected, 1e-9, AngleMode.RAD, vars);
    }

    private static void eq(String expr, double expected, double tol) {
        run(expr, expected, tol, AngleMode.RAD, Map.of());
    }

    private static void eqDeg(String expr, double expected) {
        run(expr, expected, 1e-9, AngleMode.DEG, Map.of());
    }

    private static void run(String expr, double expected, double tol, AngleMode mode, Map<String, Double> vars) {
        try {
            double v = new Evaluator(mode, vars).eval(Parser.parse(expr));
            boolean ok = expected == v || Math.abs(v - expected) <= tol * Math.max(1, Math.abs(expected));
            check(expr + " = " + expected, ok, String.valueOf(v));
        } catch (RuntimeException e) {
            check(expr + " = " + expected, false, e.toString());
        }
    }

    private static void isNaN(String expr, AngleMode mode) {
        double v = new Evaluator(mode).eval(Parser.parse(expr));
        check(expr + " é NaN", Double.isNaN(v), String.valueOf(v));
    }

    private static void deriv(String expr, String expected) {
        try {
            String d = Formatter.expression(Derivative.of(Parser.parse(expr), "x"));
            check("d/dx " + expr + " = " + expected, d.equals(expected), d);
        } catch (RuntimeException e) {
            check("d/dx " + expr, false, e.toString());
        }
    }

    private static void fmt(double v, String expected) {
        String s = Formatter.number(v);
        check("formata " + v, s.equals(expected), s);
    }

    private static void throwsErr(String expr) {
        try {
            new Evaluator(AngleMode.RAD).eval(Parser.parse(expr));
            check("'" + expr + "' deveria dar erro", false, "não deu erro");
        } catch (CalcException e) {
            check("'" + expr + "' dá erro", true, e.getMessage());
        }
    }

    private static void check(String name, boolean ok, String actual) {
        if (ok) {
            passed++;
        } else {
            failed++;
            System.out.println("FALHOU: " + name + "  →  obtido: " + actual);
        }
    }
}
