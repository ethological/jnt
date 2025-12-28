package war.metaphor.mutator.integrity.mainCallCheck.math.impl;

import org.objectweb.asm.tree.InsnList;
import org.objectweb.asm.tree.InsnNode;
import war.metaphor.mutator.integrity.mainCallCheck.math.IMath;
import war.metaphor.util.asm.BytecodeUtil;
import war.metaphor.util.interfaces.IRandom;

public class ConstantXORMath implements IMath, IRandom {
    private final int key = RANDOM.nextInt();

    @Override
    public int apply(int input) {
        return input ^ key;
    }

    @Override
    public InsnList dump() {
        InsnList list = new InsnList();

        list.add(BytecodeUtil.makeInteger(key));
        list.add(new InsnNode(IXOR));

        return list;
    }
}
