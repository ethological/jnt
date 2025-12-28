package war.metaphor.mutator.data.strings.poly2.decryptionMethod.args.impl.integer;

import org.objectweb.asm.tree.InsnList;
import org.objectweb.asm.tree.InsnNode;
import org.objectweb.asm.tree.VarInsnNode;
import war.metaphor.mutator.data.strings.poly2.decryptionMethod.args.AbstractDecryptionMethodArgument;

import java.util.concurrent.ThreadLocalRandom;

public class IntegerArgument extends AbstractDecryptionMethodArgument
{
    @Override
    public String getDescriptor()
    {
        return "I";
    }

    @Override
    public InsnList load(final int var) {
        final InsnList list = new InsnList();

        list.add(new VarInsnNode(ILOAD, var));

        return list;
    }

    @Override
    public Object makeRandomValue()
    {
        return ThreadLocalRandom.current().nextInt(Integer.MIN_VALUE, Integer.MAX_VALUE);
    }
}
