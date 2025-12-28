package war.metaphor.mutator.data.strings;

import org.objectweb.asm.tree.AbstractInsnNode;
import org.objectweb.asm.tree.InsnList;
import org.objectweb.asm.tree.LdcInsnNode;
import org.objectweb.asm.tree.MethodNode;
import war.configuration.ConfigurationSection;
import war.jnt.annotate.Level;
import war.jnt.annotate.Stability;
import war.metaphor.base.ObfuscatorContext;
import war.metaphor.mutator.Mutator;
import war.metaphor.mutator.data.strings.polymorphic.PolymorphicStringMethod;
import war.metaphor.mutator.misc.LiftInitializersMutator;
import war.metaphor.tree.JClassNode;
import war.metaphor.util.asm.BytecodeUtil;

import java.lang.reflect.Modifier;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * String encryption, not much else to say tbh
 *
 * @author jan
 */
@Stability(Level.HIGH)
public class StringMutator extends Mutator {

    public StringMutator(ObfuscatorContext base, ConfigurationSection config) {
        super(base, config);
    }

    @Override
    public void run(ObfuscatorContext base) {
        AtomicBoolean fucked = new AtomicBoolean(false);

        for (JClassNode node : base.getClasses()) {
            if (node.isExempt()) continue;
            if (node.isInterface()) continue;

            PolymorphicStringMethod decryptor = PolymorphicStringMethod.generate(node);
            AtomicBoolean modified = new AtomicBoolean();

            for (MethodNode method : node.methods) {
                if (Modifier.isAbstract(method.access)) continue;
                if (node.isExempt(method)) continue;

                BytecodeUtil.translateConcatenation(method);

                int size = BytecodeUtil.leeway(method);
                for (AbstractInsnNode instruction : method.instructions) {
                    if (size < 30000)
                        break;

                    if (instruction instanceof LdcInsnNode ldc) {
                        if (ldc.cst instanceof String s) {
                            if (s.length() < 2) continue;
                            InsnList insnList = decryptor.storeString(s, node.getLiftedName(method.name));
                            method.instructions.insert(ldc, insnList);
                            method.instructions.remove(ldc);
                            modified.set(true);
                        }
                    }

                    size = BytecodeUtil.leeway(method);
                }
            }

            if (modified.get()) {
                decryptor.addMethod();
                fucked.set(true);
            }
        }

        if (fucked.get()) {
            LiftInitializersMutator.crashReasons.add("Polymorphic String Mutation");
        }
    }
}
