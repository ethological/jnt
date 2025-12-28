package war.mayday.intrin.expr.impl;

import war.mayday.intrin.expr.Expression;

/**
 * Represents a bitwise OR expression (a | b)
 */
public class OrExpr implements Expression {
    private final String a, b;

    public OrExpr(String a, String b) {
        this.a = a;
        this.b = b;
    }

    @Override
    public String toString() {
        return String.format("%s | %s", a, b);
    }
} 