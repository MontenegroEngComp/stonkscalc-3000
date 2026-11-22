package com.stonkscalc.engine;

/** Erro de cálculo com mensagem amigável (em português) para o usuário. */
public class CalcException extends RuntimeException {
    public CalcException(String message) {
        super(message);
    }
}
