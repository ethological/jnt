package war.metaphor.util;

import java.io.IOException;
import java.io.InputStream;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class JavaInternalClasses {

    private static final Map<String, byte[]> cache = new ConcurrentHashMap<>();

    public static byte[] get(String internalName) {
        return cache.computeIfAbsent(internalName, JavaInternalClasses::load);
    }

    private static byte[] load(String internalName) {
        String path = internalName + ".class";
        try (InputStream in = ClassLoader.getSystemResourceAsStream(path)) {
            if (in != null) {
                return in.readAllBytes();
            }
        } catch (IOException ignored) {
            //
        }
        return null;
    }
}
