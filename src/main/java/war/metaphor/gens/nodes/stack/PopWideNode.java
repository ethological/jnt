package war.metaphor.gens.nodes.stack;

import org.objectweb.asm.tree.InsnList;
import war.metaphor.gens.nodes.Node;
import war.metaphor.gens.structs.GContext;
import war.metaphor.gens.structs.GValue;
import war.metaphor.util.builder.InsnListBuilder;

public class PopWideNode implements Node {

    @Override
    public int type() {
        return T_POP;
    }

    @Override
    public int minStackHeight() {
        return 2;
    }

    @Override
    public boolean canGenerate(GContext ctx) {
        GValue v2 = ctx.peekStack(1);
        return !v2.isTop();
    }

    @Override
    public InsnList generate(GContext ctx) {
        ctx.popStack();
        ctx.popStack();
        return InsnListBuilder
                .builder()
                .pop2()
                .build();
    }


}
