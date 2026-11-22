package com.stonkscalc.engine;

import com.stonkscalc.engine.Node.Bin;
import com.stonkscalc.engine.Node.Call;
import com.stonkscalc.engine.Node.Neg;
import com.stonkscalc.engine.Node.Num;
import com.stonkscalc.engine.Node.Var;
import com.stonkscalc.engine.Token.Type;

import java.util.ArrayList;
import java.util.List;

/**
 * Parser descendente recursivo.
 *
 * <pre>
 * expression := term (('+' | '-') term)*
 * term       := unary (('*' | '/') unary | power)*      -- o segundo caso é multiplicação implícita: 2x, 3(x+1)
 * unary      := ('-' | '+') unary | power
 * power      := postfix ('^' unary)?                    -- associativo à direita: 2^3^2 = 2^9
 * postfix    := primary ('!' | '%')*
 * primary    := NUMBER | IDENT | IDENT '(' args ')' | '(' expression ')'
 * </pre>
 * Parênteses não fechados no final são fechados automaticamente, como numa calculadora de verdade.
 */
public final class Parser {

    private final List<Token> tokens;
    private int pos;

    private Parser(List<Token> tokens) {
        this.tokens = tokens;
    }

    public static Node parse(String src) {
        if (src == null || src.isBlank()) {
            throw new CalcException("Expressão vazia. Digita alguma coisa aí, campeão.");
        }
        Parser p = new Parser(Lexer.tokenize(src));
        Node n = p.expression();
        if (p.peek().type() != Type.EOF) {
            Token t = p.peek();
            throw new CalcException("Não entendi o '" + t.text() + "' na posição " + (t.pos() + 1) + ".");
        }
        return n;
    }

    private Token peek() {
        return tokens.get(pos);
    }

    private Token next() {
        return tokens.get(pos++);
    }

    private boolean match(Type type) {
        if (peek().type() == type) {
            pos++;
            return true;
        }
        return false;
    }

    private void closeParen() {
        if (match(Type.RPAREN)) return;
        if (peek().type() == Type.EOF) return; // fecha automaticamente
        throw new CalcException("Esperava ')' na posição " + (peek().pos() + 1) + ", mas veio '" + peek().text() + "'.");
    }

    private Node expression() {
        Node left = term();
        while (true) {
            if (match(Type.PLUS)) left = new Bin('+', left, term());
            else if (match(Type.MINUS)) left = new Bin('-', left, term());
            else return left;
        }
    }

    private Node term() {
        Node left = unary();
        while (true) {
            Type t = peek().type();
            if (t == Type.STAR || t == Type.SLASH) {
                next();
                left = new Bin(t == Type.STAR ? '*' : '/', left, unary());
            } else if (t == Type.NUMBER || t == Type.IDENT || t == Type.LPAREN) {
                left = new Bin('*', left, power()); // multiplicação implícita
            } else {
                return left;
            }
        }
    }

    private Node unary() {
        if (match(Type.MINUS)) return new Neg(unary());
        if (match(Type.PLUS)) return unary();
        return power();
    }

    private Node power() {
        Node base = postfix();
        if (match(Type.CARET)) {
            return new Bin('^', base, unary());
        }
        return base;
    }

    private Node postfix() {
        Node n = primary();
        while (true) {
            if (match(Type.BANG)) n = new Call("fact", List.of(n));
            else if (match(Type.PERCENT)) n = new Bin('/', n, new Num(100));
            else return n;
        }
    }

    private Node primary() {
        Token t = next();
        switch (t.type()) {
            case NUMBER:
                return new Num(t.number());
            case LPAREN: {
                Node e = expression();
                closeParen();
                return e;
            }
            case IDENT: {
                String fn = Functions.canonical(t.text());
                if (fn == null) return new Var(t.text());
                List<Node> args = new ArrayList<>();
                if (match(Type.LPAREN)) {
                    if (peek().type() != Type.RPAREN && peek().type() != Type.EOF) {
                        do {
                            args.add(expression());
                        } while (match(Type.COMMA));
                    }
                    closeParen();
                } else if (Functions.info(fn).minArgs() == 1) {
                    args.add(power()); // permite "√2", "sin x", "ln 5"
                } else {
                    throw new CalcException("A função " + t.text() + " precisa de parênteses: "
                            + Functions.info(fn).signature());
                }
                Functions.checkArity(fn, args.size());
                return new Call(fn, args);
            }
            case EOF:
                throw new CalcException("A expressão terminou do nada. Faltou alguma coisa no final.");
            default:
                throw new CalcException("Não esperava '" + t.text() + "' na posição " + (t.pos() + 1) + ".");
        }
    }
}
