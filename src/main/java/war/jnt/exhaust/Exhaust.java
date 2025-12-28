package war.jnt.exhaust;

import com.google.common.io.MoreFiles;
import com.google.common.io.RecursiveDeleteOption;
import war.jnt.dash.Ansi;
import war.jnt.dash.Level;
import war.jnt.dash.Logger;
import war.jnt.dash.Origin;
import war.jnt.utility.timing.Timing;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

import static war.jnt.dash.Ansi.Color.WHITE;

public class Exhaust {

    private static final Logger logger = Logger.INSTANCE;
    private static final Timing timing = new Timing();

    public void prepare(String path) {
        timing.begin();

        try {
            Files.createDirectories(Paths.get(path));

            Path build = Paths.get(path + "/build");
            Path classes = Paths.get(path + "/classes");

            if (classes.toFile().exists()) {
                MoreFiles.deleteDirectoryContents(classes, RecursiveDeleteOption.ALLOW_INSECURE);
            }

            if (build.toFile().exists()) {
                MoreFiles.deleteDirectoryContents(build, RecursiveDeleteOption.ALLOW_INSECURE);
            }

            Files.deleteIfExists(classes);
            Files.createDirectories(classes);

            Files.deleteIfExists(build);
            Files.createDirectories(build);

            Files.createDirectories(Paths.get(path + "/lib"));

            String header = Files.readString(Paths.get("intrinsics/intrinsics.h"));
            String impl = Files.readString(Paths.get("intrinsics/intrinsics.c"));

            Files.write(Paths.get(path + "/lib/intrinsics.h"), header.getBytes());
            Files.write(Paths.get(path + "/lib/intrinsics.c"), impl.getBytes());

            String jni = Files.readString(Paths.get("jni/jni.h"));
            Files.write(Paths.get(path + "/lib/jni.h"), jni.getBytes());

            File[] helpers = new File("helpers").listFiles();
            if (helpers != null) {
                for (File helper : helpers) {
                    if (helper.isFile()) {
                        String content = Files.readString(helper.toPath());
                        Files.write(Paths.get(path + "/lib/" + helper.getName()), content.getBytes());
                    }
                }
            }

        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        timing.end();

        long elapsed = timing.calc();
        logger.logln(Level.INFO, Origin.EXHAUST, String.format("Prepared output directories in %s.", new Ansi().c(WHITE).s(String.format("%dms", elapsed))));
    }

    public static void write(String fileName, byte[] data, String path) {
        try {
            Files.write(Paths.get(String.format("%s/%s", path, fileName)), data);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }
}
