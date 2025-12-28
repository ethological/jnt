package war.metaphor.mutator.misc;

import org.objectweb.asm.tree.AbstractInsnNode;
import org.objectweb.asm.tree.LineNumberNode;
import org.objectweb.asm.tree.MethodNode;
import war.configuration.ConfigurationSection;
import war.jnt.annotate.Level;
import war.jnt.annotate.Stability;
import war.metaphor.base.ObfuscatorContext;
import war.metaphor.mutator.Mutator;
import war.metaphor.tree.JClassNode;

import java.lang.reflect.Modifier;

@Stability(Level.VERY_HIGH)
public class StripMutator extends Mutator {

    public StripMutator(ObfuscatorContext base, ConfigurationSection config) {
        super(base, config);
    }

    @Override
    public void run(ObfuscatorContext base) {
        for (JClassNode classNode : base.getClasses()) {
            if (classNode.isExempt()) continue;
            classNode.sourceDebug = null;
            classNode.sourceFile = null;
            classNode.innerClasses.clear();
            for (MethodNode method : classNode.methods) {
                if (classNode.isExempt(method)) continue;
                if (Modifier.isAbstract(method.access)) continue;
                for (AbstractInsnNode instruction : method.instructions) {
                    if (instruction instanceof LineNumberNode) {
                        method.instructions.remove(instruction);
                    }
                }
                method.localVariables = null;
                if (method.signature != null && !method.signature.startsWith("pass::jnt")) {
                    method.signature = null;
                }
                method.parameters = null;
            }
            for (var field : classNode.fields) {
                if (classNode.isExempt(field)) continue;
                field.signature = null;
            }
        }
    }

}
