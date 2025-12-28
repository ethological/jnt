package war.jnt.exhaust.compiler;

import lombok.SneakyThrows;
import war.configuration.ConfigurationSection;
import war.jnt.cache.Cache;
import war.jnt.core.Processor;
import war.jnt.core.header.Header;
import war.jnt.core.source.Source;
import war.jnt.dash.Ansi;
import war.jnt.dash.Level;
import war.jnt.dash.Logger;
import war.jnt.dash.Origin;
import war.jnt.utility.timing.Timing;

import java.io.*;
import java.nio.file.Files;
import java.util.*;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.zip.GZIPOutputStream;

import static war.jnt.dash.Ansi.Color.*;

public class CompilerExperimental implements ICompiler {
    
    private static final Logger logger = Logger.INSTANCE;
    private static final Timing timing = new Timing();
    private final Processor processor;

    public CompilerExperimental(Processor processor) {
        this.processor = processor;
    }

    @SneakyThrows @Override
    public void run(ConfigurationSection config, String dir) {
        timing.begin();

        String holder = String.format("%s/", dir);

        List<String> targets = config.getStringList("targets");
        List<String> debugging = config.getStringList("debug.compilation");

        File buildDir = new File(holder, "build/");

        for (String target : targets) {
            File targetDir = new File(buildDir, target);
            if (targetDir.exists()) {
                Files.walk(targetDir.toPath()).forEach(path -> path.toFile().delete());
            }

            if (!targetDir.exists() && !targetDir.mkdirs()) {
                logger.logln(Level.FATAL, Origin.EXHAUST, String.format("Failed to create target directory: %s", new Ansi().c(RED).s(targetDir)));
                return;
            }

            Set<String> sources = new HashSet<>();
            for (Source source : processor.getSources())
                sources.add(source.getName());
            for (Header header : processor.getHeaders())
                sources.add(header.getName());
            sources.add("lib/intrinsics.c");
            sources.add("lib/intrinsics.h");
            sources.add("lib/jni.h");
            for (String source : sources) {
                File srcFile = new File(holder, source);
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
            }
        }

        //Stop raping the memory
        for (Source source : processor.getSources()) {
            source.clear();
        }
        System.gc();

        int threads = Runtime.getRuntime().availableProcessors() / targets.size();
        try (ExecutorService executor = Executors.newFixedThreadPool(targets.size())) {
            for (String target : targets) {
                executor.submit(() -> {
                    try {
                        File targetDir = new File(buildDir, target);

                        String targetPath = targetDir.getAbsolutePath();

                        logger.logln(Level.INFO, Origin.EXHAUST, String.format("Linking for %s.", new Ansi().c(WHITE).s(target)));
                        String gcc = switch (target) {
                            case "x86_64-linux" -> "x86_64-linux-gnu-gcc";
                            case "x86_64-windows" -> "x86_64-w64-mingw32-gcc";
                            case "aarch64-linux" -> "aarch64-linux-gnu-gcc";
                            case "x86-linux" -> "i686-linux-gnu-gcc";
                            case "x86-windows" -> "i686-w64-mingw32-gcc";
                            case "arm-linux" -> "arm-linux-gnueabihf-gcc";
                            default -> throw new IllegalArgumentException("Unsupported target: " + target);
                        };

                        List<String> sources = new ArrayList<>();
                        for (Source source : processor.getSources()) {
                            sources.add(source.getName());
                        }
                        sources.add("lib/intrinsics.c");

                        sources.sort(Comparator.comparingLong(src -> {
                            File f = new File(targetPath, src);
                            return -(f.exists() ? f.length() : 0L);
                        }));

                        List<Future<?>> futures = new ArrayList<>();
                        try (ExecutorService ex = Executors.newFixedThreadPool(threads)) {
                            for (String src : sources) {
                                futures.add(ex.submit(() -> {
                                    try {
                                        String srcPath = new File(targetPath, src).getAbsolutePath();
                                        process(String.format("%s -I %s -I %s -I %s %s -c %s -o %s -fPIC", gcc, new File(targetPath, "lib").getAbsolutePath(), new File(targetPath).getAbsolutePath(), new File(targetPath, "classes").getAbsolutePath(), getLinkCommandLine(target), srcPath, srcPath.replaceAll("\\.c$", ".o")), debugging.contains(target));
                                    } catch (Exception e) {
                                        logger.logln(Level.FATAL, Origin.EXHAUST, String.format("Failed to compile source %s for %s.", new Ansi().c(RED).s(src).r(false).c(BRIGHT_RED), new Ansi().c(RED).s(target).r(false).c(BRIGHT_RED)));
                                        e.printStackTrace(System.err);
                                    }
                                }));
                            }
                            for (Future<?> f : futures) {
                                f.get();
                            }
                            ex.shutdown();
                        } catch (Exception e) {
                            logger.logln(Level.FATAL, Origin.EXHAUST, String.format("Failed to compile for %s.", new Ansi().c(RED).s(target).r(false).c(BRIGHT_RED)));
                        }

                        StringBuilder objectsFile = new StringBuilder();
                        StringBuilder link = new StringBuilder(gcc).append(" -shared ").append(getLinkCommandLine(target));
                        for (String src : sources) {
                            String objPath = new File(targetPath, src.replaceAll("\\.c$", ".o")).getAbsolutePath();
                            objectsFile
                                    .append(objPath.replace("\\", "/"))
                                    .append("\n");
                        }
                        File objects = new File(targetPath, "objects.txt");
                        try (FileWriter writer = new FileWriter(objects)) {
                            writer.write(objectsFile.toString().trim());
                        }
                        link.append(" @").append(objects.getAbsolutePath());
                        link.append(" -o ").append(targetPath).append(File.separator).append("out.jnt");

                        try {
                            process(link.toString(), debugging.contains(target));
                        } catch (Exception e) {
                            logger.logln(Level.FATAL, Origin.EXHAUST, String.format("Failed to compile native %s.", new Ansi().c(RED).s(target).r(false).c(BRIGHT_RED)));
                            e.printStackTrace(System.err);
                        }

                        File out = new File(targetPath + "/out.jnt");
                        File compressed = new File(targetPath + "/out.gz");

                        byte[] bin = Files.readAllBytes(out.toPath());
                        byte[] compressedBin = compress(bin);

                        try (FileOutputStream fos = new FileOutputStream(compressed)) {
                            fos.write(compressedBin);
                        }
                    } catch (Exception e) {
                        logger.logln(Level.FATAL, Origin.EXHAUST, String.format("Failed to compile for %s.", new Ansi().c(RED).s(target).r(false).c(BRIGHT_RED)));
                    }
                });
            }
        } catch (Exception e) {
            processor.clear();
            throw new RuntimeException(e);
        }

        timing.end();
        processor.clear();

        long elapsed = timing.calc();
        logger.logln(Level.INFO, Origin.EXHAUST, String.format("Compiled natives in %s.", new Ansi().c(WHITE).s(String.format("%sms", elapsed))));
    }

