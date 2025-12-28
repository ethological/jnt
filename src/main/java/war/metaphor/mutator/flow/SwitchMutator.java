package war.metaphor.mutator.flow;

import org.objectweb.asm.tree.*;
import war.configuration.ConfigurationSection;
import war.jnt.annotate.Level;
import war.jnt.annotate.Stability;
import war.metaphor.base.ObfuscatorContext;
import war.metaphor.engine.Engine;
import war.metaphor.engine.types.IntegerEngine;
import war.metaphor.mutator.Mutator;
import war.metaphor.tree.JClassNode;
import war.metaphor.util.asm.BytecodeUtil;

import java.lang.reflect.Modifier;

@Stability(Level.HIGH)
public class SwitchMutator extends Mutator {

    public SwitchMutator(ObfuscatorContext base, ConfigurationSection config) {
        super(base, config);
    }

    @Override
    public void run(ObfuscatorContext base) {
        for (JClassNode classNode : base.getClasses()) {
            if (classNode.isExempt()) continue;
            for (MethodNode method : classNode.methods) {
                if (Modifier.isAbstract(method.access)) continue;
                if (classNode.isExempt(method)) continue;

                int leeway = BytecodeUtil.leeway(method);
                if (leeway < 30000)
                    break;
                for (AbstractInsnNode instruction : method.instructions) {
                    if (instruction instanceof TableSwitchInsnNode node) {
                        int[] keys = new int[node.labels.size()];
                        for (int i = 0; i < node.labels.size(); i++) {
                            keys[i] = node.min + i;
                        }
                        LookupSwitchInsnNode switchInsnNode = new LookupSwitchInsnNode(
                                node.dflt,
                                keys,
                                node.labels.toArray(new LabelNode[0])
                        );
                        BytecodeUtil.fixLookupSwitch(switchInsnNode);
                        method.instructions.insertBefore(node, switchInsnNode);
                        method.instructions.remove(node);
                    }
                }
                for (AbstractInsnNode instruction : method.instructions) {
                    if (instruction instanceof LookupSwitchInsnNode node) {

                        InsnList instructions = new InsnList();

                        Engine engine = new IntegerEngine(6);
                        instructions.add(engine.getForwardInstructions());

                        method.instructions.insertBefore(node, instructions);

                        node.keys.replaceAll(
                                engine::run
                        );

                        BytecodeUtil.fixLookupSwitch(node);
                    }
                }
            }
        }
    }
}
