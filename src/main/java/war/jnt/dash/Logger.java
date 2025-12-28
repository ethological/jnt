package war.jnt.dash;

import lombok.Setter;
import war.Entrypoint;

import java.io.IOException;
import java.io.PrintStream;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.concurrent.locks.ReentrantLock;

import static war.jnt.dash.Ansi.Attribute.RESET;
import static war.jnt.dash.Ansi.Color.BRIGHT_RED;
import static war.jnt.dash.Ansi.Color.BRIGHT_YELLOW;

@Setter
public class Logger {

    public static final Logger INSTANCE = new Logger();

    private static final StringBuilder output = new StringBuilder();
    private static final StringBuilder copy = new StringBuilder();
    private Level level = Level.DEBUG;

    private ReentrantLock lock = new ReentrantLock();

    public Logger() {}

    public Logger(Level level) {
        this.level = level;
    }

    public void ascii() {
        if (level == Level.NONE) return;
        if (Entrypoint.JNT_DISTRO == 2) {
            System.out.println("""    
                        /$$$$$ /$$   /$$ /$$$$$$$$
                       |__  $$| $$$ | $$|__  $$__/
                          | $$| $$$$| $$   | $$  \s
                          | $$| $$ $$ $$   | $$  \s
                     /$$  | $$| $$  $$$$   | $$  \s
                    | $$  | $$| $$\\  $$$   | $$  \s
                    |  $$$$$$/| $$ \\  $$   | $$  \s
                     \\______/ |__/  \\__/   |__/  \s
                    """);
        } else if (Entrypoint.JNT_DISTRO == 3) {
            System.out.println("""    
                        /$$$$$ /$$   /$$ /$$$$$$$$ /$$$$$$\s
                       |__  $$| $$$ | $$|__  $$__//$$__  $$
                          | $$| $$$$| $$   | $$  |__/  \\ $$
                          | $$| $$ $$ $$   | $$     /$$$$$/
                     /$$  | $$| $$  $$$$   | $$    |___  $$
                    | $$  | $$| $$\\  $$$   | $$   /$$  \\ $$
                    |  $$$$$$/| $$ \\  $$   | $$  |  $$$$$$/
                     \\______/ |__/  \\__/   |__/   \\______/\s
                    """);
        }
    }

    public synchronized void rlog(Level level, Origin origin, Object... objects) {
        if (level.ordinal() < this.level.ordinal()) {
            return;
        }
        lock.lock();
        long time = System.currentTimeMillis();

        String start = String.format("\r<%d @ %s> [%s] ", time, origin.getOrigin(), level.getLevel());

        for (var obj : objects) {
            System.out.printf("%s%s", start, obj);
            append(start, obj);
        }
        lock.unlock();
    }

    public synchronized void log(Level level, Origin origin, Object... objects) {
        if (level.ordinal() < this.level.ordinal()) {
            return;
        }

        lock.lock();

        String start = String.format("<%d @ %s> [%s] ", System.currentTimeMillis(), origin.getOrigin(), level.getLevel());

        PrintStream out = System.out;

        for (var obj : objects) {
            String color = String.format("%s%sm", Ansi.esc, switch (level) {
                case WARNING -> BRIGHT_YELLOW.getCode();
                case ERROR, FATAL -> BRIGHT_RED.getCode();
                default -> RESET.getCode();
            });
            String reset = String.format("%s%sm", Ansi.esc, RESET.getCode());
            if (obj instanceof Throwable) {
                out.println();
                append("\n");
            }
            out.printf("%s%s%s%s", color, start, obj, reset);
            append(String.format("%s%s%s%s", color, start, obj, reset), String.format("%s%s", start, obj));
            if (obj instanceof Throwable throwable) {
                StringBuilder sb = new StringBuilder();
                for (StackTraceElement ste : throwable.getStackTrace()) {
                    String traceLine = String.format("%n\t%sat %s.%s(%s:%d)%s", color, ste.getClassName(), ste.getMethodName(), ste.getFileName(), ste.getLineNumber(), reset);
                    sb.append(traceLine);
                    out.print(traceLine);
                }
                append(sb.toString(), sb.toString());
            }
        }
        lock.unlock();
    }

    public synchronized void logln(Level level, Origin origin, Object... objects) {
        if (level.ordinal() < this.level.ordinal()) {
            return;
        }
        lock.lock();
        log(level, origin, objects);
        System.out.println();
        append("\n");
        lock.unlock();
    }

    private void append(String log, String raw) {
        output.append(raw);
        copy.append(log);
    }

    public void append(Object... objects) {
        for (var obj : objects) {
            copy.append(obj);
        }
    }

    public String getLog() {
        String log = copy.toString();
        copy.setLength(0);
        return log;
    }

    public void clear() {
        copy.setLength(0);
    }

    public void dump() {
        dump("latest-jnt.txt");
    }

    public void dump(String path) {
        try {
            Files.write(Paths.get(path), output.toString().getBytes());
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }
}
