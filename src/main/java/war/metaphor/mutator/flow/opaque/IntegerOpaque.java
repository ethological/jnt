package war.metaphor.mutator.flow.opaque;

import org.objectweb.asm.Type;
import org.objectweb.asm.tree.AbstractInsnNode;
import org.objectweb.asm.tree.InsnList;
import org.objectweb.asm.tree.MethodNode;
import war.metaphor.analysis.graph.Block;
import war.metaphor.util.asm.BytecodeUtil;

public class IntegerOpaque extends Opaque {
    @Override
    public void handle(MethodNode method, Block block, int predicateLocal) {
        for (AbstractInsnNode instruction : block.getInstructions()) {
            if (BytecodeUtil.isLong(instruction)) {
                long value = BytecodeUtil.getLong(instruction);
                InsnList list = BytecodeUtil.generateSeeded(predicateLocal, value, block.getSeed(), Type.LONG_TYPE, Type.INT_TYPE);
                method.instructions.insert(instruction, list);
                method.instructions.remove(instruction);
            } else if (BytecodeUtil.isInteger(instruction)) {
                long lvalue = BytecodeUtil.getInteger(instruction);
                InsnList list = BytecodeUtil.generateSeeded(predicateLocal, lvalue, block.getSeed(), Type.INT_TYPE, Type.INT_TYPE);
                method.instructions.insert(instruction, list);
                method.instructions.remove(instruction);
            }
        }
    }
}
