package war.mayday.intrin.expr.impl;

import war.mayday.intrin.expr.Expression;

/**
 * Represents a right shift expression (a >> b)
 */
public class ShiftRightExpr implements Expression {
    private final String a, b;

    public ShiftRightExpr(String a, String b) {
        this.a = a;
        this.b = b;
    }

    @Override
    public String toString() {
        return String.format("%s >> %s", a, b);
    }
} 