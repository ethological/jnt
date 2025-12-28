package war.jnt.core.vm.math.impl;

import war.jnt.core.vm.math.IMath;
import war.metaphor.util.interfaces.IRandom;

import java.util.HashMap;
import java.util.Map;

/**
 * I have no idea what the fuck i did but it works (if it breaks, just delete it)
 * @author jan
 */
public class SwitchBasedMath implements IMath, IRandom {
    private final Map<Integer, Op> caseToIntMap = new HashMap<>();
    public int bits;

    public SwitchBasedMath() {
        // stinking pile of shit. Why?
        bits = RANDOM.nextInt(3) + 2;
        int bytes = (int) Math.pow(2, bits);
        for (int i = 0; i < bytes - 1; i++) {
            caseToIntMap.put(i, new Op(RANDOM.nextInt(1000) << bits));
        }
    }


    @Override
    public String compute(int indent) {
        StringBuilder sb = new StringBuilder();

        sb.append(String.format("""
                %sswitch ((*(volatile int *)&output & ((1 << %d) - 1)) %% %d)
                %s{
                """, "\t".repeat(indent), bits, caseToIntMap.size(), "\t".repeat(indent)));

        caseToIntMap.forEach((switchCase, xor) -> {
            sb.append(String.format("""
                    %scase %d:
                    %soutput %s= %d;
                    %sbreak;
                    """, "\t".repeat(indent + 1), switchCase, "\t".repeat(indent + 2), xor.op, xor.num, "\t".repeat(indent + 2)));
        });

        sb.append(String.format("%s}%n", "\t".repeat(indent)));

        return sb.toString();
    }

    @Override
    public String reverse(int indent) {
        StringBuilder sb = new StringBuilder();

        sb.append(String.format("""
                %sswitch ((*(volatile int *)&output & ((1 << %d) - 1)) %% %d)
                %s{
                """, "\t".repeat(indent), bits, caseToIntMap.size(), "\t".repeat(indent)));

        caseToIntMap.forEach((switchCase, xor) -> {
            sb.append(String.format("""
                    %scase %d:
                    %soutput %s= %d;
                    %sbreak;
                    """, "\t".repeat(indent + 1), switchCase, "\t".repeat(indent + 2), xor.getOpposite(), xor.num, "\t".repeat(indent + 2)));
        });

        sb.append(String.format("%s}%n", "\t".repeat(indent)));

        return sb.toString();
    }
}
