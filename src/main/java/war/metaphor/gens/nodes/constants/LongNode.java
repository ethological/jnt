package war.metaphor.gens.nodes.constants;

import org.objectweb.asm.tree.InsnList;
import war.metaphor.gens.nodes.Node;
import war.metaphor.gens.structs.GContext;
import war.metaphor.gens.structs.GTopValue;
import war.metaphor.gens.structs.GValue;
import war.metaphor.util.builder.InsnListBuilder;

public class LongNode implements Node {

    @Override
    public int type() {
        return T_CONSTANT_LONG;
    }

    @Override
    public InsnList generate(GContext ctx) {
        long value = rand.nextLong();
        ctx.pushStack(GValue.of(value));
        ctx.pushStack(GTopValue.INSTANCE);
        return InsnListBuilder
                .builder()
                .constant(value)
                .build();
    }
}
