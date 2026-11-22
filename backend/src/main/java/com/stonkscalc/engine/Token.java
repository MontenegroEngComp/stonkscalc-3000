package com.stonkscalc.engine;

public record Token(Type type, String text, double number, int pos) {

    public enum Type {
        NUMBER, IDENT, PLUS, MINUS, STAR, SLASH, CARET, BANG, PERCENT, LPAREN, RPAREN, COMMA, EOF
    }

    public static Token of(Type type, String text, int pos) {
        return new Token(type, text, Double.NaN, pos);
    }
}
