package war.jnt.core.vm.math.impl;

import war.jnt.core.vm.math.IMath;
import war.metaphor.util.interfaces.IRandom;

public class ParityBasedMath implements IMath, IRandom {
    private Op operation = new Op(-1); // we dont need the value
    private int v1;
    private int v2;

    public ParityBasedMath() {
        v1 = RANDOM.nextInt() & ~1;
        v2 = RANDOM.nextInt() & ~1;
    }

    @Override
    public String compute(int indent) {
        if (RANDOM.nextBoolean()) {
            return String.format("""
                    %soutput %s= (((*(volatile int *)&output & 1) == 0) ? %d : %d);
                    """, "\t".repeat(indent), operation.op, v2, v1);
        } else {
            return String.format("""
                    %soutput %s= (((*(volatile int *)&output & 1) == 1) ? %d : %d);
                    """, "\t".repeat(indent), operation.op, v1, v2);
        }
    }

    @Override
    public String reverse(int indent) {
        if (RANDOM.nextBoolean()) {
            return String.format("""
                    %soutput %s= (((*(volatile int *)&output & 1) == 0) ? %d : %d);
                    """, "\t".repeat(indent), operation.getOpposite(), v2, v1);
        } else {
            return String.format("""
                    %soutput %s= (((*(volatile int *)&output & 1) == 1) ? %d : %d);
                    """, "\t".repeat(indent), operation.getOpposite(), v1, v2);
        }
    }
}
