package war.metaphor.mutator.flow.opaque;

import org.objectweb.asm.tree.AbstractInsnNode;
import org.objectweb.asm.tree.LookupSwitchInsnNode;
import org.objectweb.asm.tree.MethodNode;
import war.metaphor.analysis.graph.Block;
import war.metaphor.util.asm.BytecodeUtil;
import war.metaphor.util.builder.InsnListBuilder;

public class SwitchOpaque extends Opaque {
    @Override
    public void handle(MethodNode method, Block block, int predicateLocal) {
        for (AbstractInsnNode instruction : block.getInstructions()) {
            if (instruction instanceof LookupSwitchInsnNode node) {
                node.keys.replaceAll(i -> i ^ block.getSeed());
                BytecodeUtil.fixLookupSwitch(node);
                method.instructions.insertBefore(instruction, InsnListBuilder
                        .builder()
                        .iload(predicateLocal)
                        .ixor()
                        .build());
            }
        }
    }
}
