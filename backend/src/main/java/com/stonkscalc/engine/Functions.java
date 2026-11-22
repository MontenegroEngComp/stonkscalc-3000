package com.stonkscalc.engine;

import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

/** Catálogo de funções suportadas (nome canônico, aridade, descrição e apelidos). */
public final class Functions {

    public record Info(String name, int minArgs, int maxArgs, String signature, String description, String category) {
    }

    /** Funções que recebem uma expressão "crua" + o nome da variável (avaliação preguiçosa). */
    public static final Set<String> LAZY = Set.of("integral", "deriv", "limit", "sum", "prod", "solve");

    private static final Map<String, Info> CATALOG = new LinkedHashMap<>();
    private static final Map<String, String> ALIASES = new HashMap<>();
    private static final int VAR = Integer.MAX_VALUE;

    static {
        String t = "Trigonometria";
        add("sin", 1, 1, "sin(x)", "Seno", t, "sen");
        add("cos", 1, 1, "cos(x)", "Cosseno", t);
        add("tan", 1, 1, "tan(x)", "Tangente", t, "tg");
        add("sec", 1, 1, "sec(x)", "Secante", t);
        add("csc", 1, 1, "csc(x)", "Cossecante", t, "cosec", "cossec");
        add("cot", 1, 1, "cot(x)", "Cotangente", t, "cotg");
        add("asin", 1, 1, "asin(x)", "Arco-seno", t, "arcsin", "asen", "arcsen");
        add("acos", 1, 1, "acos(x)", "Arco-cosseno", t, "arccos");
        add("atan", 1, 1, "atan(x)", "Arco-tangente", t, "arctan", "arctg", "atg");
        add("acot", 1, 1, "acot(x)", "Arco-cotangente", t, "arccot");
        add("asec", 1, 1, "asec(x)", "Arco-secante", t, "arcsec");
        add("acsc", 1, 1, "acsc(x)", "Arco-cossecante", t, "arccsc");
        add("atan2", 2, 2, "atan2(y, x)", "Ângulo do ponto (x, y)", t);

        String h = "Hiperbólicas";
        add("sinh", 1, 1, "sinh(x)", "Seno hiperbólico", h, "senh");
        add("cosh", 1, 1, "cosh(x)", "Cosseno hiperbólico", h);
        add("tanh", 1, 1, "tanh(x)", "Tangente hiperbólica", h, "tgh");
        add("sech", 1, 1, "sech(x)", "Secante hiperbólica", h);
        add("csch", 1, 1, "csch(x)", "Cossecante hiperbólica", h);
        add("coth", 1, 1, "coth(x)", "Cotangente hiperbólica", h);
        add("asinh", 1, 1, "asinh(x)", "Arco-seno hiperbólico", h, "arcsinh");
        add("acosh", 1, 1, "acosh(x)", "Arco-cosseno hiperbólico", h, "arccosh");
        add("atanh", 1, 1, "atanh(x)", "Arco-tangente hiperbólica", h, "arctanh");

        String l = "Potências e logaritmos";
        add("sqrt", 1, 1, "sqrt(x)", "Raiz quadrada (√)", l, "raiz");
        add("cbrt", 1, 1, "cbrt(x)", "Raiz cúbica (∛)", l);
        add("root", 2, 2, "root(x, n)", "Raiz n-ésima", l);
        add("pow", 2, 2, "pow(x, y)", "Potência x^y", l);
        add("exp", 1, 1, "exp(x)", "Exponencial e^x", l);
        add("ln", 1, 1, "ln(x)", "Logaritmo natural", l);
        add("log", 1, 2, "log(x) / log(x, b)", "Log base 10, ou base b", l, "log10");
        add("log2", 1, 1, "log2(x)", "Logaritmo base 2", l, "lb");

        String a = "Aritmética";
        add("abs", 1, 1, "abs(x)", "Valor absoluto |x|", a, "modulo");
        add("floor", 1, 1, "floor(x)", "Arredonda para baixo", a, "piso");
        add("ceil", 1, 1, "ceil(x)", "Arredonda para cima", a, "teto");
        add("round", 1, 2, "round(x) / round(x, casas)", "Arredonda", a, "arredondar");
        add("trunc", 1, 1, "trunc(x)", "Parte inteira", a);
        add("frac", 1, 1, "frac(x)", "Parte fracionária", a);
        add("sign", 1, 1, "sign(x)", "Sinal (-1, 0, 1)", a, "sgn");
        add("mod", 2, 2, "mod(a, b)", "Resto da divisão", a, "resto");
        add("gcd", 1, VAR, "gcd(a, b, ...)", "MDC", a, "mdc");
        add("lcm", 1, VAR, "lcm(a, b, ...)", "MMC", a, "mmc");
        add("min", 1, VAR, "min(a, b, ...)", "Mínimo", a);
        add("max", 1, VAR, "max(a, b, ...)", "Máximo", a);
        add("hypot", 1, VAR, "hypot(a, b, ...)", "Hipotenusa √(a²+b²+…)", a);
        add("deg", 1, 1, "deg(x)", "Radianos → graus", a, "graus");
        add("rad", 1, 1, "rad(x)", "Graus → radianos", a, "radianos");

        String c = "Combinatória e especiais";
        add("fact", 1, 1, "fact(n) ou n!", "Fatorial", c, "factorial", "fatorial");
        add("gamma", 1, 1, "gamma(x)", "Função Gama Γ(x)", c);
        add("ncr", 2, 2, "nCr(n, k)", "Combinação", c, "comb", "binom");
        add("npr", 2, 2, "nPr(n, k)", "Arranjo / permutação", c, "perm", "arranjo");
        add("rand", 0, 0, "rand()", "Aleatório entre 0 e 1", c, "random");
        add("randint", 2, 2, "randint(a, b)", "Inteiro aleatório entre a e b", c);

        String s = "Estatística";
        add("mean", 1, VAR, "mean(a, b, ...)", "Média", s, "avg", "media");
        add("median", 1, VAR, "median(a, b, ...)", "Mediana", s, "mediana");
        add("variance", 1, VAR, "variance(a, b, ...)", "Variância amostral", s, "var", "variancia");
        add("std", 1, VAR, "std(a, b, ...)", "Desvio padrão amostral", s, "stdev", "desvio");
        add("stdp", 1, VAR, "stdp(a, b, ...)", "Desvio padrão populacional", s);
        add("total", 1, VAR, "total(a, b, ...)", "Soma de uma lista de valores", s);

        String k = "Cálculo";
        add("deriv", 3, 4, "deriv(f, x, a [, ordem])", "Derivada de f no ponto x = a", k, "diff", "derivada");
        add("integral", 4, 4, "integral(f, x, a, b)", "Integral definida de a até b (aceita ±inf)", k, "integrate", "integrar");
        add("limit", 3, 4, "limit(f, x, a [, lado])", "Limite quando x → a (lado: -1 esquerda, 1 direita)", k, "lim", "limite");
        add("sum", 4, 4, "sum(f, k, a, b)", "Somatório Σ", k, "sigma", "somatorio");
        add("prod", 4, 4, "prod(f, k, a, b)", "Produtório Π", k, "product", "produtorio");
        add("solve", 2, 3, "solve(f, x [, chute])", "Resolve f(x) = 0", k, "resolver", "zero");
    }

