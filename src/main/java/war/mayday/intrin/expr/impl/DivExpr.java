package war.mayday.intrin.expr.impl;

import war.mayday.intrin.expr.Expression;

public class DivExpr implements Expression {
    private final String a, b;

    public DivExpr(String a, String b) {
        this.a = a;
        this.b = b;
    }

    @Override
    public String toString() {
        return String.format("%s / %s", a, b);
    }
}
