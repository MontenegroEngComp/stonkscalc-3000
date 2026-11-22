package com.stonkscalc.engine;

import com.stonkscalc.engine.Node.Bin;
import com.stonkscalc.engine.Node.Call;
import com.stonkscalc.engine.Node.Neg;
import com.stonkscalc.engine.Node.Num;
import com.stonkscalc.engine.Node.Var;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.DoubleUnaryOperator;

/** Avalia uma AST para um número (double). */
public final class Evaluator {

    private static final int MAX_LOOP = 10_000_000;

    private final AngleMode mode;
    private final Map<String, Double> vars;

    public Evaluator(AngleMode mode, Map<String, Double> vars) {
        this.mode = mode;
        this.vars = new HashMap<>(vars);
    }

    public Evaluator(AngleMode mode) {
        this(mode, Map.of());
    }

    public AngleMode mode() {
        return mode;
    }

    public double eval(Node n) {
        return switch (n) {
            case Num(double v) -> v;
            case Var(String name) -> lookup(name);
            case Neg(Node o) -> -eval(o);
            case Bin(char op, Node l, Node r) -> binary(op, eval(l), eval(r));
            case Call c -> call(c);
        };
    }

    /** Transforma a expressão numa função de uma variável, ex.: f(x). */
    public DoubleUnaryOperator function(Node body, String var) {
        Evaluator child = new Evaluator(mode, vars);
        return t -> {
            child.vars.put(var, t);
            return child.eval(body);
        };
    }

    private double lookup(String name) {
        Double v = vars.get(name);
        if (v != null) return v;
        return switch (name) {
            case "pi", "PI", "Pi" -> Math.PI;
            case "e" -> Math.E;
            case "tau" -> 2 * Math.PI;
            case "phi", "φ" -> (1 + Math.sqrt(5)) / 2;
            case "inf", "infinity", "Infinity" -> Double.POSITIVE_INFINITY;
            case "ans", "ANS", "Ans" -> throw new CalcException("Ainda não existe resposta anterior (ans). Calcula algo antes!");
            default -> throw new CalcException("Variável desconhecida: '" + name + "'. Quem é " + name + "? Não conheço.");
        };
    }

    private static double binary(char op, double a, double b) {
        return switch (op) {
            case '+' -> a + b;
            case '-' -> a - b;
            case '*' -> a * b;
            case '/' -> a / b;
            case '^' -> MathFunctions.pow(a, b);
            default -> throw new IllegalStateException("Operador " + op);
        };
    }

    private double call(Call c) {
        String f = c.name();
        List<Node> args = c.args();
        if (Functions.LAZY.contains(f)) return lazy(f, args);
        double[] v = new double[args.size()];
        for (int i = 0; i < v.length; i++) v[i] = eval(args.get(i));
        return MathFunctions.apply(f, v, mode);
    }

    private double lazy(String f, List<Node> a) {
        Node body = a.get(0);
        if (!(a.get(1) instanceof Var(String var))) {
            throw new CalcException("Em " + f + "(...), o 2º argumento tem que ser o nome da variável. Ex.: "
                    + Functions.info(f).signature());
        }
        DoubleUnaryOperator fn = function(body, var);
        switch (f) {
            case "integral":
                return Calculus.integrate(fn, eval(a.get(2)), eval(a.get(3)));
            case "deriv": {
                double at = eval(a.get(2));
                int order = a.size() > 3 ? (int) eval(a.get(3)) : 1;
                return derivativeAt(body, var, at, order, fn);
            }
            case "limit": {
                int side = a.size() > 3 ? (int) Math.signum(eval(a.get(3))) : 0;
                return Calculus.limit(fn, eval(a.get(2)), side);
            }
            case "sum":
            case "prod": {
                double lo = Math.ceil(eval(a.get(2)));
                double hi = Math.floor(eval(a.get(3)));
                if (Double.isInfinite(lo) || Double.isInfinite(hi)) {
                    throw new CalcException("Somatório infinito? Nem o Goku aguenta. Use limites finitos (ou um limite).");
                }
                if (hi - lo > MAX_LOOP) throw new CalcException("Intervalo grande demais (máx. 10 milhões de termos).");
                double acc = f.equals("sum") ? 0 : 1;
                for (double k = lo; k <= hi; k++) {
                    double t = fn.applyAsDouble(k);
                    acc = f.equals("sum") ? acc + t : acc * t;
                }
                return acc;
            }
            case "solve":
                return Calculus.solve(fn, a.size() > 2 ? eval(a.get(2)) : 0);
            default:
                throw new IllegalStateException(f);
        }
    }

    private double derivativeAt(Node body, String var, double at, int order, DoubleUnaryOperator fn) {
        if (order < 1) throw new CalcException("A ordem da derivada tem que ser 1 ou mais.");
        if (mode == AngleMode.RAD) {
            try {
                Node d = body;
                for (int i = 0; i < order; i++) d = Derivative.of(d, var);
                return function(d, var).applyAsDouble(at);
            } catch (CalcException unsupported) {
                // cai para a derivada numérica
            }
        }
        return Calculus.derivative(fn, at, order);
    }
}
