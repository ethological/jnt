package war.metaphor.engine.modules;

import org.objectweb.asm.tree.InsnList;
import war.metaphor.engine.Context;
import war.metaphor.util.builder.InsnListBuilder;

public class AddMod implements Module {

    @Override
    public void run(Context context) {
        int key = context.popStack();
        int curr = context.popStack();
        context.pushStack(curr + key);
    }

    @Override
    public Class<? extends Module> inverse() {
        return SubMod.class;
    }

    @Override
    public String getSourceInstructions(Context context) {
        return "(_mm_cvtsi128_si32(_mm_add_epi32(_mm_set1_epi32(((julong)" + context.varName + ")), _mm_set1_epi32((julong)" + context.popStack() + "))))";
    }

    @Override
    public InsnList getInstructions(Context context) {
        return InsnListBuilder
                .builder()
                .constant(context.popStack())
                .iadd()
                .build();
    }
}
