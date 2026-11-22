package com.stonkscalc.api;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/** JSON mínimo (sem dependências): objetos viram Map, arrays viram List, números viram Double. */
public final class Json {

    public static final class JsonException extends RuntimeException {
        JsonException(String msg) {
            super(msg);
        }
    }

    private Json() {
    }

    // ------------------------------------------------------------------ parse

    public static Object parse(String s) {
        Reader r = new Reader(s);
        r.ws();
        Object v = r.value();
        r.ws();
        if (r.i != s.length()) throw new JsonException("Lixo depois do JSON na posição " + r.i);
        return v;
    }

    private static final class Reader {
        final String s;
        int i;

        Reader(String s) {
            this.s = s;
        }

        void ws() {
            while (i < s.length() && Character.isWhitespace(s.charAt(i))) i++;
        }

        char peek() {
            if (i >= s.length()) throw new JsonException("JSON terminou antes da hora");
            return s.charAt(i);
        }

        void expect(char c) {
            if (peek() != c) throw new JsonException("Esperava '" + c + "' na posição " + i);
            i++;
        }

        Object value() {
            char c = peek();
            return switch (c) {
                case '{' -> object();
                case '[' -> array();
                case '"' -> string();
                case 't' -> literal("true", Boolean.TRUE);
                case 'f' -> literal("false", Boolean.FALSE);
                case 'n' -> literal("null", null);
                default -> number();
            };
        }

        Object literal(String word, Object v) {
            if (!s.startsWith(word, i)) throw new JsonException("Valor inválido na posição " + i);
            i += word.length();
            return v;
        }

        Map<String, Object> object() {
            expect('{');
            Map<String, Object> m = new LinkedHashMap<>();
            ws();
            if (peek() == '}') {
                i++;
                return m;
            }
            while (true) {
                ws();
                String k = string();
                ws();
                expect(':');
                ws();
                m.put(k, value());
                ws();
                if (peek() == ',') {
                    i++;
                    continue;
                }
                expect('}');
                return m;
            }
        }

        List<Object> array() {
            expect('[');
            List<Object> l = new ArrayList<>();
            ws();
            if (peek() == ']') {
                i++;
                return l;
            }
            while (true) {
                ws();
                l.add(value());
                ws();
                if (peek() == ',') {
                    i++;
                    continue;
                }
                expect(']');
                return l;
            }
        }

        String string() {
            expect('"');
            StringBuilder b = new StringBuilder();
            while (true) {
                char c = peek();
                i++;
                if (c == '"') return b.toString();
                if (c != '\\') {
                    b.append(c);
                    continue;
                }
                char e = peek();
                i++;
                switch (e) {
                    case '"', '\\', '/' -> b.append(e);
                    case 'b' -> b.append('\b');
                    case 'f' -> b.append('\f');
                    case 'n' -> b.append('\n');
                    case 'r' -> b.append('\r');
                    case 't' -> b.append('\t');
                    case 'u' -> {
                        if (i + 4 > s.length()) throw new JsonException("Escape \\u incompleto");
                        b.append((char) Integer.parseInt(s.substring(i, i + 4), 16));
                        i += 4;
                    }
                    default -> throw new JsonException("Escape inválido: \\" + e);
                }
            }
        }

        Double number() {
            int start = i;
            while (i < s.length() && "+-0123456789.eE".indexOf(s.charAt(i)) >= 0) i++;
            if (start == i) throw new JsonException("Valor inválido na posição " + i);
            try {
                return Double.parseDouble(s.substring(start, i));
            } catch (NumberFormatException ex) {
                throw new JsonException("Número inválido: " + s.substring(start, i));
            }
        }
    }

    // ------------------------------------------------------------------ stringify

    public static String stringify(Object o) {
        StringBuilder b = new StringBuilder();
        write(b, o);
        return b.toString();
    }

    private static void write(StringBuilder b, Object o) {
        switch (o) {
            case null -> b.append("null");
            case String s -> quote(b, s);
            case Boolean bool -> b.append(bool);
            case Double d -> number(b, d);
            case Float f -> number(b, f.doubleValue());
            case Number n -> b.append(n);
            case double[] arr -> {
                b.append('[');
                for (int k = 0; k < arr.length; k++) {
                    if (k > 0) b.append(',');
                    number(b, arr[k]);
                }
                b.append(']');
            }
            case Map<?, ?> m -> {
                b.append('{');
                boolean first = true;
                for (Map.Entry<?, ?> e : m.entrySet()) {
                    if (!first) b.append(',');
                    first = false;
                    quote(b, String.valueOf(e.getKey()));
                    b.append(':');
                    write(b, e.getValue());
                }
                b.append('}');
            }
            case Iterable<?> it -> {
                b.append('[');
                boolean first = true;
                for (Object x : it) {
                    if (!first) b.append(',');
                    first = false;
                    write(b, x);
                }
                b.append(']');
            }
            default -> quote(b, o.toString());
        }
    }

    private static void number(StringBuilder b, double d) {
        if (!Double.isFinite(d)) b.append("null");
        else if (d == Math.rint(d) && Math.abs(d) < 1e15) b.append((long) d);
        else b.append(d);
    }

    private static void quote(StringBuilder b, String s) {
        b.append('"');
        for (int k = 0; k < s.length(); k++) {
            char c = s.charAt(k);
            switch (c) {
                case '"' -> b.append("\\\"");
                case '\\' -> b.append("\\\\");
                case '\n' -> b.append("\\n");
                case '\r' -> b.append("\\r");
                case '\t' -> b.append("\\t");
                default -> {
                    if (c < 0x20) b.append(String.format("\\u%04x", (int) c));
                    else b.append(c);
                }
            }
        }
        b.append('"');
    }
}
