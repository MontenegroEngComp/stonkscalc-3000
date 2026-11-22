package com.stonkscalc.engine;

import java.util.ArrayList;
import java.util.List;
import java.util.function.DoubleUnaryOperator;

/** Métodos numéricos: integrais, derivadas, limites e raízes. */
public final class Calculus {

    private Calculus() {
    }

    // ------------------------------------------------------------------ Integral (Gauss–Kronrod 7-15 adaptativo)

    private static final double[] XGK = {
            0.991455371120812639206854697526329, 0.949107912342758524526189684047851,
            0.864864423359769072789712788640926, 0.741531185599394439863864773280788,
            0.586087235467691130294144845693013, 0.405845151377397166906606412076961,
            0.207784955007898467600689403773245, 0.0
    };
    private static final double[] WGK = {
            0.022935322010529224963732008058970, 0.063092092629978553290700663189204,
            0.104790010322250183839876322541518, 0.140653259715525918745189590510238,
            0.169004726639267902826583426598550, 0.190350578064785409913256402421014,
            0.204432940075298892414161999234649, 0.209482141084727828012999174891714
    };
    /** Pesos de Gauss (7 pontos) para os nós XGK[1], XGK[3], XGK[5], XGK[7]. */
    private static final double[] WG = {
            0.129484966168869693270611432679082, 0.279705391489276667901467771423780,
            0.381830050505118944950369775488975, 0.417959183673469387755102040816327
    };
    private static final int MAX_EVALS = 400_000;

    public static double integrate(DoubleUnaryOperator f, double a, double b) {
        if (Double.isNaN(a) || Double.isNaN(b)) throw new CalcException("Limites de integração inválidos.");
        if (a == b) return 0;
        if (a > b) return -integrate(f, b, a);

        DoubleUnaryOperator g;
        double lo;
        double hi;
        if (Double.isInfinite(a) && Double.isInfinite(b)) {
            // x = t / (1 - t²), t ∈ (-1, 1)
            g = t -> {
                double d = 1 - t * t;
                return f.applyAsDouble(t / d) * (1 + t * t) / (d * d);
            };
            lo = -1;
            hi = 1;
        } else if (Double.isInfinite(b)) {
            // x = a + t / (1 - t), t ∈ [0, 1)
            g = t -> f.applyAsDouble(a + t / (1 - t)) / ((1 - t) * (1 - t));
            lo = 0;
            hi = 1;
        } else if (Double.isInfinite(a)) {
            // x = b - (1 - t) / t, t ∈ (0, 1]
            g = t -> f.applyAsDouble(b - (1 - t) / t) / (t * t);
            lo = 0;
            hi = 1;
        } else {
            g = f;
            lo = a;
            hi = b;
        }
        int[] evals = {0};
        double result = adaptive(g, lo, hi, 1e-11, 0, evals);
        if (!Double.isFinite(result)) {
            throw new CalcException("A integral diverge (ou passa por uma singularidade). Nem o Newton salva essa.");
        }
        return result;
    }

    private static double adaptive(DoubleUnaryOperator f, double a, double b, double tol, int depth, int[] evals) {
        double c = (a + b) / 2;
        double h = (b - a) / 2;
        double fc = f.applyAsDouble(c);
        double kronrod = fc * WGK[7];
        double gauss = fc * WG[3];
        for (int j = 0; j < 7; j++) {
            double dx = h * XGK[j];
            double sum = f.applyAsDouble(c - dx) + f.applyAsDouble(c + dx);
            kronrod += WGK[j] * sum;
            if (j % 2 == 1) gauss += WG[j / 2] * sum;
        }
        evals[0] += 15;
        kronrod *= h;
        gauss *= h;
        double err = Math.abs(kronrod - gauss);
        if (!Double.isFinite(kronrod)) return kronrod;
        if (err <= Math.max(tol, 1e-14 * Math.abs(kronrod)) || depth >= 50 || evals[0] > MAX_EVALS) {
            return kronrod;
        }
        return adaptive(f, a, c, tol / 2, depth + 1, evals) + adaptive(f, c, b, tol / 2, depth + 1, evals);
    }

    // ------------------------------------------------------------------ Derivada numérica

    public static double derivative(DoubleUnaryOperator f, double x, int order) {
        if (order == 1) {
            double h = 1e-3 * Math.max(1, Math.abs(x));
            return (-f.applyAsDouble(x + 2 * h) + 8 * f.applyAsDouble(x + h)
                    - 8 * f.applyAsDouble(x - h) + f.applyAsDouble(x - 2 * h)) / (12 * h);
        }
        if (order == 2) {
            double h = 1e-2 * Math.max(1, Math.abs(x));
            return (-f.applyAsDouble(x + 2 * h) + 16 * f.applyAsDouble(x + h) - 30 * f.applyAsDouble(x)
                    + 16 * f.applyAsDouble(x - h) - f.applyAsDouble(x - 2 * h)) / (12 * h * h);
        }
        throw new CalcException("Derivada numérica só até a 2ª ordem. Use o modo RAD para ordens maiores (derivada simbólica).");
    }

