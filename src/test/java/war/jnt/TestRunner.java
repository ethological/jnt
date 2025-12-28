package war.jnt;

import war.Entrypoint;
import war.configuration.file.YamlConfiguration;
import war.metaphor.base.ObfuscatorContext;
import war.metaphor.tree.Hierarchy;
import war.metaphor.tree.JClassNode;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.jar.JarEntry;
import java.util.jar.JarOutputStream;
import java.util.jar.Manifest;

public class TestRunner {

    private void runPasses(File in, File out, YamlConfiguration config) {

        ObfuscatorContext.INSTANCE = null;
        Hierarchy.INSTANCE = null;

        File temp = new File("src/test/resources/temp");
        if (!temp.exists()) {
            if (!temp.mkdirs()) {
                System.err.println("Failed to create temp directory");
                return;
            }
        }

        config.set("input", in.getAbsolutePath());
        config.set("output", out.getAbsolutePath());

        File tempConfig = new File(temp, "config.yml");
        try {
            config.save(tempConfig);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }

        Entrypoint.main(new String[] {
                "--config", tempConfig.getAbsolutePath(),
                "--metaphor", "false",
                "--transpile", "false",
                "--logger", "false",
        });

    }

    /**
     * @param classNode the class node to run
     * @param config the configuration to use
     * @return the exit code of the process
     */
    public int runTest(JClassNode classNode, YamlConfiguration config) {

        byte[] classBytes = classNode.compute();

        File temp = new File("src/test/resources/temp");
        if (!temp.exists()) {
            if (!temp.mkdirs()) {
                System.err.println("Failed to create temp directory");
                return -1;
            }
        }

        File jarFile = new File(temp, "test.jar");

        try (FileOutputStream fos = new FileOutputStream(jarFile);
             JarOutputStream jos = new JarOutputStream(fos)) {

            JarEntry entry = new JarEntry(classNode.name.replace('.', '/') + ".class");
            jos.putNextEntry(entry);
            jos.write(classBytes);

            Manifest manifest = new Manifest();
            manifest.getMainAttributes().putValue("Manifest-Version", "1.0");
            manifest.getMainAttributes().putValue("Main-Class", classNode.name);
            jos.putNextEntry(new JarEntry("META-INF/MANIFEST.MF"));
            manifest.write(jos);
            jos.closeEntry();

        } catch (Exception e) {
            System.err.println("Error creating JAR: " + e.getMessage());
            e.printStackTrace();
        }

        File out = new File(temp, "test-out.jar");

        runPasses(jarFile, out, config);

        ProcessBuilder processBuilder = new ProcessBuilder("java", "-jar", out.getAbsolutePath());
        processBuilder.redirectErrorStream(true);

        try {
            Process process = processBuilder.start();
            process.waitFor();
            return process.exitValue();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }


    }

}
