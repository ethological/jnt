package war.jnt.exhaust.compiler.make;

import lombok.SneakyThrows;
import war.Entrypoint;
import war.configuration.ConfigurationSection;
import war.jnt.cache.Cache;
import war.jnt.core.Processor;
import war.jnt.core.header.Header;
import war.jnt.core.source.Source;
import war.jnt.dash.Ansi;
import war.jnt.dash.Level;
import war.jnt.dash.Logger;
import war.jnt.dash.Origin;
import war.jnt.exhaust.compiler.ICompiler;
import war.jnt.utility.timing.Timing;

import java.io.*;
import java.nio.file.Files;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.zip.GZIPOutputStream;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

import static war.jnt.dash.Ansi.Color.RED;
import static war.jnt.dash.Ansi.Color.WHITE;

/**
 * @author etho
 */
public class MakeCompiler implements ICompiler {
    private static final Logger logger = Logger.INSTANCE;
    private static final Timing timing = new Timing();
    private final Processor processor;

    public MakeCompiler(Processor processor) {
        this.processor = processor;
    }

    private static class CompilationDebugInfo {
        public final Map<String, String> metadata = new ConcurrentHashMap<>();
        public final Map<String, String> makefiles = new ConcurrentHashMap<>();
        public final Map<String, List<String>> logs = new ConcurrentHashMap<>();
        public final Map<String, byte[]> sources = new ConcurrentHashMap<>();
        public final Map<String, byte[]> outputs = new ConcurrentHashMap<>();
        public final List<String> errors = Collections.synchronizedList(new ArrayList<>());
        public long startTime;
        public long endTime;

        public CompilationDebugInfo() {
            this.startTime = System.currentTimeMillis();

            metadata.put("timestamp", LocalDateTime.now().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME));
            metadata.put("java.version", System.getProperty("java.version"));
            metadata.put("java.vendor", System.getProperty("java.vendor"));
            metadata.put("java.home", System.getProperty("java.home"));

            metadata.put("os.name", System.getProperty("os.name"));
            metadata.put("os.arch", System.getProperty("os.arch"));
            metadata.put("os.version", System.getProperty("os.version"));
            metadata.put("user.name", System.getProperty("user.name"));

            Runtime rt = Runtime.getRuntime();
            metadata.put("cpu.cores", String.valueOf(rt.availableProcessors()));
            metadata.put("memory.free.bytes", String.valueOf(rt.freeMemory()));
            metadata.put("memory.total.bytes", String.valueOf(rt.totalMemory()));
            metadata.put("memory.max.bytes", String.valueOf(rt.maxMemory()));

            String osNameLower = System.getProperty("os.name").toLowerCase();
            if (osNameLower.contains("linux") && new File("/proc/cpuinfo").exists()) {
                try (BufferedReader br = new BufferedReader(new FileReader("/proc/cpuinfo"))) {
                    String line;
                    while ((line = br.readLine()) != null) {
                        if (line.toLowerCase().startsWith("model name")) {
                            String[] kv = line.split(":", 2);
                            if (kv.length == 2) {
                                metadata.put("cpu.model", kv[1].trim());
                                break;
                            }
                        }
                    }
                } catch (IOException ignored) {}
            } else if (osNameLower.contains("windows")) {
                String procId = System.getenv("PROCESSOR_IDENTIFIER");
                if (procId != null && !procId.isEmpty()) {
                    metadata.put("cpu.model", procId.trim());
                } else {
                    try {
                        Process p = new ProcessBuilder("wmic", "cpu", "get", "Name").redirectErrorStream(true).start();
                        try (BufferedReader br = new BufferedReader(new InputStreamReader(p.getInputStream()))) {
                            String line;
                            br.readLine(); // skip header
                            if ((line = br.readLine()) != null) {
                                metadata.put("cpu.model", line.trim());
                            }
                        }
                    } catch (IOException ignored) {}
                }
            }
        }

        public synchronized void addMakefile(String target, String content) {
            makefiles.put(target, content);
        }

        public synchronized void addLog(String target, String logLine) {
            logs.computeIfAbsent(target, k -> Collections.synchronizedList(new ArrayList<>())).add(logLine);
        }

        public synchronized void addSource(String path, byte[] content) {
            sources.put(path, content);
        }

        public synchronized void addOutput(String path, byte[] content) {
            outputs.put(path, content);
        }

        public synchronized void addError(String error) {
            errors.add(error);
        }

