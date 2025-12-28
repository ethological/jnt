package war;

import joptsimple.OptionParser;
import joptsimple.OptionSet;
import lombok.SneakyThrows;
import war.configuration.file.YamlConfiguration;
import war.jar.JarReader;
import war.jnt.cache.Cache;
import war.jnt.core.Processor;
import war.jnt.core.loader.NativeLoader;
import war.jnt.dash.Ansi;
import war.jnt.dash.Level;
import war.jnt.dash.Logger;
import war.jnt.dash.Origin;
import war.jnt.exhaust.Exhaust;
import war.jnt.exhaust.compiler.Compiler;
import war.jnt.exhaust.compiler.CompilerExperimental;
import war.jnt.exhaust.compiler.CompilerZig;
import war.jnt.exhaust.compiler.ICompiler;
import war.jnt.exhaust.compiler.make.MakeCompiler;
import war.jnt.fusebox.impl.Internal;
import war.jnt.git.Git;
import war.jnt.utility.MemoryMonitor;
import war.jnt.utility.timing.Timing;
import war.locker.JarFingerprinter;
import war.metaphor.Metaphor;
import war.metaphor.base.ObfuscatorContext;

import java.io.File;
import java.lang.management.GarbageCollectorMXBean;
import java.lang.management.ManagementFactory;
import java.net.InetAddress;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.List;

import static war.jnt.dash.Ansi.Color.*;

public class Entrypoint {
    public static int JNT_DISTRO = 3;
    public static String GIT_HASH;
    public static final long ID = 0;//Timing.Companion.current();
    private static final Timing timing = new Timing();
    
    public static String outputPath = String.format("out-jnt%d/trnsp-%d", Entrypoint.JNT_DISTRO, Entrypoint.ID);

    static {
        // don't be funny, this is a very limited key :grin:
        GIT_HASH = Git.getHash("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");
    }