    // ------------------------------------------------------------------ Limite (extrapolação de Richardson)

    /** side: -1 = pela esquerda, 1 = pela direita, 0 = bilateral. */
    public static double limit(DoubleUnaryOperator f, double a, int side) {
        if (Double.isNaN(a)) throw new CalcException("Ponto do limite inválido.");
        if (Double.isInfinite(a)) {
            double s = Math.signum(a);
            return oneSided(t -> f.applyAsDouble(s / t), 0.1);
        }
        double h0 = 1e-2 * Math.max(1, Math.abs(a));
        if (side > 0) return oneSided(t -> f.applyAsDouble(a + t), h0);
        if (side < 0) return oneSided(t -> f.applyAsDouble(a - t), h0);

        double left = oneSided(t -> f.applyAsDouble(a - t), h0);
        double right = oneSided(t -> f.applyAsDouble(a + t), h0);
        if (Double.isInfinite(left) && left == right) return left;
        double scale = Math.max(1, Math.max(Math.abs(left), Math.abs(right)));
        if (Double.isFinite(left) && Double.isFinite(right) && Math.abs(left - right) <= 1e-5 * scale) {
            return clean((left + right) / 2);
        }
        throw new CalcException("O limite não existe: pela esquerda ≈ " + Formatter.number(left)
                + ", pela direita ≈ " + Formatter.number(right) + ". Os dois lados não chegaram num acordo.");
    }

    private static double oneSided(DoubleUnaryOperator g, double h0) {
        final int n = 18;
        double[] y = new double[n];
        int count = 0;
        for (int k = 0; k < n; k++) {
            double v = g.applyAsDouble(h0 / Math.pow(2, k));
            if (Double.isNaN(v)) break;
            y[count++] = v;
            if (Double.isInfinite(v)) break;
        }
        if (count == 0) throw new CalcException("A função não está definida perto desse ponto.");
        double last = y[count - 1];
        if (Double.isInfinite(last)) return last;

        // Divergência para ±∞: valores crescendo rápido e de forma monótona
        if (count >= 6 && Math.abs(last) > 1e5 && monotonicGrowth(y, count, 6)
                && Math.abs(last / y[count - 6]) > 4) {
            return Math.signum(last) * Double.POSITIVE_INFINITY;
        }

        int maxCol = 6;
        double[][] r = new double[count][maxCol + 1];
        double best = last;
        double bestErr = Double.POSITIVE_INFINITY;
        double prevDiag = Double.NaN;
        for (int k = 0; k < count; k++) {
            r[k][0] = y[k];
            int cols = Math.min(k, maxCol);
            for (int j = 1; j <= cols; j++) {
                double p = Math.pow(2, j);
                r[k][j] = r[k][j - 1] + (r[k][j - 1] - r[k - 1][j - 1]) / (p - 1);
            }
            double diag = r[k][cols];
            if (k > 0 && Double.isFinite(diag)) {
                double err = Math.abs(diag - prevDiag);
                if (err < bestErr) {
                    bestErr = err;
                    best = diag;
                }
            }
            prevDiag = diag;
        }
        if (bestErr > 1e-6 * Math.max(1, Math.abs(best))) {
            if (count >= 6 && monotonicGrowth(y, count, 8)) {
                return Math.signum(last) * Double.POSITIVE_INFINITY;
            }
            throw new CalcException("O limite não converge (a função fica oscilando). Está mais indecisa que eu no cardápio.");
        }
        return clean(best);
    }

    private static boolean monotonicGrowth(double[] y, int count, int window) {
        int start = Math.max(1, count - window);
        for (int i = start; i < count; i++) {
            if (Math.abs(y[i]) <= Math.abs(y[i - 1]) || Math.signum(y[i]) != Math.signum(y[i - 1])) return false;
        }
        return true;
    }

    /** Arredonda para 10 algarismos significativos e zera resíduos. */
    private static double clean(double v) {
        if (!Double.isFinite(v)) return v;
        if (Math.abs(v) < 1e-10) return 0;
        return new java.math.BigDecimal(v).round(new java.math.MathContext(10)).doubleValue();
    }

    // ------------------------------------------------------------------ Raízes

