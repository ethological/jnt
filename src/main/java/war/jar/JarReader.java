package war.jar;

import lombok.Getter;
import lombok.SneakyThrows;
import org.apache.commons.io.IOUtils;
import org.objectweb.asm.ClassReader;
import war.configuration.ConfigurationSection;
import war.jnt.dash.Ansi;
import war.jnt.dash.Level;
import war.jnt.dash.Logger;
import war.jnt.dash.Origin;
import war.metaphor.tree.JClassNode;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.jar.JarFile;
import java.util.jar.Manifest;
import java.util.stream.Stream;

import static war.jnt.dash.Ansi.Color.WHITE;

@Getter
public class JarReader {

    private final Set<JClassNode> classes = ConcurrentHashMap.newKeySet();
    private final Set<JClassNode> libraries = ConcurrentHashMap.newKeySet();
    private final Set<JarResource> resources = ConcurrentHashMap.newKeySet();

    private File input;
    private final List<File> libs = new ArrayList<>();
    private Manifest manifest;

    @SneakyThrows
    public void load(String path, ConfigurationSection config) {
        Logger logger = Logger.INSTANCE;

        clear();

        this.input = new File(path);

        for (String lib : config.getStringList("libraries")) {
            File file = new File(lib);
            if (file.exists()) this.libs.add(file);
        }

        long start = System.currentTimeMillis();
        loadInput();
        logger.logln(Level.INFO, Origin.INTAKE, String.format("Loaded input (%s)",
                new Ansi().c(WHITE).s(String.format("%dms", System.currentTimeMillis() - start))));

        start = System.currentTimeMillis();
        loadLibrariesFast();
        logger.logln(Level.INFO, Origin.INTAKE, String.format("Loaded libraries (%s)",
                new Ansi().c(WHITE).s(String.format("%dms", System.currentTimeMillis() - start))));

//        List<String> mappingFiles = config.getStringList("mappings");
//        if (mappingFiles != null && !mappingFiles.isEmpty()) {
//            applyMappings(mappingFiles, logger);
//        }

        classes.parallelStream().forEach(classNode -> {
            String sourceFile = classNode.sourceFile;
            if (sourceFile != null && sourceFile.startsWith("pass::jnt:")) {
                classNode.setRealName(sourceFile.substring(10));
            }
        });
    }

    private void loadLibrariesFast() {
        List<File> allJars = new ArrayList<>();
        for (File lib : libs) {
            if (lib.isFile() && lib.getName().endsWith(".jar")) {
                allJars.add(lib);
            } else if (lib.isDirectory()) {
                try (Stream<Path> walk = Files.walk(lib.toPath())) {
                    walk.filter(p -> p.toString().endsWith(".jar"))
                            .map(Path::toFile)
                            .forEach(allJars::add);
                } catch (IOException e) {
                    e.printStackTrace(System.err);
                }
            }
        }

        allJars.parallelStream().forEach(file -> loadJarFile(file, true));
    }

    private void loadJarFile(File file, boolean isLibrary) {
        try (JarFile jar = new JarFile(file)) {
            jar.stream().parallel().forEach(entry -> {
                if (entry.isDirectory()) return;
                if (entry.getName().endsWith(".class")) {
                    try (InputStream is = jar.getInputStream(entry)) {
                        try {
                            ClassReader cr = new ClassReader(is);
                            JClassNode node = new JClassNode(isLibrary);
                            cr.accept(node, (isLibrary ? ClassReader.SKIP_DEBUG : 0) | ClassReader.SKIP_FRAMES);
                            if (isLibrary) libraries.add(node);
                            else classes.add(node);
                        } catch (Exception e) {
                            if (!isLibrary) resources.add(new JarResource(entry.getName(), IOUtils.toByteArray(is)));
                            Logger.INSTANCE.logln(Level.WARNING, Origin.INTAKE, "Parsed class as resource: " + entry.getName());
                        }
                    } catch (Exception e) {
                        e.printStackTrace(System.err);
                        Logger.INSTANCE.logln(Level.ERROR, Origin.INTAKE, "Failed to parse class: " + entry.getName());
                    }
                } else if (!isLibrary) {
                    try (InputStream is = jar.getInputStream(entry)) {
                        if (entry.getName().equals(JarFile.MANIFEST_NAME)) {
                            manifest = new Manifest(is);
                        } else {
                            resources.add(new JarResource(entry.getName(), IOUtils.toByteArray(is)));
                        }
                    } catch (Exception e) {
                        e.printStackTrace(System.err);
                        Logger.INSTANCE.logln(Level.ERROR, Origin.INTAKE, "Failed to load resource: " + entry.getName());
                    }
                }
            });
        } catch (IOException e) {
            Logger.INSTANCE.logln(Level.WARNING, Origin.INTAKE, "Failed to load file: " + file.getName());
        }
    }

    private void loadInput() {
        if (!input.exists() || !input.isFile()) throw new RuntimeException("Invalid input file");
        loadJarFile(input, false);
    }

//    private void applyMappings(List<String> mappingFiles, Logger logger) {
//        logger.logln(Level.INFO, Origin.METAPHOR, String.format("Loading mappings from %s", new Ansi().c(WHITE).s(mappingFiles)));
//        Gson gson = new Gson();
//
//        for (String mappingPath : mappingFiles) {
//            File mappingFile = new File(mappingPath);
//            if (!mappingFile.exists()) continue;
//
//            try (FileReader fr = new FileReader(mappingFile);
//                 JsonReader reader = new JsonReader(fr)) {
//
//                JsonObject root = gson.fromJson(reader, JsonObject.class);
//                if (!root.has("classes")) continue;
//
//                JsonObject classesObj = root.getAsJsonObject("classes");
//
//                for (Map.Entry<String, JsonElement> entry : classesObj.entrySet()) {
//                    String originalName = entry.getKey();
//                    String mappedName = entry.getValue().getAsString();
//
//                    JClassNode classNode = classMap.get(mappedName);
//
//                    if (classNode != null) {
//                        classNode.setRealName(originalName);
//                    } else {
//                        //TODO: IDK, this was for grunt hsit
//                    }
//                }
//            } catch (IOException e) {
//                e.printStackTrace();
//            }
//        }
//    }

    public void clear() {
        classes.clear();
        libraries.clear();
        resources.clear();
        libs.clear();
        input = null;
        manifest = null;
    }
}
