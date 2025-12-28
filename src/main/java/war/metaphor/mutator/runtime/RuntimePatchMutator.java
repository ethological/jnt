package war.metaphor.mutator.runtime;

import org.objectweb.asm.Type;
import war.configuration.ConfigurationSection;
import war.jnt.annotate.Level;
import war.jnt.annotate.Stability;
import war.metaphor.base.ObfuscatorContext;
import war.metaphor.mutator.Mutator;
import war.metaphor.util.asm.BytecodeUtil;

import org.objectweb.asm.Opcodes;
import org.objectweb.asm.tree.*;
import org.objectweb.asm.ClassWriter;

import java.util.Base64;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

import war.metaphor.tree.JClassNode;

@Stability(Level.UNKNOWN)
public class RuntimePatchMutator extends Mutator {
    public RuntimePatchMutator(ObfuscatorContext base, ConfigurationSection config) {
        super(base, config);
    }

    @Override
    public void run(ObfuscatorContext base) {
        try {
            var existing = base.loadClass("war/metaphor/mutator/runtime/RuntimePatcher");
            if (existing == null) {
                JClassNode rp = base.loadClass(RuntimePatcher.class);
                if (rp != null) base.addClass(rp);
            } else if (!base.getClasses().contains(existing)) {
                base.addClass(existing);
            }
        } catch (Exception ignored) {}

        AtomicInteger patchIndex = new AtomicInteger();

        for (var jcn : base.getClasses()) {
            if (jcn.isInterface() || jcn.isExempt()) continue;

            MethodNode clinit = jcn.getStaticInit();

            InsnList embedList = new InsnList();

            for (MethodNode method : (List<MethodNode>) jcn.methods) {
                if (method.name.equals("<clinit>")) continue;
                if (method.instructions == null || method.instructions.size() == 0) continue;

                String patchName = jcn.name + "$_patch$" + patchIndex.getAndIncrement();

                ClassNode patchNode = new ClassNode();
                patchNode.version = jcn.version;
                patchNode.access = Opcodes.ACC_PUBLIC | Opcodes.ACC_SUPER;
                patchNode.name = patchName;
                patchNode.superName = "java/lang/Object";

                MethodNode copied = BytecodeUtil.clone(method);
                patchNode.methods.add(copied);

                ClassWriter cw = new ClassWriter(ClassWriter.COMPUTE_FRAMES);
                patchNode.accept(cw);
                byte[] patchBytes = cw.toByteArray();

                String encodedBytes = Base64.getEncoder().encodeToString(patchBytes);

                String methodKey = method.name + method.desc;
                embedList.add(new LdcInsnNode(methodKey + "\0" + encodedBytes));
                embedList.add(new InsnNode(Opcodes.POP)); // discard, should only be an entry in the pool
            }

            if (embedList.size() > 0) {
                embedList.add(new LdcInsnNode(Type.getObjectType(jcn.name)));
                embedList.add(new MethodInsnNode(Opcodes.INVOKESTATIC,
                        "war/metaphor/mutator/runtime/RuntimePatcher",
                        "apply",
                        "(Ljava/lang/Class;)V",
                        false));

                clinit.instructions.insert(embedList);
            }
        }
    }
}