    public static double solve(DoubleUnaryOperator f, double guess) {
        double x = guess;
        for (int i = 0; i < 100; i++) {
            double fx = f.applyAsDouble(x);
            if (!Double.isFinite(fx)) break;
            if (fx == 0) return polish(f, x);
            double d = derivative(f, x, 1);
            if (d == 0 || !Double.isFinite(d)) break;
            double nx = x - fx / d;
            if (!Double.isFinite(nx)) break;
            if (Math.abs(nx - x) < 1e-13 * Math.max(1, Math.abs(x))) {
                if (Math.abs(f.applyAsDouble(nx)) < 1e-9) return polish(f, nx);
                break;
            }
            x = nx;
        }
        double[] spans = {10, 100, 1000};
        for (double s : spans) {
            List<Double> roots = findRoots(f, guess - s, guess + s);
            if (!roots.isEmpty()) {
                double bestRoot = roots.get(0);
                for (double r : roots) if (Math.abs(r - guess) < Math.abs(bestRoot - guess)) bestRoot = r;
                return bestRoot;
            }
        }
        throw new CalcException("Não achei nenhuma raiz perto de " + Formatter.number(guess)
                + ". Tenta outro chute (3º argumento).");
    }

    public static List<Double> findRoots(DoubleUnaryOperator f, double a, double b) {
        if (!(b > a) || !Double.isFinite(a) || !Double.isFinite(b)) {
            throw new CalcException("Intervalo inválido para procurar raízes.");
        }
        int n = 2000;
        double dx = (b - a) / n;
        double[] xs = new double[n + 1];
        double[] ys = new double[n + 1];
        for (int i = 0; i <= n; i++) {
            xs[i] = a + i * dx;
            ys[i] = f.applyAsDouble(xs[i]);
        }
        List<Double> roots = new ArrayList<>();
        for (int i = 0; i <= n && roots.size() < 200; i++) {
            double y0 = ys[i];
            if (y0 == 0) {
                roots.add(xs[i]);
                continue;
            }
            if (i < n) {
                double y1 = ys[i + 1];
                if (Double.isFinite(y0) && Double.isFinite(y1) && y1 != 0 && Math.signum(y0) != Math.signum(y1)) {
                    double r = bisect(f, xs[i], xs[i + 1], y0);
                    double fr = Math.abs(f.applyAsDouble(r));
                    if (fr <= 1e-8 * Math.max(1, Math.max(Math.abs(y0), Math.abs(y1)))) roots.add(r);
                    continue;
                }
            }
            // raízes que só "encostam" no eixo (ex.: x²)
            if (i > 0 && i < n && Double.isFinite(ys[i - 1]) && Double.isFinite(ys[i + 1])
                    && Math.abs(y0) <= Math.abs(ys[i - 1]) && Math.abs(y0) <= Math.abs(ys[i + 1])
                    && Math.signum(ys[i - 1]) == Math.signum(y0) && Math.signum(ys[i + 1]) == Math.signum(y0)) {
                double m = goldenMinAbs(f, xs[i - 1], xs[i + 1]);
                if (Math.abs(f.applyAsDouble(m)) < 1e-10) roots.add(m);
            }
        }
        List<Double> out = new ArrayList<>();
        roots.sort(Double::compare);
        for (double r : roots) {
            double p = polish(f, r);
            if (out.isEmpty() || Math.abs(out.get(out.size() - 1) - p) > Math.max(dx / 2, 1e-9)) out.add(p);
        }
        return out;
    }

    private static double bisect(DoubleUnaryOperator f, double lo, double hi, double flo) {
        for (int i = 0; i < 200; i++) {
            double mid = (lo + hi) / 2;
            if (mid == lo || mid == hi) break;
            double fm = f.applyAsDouble(mid);
            if (fm == 0) return mid;
            if (Math.signum(fm) == Math.signum(flo)) {
                lo = mid;
                flo = fm;
            } else {
                hi = mid;
            }
        }
        return (lo + hi) / 2;
    }

    private static double goldenMinAbs(DoubleUnaryOperator f, double a, double b) {
        double gr = (Math.sqrt(5) - 1) / 2;
        double c = b - gr * (b - a);
        double d = a + gr * (b - a);
        for (int i = 0; i < 200 && Math.abs(b - a) > 1e-14 * Math.max(1, Math.abs(a)); i++) {
            if (Math.abs(f.applyAsDouble(c)) < Math.abs(f.applyAsDouble(d))) b = d;
            else a = c;
            c = b - gr * (b - a);
            d = a + gr * (b - a);
        }
        return (a + b) / 2;
    }

    /** Troca 1.9999999999 por 2 quando isso não piora o resultado. */
    private static double polish(DoubleUnaryOperator f, double r) {
        if (Math.abs(r) < 1e-12) return 0;
        double rounded = new java.math.BigDecimal(r).round(new java.math.MathContext(10)).doubleValue();
        double fr = Math.abs(f.applyAsDouble(r));
        double fRounded = Math.abs(f.applyAsDouble(rounded));
        return fRounded <= Math.max(fr, 1e-12) ? rounded : r;
    }
}
