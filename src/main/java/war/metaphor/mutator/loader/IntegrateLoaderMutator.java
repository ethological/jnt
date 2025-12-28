package war.metaphor.mutator.loader;

import org.objectweb.asm.ClassReader;
import org.objectweb.asm.tree.AbstractInsnNode;
import org.objectweb.asm.tree.LdcInsnNode;
import org.objectweb.asm.tree.MethodNode;
import war.configuration.ConfigurationSection;
import war.jar.JarResource;
import war.jnt.annotate.Stability;
import war.jnt.dash.Ansi;
import war.jnt.dash.Level;
import war.jnt.dash.Logger;
import war.jnt.dash.Origin;
import war.metaphor.base.ObfuscatorContext;
import war.metaphor.mutator.Mutator;
import war.metaphor.mutator.flow.BlockBreakMutator;
import war.metaphor.mutator.flow.ControlFlowFlatteningMutator;
import war.metaphor.mutator.misc.ClassRenameMutator;
import war.metaphor.tree.JClassNode;

import java.io.File;
import java.io.InputStream;
import java.nio.file.Files;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

@Stability(war.jnt.annotate.Level.VERY_HIGH)
public class IntegrateLoaderMutator extends Mutator {

    public IntegrateLoaderMutator(ObfuscatorContext base, ConfigurationSection config) {
        super(base, config);
    }

    @Override
    public void run(ObfuscatorContext base) {
        ConfigurationSection cfg = base.getConfig();
        String libPath = cfg.getString("jnt-path", "war/jnt");
        try (InputStream resourceAsStream = IntegrateLoaderMutator.class.getResourceAsStream("/war/jnt/Loader.class")) {
            if (resourceAsStream == null)
                throw new RuntimeException("Failed to load Loader class");
            byte[] bytes = resourceAsStream.readAllBytes();
            ClassReader cr = new ClassReader(bytes);
            JClassNode cn = new JClassNode();
            cr.accept(cn, ClassReader.SKIP_FRAMES);
            cn.version = V1_8;

            BlockBreakMutator blockBreakMutator = new BlockBreakMutator(base, null);
            ControlFlowFlatteningMutator flatteningMutator = new ControlFlowFlatteningMutator(base, null);

            blockBreakMutator.run(ObfuscatorContext.builder().classes(Set.of(cn)).build());
            flatteningMutator.run(ObfuscatorContext.builder().classes(Set.of(cn)).build());

            for (AbstractInsnNode instruction : cn.getStaticInit().instructions) {
                if (instruction instanceof LdcInsnNode node) {
                    if (node.cst instanceof String str && str.equals("/war/jnt/")) {
                        node.cst = "/" + libPath + "/";
                    }
                }
            }

            base.addClass(cn);

            cn.name = libPath + "/Loader";
            ClassRenameMutator renamer = new ClassRenameMutator(base, null);
            renamer.map(base, Map.of("war/jnt/Loader", libPath + "/Loader"));

        } catch (Throwable t) {
            throw new RuntimeException("Failed to load Loader class", t);
        }

        for (JClassNode classNode : base.getClasses()) {
            if (classNode.isExempt()) continue;
            if (classNode.isInterface()) continue;
            MethodNode guard = new MethodNode(ACC_PUBLIC | ACC_STATIC | ACC_NATIVE, "guard", "()V", null, null);
            classNode.methods.add(guard);
        }

        for (String targets : cfg.getStringList("targets")) {
            String build = String.format("%s/build/", base.getDir());
            String target = targets.toLowerCase(Locale.ROOT);
            File targetFile = new File(build + "/" + target + "/out.gz");
            if (!targetFile.exists()) {
                Logger.INSTANCE.logln(Level.FATAL, Origin.METAPHOR, String.format("Failed to find target file: %s", new Ansi().c(Ansi.Color.RED).s(targetFile.getAbsolutePath())));
                continue;
            }
            try {
                byte[] bytes = Files.readAllBytes(targetFile.toPath());
                JarResource resource = new JarResource(libPath + "/" + target, bytes);
                base.addResource(resource);
            } catch (Exception ex) {
                Logger.INSTANCE.logln(Level.FATAL, Origin.METAPHOR, String.format("Failed to read target file: %s", new Ansi().c(Ansi.Color.RED).s(targetFile.getAbsolutePath())));
            }
        }
    }
}
