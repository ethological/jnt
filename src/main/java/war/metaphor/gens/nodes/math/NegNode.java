package war.metaphor.gens.nodes.math;

import org.objectweb.asm.Type;
import org.objectweb.asm.tree.InsnList;
import war.metaphor.gens.nodes.Node;
import war.metaphor.gens.structs.GContext;
import war.metaphor.gens.structs.GTopValue;
import war.metaphor.gens.structs.GValue;
import war.metaphor.util.builder.InsnListBuilder;

public class NegNode implements Node {

    @Override
    public int minStackHeight() {
        return 1;
    }

    @Override
    public boolean canGenerate(GContext ctx) {
        GValue v1 = ctx.peekStack();
        return v1.getType() == Type.INT_TYPE || v1.getType() == Type.FLOAT_TYPE || v1.isTop();
    }

    @Override
    public int type() {
        return T_CONV;
    }

    @Override
    public InsnList generate(GContext ctx) {
        GValue v1 = ctx.popStack();
        if (v1.isTop())
            v1 = ctx.popStack();

        switch (v1.getType().getSort()) {
            case Type.LONG:
                ctx.pushStack(GValue.of(-(Long) v1.getValue()));
                ctx.pushStack(GTopValue.INSTANCE);
                return InsnListBuilder
                        .builder()
                        .lneg()
                        .build();
            case Type.DOUBLE:
                ctx.pushStack(GValue.of(-(Double) v1.getValue()));
                ctx.pushStack(GTopValue.INSTANCE);
                return InsnListBuilder
                        .builder()
                        .dneg()
                        .build();
            case Type.FLOAT:
                ctx.pushStack(GValue.of(-(Float) v1.getValue()));
                return InsnListBuilder
                        .builder()
                        .fneg()
                        .build();
            default:
                ctx.pushStack(GValue.of(-(Integer) v1.getValue()));
                return InsnListBuilder
                        .builder()
                        .ineg()
                        .build();
        }
    }
}
