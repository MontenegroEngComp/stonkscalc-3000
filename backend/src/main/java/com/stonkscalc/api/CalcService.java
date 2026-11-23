package com.stonkscalc.api;

import com.stonkscalc.engine.AngleMode;
import com.stonkscalc.engine.CalcException;
import com.stonkscalc.engine.Calculus;
import com.stonkscalc.engine.Derivative;
import com.stonkscalc.engine.Evaluator;
import com.stonkscalc.engine.Formatter;
import com.stonkscalc.engine.Functions;
import com.stonkscalc.engine.Node;
import com.stonkscalc.engine.Parser;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.DoubleUnaryOperator;

/** Regras de cada endpoint: recebe o corpo JSON (Map) e devolve a resposta (Map). */
public final class CalcService {

    private static final int MAX_SAMPLES = 4000;
    private static final int MAX_FUNCTIONS = 10;

    private CalcService() {
    }

    /**
     * Roteia uma requisição para o endpoint certo. Usado tanto pelo servidor HTTP quanto
     * pela versão web (motor compilado para JavaScript). Retorna null se a rota não existir.
     */
    public static Map<String, Object> route(String path, Map<String, Object> body) {
        return switch (path) {
            case "/api/health" -> health();
            case "/api/functions" -> functions();
            case "/api/evaluate" -> evaluate(body);
            case "/api/derivative" -> derivative(body);
            case "/api/plot" -> plot(body);
            case "/api/roots" -> roots(body);
            default -> null;
        };
    }

    public static Map<String, Object> health() {
        return ok(Map.of("status", "stonks 📈"));
    }

    public static Map<String, Object> functions() {
        List<Object> list = new ArrayList<>();
        for (Functions.Info i : Functions.all()) {
            list.add(Map.of("name", i.name(), "signature", i.signature(),
                    "description", i.description(), "category", i.category()));
        }
        return ok(Map.of("functions", list));
    }

    /** {expression, angleMode?, variables?} → {value, display} */
    public static Map<String, Object> evaluate(Map<String, Object> body) {
        return guard(() -> {
            String expr = str(body, "expression", null);
            Evaluator ev = new Evaluator(AngleMode.parse(body.get("angleMode")), variables(body));
            double v = ev.eval(Parser.parse(expr));
            if (Double.isNaN(v)) {
                throw new CalcException("Resultado indefinido (fora do domínio da função). A matemática disse: não.");
            }
            Map<String, Object> r = new LinkedHashMap<>();
            r.put("value", v);
            r.put("display", Formatter.number(v));
            r.put("infinite", Double.isInfinite(v));
            return r;
        });
    }

    /** {expression, variable?, order?, at?} → {derivative, steps[], valueAt?} */
    public static Map<String, Object> derivative(Map<String, Object> body) {
        return guard(() -> {
            String expr = str(body, "expression", null);
            String var = str(body, "variable", "x");
            int order = (int) num(body, "order", 1);
            if (order < 1 || order > 10) throw new CalcException("Ordem da derivada deve ser entre 1 e 10.");
            Node d = Parser.parse(expr);
            List<String> steps = new ArrayList<>();
            for (int i = 0; i < order; i++) {
                d = Derivative.of(d, var);
                steps.add(Formatter.expression(d));
            }
            Map<String, Object> r = new LinkedHashMap<>();
            r.put("derivative", steps.get(steps.size() - 1));
            r.put("steps", steps);
            if (body.get("at") != null) {
                Map<String, Double> vars = variables(body);
                vars.put(var, evalNumber(body.get("at"), vars));
                double v = new Evaluator(AngleMode.RAD, vars).eval(d);
                r.put("valueAt", v);
                r.put("valueAtDisplay", Formatter.number(v));
            }
            return r;
        });
    }

