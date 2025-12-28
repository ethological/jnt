package war.mayday.intrin.expr.impl;

import war.mayday.intrin.expr.Expression;

/**
 * Represents a left shift expression (a << b)
 */
public class ShiftLeftExpr implements Expression {
    private final String a, b;

    public ShiftLeftExpr(String a, String b) {
        this.a = a;
        this.b = b;
    }

    @Override
    public String toString() {
        return String.format("%s << %s", a, b);
    }
} 