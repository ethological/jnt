package war.metaphor.mutator.optimization;

import org.objectweb.asm.Type;
import org.objectweb.asm.tree.*;
import war.configuration.ConfigurationSection;
import war.jnt.annotate.Level;
import war.jnt.annotate.Stability;
import war.metaphor.base.ObfuscatorContext;
import war.metaphor.mutator.Mutator;
import war.metaphor.tree.JClassNode;

import java.lang.reflect.Modifier;
import java.util.HashSet;

@Stability(Level.UNKNOWN)
public class UnusedClassMutator extends Mutator {
    public UnusedClassMutator(ObfuscatorContext base, ConfigurationSection config) {
        super(base, config);
    }

    @Override
    public void run(ObfuscatorContext base) {
        final HashSet<JClassNode> toRemove = new HashSet<>();

        int beforeSize;
        do {
            beforeSize = toRemove.size();

            for (JClassNode clazz : base.getClasses()) {
                if (canRemove(base, clazz)) {
                    toRemove.add(clazz);
                }
            }

            toRemove.forEach(base.getClasses()::remove);
        } while (beforeSize != toRemove.size());
    }

    private static boolean canRemove(final ObfuscatorContext base, final JClassNode clazz) {
        for (MethodNode method : clazz.methods) {
            if (!method.name.equals("main")) continue;
            if (!method.desc.equals("([Ljava/lang/String;)V")) continue;
            if (!Modifier.isStatic(method.access)) continue;
            return false;
        }
        for (final JClassNode cn : base.getClasses()) {
            if (cn.superName.equals(clazz.name)) {
                return false;
            }
            for (final MethodNode mn : cn.methods) {
                for (final AbstractInsnNode instruction : mn.instructions) {
                    if (instruction instanceof MethodInsnNode min) {
                        if (min.owner.equals(clazz.name)) {
                            return false;
                        }
                    } else if (instruction instanceof FieldInsnNode fin) {
                        if (fin.owner.equals(clazz.name)) {
                            return false;
                        }
                    } else if (instruction instanceof LdcInsnNode ldc) {
                        if (ldc.cst instanceof Type type) {
                            if (type.getClassName().contains(clazz.name)) {
                                return false;
                            }
                        }
                    } else if (instruction instanceof InvokeDynamicInsnNode indy) {
                        if (indy.bsm.owner.equals(clazz.name)) {
                            return false;
                        } else {
                            for (final Object bsmArg : indy.bsmArgs) {
                                if (bsmArg instanceof String s) {
                                    if (s.contains(clazz.name)) {
                                        return false;
                                    }
                                }
                            }
                        }
                    } else if (instruction instanceof TypeInsnNode tin) {
                        if (tin.getOpcode() == CHECKCAST && tin.desc.equals(clazz.name)) {
                            return false;
                        }
                    }
                }
            }
        }

        return true;
    }
}
