package war.metaphor.mutator.data.strings.polymorphic.math.impl;

import org.objectweb.asm.Opcodes;
import org.objectweb.asm.tree.InsnList;
import org.objectweb.asm.tree.InsnNode;
import org.objectweb.asm.tree.MethodInsnNode;
import org.objectweb.asm.tree.TypeInsnNode;
import war.metaphor.mutator.data.strings.polymorphic.math.IPolymorphicMath;

import java.util.Random;

/**
 * @author jan
 */
public class SeededRandom implements IPolymorphicMath {
    @Override
    public int apply(int input) {
        return new Random(input).nextInt();
    }

    @Override
    public InsnList dump() {
        InsnList list = new InsnList();

        list.add(new TypeInsnNode(Opcodes.NEW, "java/util/Random"));
        list.add(new InsnNode(Opcodes.DUP_X1));
        list.add(new InsnNode(Opcodes.SWAP));
        list.add(new InsnNode(Opcodes.I2L));
        list.add(new MethodInsnNode(Opcodes.INVOKESPECIAL, "java/util/Random", "<init>", "(J)V"));
        list.add(new MethodInsnNode(Opcodes.INVOKEVIRTUAL, "java/util/Random", "nextInt", "()I"));

        return list;
    }
}
