package war.metaphor.gens.nodes.math;

import org.objectweb.asm.Type;
import org.objectweb.asm.tree.InsnList;
import war.metaphor.gens.nodes.Node;
import war.metaphor.gens.structs.GContext;
import war.metaphor.gens.structs.GTopValue;
import war.metaphor.gens.structs.GValue;
import war.metaphor.util.builder.InsnListBuilder;

public class RShiftNode implements Node {

    @Override
    public int minStackHeight() {
        return 2;
    }

    @Override
    public boolean canGenerate(GContext ctx) {
        GValue v1 = ctx.peekStack();
        GValue v2 = ctx.peekStack(1);
        if (v1.getType() == Type.INT_TYPE && v2.getType() == Type.INT_TYPE) return true;
        if (v1.getType() == Type.INT_TYPE) {
            if (ctx.stackHeight > 2) {
                if (!v2.isTop()) return false;
                GValue v3 = ctx.peekStack(2);
                return v3.getType() == Type.LONG_TYPE;
            }
        }
        return false;
    }

    @Override
    public int type() {
        return T_MATH;
    }

    @Override
    public InsnList generate(GContext ctx) {
        GValue v1 = ctx.popStack();
        GValue v2 = ctx.popStack();

        if (v2.isTop()) {
            v2 = ctx.popStack();
            if (v1.isValueKnown() && v2.isValueKnown()) {
                long l2 = (long) v2.getValue();
                int l1 = (int) v1.getValue();
                ctx.pushStack(GValue.of(l2 >> l1));
                ctx.pushStack(GTopValue.INSTANCE);
            } else {
                ctx.pushStack(GValue.of(Type.LONG_TYPE));
                ctx.pushStack(GTopValue.INSTANCE);
            }
            return InsnListBuilder
                    .builder()
                    .lshr()
                    .build();
        } else {
            if (v1.isValueKnown() && v2.isValueKnown()) {
                int i1 = (int) v1.getValue();
                int i2 = (int) v2.getValue();
                ctx.pushStack(GValue.of(i2 >> i1));
            } else {
                ctx.pushStack(GValue.of(Type.INT_TYPE));
            }
            return InsnListBuilder
                    .builder()
                    .ishr()
                    .build();
        }
    }
}
