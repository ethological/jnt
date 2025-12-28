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
import java.util.zip.GZIPOutputStream;

import static war.jnt.dash.Ansi.Color.*;

public class Compiler implements ICompiler {

    private static final Logger logger = Logger.INSTANCE;
    private static final Timing timing = new Timing();
    private final Processor processor;

    public Compiler(Processor processor) {
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

                        String commandLine = gcc + " " + getLinkCommandLine(targetPath, target);
                        process(commandLine, debugging.contains(target));

                        File out = new File(targetPath + "/out.jnt");
                        File compressed = new File(targetPath + "/out.gz");

                        byte[] bin = Files.readAllBytes(out.toPath());
                        byte[] compressedBin = compress(bin);

                        try (FileOutputStream fos = new FileOutputStream(compressed)) {
                            fos.write(compressedBin);
                        }
                    } catch (Exception e) {
                        logger.logln(Level.FATAL, Origin.EXHAUST, String.format("Failed to compile for %s.", new Ansi().c(RED).s(target).r(false).c(BRIGHT_RED)));
                        e.printStackTrace(System.err);
                    }
                });
            }
        } catch (Exception e) {
            throw new RuntimeException(e);
        }

        timing.end();

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

    public String getLinkCommandLine(String out, String target) {

        List<String> sources = new ArrayList<>();
        for (Source source : processor.getSources()) {
            sources.add(source.getName());
        }

        sources.add("lib/intrinsics.c");

        StringBuilder cmd = new StringBuilder();

        // Base compiler configuration
        cmd.append("-O0 ")
                .append("-fno-semantic-interposition ")
                .append("-march=native -mtune=native ")
                .append("-funroll-loops ")
                .append("-finline-functions ")
                .append("-ftree-slp-vectorize -ftree-vectorize -ftree-loop-vectorize ")
                .append("-ffast-math -fno-math-errno -fno-signed-zeros -fno-trapping-math ")
                .append("-fstrict-overflow -fstrict-aliasing -fomit-frame-pointer ")
                .append("-fPIC -fvisibility=hidden -ffunction-sections -fdata-sections ")
                .append("-Wno-incompatible-pointer-types ")
                .append("-D_GLIBCXX_ASSERTIONS -Wformat -Werror=format-security ")
                .append("-shared ");
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

        // Linker flags
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

        // Append source files properly
        for (String source : sources) {
            // Here we format the source paths properly
            cmd.append(" ").append(new File(out, source).getAbsolutePath());
        }

        // Output path formatted properly
        cmd.append(" -o ").append(out).append(File.separator).append("out.jnt");

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
