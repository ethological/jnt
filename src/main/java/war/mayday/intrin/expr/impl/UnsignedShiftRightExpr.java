package war.mayday.intrin.expr.impl;

import war.mayday.intrin.expr.Expression;

/**
 * Represents an unsigned right shift expression ((unsigned)a >> b)
 */
public class UnsignedShiftRightExpr implements Expression {
    private final String a, b;

    public UnsignedShiftRightExpr(String a, String b) {
        this.a = a;
        this.b = b;
    }

    @Override
    public String toString() {
        return String.format("(unsigned int)%s >> %s", a, b);
    }
} 