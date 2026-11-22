package com.stonkscalc.engine;

import java.util.Arrays;
import java.util.concurrent.ThreadLocalRandom;

/**
 * Implementação das funções "comuns" (não preguiçosas). Erros de domínio retornam NaN,
 * para que o gráfico simplesmente não desenhe aquele trecho.
 */
public final class MathFunctions {

    private static final double[] LANCZOS = {
            0.99999999999980993, 676.5203681218851, -1259.1392167224028, 771.32342877765313,
            -176.61502916214059, 12.507343278686905, -0.13857109526572012, 9.9843695780195716e-6,
            1.5056327351493116e-7
    };

    private MathFunctions() {
    }

    public static double apply(String f, double[] v, AngleMode m) {
        double x = v.length > 0 ? v[0] : Double.NaN;
        double y = v.length > 1 ? v[1] : Double.NaN;
        return switch (f) {
            // Trigonometria
            case "sin" -> sin(x, m);
            case "cos" -> cos(x, m);
            case "tan" -> div(sin(x, m), cos(x, m));
            case "sec" -> div(1, cos(x, m));
            case "csc" -> div(1, sin(x, m));
            case "cot" -> div(cos(x, m), sin(x, m));
            case "asin" -> m.fromRadians(Math.asin(x));
            case "acos" -> m.fromRadians(Math.acos(x));
            case "atan" -> m.fromRadians(Math.atan(x));
            case "acot" -> m.fromRadians(x == 0 ? Math.PI / 2 : Math.atan(1 / x));
            case "asec" -> m.fromRadians(Math.acos(1 / x));
            case "acsc" -> m.fromRadians(Math.asin(1 / x));
            case "atan2" -> m.fromRadians(Math.atan2(x, y));
            // Hiperbólicas
            case "sinh" -> Math.sinh(x);
            case "cosh" -> Math.cosh(x);
            case "tanh" -> Math.tanh(x);
            case "sech" -> 1 / Math.cosh(x);
            case "csch" -> div(1, Math.sinh(x));
            case "coth" -> div(Math.cosh(x), Math.sinh(x));
            case "asinh" -> Math.log(x + Math.sqrt(x * x + 1));
            case "acosh" -> x < 1 ? Double.NaN : Math.log(x + Math.sqrt(x * x - 1));
            case "atanh" -> Math.abs(x) > 1 ? Double.NaN : 0.5 * Math.log((1 + x) / (1 - x));
            // Potências e logaritmos
            case "sqrt" -> Math.sqrt(x);
            case "cbrt" -> Math.cbrt(x);
            case "root" -> root(x, y);
            case "pow" -> pow(x, y);
            case "exp" -> Math.exp(x);
            case "ln" -> Math.log(x);
            case "log" -> v.length == 2 ? Math.log(x) / Math.log(y) : Math.log10(x);
            case "log2" -> Math.log(x) / Math.log(2);
            // Aritmética
            case "abs" -> Math.abs(x);
            case "floor" -> Math.floor(x);
            case "ceil" -> Math.ceil(x);
            case "round" -> v.length == 2 ? roundTo(x, (int) y) : Math.round(x);
            case "trunc" -> x < 0 ? Math.ceil(x) : Math.floor(x);
            case "frac" -> x - (x < 0 ? Math.ceil(x) : Math.floor(x));
            case "sign" -> Math.signum(x);
            case "mod" -> y == 0 ? Double.NaN : x - y * Math.floor(x / y);
            case "gcd" -> gcdAll(v);
            case "lcm" -> lcmAll(v);
            case "min" -> Arrays.stream(v).min().orElse(Double.NaN);
            case "max" -> Arrays.stream(v).max().orElse(Double.NaN);
            case "hypot" -> Math.sqrt(Arrays.stream(v).map(a -> a * a).sum());
            case "deg" -> Math.toDegrees(x);
            case "rad" -> Math.toRadians(x);
            // Combinatória e especiais
            case "fact" -> factorial(x);
            case "gamma" -> gamma(x);
            case "ncr" -> ncr(x, y);
            case "npr" -> npr(x, y);
            case "rand" -> Math.random();
            case "randint" -> randint(x, y);
            // Estatística
            case "mean" -> Arrays.stream(v).average().orElse(Double.NaN);
            case "median" -> median(v);
            case "variance" -> variance(v, true);
            case "std" -> Math.sqrt(variance(v, true));
            case "stdp" -> Math.sqrt(variance(v, false));
            case "total" -> Arrays.stream(v).sum();
            default -> throw new CalcException("Função desconhecida: " + f);
        };
    }

    /** Corrige resíduos de ponto flutuante: sin(π) dá 1.2e-16, mas todo mundo sabe que é 0. */
    private static double snap(double v) {
        return Math.abs(v) < 1e-15 ? 0 : v;
    }

