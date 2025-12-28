package war.metaphor.mutator.splash;

import org.objectweb.asm.ClassReader;
import org.objectweb.asm.tree.MethodInsnNode;
import org.objectweb.asm.tree.MethodNode;
import war.configuration.ConfigurationSection;
import war.jnt.annotate.Level;
import war.jnt.annotate.Stability;
import war.metaphor.base.ObfuscatorContext;
import war.metaphor.mutator.Mutator;
import war.metaphor.mutator.loader.IntegrateLoaderMutator;
import war.metaphor.tree.JClassNode;

import java.io.InputStream;

@Stability(Level.UNKNOWN)
public class SplashScreenMutator extends Mutator {
    public SplashScreenMutator(ObfuscatorContext base, ConfigurationSection config) {
        super(base, config);
    }

    @Override
    public void run(ObfuscatorContext base) {

        try (InputStream ssResource = IntegrateLoaderMutator.class.getResourceAsStream("/war/toolkit/SplashScreen.class")) {
            if (ssResource == null)
                throw new RuntimeException("Failed to load SplashScreen class");

            byte[] bytes = ssResource.readAllBytes();
            ClassReader cr = new ClassReader(bytes);
            JClassNode cn = new JClassNode();
            cr.accept(cn, ClassReader.SKIP_FRAMES);
            cn.version = V1_8;
            base.addClass(cn);

            for (JClassNode classNode : base.getClasses()) {
                if (classNode.isExempt()) continue;
                if (classNode.isInterface()) continue;
                MethodNode clinit = classNode.getStaticInit();
                if (clinit == null) continue;
                clinit.instructions.insert(new MethodInsnNode(INVOKESTATIC, cn.name, "load", "()V", false));
            }

        } catch (Throwable t) {
            throw new RuntimeException("Failed to load SplashScreen class", t);
        }
    }
}
