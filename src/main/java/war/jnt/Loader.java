package war.jnt;

import java.io.*;
import java.util.zip.GZIPInputStream;

public class Loader {

    static {
        File file;
        String string = System.getProperty("os.name").toLowerCase();
        String string2 = System.getProperty("os.arch").toLowerCase();
        String string3 = "/war/jnt/";
        String string4 = string3;
        if (string.contains("mac")) {
            if (string2.equals("aarch64")) {
                string3 = string3.concat("aarch64-macos");
            } else if (string2.equals("x86_64") || string2.equals("amd64")) {
                string3 = string3.concat("x86_64-macos");
            }
        } else if (string.contains("win")) {
            if (string2.equals("aarch64")) {
                string3 = string3.concat("aarch64-windows");
            } else if (string2.equals("x86_64") || string2.equals("amd64")) {
                string3 = string3.concat("x86_64-windows");
            }
        } else if (string.contains("lin")) {
            if (string2.equals("aarch64")) {
                string3 = string3.concat("aarch64-linux");
            } else if (string2.equals("x86_64") || string2.equals("amd64")) {
                string3 = string3.concat("x86_64-linux");
                string4 = string4.concat("x86_64-linux-gnu");
            }
        }
        if (string3.hashCode() == 1388925557) {
            throw new UnsatisfiedLinkError("Unsupported os/arch: ".concat(string).concat("/").concat(string2));
        }
        try {
            file = File.createTempFile("lib", null);
            file.deleteOnExit();
            if (!file.exists()) {
                throw new IOException();
            }
        }
        catch (IOException iOException) {
            throw new UnsatisfiedLinkError("Failed to create temp file");
        }
        byte[] byArray = new byte[2048];
        try (FileOutputStream fileOutputStream = new FileOutputStream(file)) {
            Class<Loader> clazz = Loader.class;
            try (InputStream inputStream = clazz.getResourceAsStream(string3)) {
                int n;
                if (inputStream == null) {
                    throw new UnsatisfiedLinkError("Couldn't find lib: ".concat(string3));
                }
                ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
                while ((n = inputStream.read(byArray)) != -1) {
                    byteArrayOutputStream.write(byArray, 0, n);
                }
                byteArrayOutputStream.close();
                byte[] decompressedData = decompress(byteArrayOutputStream.toByteArray());
                fileOutputStream.write(decompressedData);
                fileOutputStream.close();
                System.load(file.getAbsolutePath());
                file.deleteOnExit();
            }
        }
        catch (UnsatisfiedLinkError unsatisfiedLinkError) {
            if (string4.equals(string3)) throw unsatisfiedLinkError;
            try (FileOutputStream fileOutputStream = new FileOutputStream(file)) {
                Class<Loader> clazz = Loader.class;
                try (InputStream inputStream = clazz.getResourceAsStream(string4)) {
                    int n;
                    if (inputStream == null) {
                        throw new UnsatisfiedLinkError("Couldn't find lib: ".concat(string4) + " (tried " + string3 + " first)");
                    }
                    ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
                    while ((n = inputStream.read(byArray)) != -1) {
                        byteArrayOutputStream.write(byArray, 0, n);
                    }
                    byteArrayOutputStream.close();
                    byte[] decompressedData = decompress(byteArrayOutputStream.toByteArray());
                    fileOutputStream.write(decompressedData);
                    fileOutputStream.close();
                    System.load(file.getAbsolutePath());
                    file.deleteOnExit();
                }
            } catch (IOException iOException) {
                throw new UnsatisfiedLinkError("Failed to extract file: ".concat(iOException.getMessage()));
            }
        }
        catch (IOException iOException) {
            throw new UnsatisfiedLinkError("Failed to extract file: ".concat(iOException.getMessage()));
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

    public static native void init(Class<?> clazz);

}
