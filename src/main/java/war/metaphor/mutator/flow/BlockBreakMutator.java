package war.metaphor.mutator.flow;

import org.objectweb.asm.tree.AbstractInsnNode;
import org.objectweb.asm.tree.LabelNode;
import org.objectweb.asm.tree.MethodNode;
import war.configuration.ConfigurationSection;
import war.jnt.annotate.Level;
import war.jnt.annotate.Stability;
import war.metaphor.analysis.graph.Block;
import war.metaphor.analysis.graph.ControlFlowGraph;
import war.metaphor.base.ObfuscatorContext;
import war.metaphor.mutator.Mutator;
import war.metaphor.tree.JClassNode;
import war.metaphor.util.asm.BytecodeUtil;

import java.lang.reflect.Modifier;
import java.util.Collection;
import java.util.List;

@Stability(Level.MEDIUM)
public class BlockBreakMutator extends Mutator {

    public BlockBreakMutator(ObfuscatorContext base, ConfigurationSection config) {
        super(base, config);
    }

    @Override
    public void run(ObfuscatorContext ctx) {

        for (JClassNode classNode : ctx.getClasses()) {
            if (classNode.isExempt()) continue;

            for (MethodNode method : classNode.methods) {
                if (Modifier.isAbstract(method.access)) continue;
                if (classNode.isExempt(method)) continue;
                ControlFlowGraph graph = new ControlFlowGraph(classNode, method);
                if (!graph.compute()) continue;

                Collection<Block> blocks = graph.getBlocks();

                int size = BytecodeUtil.leeway(method);
                for (Block block : blocks) {
                    if (size < 30000)
                        break;

                    List<List<AbstractInsnNode>> sameFrames = graph.groupSameFrames(block.getInstructions());

                    sameFrames.forEach(list -> list.removeIf(insn -> list.stream().anyMatch(otherInsn -> insn != otherInsn && Math.abs(insn.index - otherInsn.index) < 2)));
                    sameFrames.removeIf(list -> list.size() < 2 || list.size() >= 20);

                    sameFrames.forEach(list -> {
                        for (AbstractInsnNode node : list) {
                            LabelNode label = new LabelNode();
                            method.instructions.insertBefore(node, label);
                        }
                    });

                    size = BytecodeUtil.leeway(method);
                }
            }
        }
    }

}