    private static double sin(double x, AngleMode m) {
        if (m == AngleMode.DEG && x % 180 == 0) return 0;
        return snap(Math.sin(m.toRadians(x)));
    }

    private static double cos(double x, AngleMode m) {
        if (m == AngleMode.DEG && (x - 90) % 180 == 0) return 0;
        return snap(Math.cos(m.toRadians(x)));
    }

    /** Divisão onde o denominador zero significa "indefinido" (ex.: tan(90°)). */
    private static double div(double a, double b) {
        return b == 0 ? Double.NaN : a / b;
    }

    /** x^y com suporte a raízes ímpares de negativos: (-8)^(1/3) = -2. */
    public static double pow(double base, double e) {
        if (base < 0 && e != Math.rint(e)) {
            double inv = 1 / e;
            double r = Math.rint(inv);
            if (Math.abs(inv - r) < 1e-9 && Math.abs(r % 2) == 1) {
                return -Math.pow(-base, e);
            }
        }
        return Math.pow(base, e);
    }

    private static double root(double x, double n) {
        if (n == 0) return Double.NaN;
        if (x < 0 && n == Math.rint(n) && Math.abs(n % 2) == 1) return -Math.pow(-x, 1 / n);
        return Math.pow(x, 1 / n);
    }

    private static double roundTo(double x, int digits) {
        double f = Math.pow(10, digits);
        return Math.round(x * f) / f;
    }

    private static boolean isInt(double x) {
        return Double.isFinite(x) && x == Math.rint(x);
    }

    private static double gcd(double a, double b) {
        a = Math.abs(a);
        b = Math.abs(b);
        while (b != 0) {
            double t = a % b;
            a = b;
            b = t;
        }
        return a;
    }

    private static double gcdAll(double[] v) {
        double g = 0;
        for (double a : v) {
            if (!isInt(a)) return Double.NaN;
            g = gcd(g, a);
        }
        return g;
    }

    private static double lcmAll(double[] v) {
        double l = 1;
        for (double a : v) {
            if (!isInt(a)) return Double.NaN;
            if (a == 0) return 0;
            l = Math.abs(l * a) / gcd(l, a);
        }
        return l;
    }

    public static double factorial(double n) {
        if (isInt(n)) {
            if (n < 0) return Double.NaN;
            if (n > 170) return Double.POSITIVE_INFINITY;
            double r = 1;
            for (int i = 2; i <= n; i++) r *= i;
            return r;
        }
        return gamma(n + 1);
    }

    public static double gamma(double x) {
        if (isInt(x)) {
            if (x <= 0) return Double.NaN;
            return factorial(x - 1);
        }
        if (x < 0.5) {
            return Math.PI / (Math.sin(Math.PI * x) * gamma(1 - x));
        }
        x -= 1;
        double a = LANCZOS[0];
        double t = x + 7.5;
        for (int i = 1; i < LANCZOS.length; i++) a += LANCZOS[i] / (x + i);
        return Math.sqrt(2 * Math.PI) * Math.pow(t, x + 0.5) * Math.exp(-t) * a;
    }

    private static double ncr(double n, double k) {
        if (isInt(n) && isInt(k)) {
            if (k < 0 || n < 0 || k > n) return 0;
            k = Math.min(k, n - k);
            double r = 1;
            for (int i = 1; i <= k; i++) r = r * (n - k + i) / i;
            return Math.rint(r);
        }
        return gamma(n + 1) / (gamma(k + 1) * gamma(n - k + 1));
    }

    private static double npr(double n, double k) {
        if (isInt(n) && isInt(k)) {
            if (k < 0 || n < 0 || k > n) return 0;
            double r = 1;
            for (int i = 0; i < k; i++) r *= n - i;
            return r;
        }
        return gamma(n + 1) / gamma(n - k + 1);
    }

    private static double randint(double a, double b) {
        long lo = (long) Math.ceil(Math.min(a, b));
        long hi = (long) Math.floor(Math.max(a, b));
        if (lo > hi) return Double.NaN;
        return ThreadLocalRandom.current().nextLong(lo, hi + 1);
    }

    private static double median(double[] v) {
        double[] s = v.clone();
        Arrays.sort(s);
        int n = s.length;
        return n % 2 == 1 ? s[n / 2] : (s[n / 2 - 1] + s[n / 2]) / 2;
    }

    private static double variance(double[] v, boolean sample) {
        int n = v.length;
        if (sample && n < 2) return Double.NaN;
        double mean = Arrays.stream(v).average().orElse(Double.NaN);
        double ss = Arrays.stream(v).map(a -> (a - mean) * (a - mean)).sum();
        return ss / (sample ? n - 1 : n);
    }
}
