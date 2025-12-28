package war.metaphor.gens.nodes.stack;

import org.objectweb.asm.tree.InsnList;
import war.metaphor.gens.nodes.Node;
import war.metaphor.gens.structs.GContext;
import war.metaphor.gens.structs.GValue;
import war.metaphor.util.builder.InsnListBuilder;

public class DupWideX2Node implements Node {

    @Override
    public int type() {
        return T_PUSH;
    }

    @Override
    public int minStackHeight() {
        return 4;
    }

    @Override
    public boolean canGenerate(GContext ctx) {
        GValue v1 = ctx.peekStack();
        GValue v2 = ctx.peekStack(1);
        GValue v3 = ctx.peekStack(2);
        GValue v4 = ctx.peekStack(3);
        return (!v1.isTop() && !v2.isTop() && !v3.isTop() && !v4.isTop()) ||
                (!v1.isTop() && !v2.isTop() && v3.isTop()) ||
                (v1.isTop() && ((!v2.isTop() && !v3.isTop()) || (v2.isTop())));
    }

    @Override
    public InsnList generate(GContext ctx) {
        GValue v1 = ctx.popStack();
        GValue v2 = ctx.popStack();
        GValue v3 = ctx.popStack();
        GValue v4 = ctx.popStack();
        ctx.pushStack(v2);
        ctx.pushStack(v1);
        ctx.pushStack(v4);
        ctx.pushStack(v3);
        ctx.pushStack(v2);
        ctx.pushStack(v1);
        return InsnListBuilder
                .builder()
                .dup2_x2()
                .build();
    }


}
