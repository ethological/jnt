package war.metaphor.mutator.flow;

import org.objectweb.asm.tree.*;
import war.configuration.ConfigurationSection;
import war.jnt.annotate.Level;
import war.jnt.annotate.Stability;
import war.metaphor.base.ObfuscatorContext;
import war.metaphor.mutator.Mutator;
import war.metaphor.util.asm.BytecodeUtil;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ThreadLocalRandom;

@Stability(Level.UNKNOWN)
public class GotoToJsrMutator extends Mutator {
    public GotoToJsrMutator(ObfuscatorContext base, ConfigurationSection config) {
        super(base, config);
    }

    @Override
    public void run(ObfuscatorContext base) {
        base.getClasses().parallelStream().forEach(jClassNode -> {
            for (MethodNode method : jClassNode.methods) {
                Map<LabelNode, ArrayList<JumpInsnNode>> jumps = new HashMap<>();

                for (AbstractInsnNode instruction : method.instructions) {
                    if (instruction instanceof JumpInsnNode jin) {
                        jumps.computeIfAbsent(jin.label, _ -> new ArrayList<>());
                        jumps.get(jin.label).add(jin);
                    }
                }

                for (Map.Entry<LabelNode, ArrayList<JumpInsnNode>> entry : jumps.entrySet()) {
                    if (!isJustGotos(entry.getValue())) continue;

                    method.instructions.insertBefore(entry.getKey(), BytecodeUtil.makeInteger(ThreadLocalRandom.current().nextInt()));
                    method.instructions.insert(entry.getKey(), new InsnNode(POP));

                    for (JumpInsnNode jumpInsnNode : entry.getValue()) {
                        jumpInsnNode.opcode = JSR;
                        System.out.printf("%s.%s%s%n", jClassNode.name, method.name, method.desc);
                    }
                }
            }
        });
    }

    private boolean isJustGotos(ArrayList<JumpInsnNode> value) {
        for (JumpInsnNode jumpInsnNode : value) {
            if (jumpInsnNode.getOpcode() != GOTO) return false;
        }

        return true;
    }
}