    private Functions() {
    }

    private static void add(String name, int min, int max, String sig, String desc, String cat, String... aliases) {
        CATALOG.put(name, new Info(name, min, max, sig, desc, cat));
        ALIASES.put(name, name);
        for (String alias : aliases) ALIASES.put(alias, name);
    }

    /** Nome canônico da função, ou null se não for função. */
    public static String canonical(String name) {
        return ALIASES.get(name.toLowerCase(Locale.ROOT));
    }

    public static Info info(String canonicalName) {
        return CATALOG.get(canonicalName);
    }

    public static Collection<Info> all() {
        return Collections.unmodifiableCollection(CATALOG.values());
    }

    public static void checkArity(String name, int count) {
        Info info = CATALOG.get(name);
        if (info == null) throw new CalcException("Função desconhecida: " + name);
        if (count < info.minArgs() || count > info.maxArgs()) {
            String expected = info.minArgs() == info.maxArgs()
                    ? String.valueOf(info.minArgs())
                    : info.maxArgs() == VAR ? "pelo menos " + info.minArgs()
                    : "de " + info.minArgs() + " a " + info.maxArgs();
            throw new CalcException("A função " + name + " espera " + expected + " argumento(s), mas recebeu "
                    + count + ". Uso: " + info.signature());
        }
    }
}
