package war.metaphor.mutator.misc;

import org.objectweb.asm.tree.*;
import war.configuration.ConfigurationSection;
import war.jnt.annotate.Level;
import war.jnt.annotate.Stability;
import war.metaphor.base.ObfuscatorContext;
import war.metaphor.mutator.Mutator;
import war.metaphor.tree.JClassNode;

import java.util.List;

@Stability(Level.HIGH)
public class BootstrapEntryMutator extends Mutator {
    public BootstrapEntryMutator(ObfuscatorContext base, ConfigurationSection config) {
        super(base, config);
    }

    @Override
    public void run(ObfuscatorContext base) {
        for (JClassNode jcn : base.getClasses()) {
            for (MethodNode mn : jcn.methods) {
                if (mn.name.equals("main")) {
                    MethodNode bsm = buildBootstrap(mn.instructions, mn.signature, mn.exceptions);
                    jcn.methods.add(bsm);

                    InsnList list = new InsnList();
                    list.add(new VarInsnNode(ALOAD, 0));
                    list.add(new MethodInsnNode(
                            INVOKESTATIC,
                            jcn.name,
                            "__bootstrap_jnt",
                            "([Ljava/lang/String;)V"
                    ));
                    list.add(new InsnNode(RETURN));

                    mn.instructions.clear();
                    mn.instructions.insert(list);

                    mn.tryCatchBlocks = null;
                    mn.exceptions = null;
                    break;
                }
            }
        }
    }

    private MethodNode buildBootstrap(InsnList instructions, String sig, List<String> exceptions) {
        MethodNode bsm = new MethodNode(
                ACC_PUBLIC | ACC_STATIC,
                "__bootstrap_jnt",
                "([Ljava/lang/String;)V",
                sig,
                exceptions.toArray(new String[0])
        );
        bsm.instructions.insert(instructions);
        return bsm;
    }
}
