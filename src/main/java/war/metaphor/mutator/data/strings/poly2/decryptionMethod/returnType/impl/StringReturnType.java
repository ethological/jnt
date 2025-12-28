package war.metaphor.mutator.data.strings.poly2.decryptionMethod.returnType.impl;

import org.objectweb.asm.Opcodes;
import org.objectweb.asm.tree.*;
import war.metaphor.mutator.data.strings.poly2.decryptionMethod.returnType.AbstractDecryptionMethodReturnType;

public class StringReturnType extends AbstractDecryptionMethodReturnType
{
    private final int byteArrayIdx;

    public StringReturnType(final int byteArrayIdx)
    {
        this.byteArrayIdx = byteArrayIdx;
    }

    @Override
    public InsnList getPackingCode()
    {
        final InsnList list = new InsnList();

        list.add(new TypeInsnNode(Opcodes.NEW, "java/lang/String"));
        list.add(new InsnNode(Opcodes.DUP));
        list.add(new VarInsnNode(Opcodes.ALOAD, byteArrayIdx));
        list.add(new MethodInsnNode(Opcodes.INVOKESPECIAL, "java/lang/String", "<init>", "([B)V"));

        return list;
    }

    @Override
    public InsnList getCachePackingCode()
    {
        final InsnList list = new InsnList();

        list.add(new TypeInsnNode(Opcodes.NEW, "java/lang/String"));
        list.add(new InsnNode(DUP_X1));
        list.add(new InsnNode(SWAP));
        list.add(new MethodInsnNode(Opcodes.INVOKESPECIAL, "java/lang/String", "<init>", "([B)V"));

        return list;
    }

    @Override
    public InsnList getUnpackingCode()
    {
        return new InsnList();
    }

    @Override
    public String getDescriptor()
    {
        return "Ljava/lang/String;";
    }
}
