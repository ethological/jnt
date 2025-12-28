package war.mayday.intrin.expr.impl;

import war.mayday.intrin.expr.Expression;

/**
 * Represents a bitwise AND expression (a & b)
 */
public class AndExpr implements Expression {
    private final String a, b;

    public AndExpr(String a, String b) {
        this.a = a;
        this.b = b;
    }

    @Override
    public String toString() {
        return String.format("%s & %s", a, b);
    }
} 