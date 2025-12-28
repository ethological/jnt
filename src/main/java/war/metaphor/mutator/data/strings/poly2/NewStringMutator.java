package war.metaphor.mutator.data.strings.poly2;

import org.objectweb.asm.ClassReader;
import org.objectweb.asm.tree.*;
import war.configuration.ConfigurationSection;
import war.metaphor.base.ObfuscatorContext;
import war.metaphor.mutator.Mutator;
import war.metaphor.mutator.data.strings.poly2.decryptionMethod.DecryptionMethod;
import war.metaphor.mutator.data.strings.poly2.decryptionMethod.args.AbstractDecryptionMethodArgument;
import war.metaphor.mutator.data.strings.poly2.decryptionMethod.args.impl.StringArgument;
import war.metaphor.mutator.data.strings.poly2.decryptionMethod.args.impl.integer.IntegerArgument;
import war.metaphor.mutator.data.strings.poly2.init.Initializer;
import war.metaphor.mutator.flow.BlockBreakMutator;
import war.metaphor.mutator.flow.ControlFlowFlatteningMutator;
import war.metaphor.mutator.loader.IntegrateLoaderMutator;
import war.metaphor.mutator.misc.ClassRenameMutator;
import war.metaphor.tree.JClassNode;
import war.metaphor.util.Pair;
import war.metaphor.util.asm.BytecodeUtil;

import java.io.InputStream;
import java.util.Map;
import java.util.Set;

/**
 * Okay so this is my 4th (I think) try of not ragequitting the
 * fact that string encryption does indeed need a rewrite
 * @see war.metaphor.mutator.data.strings.StringMutator
 * @author Jan
 */
public final class NewStringMutator extends Mutator
{
    private boolean needsCryptoClass;

    public NewStringMutator(final ObfuscatorContext base,
                            final ConfigurationSection config)
    {
        super(base, config);
    }

    @Override
    public void run(final ObfuscatorContext base)
    {

        ConfigurationSection cfg = base.getConfig();
        String libPath = cfg.getString("jnt-path", "war/jnt");

        base.getClasses().forEach(jClassNode -> {
            if (jClassNode.isExempt()) return;
            if (jClassNode.isInterface()) return;

            boolean added = false;

            final DecryptionMethod method = new DecryptionMethod(jClassNode);

            final MethodNode methodNode = method.toMethodNode();

            for (final MethodNode node : jClassNode.methods)
            {
                if (jClassNode.isExempt(node)) continue;

                for (final AbstractInsnNode instruction : node.instructions)
                {
                    if (instruction instanceof LdcInsnNode ldc)
                    {
                        if (ldc.cst instanceof String str)
                        {
                            added = true;
                            final InsnList insns = makeCall(jClassNode, method, methodNode, str);

                            node.instructions.insertBefore(instruction, insns);
                            node.instructions.remove(instruction);
                        }
                    }
                }
            }

            if (added)
            {
                jClassNode.methods.add(methodNode);
                jClassNode.fields.add(method.initField);
                jClassNode.fields.add(method.cacheField);

                final Initializer initializer = method.makeInitializer();
                final MethodNode clinit = jClassNode.getStaticInit();
                clinit.instructions.insertBefore(clinit.instructions.getFirst(), initializer.code);
            }

            if (method.needsCryptoClass)
            {
                needsCryptoClass = true;
            }
        });

        if (needsCryptoClass)
        {
            try (InputStream resourceAsStream = IntegrateLoaderMutator.class.getResourceAsStream("/war/jnt/crypto/Crypto.class")) {
                if (resourceAsStream == null)
                    throw new RuntimeException("Failed to load Crypto class");
                byte[] bytes = resourceAsStream.readAllBytes();
                ClassReader cr = new ClassReader(bytes);
                JClassNode cn = new JClassNode();
                cr.accept(cn, ClassReader.SKIP_FRAMES);
                cn.version = V1_8;

                // i am fairly certain this is not needed
/*
                BlockBreakMutator blockBreakMutator = new BlockBreakMutator(base, null);
                ControlFlowFlatteningMutator flatteningMutator = new ControlFlowFlatteningMutator(base, null);

                blockBreakMutator.run(ObfuscatorContext.builder().classes(Set.of(cn)).build());
                flatteningMutator.run(ObfuscatorContext.builder().classes(Set.of(cn)).build());
*/
                base.addClass(cn);

                cn.name = libPath + "/crypto/Crypto";
                ClassRenameMutator renamer = new ClassRenameMutator(base, null);
                renamer.map(base, Map.of("war/jnt/crypto/Crypto", libPath + "/crypto/Crypto"));

            } catch (Throwable t) {
                throw new RuntimeException("Failed to load Crypto class", t);
            }
        }

        try (InputStream resourceAsStream = IntegrateLoaderMutator.class.getResourceAsStream("/war/jnt/base64/Base64.class")) {
            if (resourceAsStream == null)
                throw new RuntimeException("Failed to load Base64 class");
            byte[] bytes = resourceAsStream.readAllBytes();
            ClassReader cr = new ClassReader(bytes);
            JClassNode cn = new JClassNode();
            cr.accept(cn, ClassReader.SKIP_FRAMES);
            cn.version = V1_8;

            BlockBreakMutator blockBreakMutator = new BlockBreakMutator(base, null);
            ControlFlowFlatteningMutator flatteningMutator = new ControlFlowFlatteningMutator(base, null);

            blockBreakMutator.run(ObfuscatorContext.builder().classes(Set.of(cn)).build());
            flatteningMutator.run(ObfuscatorContext.builder().classes(Set.of(cn)).build());

            base.addClass(cn);

            cn.name = libPath + "/base64/Base64";
            ClassRenameMutator renamer = new ClassRenameMutator(base, null);
            renamer.map(base, Map.of("war/jnt/base64/Base64", libPath + "/base64/Base64"));

        } catch (Throwable t) {
            throw new RuntimeException("Failed to load Base64 class", t);
        }
    }

    private static InsnList makeCall(final JClassNode jClassNode,
                                     final DecryptionMethod method,
                                     final MethodNode methodNode,
                                     final String str)
    {
        final InsnList list = new InsnList();

        for (final Pair<AbstractDecryptionMethodArgument, Object> arg : method.storeString(str))
        {
            switch (arg.a)
            {
                case IntegerArgument _ -> list.add(BytecodeUtil.makeInteger((int)arg.b));
                case StringArgument _ -> list.add(new LdcInsnNode(arg.b));
                default -> throw new IllegalStateException("Illegal argument while trying to build call");
            }
        }

        list.add(new MethodInsnNode(INVOKESTATIC, jClassNode.name, methodNode.name, methodNode.desc));
        //list.add(new InsnNode(POP)); // yes

        list.add(method.returnType.getUnpackingCode());

        return list;
    }
}