    /** {functions: [..], xMin, xMax, samples?, angleMode?, roots?} → {series: [{expression, ys[], roots[], error?}]} */
    public static Map<String, Object> plot(Map<String, Object> body) {
        return guard(() -> {
            double xMin = num(body, "xMin", -10);
            double xMax = num(body, "xMax", 10);
            int samples = (int) Math.min(MAX_SAMPLES, Math.max(2, num(body, "samples", 800)));
            if (!(xMax > xMin) || !Double.isFinite(xMin) || !Double.isFinite(xMax)) {
                throw new CalcException("Intervalo do gráfico inválido.");
            }
            boolean withRoots = Boolean.TRUE.equals(body.get("roots"));
            AngleMode mode = AngleMode.parse(body.get("angleMode"));
            Map<String, Double> vars = variables(body);
            List<?> fns = body.get("functions") instanceof List<?> l ? l : List.of();
            if (fns.size() > MAX_FUNCTIONS) throw new CalcException("No máximo " + MAX_FUNCTIONS + " funções por vez.");

            List<Object> series = new ArrayList<>();
            double dx = (xMax - xMin) / (samples - 1);
            for (Object f : fns) {
                String expr = String.valueOf(f);
                Map<String, Object> s = new LinkedHashMap<>();
                s.put("expression", expr);
                try {
                    DoubleUnaryOperator fn = new Evaluator(mode, vars).function(Parser.parse(expr), "x");
                    double[] ys = new double[samples];
                    for (int i = 0; i < samples; i++) ys[i] = safe(fn, xMin + i * dx);
                    s.put("ys", ys);
                    if (withRoots) s.put("roots", Calculus.findRoots(x -> safe(fn, x), xMin, xMax));
                } catch (CalcException e) {
                    s.put("error", e.getMessage());
                }
                series.add(s);
            }
            Map<String, Object> r = new LinkedHashMap<>();
            r.put("xMin", xMin);
            r.put("xMax", xMax);
            r.put("samples", samples);
            r.put("series", series);
            return r;
        });
    }

    /** {expression, variable?, a, b, angleMode?} → {roots[]} */
    public static Map<String, Object> roots(Map<String, Object> body) {
        return guard(() -> {
            String var = str(body, "variable", "x");
            Map<String, Double> vars = variables(body);
            Evaluator ev = new Evaluator(AngleMode.parse(body.get("angleMode")), vars);
            DoubleUnaryOperator fn = ev.function(Parser.parse(str(body, "expression", null)), var);
            double a = evalNumber(body.get("a"), vars);
            double b = evalNumber(body.get("b"), vars);
            List<Double> roots = Calculus.findRoots(x -> safe(fn, x), Math.min(a, b), Math.max(a, b));
            List<String> display = roots.stream().map(Formatter::number).toList();
            return Map.of("roots", roots, "display", display);
        });
    }

    // ------------------------------------------------------------------ helpers

    private interface Body {
        Map<String, Object> run();
    }

    private static Map<String, Object> guard(Body body) {
        try {
            return ok(body.run());
        } catch (CalcException e) {
            return Map.of("ok", false, "error", e.getMessage());
        } catch (StackOverflowError e) {
            return Map.of("ok", false, "error", "Expressão profunda demais. Até a pilha do Java pediu arrego.");
        }
    }

    private static Map<String, Object> ok(Map<String, Object> data) {
        Map<String, Object> r = new LinkedHashMap<>();
        r.put("ok", true);
        r.putAll(data);
        return r;
    }

    /** Avaliação para o gráfico: erros viram "buraco" (NaN) em vez de derrubar a série. */
    private static double safe(DoubleUnaryOperator fn, double x) {
        try {
            return fn.applyAsDouble(x);
        } catch (CalcException e) {
            return Double.NaN;
        }
    }

    private static String str(Map<String, Object> body, String key, String def) {
        Object v = body.get(key);
        if (v == null || v.toString().isBlank()) {
            if (def == null) throw new CalcException("Campo obrigatório ausente: " + key);
            return def;
        }
        return v.toString();
    }

    private static double num(Map<String, Object> body, String key, double def) {
        Object v = body.get(key);
        if (v == null) return def;
        if (v instanceof Number n) return n.doubleValue();
        return evalNumber(v, Map.of());
    }

    /** Aceita número ou texto ("pi/2", "-inf"). */
    private static double evalNumber(Object v, Map<String, Double> vars) {
        if (v == null) throw new CalcException("Faltou um valor numérico.");
        if (v instanceof Number n) return n.doubleValue();
        return new Evaluator(AngleMode.RAD, vars).eval(Parser.parse(v.toString()));
    }

    private static Map<String, Double> variables(Map<String, Object> body) {
        Map<String, Double> vars = new HashMap<>();
        if (body.get("variables") instanceof Map<?, ?> m) {
            for (Map.Entry<?, ?> e : m.entrySet()) {
                if (e.getValue() instanceof Number n) vars.put(String.valueOf(e.getKey()), n.doubleValue());
            }
        }
        return vars;
    }
}
