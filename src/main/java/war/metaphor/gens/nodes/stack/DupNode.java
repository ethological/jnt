package war.metaphor.gens.nodes.stack;

import org.objectweb.asm.tree.InsnList;
import war.metaphor.gens.nodes.Node;
import war.metaphor.gens.structs.GContext;
import war.metaphor.gens.structs.GValue;
import war.metaphor.util.builder.InsnListBuilder;

public class DupNode implements Node {

    @Override
    public int type() {
        return T_PUSH;
    }

    @Override
    public int minStackHeight() {
        return 1;
    }

    @Override
    public boolean canGenerate(GContext ctx) {
        GValue v = ctx.peekStack();
        return !v.isTop();
    }

    @Override
    public InsnList generate(GContext ctx) {
        ctx.pushStack(ctx.peekStack());
        return InsnListBuilder
                .builder()
                .dup()
                .build();
    }


}
