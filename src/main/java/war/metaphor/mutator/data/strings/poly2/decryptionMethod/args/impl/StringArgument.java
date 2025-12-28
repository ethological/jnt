package war.metaphor.mutator.data.strings.poly2.decryptionMethod.args.impl;

import org.apache.commons.text.RandomStringGenerator;
import org.objectweb.asm.tree.InsnList;
import org.objectweb.asm.tree.MethodInsnNode;
import org.objectweb.asm.tree.VarInsnNode;
import war.metaphor.mutator.data.strings.poly2.decryptionMethod.args.AbstractDecryptionMethodArgument;
import war.metaphor.util.Dictionary;

import java.util.concurrent.ThreadLocalRandom;

public class StringArgument extends AbstractDecryptionMethodArgument
{
    @Override
    public String getDescriptor()
    {
        return "Ljava/lang/String;";
    }

    @Override
    public InsnList load(final int var)
    {
        final InsnList list = new InsnList();

        list.add(new VarInsnNode(ALOAD, var));
        list.add(new MethodInsnNode(INVOKEVIRTUAL, "java/lang/String", "hashCode", "()I"));

        return list;
    }

    @Override
    public Object makeRandomValue()
    {
        return Dictionary.gen(ThreadLocalRandom.current().nextInt(1, 32), null);
    }
}
