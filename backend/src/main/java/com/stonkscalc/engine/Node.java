package com.stonkscalc.engine;

import java.util.List;

/** Árvore sintática (AST) das expressões. */
public sealed interface Node permits Node.Num, Node.Var, Node.Neg, Node.Bin, Node.Call {

    record Num(double value) implements Node {
    }

    record Var(String name) implements Node {
    }

    record Neg(Node operand) implements Node {
    }

    /** op: um de + - * / ^ */
    record Bin(char op, Node left, Node right) implements Node {
    }

    record Call(String name, List<Node> args) implements Node {
        public Call {
            args = List.copyOf(args);
        }
    }

    Num ZERO = new Num(0);
    Num ONE = new Num(1);

    static Num num(double v) {
        return new Num(v);
    }

    static Node add(Node a, Node b) {
        return new Bin('+', a, b);
    }

    static Node sub(Node a, Node b) {
        return new Bin('-', a, b);
    }

    static Node mul(Node a, Node b) {
        return new Bin('*', a, b);
    }

    static Node div(Node a, Node b) {
        return new Bin('/', a, b);
    }

    static Node pow(Node a, Node b) {
        return new Bin('^', a, b);
    }

    static Node neg(Node a) {
        return new Neg(a);
    }

    static Node call(String name, Node... args) {
        return new Call(name, List.of(args));
    }

    static boolean isNum(Node n, double v) {
        return n instanceof Num(double x) && x == v;
    }

    /** true se a variável aparece em algum lugar da expressão. */
    static boolean contains(Node n, String var) {
        return switch (n) {
            case Num x -> false;
            case Var(String name) -> name.equals(var);
            case Neg(Node o) -> contains(o, var);
            case Bin(char op, Node l, Node r) -> contains(l, var) || contains(r, var);
            case Call(String name, List<Node> args) -> args.stream().anyMatch(a -> contains(a, var));
        };
    }
}
