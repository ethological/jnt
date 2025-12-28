package war.metaphor.gens.nodes.stack;

import org.objectweb.asm.tree.InsnList;
import war.metaphor.gens.nodes.Node;
import war.metaphor.gens.structs.GContext;
import war.metaphor.gens.structs.GValue;
import war.metaphor.util.builder.InsnListBuilder;

public class DupWideNode implements Node {

    @Override
    public int type() {
        return T_PUSH;
    }

    @Override
    public int minStackHeight() {
        return 2;
    }

    @Override
    public boolean canGenerate(GContext ctx) {
        GValue v1 = ctx.peekStack();
        GValue v2 = ctx.peekStack(1);
        return v1.isTop() || !v2.isTop();
    }

    @Override
    public InsnList generate(GContext ctx) {
        GValue v1 = ctx.popStack();
        GValue v2 = ctx.popStack();
        ctx.pushStack(v2);
        ctx.pushStack(v1);
        ctx.pushStack(v2);
        ctx.pushStack(v1);
        return InsnListBuilder
                .builder()
                .dup2()
                .build();
    }


}
