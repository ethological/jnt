package war.mayday.impl.sim.typed.impl;

import war.mayday.impl.sim.typed.SIMPoly;
import war.mayday.intrin.MachinePrinter;

import java.security.SecureRandom;

public class SIMParity implements SIMPoly {
    private final String operator;
    private final String reverseOperator;
    private final int value1;
    private final int value2;

    public SIMParity() {
        this.operator = new SecureRandom().nextBoolean() ? "+=" : "*=";
        this.reverseOperator = operator.equals("+=") ? "-=" : "/=";
        this.value1 = new SecureRandom().nextInt(100) * 2;
        this.value2 = new SecureRandom().nextInt(100) * 2 + 1;
    }

    @Override
    public String compute(MachinePrinter printer, String outputName) {
        return String.format("%s %s (((%s & 1) == 0) ? %d : %d)",
                outputName, operator, outputName, value1, value2);
    }

    @Override
    public String reverse(MachinePrinter printer, String outputName) {
        return String.format("%s %s (((%s & 1) == 0) ? %d : %d)",
                outputName, reverseOperator, outputName, value1, value2);
    }
}