    @SneakyThrows
    public static void main(String[] args) {
        var logger = Logger.INSTANCE;

        OptionParser parser = new OptionParser();
        parser.accepts("config", "Config path").withRequiredArg().ofType(File.class);
        parser.accepts("metaphor", "Run the metaphor engine only").withOptionalArg().ofType(Boolean.class).defaultsTo(false);
        parser.accepts("transpile", "Run the exhaust engine only").withOptionalArg().ofType(Boolean.class).defaultsTo(false);
        parser.accepts("logger", "Enable logger").withOptionalArg().ofType(Boolean.class).defaultsTo(true);
        parser.accepts("compiler", "Choose the compiler explicitly").withOptionalArg().ofType(String.class).defaultsTo("zig");

        OptionSet options;
        try {
            options = parser.parse(args);
        } catch (Exception ex) {
            logger.logln(Level.FATAL, Origin.ARGS, "Usage: java -jar jnt.jar --config %s", new Ansi().c(RED).s("<config.yml>"));
            System.exit(1);
            return;
        }

        boolean metaphorOnly = (Boolean) options.valueOf("metaphor");
        boolean transOnly = (Boolean) options.valueOf("transpile");
        boolean loggerEnabled = (Boolean) options.valueOf("logger");

        logger.setLevel(loggerEnabled ? Level.DEBUG : Level.WARNING);

        if (loggerEnabled) logger.ascii();
        logger.logln(Level.INFO, Origin.FUSEBOX, "Welcome to JNT" + JNT_DISTRO + (JNT_DISTRO == 2 ? " (LTS)" : " (BETA)"));
        logger.logln(Level.INFO, Origin.FUSEBOX, String.format("Running on %s, JVM-%s", new Ansi().c(WHITE).s(System.getProperty("java.vendor")), new Ansi().c(WHITE).s(System.getProperty("java.version"))));
        logger.logln(Level.INFO, Origin.FUSEBOX, "Software build: jnt" + JNT_DISTRO + "+" + new Ansi().c(GREEN).s(GIT_HASH));

        File configFile = (File) options.valueOf("config");
        if (configFile == null) {
            logger.logln(Level.INFO, Origin.FUSEBOX, "Failed to read config file, please make sure you supplied a config.yml file.");
            Internal.panic(1);
        }

        YamlConfiguration config = YamlConfiguration.loadConfiguration(configFile);
        String profile = config.getRoot().getString("profile");

        if (profile != null && !profile.equals("custom")) {
            String path = String.format("profiles/%s/config.yml", profile);
            config = YamlConfiguration.loadConfiguration(Paths.get(path).toFile());
            logger.logln(Level.INFO, Origin.FUSEBOX, "Loaded " + profile + " obfuscation profile.");
        } else {
            logger.logln(Level.INFO, Origin.FUSEBOX, "Did not load an obfuscation profile, reason: was not specified.");
        }

        if (!config.isString("jnt-path")) {
            logger.logln(Level.INFO, Origin.FUSEBOX, "No jnt-path specified, using default path war/jnt");
        }

        String host = "jnt.so";
        InetAddress inet = InetAddress.getByName(host);

        long start = System.currentTimeMillis();
        boolean reachable = inet.isReachable(2500);
        long end = System.currentTimeMillis();

        if (reachable) {
            long ping = end - start;
            logger.logln(Level.INFO, Origin.FUSEBOX, "Heartbeat (jnt.so): " + (ping > 400 ? new Ansi().c(RED).s(ping + "ms") : ping + "ms"));

            if (ping > 400) {
                logger.logln(Level.WARNING, Origin.FUSEBOX, "Expect slow obfuscation speed due to slow network speed.");
            }
        }

        StringBuilder sb = new StringBuilder();
        sb.append("GCs: ");

        List<GarbageCollectorMXBean> gcBeans = ManagementFactory.getGarbageCollectorMXBeans();
        for (GarbageCollectorMXBean gc : gcBeans) {
            sb.append(gc.getName()).append(" [").append(gc.getCollectionTime()).append("]");
            if (gc != gcBeans.getLast()) {
                sb.append(", ");
            }
        }

        logger.logln(Level.INFO, Origin.FUSEBOX, sb.toString());

        String path = config.getString("input");

        if (!path.endsWith(".jar")) {
            logger.logln(Level.INFO, Origin.FUSEBOX, "Failed to read file given, please make sure you supplied a JAR file");
            Internal.panic(1);
        }

        timing.begin();
        
        // Log initial memory usage
        MemoryMonitor.logMemoryUsage("Startup");

        var intake = new JarReader();
        var exhaust = new Exhaust();
        var processor = new Processor();

        ICompiler compiler;
        String compilerOption = (String) options.valueOf("compiler");

        if (compilerOption.equals("gcc")) {
            compiler = new CompilerExperimental(processor);
        } else if (compilerOption.equals("zig")) {
            compiler = new CompilerZig(processor);
        } else if (compilerOption.equals("make-gcc")) {
            compiler = new MakeCompiler(processor);
        } else {
            compiler = new CompilerZig(processor);
        }

        var metaphor = new Metaphor();
        var nativeLoader = new NativeLoader();

        String output = config.getString("output", String.format("%s/output-final.jar", outputPath));

        exhaust.prepare(outputPath);

        if (!transOnly) {
            logger.logln(Level.INFO, Origin.FUSEBOX, "Running metaphor engine...");
            MemoryMonitor.logMemoryUsage("Before Metaphor");

            intake.load(path, config);
            MemoryMonitor.logMemoryUsage("After JAR Loading");
            MemoryMonitor.forceGCIfNeeded();
            
            ObfuscatorContext ctx = metaphor.buildObfuscatePass(intake, config, outputPath);
            ctx.init(config, outputPath);
            ctx.run(config);
            MemoryMonitor.logMemoryUsage("After Metaphor");

            // Clear intake data to free memory
            intake.clear();
            System.gc();
            MemoryMonitor.logMemoryUsage("After Metaphor Cleanup");
        }

        if (!metaphorOnly) {

            logger.logln(Level.INFO, Origin.FUSEBOX, "Running exhaust engine...");
            MemoryMonitor.logMemoryUsage("Before Exhaust");

            intake.load(String.format("%s/metaphor-temp.jar", outputPath), config);
            MemoryMonitor.logMemoryUsage("After Second JAR Loading");
            MemoryMonitor.forceGCIfNeeded();

            ObfuscatorContext ctx = metaphor.buildPackagePass(intake, config, outputPath);
            ctx.init(config, outputPath);

            processor.process(intake, nativeLoader, config, outputPath);
            MemoryMonitor.logMemoryUsage("After Processing");

            System.gc();
            MemoryMonitor.logMemoryUsage("After Processor Cleanup");

            compiler.run(config, outputPath);
            MemoryMonitor.logMemoryUsage("After Compilation");

            ctx.run(config);
            MemoryMonitor.logMemoryUsage("After Packaging");
            
            // Final cleanup
            intake.clear();
            Cache.Companion.clearAll(); // Clear all caches
            System.gc();
            MemoryMonitor.logMemoryUsage("Final Cleanup");
        }

        if (metaphorOnly) {
            Files.copy(new File(String.format("%s/metaphor-temp.jar", outputPath)).toPath(), new File(output).toPath(), StandardCopyOption.REPLACE_EXISTING);
        } else {
            Files.copy(new File(String.format("%s/output-final.jar", outputPath)).toPath(), new File(output).toPath(), StandardCopyOption.REPLACE_EXISTING);
        }

        if (config.getBoolean("fingerprint", true)) {
            JarFingerprinter.setLibraryPath(config.getString("jnt-path", "war/jnt"));
            JarFingerprinter.configurePrivateKeyFromClasspath("/private_key.pem");
            JarFingerprinter.inject(new File(output));
            logger.logln(Level.INFO, Origin.FUSEBOX, "Injected fingerprint into the output JAR");
        }

        timing.end();
        logger.logln(Level.INFO, Origin.FUSEBOX, String.format("Finished running in %s!", new Ansi().c(WHITE).s(String.format("%sms", timing.calc()))));
        logger.dump();
    }
}
