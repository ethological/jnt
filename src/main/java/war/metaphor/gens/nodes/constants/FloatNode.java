package war.metaphor.gens.nodes.constants;

import org.objectweb.asm.tree.InsnList;
import war.metaphor.gens.nodes.Node;
import war.metaphor.gens.structs.GContext;
import war.metaphor.gens.structs.GValue;
import war.metaphor.util.builder.InsnListBuilder;

public class FloatNode implements Node {

    @Override
    public int type() {
        return T_CONSTANT_FLOAT;
    }

    @Override
    public InsnList generate(GContext ctx) {
        float value = rand.nextFloat();
        ctx.pushStack(GValue.of(value));
        return InsnListBuilder
                .builder()
                .constant(value)
                .build();
    }
}
