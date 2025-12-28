package war.metaphor.base;

import lombok.*;
import org.objectweb.asm.ClassReader;
import org.objectweb.asm.tree.MethodNode;
import war.configuration.ConfigurationSection;
import war.jar.JarResource;
import war.jar.JarWriter;
import war.jnt.dash.Ansi;
import war.jnt.dash.Level;
import war.jnt.dash.Logger;
import war.jnt.dash.Origin;
import war.jnt.utility.mapping.MappingRepository;
import war.metaphor.mutator.Mutator;
import war.metaphor.processor.Executor;
import war.metaphor.processor.FilterProcessor;
import war.metaphor.tree.Hierarchy;
import war.metaphor.tree.JClassNode;
import war.metaphor.util.JavaInternalClasses;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.jar.Manifest;

import static war.jnt.dash.Ansi.Color.*;

@Getter
@Setter
@Builder
public final class ObfuscatorContext {

    public static ObfuscatorContext INSTANCE;

    private static final Logger logger = Logger.INSTANCE;

    public Set<JClassNode> classes;
    private Set<JarResource> resources;
    private Set<JClassNode> libraries;

    private Map<String, JClassNode> classCache;

    private final MappingRepository repo = new MappingRepository();

    private Manifest manifest;

    private final Path input;
    private final Path output;

    private List<String> mappings;

    private String section;

    private ConfigurationSection config;
    private String dir;

    @Singular("mutator")
    private final Map<String, Class<? extends Mutator>> mutators;

    @SneakyThrows
    public void init(ConfigurationSection cfg, String dir) {
        INSTANCE = this;
        this.config = cfg;
        this.dir = dir;

        classCache = new ConcurrentHashMap<>();

        for (JClassNode node : classes)
            classCache.put(node.name, node);

        for (JClassNode node : libraries)
            classCache.put(node.name, node);

        long start = System.currentTimeMillis();

        new Hierarchy().ensureGraphBuilt();

        logger.logln(Level.INFO, Origin.INTAKE, String.format("Loaded hierarchy (%s)", new Ansi().c(WHITE).s(String.format("%dms", System.currentTimeMillis() - start))));

    }

    @SneakyThrows
    public void run(ConfigurationSection cfg) {

        Executor executor = new Executor(cfg, this);
        List<String> executionOrder = cfg.getStringList(section + ".order");
        for (String path : executionOrder) {
            Class<? extends Mutator> mutatorClass = mutators.get(path);
            if (mutatorClass == null) {
                logger.logln(Level.WARNING, Origin.METAPHOR, String.format("Mutator %s not found!", new Ansi().c(YELLOW).s(String.valueOf(path)).r(false).c(BRIGHT_YELLOW)));
                continue;
            }
            try {
                executor.process(classes, mutatorClass, section, path);
            } catch (Exception e) {
                logger.logln(Level.WARNING, Origin.METAPHOR, String.format("Failed to run mutator %s: %s", new Ansi().c(YELLOW).s(String.valueOf(path)).r(false).c(BRIGHT_YELLOW), e.getMessage()));
            }
        }

        logger.logln(Level.INFO, Origin.METAPHOR, "Finished mutations!");

        logger.logln(Level.INFO, Origin.METAPHOR, "Writing jar...");

        if (section.contains("metaphor")) {
            for (JClassNode classNode : classes) {
                if (!classNode.getRealName().equals(classNode.name))
                    classNode.sourceFile = "pass::jnt:" + classNode.getRealName();
            }
        } else {
            for (JClassNode classNode : classes) {
                if (classNode.sourceFile != null && classNode.sourceFile.startsWith("pass::jnt"))
                    classNode.sourceFile = null;
                for (MethodNode method : classNode.methods) {
                    if (method.signature != null && method.signature.startsWith("pass::jnt"))
                        method.signature = null;
                }
            }
        }

        JarWriter writer = new JarWriter(classes, resources, manifest);
        writer.write(input, output);

        // TODO: Transport the map file to out-jnt*/trnsp-*, add original mappings before applying transformations and then add the current ones after transformations so descriptors can actually be used.
        if (!repo.isEmpty()) {
            exportRepository();
        }
        logger.logln(Level.INFO, Origin.METAPHOR, "Exported mapping repository!");

        logger.logln(Level.INFO, Origin.METAPHOR, "Finished writing jar!");

        classes.clear();
        libraries.clear();
        resources.clear();
        classCache.clear();
        Hierarchy.INSTANCE.reset();
    }

    public JClassNode loadClass(String name) {
        JClassNode cn = classCache.computeIfAbsent(name, k -> {
            byte[] internal = JavaInternalClasses.get(k);
            if (internal == null) return null;
            ClassReader reader = new ClassReader(internal);
            JClassNode node = new JClassNode(true);
            reader.accept(node, ClassReader.SKIP_FRAMES | ClassReader.SKIP_DEBUG);
            libraries.add(node);
            return node;
        });
        Hierarchy.INSTANCE.iterateClass(cn);
        return cn;
    }

    public JClassNode loadClass(Class<?> clazz) throws Exception {
        JClassNode node = new JClassNode(true);
        try (InputStream stream = ObfuscatorContext.class.getResourceAsStream("/" + clazz.getName().replace(".", "/") + ".class")) {
            assert stream != null;
            ClassReader reader = new ClassReader(stream);
            reader.accept(node, ClassReader.SKIP_FRAMES | ClassReader.SKIP_DEBUG);
        }
        return node;
    }

    public void addClass(JClassNode cn) {
        classes.add(cn);
    }

    public void addResource(JarResource resource) {
        resources.add(resource);
    }

    public MappingRepository getRepository() {
        return repo;
    }

    public void exportRepository() {
        try {
            Files.write(Paths.get(System.currentTimeMillis() + ".repo"), getRepository().toString().getBytes());
        } catch (IOException e) {
            e.printStackTrace(System.err);
        }
    }

    public boolean isBeingTranspiled(JClassNode node, MethodNode method) {

        if (node.isLibrary()) return false;

        boolean wasExemptSelf = node.isExempt();
        Set<String> wasExemptMembers = node.getExemptMembers();

        node.removeExempt();

        FilterProcessor processor = new FilterProcessor(config.getConfigurationSection("mutators.jnt"));
        processor.process(List.of(node), null);

        boolean safe = !node.isExempt() && !node.isExempt(method);

        node.removeExempt();
        if (wasExemptSelf) node.addExempt();
        wasExemptMembers.forEach(node::addExemptMember);

        return safe;
    }
}