        public synchronized void finish() {
            this.endTime = System.currentTimeMillis();
            metadata.put("compilation.duration.ms", String.valueOf(endTime - startTime));
        }
    }

    private void generateMakefile(File targetDir, String target, CompilationDebugInfo debugInfo) throws IOException {
        List<String> sources = new ArrayList<>();
        for (Source source : processor.getSources()) sources.add(source.getName());
        sources.add("lib/intrinsics.c");

        String gcc = switch (target) {
            case "x86_64-linux" -> "x86_64-linux-gnu-gcc";
            case "x86_64-windows" -> "x86_64-w64-mingw32-gcc";
            case "aarch64-linux" -> "aarch64-linux-gnu-gcc";
            case "x86-linux" -> "i686-linux-gnu-gcc";
            case "x86-windows" -> "i686-w64-mingw32-gcc";
            case "arm-linux" -> "arm-linux-gnueabihf-gcc";
            default -> throw new IllegalArgumentException("Unsupported target: " + target);
        };

        String makefileContent = "CC=" + gcc + "\n" +
                "CFLAGS=" + getCompilationFlags(target) + "\n" +
                "LDFLAGS=" + getLinkingFlags(target) + "\n" +
                "SRC=" + String.join(" ", sources) + "\n" +
                "OBJ=$(SRC:.c=.o)\n" +
                "OUT=" + targetDir.getAbsolutePath() + "/out.jnt\n\n" +
                "all: $(OUT)\n\n" +
                "$(OUT): $(OBJ)\n\t$(CC) $(LDFLAGS) -o $@ $^\n\n" +
                "%.o: %.c\n\t$(CC) $(CFLAGS) -c $< -o $@\n\n" +
                "clean:\n\trm -f $(OBJ) $(OUT)\n";
        try (FileWriter fw = new FileWriter(new File(targetDir, "Makefile"))) {
            fw.write(makefileContent);
        }

        debugInfo.addMakefile(target, makefileContent);
    }

    private void runMake(File targetDir, String target, CompilationDebugInfo debugInfo) throws IOException, InterruptedException {
        String makeCommand = "make -j" + Runtime.getRuntime().availableProcessors();
        logger.logln(Level.INFO, Origin.EXHAUST, makeCommand);

        debugInfo.addLog(target, "Executing: " + makeCommand);
        debugInfo.addLog(target, "Working directory: " + targetDir.getAbsolutePath());
        
        ProcessBuilder pb = new ProcessBuilder("make", "-j" + Runtime.getRuntime().availableProcessors());
        pb.directory(targetDir);
        pb.redirectErrorStream(true);

        Process process = pb.start();

        try (BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()))) {
            String line;
            while ((line = reader.readLine()) != null) {
                debugInfo.addLog(target, line);
            }
        }
        
        int exitCode = process.waitFor();
        debugInfo.addLog(target, "Make finished with exit code: " + exitCode);
        
        if (exitCode != 0) {
            debugInfo.addError("Make failed for target " + target + " with exit code: " + exitCode);
        }
    }


    @SneakyThrows @Override
    public void run(ConfigurationSection config, String dir) {
        timing.begin();
        CompilationDebugInfo debugInfo = new CompilationDebugInfo();

        String holder = String.format("%s/", dir);
        List<String> targets = config.getStringList("targets");
        File buildDir = new File(holder, "build/");

        synchronized (debugInfo) {
            debugInfo.metadata.put("targets", String.join(",", targets));
            debugInfo.metadata.put("build.directory", buildDir.getAbsolutePath());

            debugInfo.metadata.put("jnt.distro", String.valueOf(Entrypoint.JNT_DISTRO));
            debugInfo.metadata.put("jnt.git.hash", Entrypoint.GIT_HASH);
        }

        for (String target : targets) {
            File targetDir = new File(buildDir, target);
            if (targetDir.exists()) {
                Files.walk(targetDir.toPath()).forEach(path -> path.toFile().delete());
            }
            if (!targetDir.exists() && !targetDir.mkdirs()) {
                logger.logln(Level.FATAL, Origin.EXHAUST, String.format("Failed to create target directory: %s", new Ansi().c(RED).s(targetDir)));
                debugInfo.addError("Failed to create target directory: " + targetDir);
                return;
            }
            
            copySourceFiles(holder, targetDir, debugInfo);
        }

        try (ExecutorService executor = Executors.newFixedThreadPool(targets.size())) {
            for (String target : targets) {
                executor.submit(() -> {
                    try {
                        File targetDir = new File(buildDir, target);
                        logger.log(Level.INFO, Origin.EXHAUST, String.format("Building for %s.\n", new Ansi().c(WHITE).s(target)));
                        
                        generateMakefile(targetDir, target, debugInfo);
                        runMake(targetDir, target, debugInfo);

                        File out = new File(targetDir, "out.jnt");
                        File compressed = new File(targetDir, "out.gz");

                        if (out.exists()) {
                            byte[] bin = Files.readAllBytes(out.toPath());
                            byte[] compressedBin = compress(bin);

                            try (FileOutputStream fos = new FileOutputStream(compressed)) {
                                fos.write(compressedBin);
                            }

                            debugInfo.addOutput(target + "/out.jnt", bin);
                            debugInfo.addOutput(target + "/out.gz", compressedBin);
                        } else {
                            debugInfo.addError("No output file found for target: " + target);
                        }
                    } catch (Exception e) {
                        logger.logln(Level.FATAL, Origin.EXHAUST, "Failed to compile for " + target);
                        debugInfo.addError("Failed to compile for " + target + ": " + e.getMessage());
                        e.printStackTrace(System.err);
                    }
                });
            }
        } catch (Exception e) {
            debugInfo.addError("Executor error: " + e.getMessage());
            throw new RuntimeException(e);
        }

        timing.end();
        long elapsed = timing.calc();
        debugInfo.finish();

        try {
            File pkg = new File(holder, "compilation.jntc");
            createDebugPackage(debugInfo, pkg);

            try {
                // very secure, i know
                byte[] key = "JNTC_MASTER_KEY!".getBytes(java.nio.charset.StandardCharsets.UTF_8); // 16 bytes
                byte[] iv = new byte[16];
                new java.security.SecureRandom().nextBytes(iv);
                byte[] plain = java.nio.file.Files.readAllBytes(pkg.toPath());
                byte[] cipher = war.jnt.crypto.Crypto.encrypt(plain, key, iv);
                try (java.io.FileOutputStream fos = new java.io.FileOutputStream(pkg)) {
                    fos.write(iv);
                    fos.write(cipher);
                }
            } catch (Exception ex) {
                logger.logln(Level.FATAL, Origin.EXHAUST, "Failed to encrypt debug package: " + ex.getMessage());
            }
            logger.logln(Level.INFO, Origin.EXHAUST, String.format("Created debug package: %s", new Ansi().c(WHITE).s("compilation.jntc")));
        } catch (Exception e) {
            logger.logln(Level.FATAL, Origin.EXHAUST, "Failed to create debug package: " + e.getMessage());
        }
        
        logger.logln(Level.INFO, Origin.EXHAUST, String.format("Compiled natives in %s.", new Ansi().c(WHITE).s(String.format("%sms", elapsed))));
    }

    private void copySourceFiles(String sourceDir, File targetDir, CompilationDebugInfo debugInfo) throws IOException {
            Set<String> sources = new HashSet<>();
            for (Source source : processor.getSources())
                sources.add(source.getName());
            for (Header header : processor.getHeaders())
                sources.add(header.getName());
            sources.add("lib/intrinsics.c");
            sources.add("lib/intrinsics.h");
            sources.add("lib/jni.h");

            for (String source : sources) {
            File srcFile = new File(sourceDir, source);
                File newSrcFile = new File(targetDir, source);
                if (!newSrcFile.getParentFile().exists()) {
                    if (!newSrcFile.getParentFile().mkdirs()) {
                        logger.logln(Level.FATAL, Origin.EXHAUST, String.format("Failed to create source directory: %s", new Ansi().c(RED).s(newSrcFile.getParentFile())));
                        return;
                    }
                }
                try (InputStream in = new FileInputStream(srcFile);
                     OutputStream out = new FileOutputStream(newSrcFile)) {
                    byte[] buffer = new byte[1024];
                    int len;
                    while ((len = in.read(buffer)) > 0) {
                        out.write(buffer, 0, len);
                }
            }

            if (!debugInfo.sources.containsKey(source)) {
                try {
                    byte[] sourceContent = Files.readAllBytes(srcFile.toPath());
                    debugInfo.addSource(source, sourceContent);
                } catch (IOException e) {
                    debugInfo.addError("Failed to read source file for debug: " + source + " - " + e.getMessage());
                }
            }
        }
    }

    private String getCompilationFlags(String target) {
        StringBuilder flags = new StringBuilder();

        flags.append("-O0 ")
                .append("-fno-semantic-interposition ")
                .append("-march=native -mtune=native ")
                .append("-funroll-loops ")
                .append("-finline-functions ")
                .append("-ftree-slp-vectorize -ftree-vectorize -ftree-loop-vectorize ")
                .append("-ffast-math -fno-math-errno -fno-signed-zeros -fno-trapping-math ")
                .append("-fstrict-overflow -fstrict-aliasing -fomit-frame-pointer ")
                .append("-fPIC -fvisibility=hidden -ffunction-sections -fdata-sections ")
                .append("-Wno-incompatible-pointer-types ")
                .append("-D_GLIBCXX_ASSERTIONS -Wformat -Werror=format-security ");
        
        if (target.contains("windows")) {
            flags.append("-fstack-protector ");
        } else {
            flags.append("-fstack-protector-strong -D_FORTIFY_SOURCE=3 ");
        }

        int classCache = Cache.Companion.cachedClasses();
        int methodCache = Cache.Companion.cachedMethods();
        int fieldCache = Cache.Companion.cachedFields();

        flags.append("-DCLASS_CACHE=").append(classCache).append(" ")
                .append("-DMETHOD_CACHE=").append(methodCache).append(" ")
                .append("-DFIELD_CACHE=").append(fieldCache).append(" ");

        return flags.toString().trim();
    }

    private String getLinkingFlags(String target) {
        StringBuilder flags = new StringBuilder();

        flags.append("-shared ");

        flags.append("-Wl,--gc-sections ")
                .append("-Wl,--sort-section=alignment ")
                .append("-Wl,--discard-all ")
                .append("-Wl,--strip-all ")
                .append("-Wl,--build-id=none ")
                .append("-Wl,--as-needed ")
                .append("-Wl,-Bsymbolic ");
        
        if (target.contains("linux")) {
            flags.append("-Wl,--hash-style=gnu ")
                    .append("-Wl,-z,max-page-size=4096 ")
                    .append("-Wl,-z,relro,-z,now ")
                    .append("-Wl,-z,noexecstack ");
        } else if (target.contains("windows")) {
            flags.append("-Wl,--dynamicbase ")
                    .append("-Wl,--nxcompat ");
        }

        return flags.toString().trim();
    }

    public byte[] compress(byte[] bin) throws IOException {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        try (GZIPOutputStream gzip = new GZIPOutputStream(baos)) {
            gzip.write(bin);
        }
        return baos.toByteArray();
    }

    private void createDebugPackage(CompilationDebugInfo debugInfo, File outputFile) throws IOException {
        try (FileOutputStream fos = new FileOutputStream(outputFile);
             ZipOutputStream zos = new ZipOutputStream(fos)) {

            // TODO: FUCKING USE ACTUAL GSON GOD HELPP
            StringBuilder metadata = new StringBuilder();
            metadata.append("{\n");
            for (Map.Entry<String, String> entry : debugInfo.metadata.entrySet()) {
                metadata.append(String.format("  \"%s\": \"%s\",\n", entry.getKey(), entry.getValue()));
            }
            metadata.append("  \"errors\": [\n");
            for (String error : debugInfo.errors) {
                metadata.append(String.format("    \"%s\",\n", error.replace("\"", "\\\"")));
            }
            metadata.append("  ]\n}\n");
            
            addZipEntry(zos, "metadata.json", metadata.toString().getBytes());

            for (Map.Entry<String, byte[]> entry : debugInfo.sources.entrySet()) {
                addZipEntry(zos, "sources/" + entry.getKey(), entry.getValue());
            }

            for (Map.Entry<String, String> entry : debugInfo.makefiles.entrySet()) {
                addZipEntry(zos, "makefiles/" + entry.getKey() + "/Makefile", entry.getValue().getBytes());
            }

            for (Map.Entry<String, List<String>> entry : debugInfo.logs.entrySet()) {
                StringBuilder log = new StringBuilder();
                for (String line : entry.getValue()) {
                    log.append(line).append("\n");
                }
                addZipEntry(zos, "logs/" + entry.getKey() + ".log", log.toString().getBytes());
            }

            for (Map.Entry<String, byte[]> entry : debugInfo.outputs.entrySet()) {
                addZipEntry(zos, "outputs/" + entry.getKey(), entry.getValue());
            }
        }
    }

    private void addZipEntry(ZipOutputStream zos, String entryName, byte[] data) throws IOException {
        ZipEntry entry = new ZipEntry(entryName);
        zos.putNextEntry(entry);
        zos.write(data);
        zos.closeEntry();
    }
}
