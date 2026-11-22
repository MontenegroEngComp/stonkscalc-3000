package com.stonkscalc.engine;

import com.stonkscalc.engine.Token.Type;

import java.util.ArrayList;
import java.util.List;

/**
 * Converte texto em tokens. Aceita também os símbolos "bonitos" que a interface usa
 * (×, ÷, −, π, √, ∞, ²,³).
 */
public final class Lexer {

    private Lexer() {
    }

    public static List<Token> tokenize(String src) {
        List<Token> out = new ArrayList<>();
        int i = 0;
        int n = src.length();
        while (i < n) {
            char c = src.charAt(i);
            if (Character.isWhitespace(c)) {
                i++;
                continue;
            }
            if (Character.isDigit(c) || (c == '.' && i + 1 < n && Character.isDigit(src.charAt(i + 1)))) {
                int start = i;
                while (i < n && Character.isDigit(src.charAt(i))) i++;
                if (i < n && src.charAt(i) == '.') {
                    i++;
                    while (i < n && Character.isDigit(src.charAt(i))) i++;
                }
                // Notação científica só com 'E' maiúsculo, pois 'e' é a constante de Euler (2e = 2·e).
                if (i < n && src.charAt(i) == 'E') {
                    int j = i + 1;
                    if (j < n && (src.charAt(j) == '+' || src.charAt(j) == '-')) j++;
                    if (j < n && Character.isDigit(src.charAt(j))) {
                        i = j;
                        while (i < n && Character.isDigit(src.charAt(i))) i++;
                    }
                }
                String text = src.substring(start, i);
                out.add(new Token(Type.NUMBER, text, Double.parseDouble(text), start));
                continue;
            }
            if (c == 'π') {
                out.add(Token.of(Type.IDENT, "pi", i++));
                continue;
            }
            if (Character.isLetter(c) || c == '_') {
                int start = i;
                while (i < n && (Character.isLetterOrDigit(src.charAt(i)) || src.charAt(i) == '_')
                        && src.charAt(i) != 'π') {
                    i++;
                }
                out.add(Token.of(Type.IDENT, src.substring(start, i), start));
                continue;
            }
            int pos = i++;
            switch (c) {
                case '+' -> out.add(Token.of(Type.PLUS, "+", pos));
                case '-', '−', '–' -> out.add(Token.of(Type.MINUS, "-", pos));
                case '*', '×', '·' -> out.add(Token.of(Type.STAR, "*", pos));
                case '/', '÷' -> out.add(Token.of(Type.SLASH, "/", pos));
                case '^' -> out.add(Token.of(Type.CARET, "^", pos));
                case '!' -> out.add(Token.of(Type.BANG, "!", pos));
                case '%' -> out.add(Token.of(Type.PERCENT, "%", pos));
                case '(', '[' -> out.add(Token.of(Type.LPAREN, "(", pos));
                case ')', ']' -> out.add(Token.of(Type.RPAREN, ")", pos));
                case ',', ';' -> out.add(Token.of(Type.COMMA, ",", pos));
                case '√' -> out.add(Token.of(Type.IDENT, "sqrt", pos));
                case '∛' -> out.add(Token.of(Type.IDENT, "cbrt", pos));
                case '∞' -> out.add(Token.of(Type.IDENT, "inf", pos));
                case '²' -> {
                    out.add(Token.of(Type.CARET, "^", pos));
                    out.add(new Token(Type.NUMBER, "2", 2, pos));
                }
                case '³' -> {
                    out.add(Token.of(Type.CARET, "^", pos));
                    out.add(new Token(Type.NUMBER, "3", 3, pos));
                }
                default -> throw new CalcException(
                        "Caractere estranho '" + c + "' na posição " + (pos + 1) + ". Isso não é matemática, é hieróglifo.");
            }
        }
        out.add(Token.of(Type.EOF, "", n));
        return out;
    }
}
