package war.jar;

import lombok.AllArgsConstructor;
import war.jnt.dash.Ansi;
import war.jnt.dash.Level;
import war.jnt.dash.Logger;
import war.jnt.dash.Origin;
import war.metaphor.tree.JClassNode;

import java.io.BufferedOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Set;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;
import java.util.jar.JarOutputStream;
import java.util.jar.Manifest;
import java.util.zip.ZipEntry;

import static war.jnt.dash.Ansi.Color.RED;
import static war.jnt.dash.Ansi.Color.WHITE;

@AllArgsConstructor
public class JarWriter {

    private final Set<JClassNode> classes;
    private final Set<JarResource> resources;
    private final Manifest manifest;

    public void write(Path in, Path out) {
        try (OutputStream fileOut = Files.newOutputStream(out);
             BufferedOutputStream bos = new BufferedOutputStream(fileOut, 65536);
             JarOutputStream jos = new JarOutputStream(bos)) {

            jos.setLevel(9);

            if (manifest != null) {
                ZipEntry entry = new ZipEntry(JarFile.MANIFEST_NAME);
                jos.putNextEntry(entry);
                manifest.write(jos);
                jos.closeEntry();
            }

            classes.parallelStream().forEach(node -> {
                try {
                    byte[] bytes = node.compute();
                    String entryName = node.name + ".class";
                    synchronized (jos) {
                        jos.putNextEntry(new JarEntry(entryName));
                        jos.write(bytes);
                        jos.closeEntry();
                    }
                } catch (Exception e) {
                    Logger.INSTANCE.logln(Level.FATAL, Origin.INTAKE,
                            String.format("Failed to write class %s", new Ansi().c(RED).s(node.name)));
                }
            });

            resources.parallelStream().forEach(resource -> {
                try {
                    byte[] content = resource.content();
                    String entryName = resource.name();
                    synchronized (jos) {
                        jos.putNextEntry(new JarEntry(entryName));
                        jos.write(content);
                        jos.closeEntry();
                    }
                } catch (Exception e) {
                    Logger.INSTANCE.logln(Level.FATAL, Origin.INTAKE,
                            String.format("Failed to write resource %s", new Ansi().c(RED).s(resource.name())));
                }
            });

        } catch (IOException e) {
            throw new RuntimeException("Disk IO failure", e);
        }

        try {
            long inputLength = Files.size(in);
            long outputLength = Files.size(out);

            if (inputLength == 0) inputLength = 1;

            double ratio = ((double) outputLength - (double) inputLength) / (double) inputLength;
            double diff = Math.abs(outputLength - inputLength) / 1024D;

            Logger.INSTANCE.logln(Level.INFO, Origin.INTAKE,
                    String.format("File size changed by %s, (%s -> %s, %s)",
                            new Ansi().c(WHITE).s(String.format("%.2f%%", ratio * 100)),
                            new Ansi().c(WHITE).s(String.format("%.2fKB", inputLength / 1024D)),
                            new Ansi().c(WHITE).s(String.format("%.2fKB", outputLength / 1024D)),
                            new Ansi().c(WHITE).s(String.format("%s%.2fKB", ratio > 0 ? "+" : "-", diff))));

        } catch (IOException e) {
            Logger.INSTANCE.logln(Level.ERROR, Origin.INTAKE, "Failed to calculate file stats: " + e.getMessage());
        }
    }
}
