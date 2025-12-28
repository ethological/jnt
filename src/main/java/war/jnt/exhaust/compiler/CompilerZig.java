package war.jnt.exhaust.compiler;

import lombok.SneakyThrows;
import war.configuration.ConfigurationSection;
import war.jnt.cache.Cache;
import war.jnt.core.Processor;
import war.jnt.core.source.Source;
import war.jnt.dash.Ansi;
import war.jnt.dash.Level;
import war.jnt.dash.Logger;
import war.jnt.dash.Origin;
import war.jnt.utility.timing.Timing;

import java.io.*;
import java.net.URI;
import java.net.URL;
import java.net.URLConnection;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.zip.Deflater;
import java.util.zip.GZIPOutputStream;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

import static war.jnt.dash.Ansi.Color.BRIGHT_CYAN;
import static war.jnt.dash.Ansi.Color.WHITE;

public class CompilerZig implements ICompiler {

    private static final Logger logger = Logger.INSTANCE;
    private static final Timing timing = new Timing();
    private final Processor processor;

    public CompilerZig(Processor processor) {
        this.processor = processor;
    }

    public File downloadZig(ConfigurationSection config) {
        logger.logln(Level.INFO, Origin.EXHAUST, "Searching for zig compiler...");
        boolean isWindows = System.getProperty("os.name").toLowerCase().contains("windows");
        String extension = isWindows ? ".exe" : "";
        File root = new File(config.getString("zig.installation"));

        if (root.exists()) {
            try {
                Optional<Path> zig = Files.walk(root.toPath())
                        .filter(p -> p.toFile().getName().equalsIgnoreCase("zig" + extension))
                        .findFirst();
                if (zig.isPresent()) return zig.get().toFile();
            } catch (IOException ignored) {}
        }

        logger.logln(Level.INFO, Origin.EXHAUST, "Zig compiler not found, downloading...");

        String version = config.getString("zig.version");
        String os = config.getString("zig.os");
        String arch = config.getString("zig.arch");
        String zipName = String.format("zig-%s-%s-%s.zip", arch, os, version);
        String urlStr = String.format("https://ziglang.org/download/%s/%s", version, zipName);

        try {
            URL url = URI.create(urlStr).toURL();
            File temp = File.createTempFile("zig", ".zip");
            temp.deleteOnExit();

            URLConnection conn = url.openConnection();
            int totalBytes = conn.getContentLength();

            logger.logln(Level.INFO, Origin.EXHAUST, String.format("Downloading zig compiler from %s (%s bytes)",
                    new Ansi().c(WHITE).s(urlStr),
                    totalBytes > 0 ? new Ansi().c(WHITE).s(String.valueOf(totalBytes)) : "unknown"));

            try (InputStream in = conn.getInputStream();
                 FileOutputStream out = new FileOutputStream(temp)) {

                byte[] buffer = new byte[8192];
                long downloaded = 0;
                int r;
                final int BAR_WIDTH = 50;

                while ((r = in.read(buffer)) != -1) {
                    out.write(buffer, 0, r);
                    if (totalBytes > 0) {
                        downloaded += r;
                        int percent = (int) ((downloaded * 100) / totalBytes);
                        int filled = (percent * BAR_WIDTH) / 100;
                        String bar = String.format("[%s%s] %3d%%",
                                "=".repeat(Math.max(0, filled)),
                                " ".repeat(Math.max(0, BAR_WIDTH - filled)),
                                percent);
                        logger.rlog(Level.INFO, Origin.EXHAUST, bar);
                    }
                }
                logger.rlog(Level.INFO, Origin.EXHAUST, String.format("[%s] 100%%\n", "=".repeat(BAR_WIDTH)));
            }

            logger.logln(Level.INFO, Origin.EXHAUST, String.format("Extracting to %s...", new Ansi().c(WHITE).s(root.getAbsolutePath())));

            if (!root.exists() && !root.mkdirs()) {
                throw new IOException("Failed to create output directory: " + root);
            }

            try (ZipInputStream zis = new ZipInputStream(new FileInputStream(temp))) {
                ZipEntry entry;
                while ((entry = zis.getNextEntry()) != null) {
                    Path outPath = root.toPath().resolve(entry.getName());
                    if (entry.isDirectory()) {
                        Files.createDirectories(outPath);
                    } else {
                        Files.createDirectories(outPath.getParent());
                        try (OutputStream fos = Files.newOutputStream(outPath)) {
                            byte[] buf = new byte[8192];
                            int len;
                            while ((len = zis.read(buf)) > 0) fos.write(buf, 0, len);
                        }

                        if (entry.getName().endsWith("zig" + extension)) {
                            outPath.toFile().setExecutable(true);
                            return outPath.toFile();
                        }
                    }
                    zis.closeEntry();
                }
            }

            return Files.walk(root.toPath())
                    .filter(p -> p.toFile().getName().equalsIgnoreCase("zig" + extension))
                    .findFirst()
                    .map(Path::toFile)
                    .orElse(null);

        } catch (IOException e) {
            logger.logln(Level.FATAL, Origin.EXHAUST, "Failed to download/extract zig: " + e.getMessage());
            return null;
        }
    }

