package war.metaphor.gens.nodes.invokes;

import org.objectweb.asm.tree.InsnList;
import war.metaphor.gens.nodes.Node;
import war.metaphor.gens.structs.GContext;
import war.metaphor.gens.structs.GValue;
import war.metaphor.util.builder.InsnListBuilder;

public class ParseIntNode implements Node {

    @Override
    public int type() {
        return T_CONSTANT_INT;
    }

    @Override
    public InsnList generate(GContext ctx) {
        int value = rand.nextInt();
        ctx.pushStack(GValue.of(value));
        int mode = rand.nextInt(Character.MIN_RADIX, Character.MAX_RADIX);
        return InsnListBuilder
                .builder()
                .constant(Integer.toString(value, mode))
                .constant(mode)
                .invokestatic("java/lang/Integer", "parseInt", "(Ljava/lang/String;I)I")
                .build();
    }
}
