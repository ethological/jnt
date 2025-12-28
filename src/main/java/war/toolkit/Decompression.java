package war.toolkit;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.zip.GZIPInputStream;

public class Decompression {
    public static void main(String[] args) {
        try {
            byte[] bytes = Files.readAllBytes(Paths.get("x86_64-win.dll"));
            byte[] decompressed = decompress(bytes);

            Files.write(Paths.get("decompressed.dll"), decompressed);
        } catch (Throwable t) {
            throw new RuntimeException(t);
        }
    }

    public static byte[] decompress(byte[] bin) throws IOException {
        ByteArrayInputStream bais = new ByteArrayInputStream(bin);
        try (GZIPInputStream gzip = new GZIPInputStream(bais);
             ByteArrayOutputStream baos = new ByteArrayOutputStream()) {

            byte[] buffer = new byte[1024];
            int length;
            while ((length = gzip.read(buffer)) > 0) {
                baos.write(buffer, 0, length);
            }

            return baos.toByteArray();
        }
    }
}
