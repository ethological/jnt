package war.metaphor.mutator.misc;

import org.objectweb.asm.tree.*;
import war.configuration.ConfigurationSection;
import war.jnt.annotate.Level;
import war.jnt.annotate.Stability;
import war.metaphor.base.ObfuscatorContext;
import war.metaphor.mutator.Mutator;
import war.metaphor.tree.JClassNode;

import java.lang.reflect.Modifier;

/**
 * @author ssheera
 * Puts field values into the correct initalising methods
 */
@Stability(Level.UNKNOWN)
public class FieldInlinerMutator extends Mutator {

    public FieldInlinerMutator(ObfuscatorContext base, ConfigurationSection config) {
        super(base, config);
    }

    @Override
    public void run(ObfuscatorContext base) {
        for (JClassNode classNode : base.getClasses()) {
            if (classNode.isExempt()) continue;
            MethodNode clinit = classNode.getStaticInit();
            for (FieldNode field : classNode.fields) {
                if (classNode.isExempt(field)) continue;
                if (field.value != null && Modifier.isStatic(field.access)) {
                    InsnList list = new InsnList();
                    list.add(new LdcInsnNode(field.value));
                    list.add(new FieldInsnNode(PUTSTATIC, classNode.name, field.name, field.desc));
                    clinit.instructions.insert(list);
                    field.value = null;
                }
            }
        }
    }

}
