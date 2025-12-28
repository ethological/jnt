package war.metaphor.mutator.data.strings.polymorphic.math.impl;

import org.objectweb.asm.Opcodes;
import org.objectweb.asm.tree.InsnList;
import org.objectweb.asm.tree.InsnNode;
import org.objectweb.asm.tree.JumpInsnNode;
import org.objectweb.asm.tree.LabelNode;
import war.metaphor.mutator.data.strings.polymorphic.math.IPolymorphicMath;
import war.metaphor.util.asm.BytecodeUtil;
import war.metaphor.util.interfaces.IRandom;

/**
 * @author jan
 */
public class ReminderShift implements IPolymorphicMath, IRandom {
    private final int left = RANDOM.nextInt(1) + 1;
    private final int right = RANDOM.nextInt(1) + 1;
    private final int rem = RANDOM.nextInt(5) + 1;

    @Override
    public int apply(int input) {
        if (input % rem == 0) {
            input <<= left;
        } else {
            input >>= right;
        }
        return input;
    }

    @Override
    public InsnList dump() {
        InsnList list = new InsnList();

        LabelNode r = new LabelNode();
        LabelNode end = new LabelNode();

        list.add(new InsnNode(Opcodes.DUP));
        list.add(BytecodeUtil.makeInteger(rem));
        list.add(new InsnNode(Opcodes.IREM));
        list.add(new JumpInsnNode(Opcodes.IFNE, r));
        list.add(BytecodeUtil.makeInteger(left));
        list.add(new InsnNode(Opcodes.ISHL));
        list.add(new JumpInsnNode(Opcodes.GOTO, end));
        list.add(r);
        list.add(BytecodeUtil.makeInteger(right));
        list.add(new InsnNode(Opcodes.ISHR));
        list.add(end);

        return list;
    }
}
