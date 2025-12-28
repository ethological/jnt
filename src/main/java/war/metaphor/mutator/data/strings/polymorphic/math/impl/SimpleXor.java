package war.metaphor.mutator.data.strings.polymorphic.math.impl;

import org.objectweb.asm.Opcodes;
import org.objectweb.asm.tree.InsnList;
import org.objectweb.asm.tree.InsnNode;
import war.metaphor.mutator.data.strings.polymorphic.math.IPolymorphicMath;
import war.metaphor.util.asm.BytecodeUtil;
import war.metaphor.util.interfaces.IRandom;

/**
 * Mostly used for testing the engine
 * @author jan
 */
public class SimpleXor implements IPolymorphicMath, IRandom, Opcodes {
    private final int xor = RANDOM.nextInt(-127, 128);

    @Override
    public int apply(int input) {
        return input ^ xor;
    }

    @Override
    public InsnList dump() {
        InsnList list = new InsnList();

        list.add(BytecodeUtil.makeInteger(xor));
        list.add(new InsnNode(IXOR));

        return list;
    }
}
