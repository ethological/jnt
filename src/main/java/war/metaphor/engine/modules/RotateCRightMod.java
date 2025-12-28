package war.metaphor.engine.modules;

import org.objectweb.asm.tree.InsnList;
import war.metaphor.engine.Context;
import war.metaphor.util.builder.InsnListBuilder;

public class RotateCRightMod implements Module {

    @Override
    public void run(Context context) {
        int key = context.popStack();
        int curr = context.popStack();
        key = key % 8;
        int other = 16 - key;
        char ch = (char) curr;
        int v = (char) (((ch & bitsLeft(key)) >> other) | (ch << key));
        context.pushStack(v);
    }

    private int bitsLeft(int bits) {
        if (bits < 0 || bits > 16) {
            throw new IllegalArgumentException("Number of bits must be between 0 and 16.");
        }
        return ((1 << bits) - 1) << (16 - bits);
    }

    @Override
    public Class<? extends Module> inverse() {
        return RotateCLeftMod.class;
    }

    @Override
    public String getSourceInstructions(Context context) {
        int key = context.popStack() % 8;
        int other = 16 - key;
        int mask = bitsLeft(key);

        // Equivalent to:
        //   (_mm_cvtsi128_si32(
        //      _mm_or_si128(
        //        _mm_srli_epi32(_mm_and_si128(x, mask), other),
        //        _mm_slli_epi32(x, key)
        //      )
        //   ))
//        return "(_mm_cvtsi128_si32(" +
//                "_mm_or_si128(" +
//                "_mm_srli_epi32(_mm_and_si128(_mm_set1_epi32(" + var + "), _mm_set1_epi32(" + mask + ")), " + other + "), " +
//                "_mm_slli_epi32(_mm_set1_epi32(" + var + "), " + key + ")" +
//                ")" +
//                "))";
        return "(jlong)( (((julong)(" + context.varName + " & " + mask + ")) >> " + other + ") | ((julong)" + context.varName + " << " + key + ") )";
    }


    @Override
    public InsnList getInstructions(Context context) {
        int key = context.popStack();
        key = key % 8;
        int other = 16 - key;
        int mask = bitsLeft(key);
        return InsnListBuilder
                .builder()
                .dup()
                .constant(mask)
                .iand()
                .constant(other)
                .ishr()
                .swap()
                .constant(key)
                .ishl()
                .ior()
                .build();
    }
}
