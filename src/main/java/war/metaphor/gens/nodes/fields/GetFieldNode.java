package war.metaphor.gens.nodes.fields;

import org.objectweb.asm.Type;
import org.objectweb.asm.tree.FieldNode;
import org.objectweb.asm.tree.InsnList;
import war.metaphor.gens.nodes.Node;
import war.metaphor.gens.structs.GContext;
import war.metaphor.gens.structs.GTopValue;
import war.metaphor.gens.structs.GValue;
import war.metaphor.tree.ClassField;
import war.metaphor.tree.JClassNode;
import war.metaphor.util.builder.InsnListBuilder;

import java.lang.reflect.Modifier;

public class GetFieldNode implements Node {

    @Override
    public int type() {
        return T_CONSTANT_INT;
    }

    @Override
    public InsnList generate(GContext ctx) {
        ClassField field = ctx.fields[rand.nextInt(ctx.fields.length)];
        JClassNode node = field.getClassNode();
        FieldNode fieldNode = field.getMember();
        Type fieldType = Type.getType(fieldNode.desc);

        ctx.pushStack(GValue.of(fieldType, fieldNode.value));
        if (fieldType.getSize() == 2)
            ctx.pushStack(GTopValue.INSTANCE);

        InsnListBuilder builder = InsnListBuilder
                .builder();
        if (Modifier.isStatic(fieldNode.access))
            builder
                    .getstatic(node.name, fieldNode.name, fieldNode.desc);
        else
            builder
                    .aload(0)
                    .getfield(node.name, fieldNode.name, fieldNode.desc);
        return builder.build();
    }
}