    @SneakyThrows @Override
    public void run(ConfigurationSection config, String dir) {
        timing.begin();
        File zigInstallation = downloadZig(config);
        if (zigInstallation == null || !zigInstallation.exists()) {
            logger.logln(Level.FATAL, Origin.EXHAUST, "Zig compiler not found");
            return;
        }

        List<String> targets = config.getStringList("targets");
        List<String> debugging = config.getStringList("debug.compilation");
        File buildDir = new File(dir, "build/");

        Set<String> sourceFiles = new LinkedHashSet<>();
        processor.getSources().forEach(s -> sourceFiles.add(s.getName()));
        processor.getHeaders().forEach(h -> sourceFiles.add(h.getName()));
        Collections.addAll(sourceFiles, "lib/intrinsics.c", "lib/intrinsics.h", "lib/jni.h");

        for (String source : sourceFiles) {
            File srcFile = new File(dir, source);
            if (!srcFile.exists()) continue;

            for (String target : targets) {
                File targetDir = new File(buildDir, target);
                File newSrcFile = new File(targetDir, source);
                if (!newSrcFile.getParentFile().exists()) newSrcFile.getParentFile().mkdirs();
                Files.copy(srcFile.toPath(), newSrcFile.toPath(), StandardCopyOption.REPLACE_EXISTING);
            }
        }

        processor.getSources().forEach(Source::clear);

        int threads = Math.max(1, Runtime.getRuntime().availableProcessors() - 1);
        ExecutorService executor = Executors.newFixedThreadPool(threads);

        try {
            for (String target : targets) {
                logger.logln(Level.INFO, Origin.EXHAUST, String.format("Building for target %s", new Ansi().c(BRIGHT_CYAN).s(target)));

                File targetDir = new File(buildDir, target);
                String gcc = zigInstallation.getAbsolutePath();

                List<String> compileList = new ArrayList<>();
                for (String s : sourceFiles) {
                    if (s.endsWith(".c")) compileList.add(s);
                }

                compileList.sort((a, b) -> {
                    File f1 = new File(targetDir, a);
                    File f2 = new File(targetDir, b);
                    return Long.compare(f2.length(), f1.length());
                });

                AtomicInteger counter = new AtomicInteger(0);
                int total = compileList.size();
                final int BAR_WIDTH = 40;

                List<CompletableFuture<Void>> futures = new ArrayList<>();

                for (String src : compileList) {
                    futures.add(CompletableFuture.runAsync(() -> {
                        try {
                            File srcFile = new File(targetDir, src);
                            File objFile = new File(targetDir, src.replaceAll("\\.c$", ".o"));

                            List<String> cmd = new ArrayList<>();
                            cmd.add(gcc);
                            cmd.add("cc");
                            cmd.add("-I"); cmd.add(new File(targetDir, "lib").getAbsolutePath());
                            cmd.add("-I"); cmd.add(targetDir.getAbsolutePath());
                            cmd.add("-I"); cmd.add(new File(targetDir, "classes").getAbsolutePath());

                            cmd.addAll(Arrays.asList(getCompileCommandLine(target, Files.size(srcFile.toPath())).split(" ")));

                            cmd.add("-c");
                            cmd.add(srcFile.getAbsolutePath());
                            cmd.add("-o");
                            cmd.add(objFile.getAbsolutePath());
                            cmd.add("-fPIE");
                            cmd.add("-pie");

                            process(cmd, debugging.contains(target));

                            int c = counter.incrementAndGet();
                            int percent = (c * 100) / total;
                            int filled = (percent * BAR_WIDTH) / 100;
                            String bar = String.format("Compiling [%s%s] %3d%% (%s)",
                                    "=".repeat(filled),
                                    " ".repeat(BAR_WIDTH - filled),
                                    percent,
                                    srcFile.getName());
                            logger.rlog(Level.INFO, Origin.EXHAUST, bar);

                        } catch (Exception e) {
                            logger.logln(Level.FATAL, Origin.EXHAUST, "Failed to compile " + src);
                            e.printStackTrace(System.err);
                        }
                    }, executor));
                }

                CompletableFuture.allOf(futures.toArray(new CompletableFuture[0])).join();
                logger.rlog(Level.INFO, Origin.EXHAUST, String.format("Compiling [%s] 100%% (Complete)\n", "=".repeat(BAR_WIDTH)));

                logger.logln(Level.INFO, Origin.EXHAUST, "Linking objects...");

                List<String> linkCmd = new ArrayList<>();
                linkCmd.add(gcc);
                linkCmd.add("cc");
                linkCmd.add("-shared");
                linkCmd.addAll(Arrays.asList(getLinkCommandLine(target).split(" ")));

                File responseFile = new File(targetDir, "args.txt");
                try (PrintWriter pw = new PrintWriter(responseFile)) {
                    for (String src : compileList) {
                        String objPath = new File(targetDir, src.replaceAll("\\.c$", ".o")).getAbsolutePath();
                        pw.println("\"" + objPath.replace("\\", "\\\\") + "\"");
                    }
                }

                linkCmd.add("@" + responseFile.getAbsolutePath());

                File outFile = new File(targetDir, "out.jnt");
                linkCmd.add("-o");
                linkCmd.add(outFile.getAbsolutePath());

                process(linkCmd, debugging.contains(target));

                File compressed = new File(targetDir, "out.gz");
                byte[] bin = Files.readAllBytes(outFile.toPath());
                byte[] compressedBin = compress(bin);
                Files.write(compressed.toPath(), compressedBin);
            }

        } catch (Exception e) {
            throw new RuntimeException(e);
        } finally {
            executor.shutdown();
            processor.clear();
            timing.end();
            logger.logln(Level.INFO, Origin.EXHAUST, String.format("Compiled natives in %s",
                    new Ansi().c(WHITE).s(String.format("%sms", timing.calc()))));
        }
    }

