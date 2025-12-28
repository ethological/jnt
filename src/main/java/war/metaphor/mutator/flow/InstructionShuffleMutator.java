package war.metaphor.mutator.flow;

import org.objectweb.asm.tree.*;
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
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

@Stability(Level.LOW)
public class InstructionShuffleMutator extends Mutator {

    public InstructionShuffleMutator(ObfuscatorContext base, ConfigurationSection config) {
        super(base, config);
    }

    @Override
    public void run(ObfuscatorContext base) {
        for (JClassNode classNode : base.getClasses()) {
            if (classNode.isExempt()) continue;
            apply(classNode);
        }
    }

    public void apply(JClassNode node) {
        for (MethodNode method : node.methods) {
            if (Modifier.isAbstract(method.access)) continue;
            if (node.isExempt(method)) continue;

            int size = BytecodeUtil.leeway(method);
            if (size < 30000)
                continue;

            ControlFlowGraph graph = new ControlFlowGraph(node, method);

            graph.compute();

            if (graph.getBlocks().isEmpty())
                continue;

            Collections.shuffle(graph.getBlocks());

            for (Block block : graph.getBlocks()) {

                if (block.isTrap()) continue;

                List<AbstractInsnNode> instructions = new ArrayList<>(block.getInstructions());
                List<AbstractInsnNode> cloned = new ArrayList<>();

                LabelNode lbl = block.getLabel();
                instructions.addFirst(lbl);

                for (AbstractInsnNode instruction : instructions)
                    cloned.add(graph.clone(instruction));

                for (int i = instructions.size() - 1; i >= 0; i--)
                    method.instructions.remove(instructions.get(i));

                if (block.inTrap()) {
                    BytecodeUtil.addInstructions(method.instructions, block.getLowestTrap().end.previousInsn, cloned);
                } else {
                    BytecodeUtil.addInstructions(method.instructions, cloned);
                }
            }

            method.instructions.insert(new JumpInsnNode(GOTO, graph.getStart()));

            method.localVariables = null;
        }
    }
}
