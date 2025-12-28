package war.metaphor.mutator.flow;

import org.objectweb.asm.tree.*;
import org.objectweb.asm.tree.analysis.BasicValue;
import org.objectweb.asm.tree.analysis.Frame;
import war.configuration.ConfigurationSection;
import war.jnt.annotate.Level;
import war.jnt.annotate.Stability;
import war.metaphor.analysis.graph.ControlFlowGraph;
import war.metaphor.base.ObfuscatorContext;
import war.metaphor.mutator.Mutator;
import war.metaphor.tree.JClassNode;
import war.metaphor.util.asm.BytecodeUtil;

import java.lang.reflect.Modifier;
import java.security.SecureRandom;
import java.util.Map;

/**
 * @author etho
 */
@Stability(Level.MEDIUM)
public class TrapEdgeMutator extends Mutator {

    public TrapEdgeMutator(ObfuscatorContext base, ConfigurationSection config) {
        super(base, config);
    }

    @Override
    public void run(ObfuscatorContext base) {
        for (JClassNode classNode : base.getClasses()) {
            if (classNode.isExempt()) continue;
            for (MethodNode method : classNode.methods) {
                if (classNode.isExempt(method)) continue;
                if (Modifier.isAbstract(method.access)) continue;

                ControlFlowGraph graph = new ControlFlowGraph(classNode, method);
                if (!graph.compute()) continue;

                Map<AbstractInsnNode, Frame<BasicValue>> frames = graph.getFrames();

                int leeway = BytecodeUtil.leeway(method);
                for (AbstractInsnNode instruction : method.instructions) {
                    if (leeway < 30000)
                        break;

                    Frame<BasicValue> frame = frames.get(instruction);
                    if (frame == null) continue;

                    if (instruction.getOpcode() != GOTO) continue;

                    if (frame.getStackSize() > 0) continue;

                    LabelNode trapStart = new LabelNode();
                    LabelNode trapEnd = new LabelNode();
                    LabelNode handler = new LabelNode();

                    var list = new InsnList();

                    list.add(new JumpInsnNode(GOTO, trapStart));
                    list.add(trapStart);
                    list.add(new InsnNode(ACONST_NULL));
                    list.add(new InsnNode(ATHROW));
                    list.add(trapEnd);
                    list.add(new InsnNode(NOP));
                    list.add(handler);
                    list.add(new InsnNode(POP));
                    list.add(new LdcInsnNode(new SecureRandom().nextInt()));
                    list.add(new InsnNode(POP));
                    list.add(new JumpInsnNode(GOTO, ((JumpInsnNode) instruction).label));


                    method.tryCatchBlocks.add(new TryCatchBlockNode(
                            trapStart, trapEnd, handler,
                            null
                    ));

                    method.instructions.insertBefore(instruction, list);
                    method.instructions.remove(instruction);

                    leeway = BytecodeUtil.leeway(method);
                }
            }
        }
    }
}