    @SneakyThrows
    private void process(List<String> command, boolean debug) {
//        if (debug) {
//            Logger.INSTANCE.logln(Level.DEBUG, Origin.EXHAUST, "Cmd: " + String.join(" ", command));
//        }

        ProcessBuilder builder = new ProcessBuilder(command);
        builder.redirectErrorStream(true);
        Process process = builder.start();

        try (BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()))) {
            String line;
            while ((line = reader.readLine()) != null) {
                if (debug || line.toLowerCase().contains("error")) {
                    logger.logln(Level.FATAL, Origin.EXHAUST, line);
                }
            }
        }

        int exitCode = process.waitFor();
        if (exitCode != 0) {
            throw new RuntimeException("Process failed with exit code " + exitCode);
        }
    }

    public String getLinkCommandLine(String target) {
        StringBuilder cmd = new StringBuilder();

        cmd.append("-target ").append(target).append(" ");

        cmd.append("-O3 ");
        cmd.append("-g0 ");

        cmd.append("-fno-sanitize=all ")
                .append("-fno-stack-protector ")
                .append("-DNDEBUG ");

        if (target.contains("x86_64")) {
            cmd.append("-msse4.2 -maes ");
        }

        cmd.append("-ffunction-sections -fdata-sections ");

        cmd.append("-fno-exceptions -fno-rtti ")
                .append("-fno-unwind-tables -fno-asynchronous-unwind-tables ");

        cmd.append("-fmerge-all-constants -fno-math-errno -fno-trapping-math -fno-ident ");

        int classCache = Cache.Companion.cachedClasses();
        int methodCache = Cache.Companion.cachedMethods();
        int fieldCache = Cache.Companion.cachedFields();
        cmd.append("-DCLASS_CACHE=").append(classCache).append(" ")
                .append("-DMETHOD_CACHE=").append(methodCache).append(" ")
                .append("-DFIELD_CACHE=").append(fieldCache).append(" ");

        cmd.append("-Wl,--gc-sections ")
                .append("-Wl,-s ")
                .append("-Wl,--strip-all ")
                .append("-Wl,--discard-all ")
                .append("-Wl,--build-id=none ")
                .append("-Wl,--as-needed ")
                .append("-Wl,-Bsymbolic ");

        if (!target.contains("windows")) {
            cmd.append("-Wl,--hash-style=gnu ")
                    .append("-Wl,-z,norelro ")
                    .append("-Wl,-z,noexecstack ");
        } else {
            cmd.append("-Wl,--dynamicbase ")
                    .append("-Wl,--nxcompat ")
                    .append("-Wl,--high-entropy-va ");
        }

        return cmd.toString();
    }

    public String getCompileCommandLine(String target, long fileSize) {
        StringBuilder cmd = new StringBuilder();

        cmd.append("-target ").append(target).append(" ");

        long kb = fileSize / 1024;
        if (kb < 32) cmd.append("-O3 ");
        else if (kb < 128) cmd.append("-O2 ");
        else if (kb < 512) cmd.append("-O1 ");
        else cmd.append("-O0 ");

        cmd.append("-g0 ");

        cmd.append("-fno-sanitize=all ")
                .append("-fno-stack-protector ")
                .append("-DNDEBUG ");

        cmd.append("-w ");

        if (target.contains("x86_64")) {
            cmd.append("-msse4.2 -maes ");
        }

        cmd.append("-ffunction-sections -fdata-sections ");

        cmd.append("-fno-exceptions -fno-rtti ")
                .append("-fno-unwind-tables -fno-asynchronous-unwind-tables ");

        cmd.append("-fmerge-all-constants -fno-math-errno -fno-trapping-math -fno-ident ");

        int classCache = Cache.Companion.cachedClasses();
        int methodCache = Cache.Companion.cachedMethods();
        int fieldCache = Cache.Companion.cachedFields();
        cmd.append("-DCLASS_CACHE=").append(classCache).append(" ")
                .append("-DMETHOD_CACHE=").append(methodCache).append(" ")
                .append("-DFIELD_CACHE=").append(fieldCache).append(" ");

        return cmd.toString();
    }

    public byte[] compress(byte[] bin) throws IOException {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        Deflater deflater = new Deflater(Deflater.BEST_COMPRESSION, true);
        try (GZIPOutputStream gzip = new GZIPOutputStream(baos) {
            { this.def = deflater; }
        }) {
            gzip.write(bin);
        }
        return baos.toByteArray();
    }
}
