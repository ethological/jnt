package war.metaphor.gens.nodes.conv;

import org.objectweb.asm.Type;
import org.objectweb.asm.tree.InsnList;
import war.metaphor.gens.nodes.Node;
import war.metaphor.gens.structs.GContext;
import war.metaphor.gens.structs.GValue;
import war.metaphor.util.builder.InsnListBuilder;

public class LongToIntNode implements Node {

    @Override
    public int minStackHeight() {
        return 2;
    }

    @Override
    public boolean canGenerate(GContext ctx) {
        GValue v2 = ctx.peekStack(1);
        return v2.getType() == Type.LONG_TYPE;
    }

    @Override
    public int type() {
        return T_CONV;
    }

    @Override
    public InsnList generate(GContext ctx) {
        ctx.popStack();
        GValue v2 = ctx.popStack();
        Number l1 = (long) v2.getValue();
        ctx.pushStack(GValue.of(l1.intValue()));
        return InsnListBuilder
                .builder()
                .l2i()
                .build();
    }
}