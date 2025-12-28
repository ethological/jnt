package war.metaphor.mutator.misc;

import org.objectweb.asm.Opcodes;
import org.objectweb.asm.tree.FieldNode;
import war.configuration.ConfigurationSection;
import war.jnt.annotate.Level;
import war.jnt.annotate.Stability;
import war.metaphor.base.ObfuscatorContext;
import war.metaphor.mutator.Mutator;
import war.metaphor.tree.JClassNode;

@Stability(Level.VERY_HIGH)
public class WatermarkMutator extends Mutator {

    public WatermarkMutator(ObfuscatorContext base, ConfigurationSection config) {
        super(base, config);
    }

    @Override
    public void run(ObfuscatorContext base) {
        for (JClassNode classNode : base.getClasses()) {
            if (classNode.isExempt()) continue;
            if (classNode.isInterface()) continue;
            classNode.fields.add(new FieldNode(
                    Opcodes.ACC_TRANSIENT,
                    "myMetaphor",
                    "Ljava/lang/String;",
                    null,
                    "myMetaphor @ jnt.so"
            ));
        }
    }

}
