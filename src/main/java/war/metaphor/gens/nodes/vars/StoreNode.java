package war.metaphor.gens.nodes.vars;

import org.objectweb.asm.tree.InsnList;
import org.objectweb.asm.tree.MethodNode;
import war.metaphor.gens.nodes.Node;
import war.metaphor.gens.structs.GContext;
import war.metaphor.gens.structs.GLocal;
import war.metaphor.gens.structs.GValue;
import war.metaphor.util.builder.InsnListBuilder;

public class StoreNode implements Node {

    public GLocal lastStored = null;

    @Override
    public int type() {
        return T_STORE;
    }

    @Override
    public int minStackHeight() {
        return 1;
    }

    @Override
    public boolean canGenerate(GContext ctx) {
        return ctx.stackHeight > 0;
    }

    @Override
    public InsnList generate(GContext ctx) {
        GValue value = ctx.popStack();
        if (value.isTop())
            value = ctx.popStack();
        MethodNode method = ctx.method;
        int index = method.maxLocals;
        ctx.growLocals(value.getSize());
        ctx.locals[index] = GLocal.of(value, index);
        lastStored = ctx.locals[index];
        return InsnListBuilder
                .builder()
                .store(value.getType(), index)
                .build();
    }
}