    @SneakyThrows
    private void process(String commandLine, boolean debug) {
        Logger.INSTANCE.logln(Level.DEBUG, Origin.EXHAUST, String.format("Running command: %s", new Ansi().c(BRIGHT_CYAN).s(commandLine)));
        StringTokenizer st = new StringTokenizer(commandLine);
        String[] cmdarray = new String[st.countTokens()];
        for (int i = 0; st.hasMoreTokens(); i++)
            cmdarray[i] = st.nextToken();
        ProcessBuilder builder = new ProcessBuilder(cmdarray);

        builder.redirectErrorStream(true);

        Process process = builder.start();

        BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()));

        String line;
        while ((line = reader.readLine()) != null) {
            if (debug) {
                logger.logln(Level.FATAL, Origin.EXHAUST, line);
            }
        }

        process.waitFor();
    }

    public String getLinkCommandLine(String target) {
        StringBuilder cmd = new StringBuilder();

        // Base compiler configuration
        cmd.append(" -fno-semantic-interposition ")
//                .append("-O1     ")
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
            // Fuck you mingw-w64
            cmd.append("-fstack-protector ");
        } else {
            cmd.append("-fstack-protector-strong -D_FORTIFY_SOURCE=3 ");
        }

        // Add cache sizes
        int classCache = Cache.Companion.cachedClasses();
        int methodCache = Cache.Companion.cachedMethods();
        int fieldCache = Cache.Companion.cachedFields();

        cmd.append("-DCLASS_CACHE=").append(classCache).append(" ")
                .append("-DMETHOD_CACHE=").append(methodCache).append(" ")
                .append("-DFIELD_CACHE=").append(fieldCache).append(" ");

////        // Linker flags
        cmd.append("-Wl,--gc-sections ")
                .append("-Wl,--sort-section=alignment ")
                .append("-Wl,--discard-all ")
                .append("-Wl,--strip-all ")
                .append("-Wl,--build-id=none ")
                .append("-Wl,--as-needed ")
                .append("-Wl,-Bsymbolic ");
        if (target.contains("linux")) {
            cmd.append("-Wl,--hash-style=gnu ")
                    .append("-Wl,-z,max-page-size=4096 ")
                    .append("-Wl,-z,relro,-z,now ")
                    .append("-Wl,-z,noexecstack ");
        } else if (target.contains("windows")) {
            cmd.append("-Wl,--dynamicbase ")
                    .append("-Wl,--nxcompat ");
        }

        return cmd.toString();
    }

    public byte[] compress(byte[] bin) throws IOException {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        try (GZIPOutputStream gzip = new GZIPOutputStream(baos)) {
            gzip.write(bin);
        }
        return baos.toByteArray();
    }
}
