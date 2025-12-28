package war.metaphor.mutator.misc;

import org.objectweb.asm.tree.FieldNode;
import org.objectweb.asm.tree.MethodNode;
import org.yaml.snakeyaml.nodes.AnchorNode;
import war.configuration.ConfigurationSection;
import war.jnt.annotate.Level;
import war.jnt.annotate.Stability;
import war.metaphor.base.ObfuscatorContext;
import war.metaphor.mutator.Mutator;
import war.metaphor.tree.JClassNode;

import java.util.concurrent.SubmissionPublisher;

@Stability(Level.MEDIUM)
public class AccessUnifyMutator extends Mutator {

    public AccessUnifyMutator(ObfuscatorContext base, ConfigurationSection config) {
        super(base, config);
    }

    @Override
    public void run(ObfuscatorContext base) {
        final boolean exploit = config.getBoolean("exploit", false);
        for (JClassNode classNode : base.getClasses()) {
            if (classNode.isExempt()) continue;
            classNode.access = handle(classNode.access);
            if (classNode.isInterface()) continue;
            for (MethodNode method : classNode.methods) {
                if (classNode.isExempt(method)) continue;
                method.access = handle(method.access);
                if (exploit && !method.name.contains("<")) {
                    method.access = exploitMethod(method.access);
                }
            }
            for (FieldNode field : classNode.fields) {
                if (classNode.isExempt(field)) continue;
                field.access = handle(field.access);
                if (exploit) {
                    field.access = exploitField(field.access);
                }
            }

            if (exploit) {
                classNode.access = exploitClass(classNode.access);
            }
        }
    }

    private int exploitMethod(int access) {
        access |= ACC_SYNTHETIC;
        access |= ACC_DEPRECATED;
        access |= ACC_STRICT;
        access |= ACC_VARARGS;
        access |= ACC_BRIDGE;
        return access;
    }

    private int exploitField(int access) {
        access |= ACC_SYNTHETIC;
        access |= ACC_DEPRECATED;
        access |= ACC_VOLATILE;
        access |= ACC_ENUM;
        return access;
    }

    private int exploitClass(int access) {
        access |= ACC_SYNTHETIC;
        access |= ACC_DEPRECATED;
        access |= ACC_SUPER;
        access |= ACC_OPEN;
        access |= ACC_ENUM;
        // access |= ACC_ABSTRACT; // would require more checks, works tho
        return access;
    }

    private int handle(int access) {
        access |= ACC_PUBLIC;
        access &= ~ACC_PRIVATE;
        access &= ~ACC_PROTECTED;
        access &= ~ACC_FINAL;
        return access;
    }
}
