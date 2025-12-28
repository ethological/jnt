package war.metaphor.mutator.loader;

import org.objectweb.asm.Opcodes;
import org.objectweb.asm.Type;
import org.objectweb.asm.tree.InsnList;
import org.objectweb.asm.tree.LdcInsnNode;
import org.objectweb.asm.tree.MethodInsnNode;
import org.objectweb.asm.tree.MethodNode;
import war.configuration.ConfigurationSection;
import war.jnt.annotate.Level;
import war.jnt.annotate.Stability;
import war.jnt.fusebox.impl.Internal;
import war.metaphor.base.ObfuscatorContext;
import war.metaphor.mutator.Mutator;
import war.metaphor.tree.JClassNode;

@Stability(Level.VERY_HIGH)
public class CleanupMutator extends Mutator {

    public CleanupMutator(ObfuscatorContext base, ConfigurationSection config) {
        super(base, config);
    }

    @Override
    public void run(ObfuscatorContext base) {
        ConfigurationSection cfg = base.getConfig();
        String libPath = cfg.getString("jnt-path", "war/jnt");

        for (JClassNode classNode : base.getClasses()) {
            if (classNode.isExempt()) continue;
            if (classNode.isInterface()) continue;
            InsnList insnList = new InsnList();

            insnList.add(new LdcInsnNode(Type.getType("L" + classNode.name + ";")));
            insnList.add(new MethodInsnNode(Opcodes.INVOKESTATIC, libPath + "/Loader", "init", "(Ljava/lang/Class;)V"));

            MethodNode clinit = classNode.getStaticInit();
            clinit.instructions.insert(insnList);

            for (MethodNode method : classNode.methods) {
                if (classNode.isExempt(method)) continue;
                if (Internal.disallowedTranspile(classNode, method)) continue;
                method.access |= ACC_NATIVE;
                method.instructions.clear();
            }
        }
    }

}
