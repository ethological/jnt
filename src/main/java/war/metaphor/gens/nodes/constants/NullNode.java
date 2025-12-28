package war.metaphor.gens.nodes.constants;

import org.objectweb.asm.tree.InsnList;
import war.metaphor.gens.nodes.Node;
import war.metaphor.gens.structs.GContext;
import war.metaphor.gens.structs.GValue;
import war.metaphor.util.builder.InsnListBuilder;

public class NullNode implements Node {

    @Override
    public int type() {
        return T_CONSTANT_OBJECT;
    }

    @Override
    public InsnList generate(GContext ctx) {
        ctx.pushStack(GValue.nullType());
        return InsnListBuilder
                .builder()
                .aconst_null()
                .build();
    }


}
