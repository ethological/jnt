package war.metaphor.gens.nodes.conv;

import org.objectweb.asm.Type;
import org.objectweb.asm.tree.InsnList;
import war.metaphor.gens.nodes.Node;
import war.metaphor.gens.structs.GContext;
import war.metaphor.gens.structs.GTopValue;
import war.metaphor.gens.structs.GValue;
import war.metaphor.util.builder.InsnListBuilder;

public class IntToLongNode implements Node {

    @Override
    public int minStackHeight() {
        return 1;
    }

    @Override
    public boolean canGenerate(GContext ctx) {
        GValue v1 = ctx.peekStack();
        return v1.getType() == Type.INT_TYPE;
    }

    @Override
    public int type() {
        return T_CONV;
    }

    @Override
    public InsnList generate(GContext ctx) {
        GValue v1 = ctx.popStack();
        Number i1 = (int) v1.getValue();
        ctx.pushStack(GValue.of(i1.longValue()));
        ctx.pushStack(GTopValue.INSTANCE);
        return InsnListBuilder
                .builder()
                .i2l()
                .build();
    }

}
