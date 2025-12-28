package war.metaphor.mutator.misc;

import org.objectweb.asm.tree.AbstractInsnNode;
import org.objectweb.asm.tree.MethodInsnNode;
import org.objectweb.asm.tree.MethodNode;
import war.configuration.ConfigurationSection;
import war.jnt.annotate.Level;
import war.jnt.annotate.Stability;
import war.metaphor.base.ObfuscatorContext;
import war.metaphor.mutator.Mutator;
import war.metaphor.tree.JClassNode;

/**
 * Fixes java being weird again (or rather the transpiler not handling this edge case)
 * <p />
 * <a href="https://docs.oracle.com/javase/specs/jvms/se7/html/jvms-6.html#jvms-6.5.invokespecial">jvm spec: "invokespecial"</a>
 * @author Jan
 */
@Stability(Level.UNKNOWN)
public class MethodCallFixer extends Mutator {
    public MethodCallFixer(ObfuscatorContext base, ConfigurationSection config) {
        super(base, config);
    }

    @Override
    public void run(ObfuscatorContext base) {
        for (JClassNode aClass : base.getClasses()) {
            if (aClass.isExempt()) continue;
            for (MethodNode method : aClass.methods) {
                if (aClass.isExempt(method)) continue;

                for (AbstractInsnNode instruction : method.instructions) {
                    if (instruction instanceof MethodInsnNode min) {
                        if (min.getOpcode() == INVOKESPECIAL && !min.name.equals("<init>")) {
                            // System.out.printf("%s.%s%s%n", aClass.name, method.name, method.desc);
                            min.setOpcode(INVOKEVIRTUAL);
                        }
                    }
                }
            }
        }
    }
}
