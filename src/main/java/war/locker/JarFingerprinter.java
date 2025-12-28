package war.locker;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.math.BigInteger;
import java.nio.charset.StandardCharsets;
import java.nio.file.AtomicMoveNotSupportedException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.security.*;
import java.security.spec.MGF1ParameterSpec;
import java.security.spec.PKCS8EncodedKeySpec;
import java.security.spec.PSSParameterSpec;
import java.security.spec.RSAPrivateCrtKeySpec;
import java.util.*;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;
import java.util.zip.ZipOutputStream;

public final class JarFingerprinter {

    private JarFingerprinter() {}

    // Configured once, then used by inject(File)
    private static volatile PrivateKey DEFAULT_KEY;
    private static volatile String libPath = null;

    /** Configure the private key (preferred if you already have a PrivateKey). */
    public static void configurePrivateKey(PrivateKey key) {
        if (key == null) throw new NullPointerException("key");
        DEFAULT_KEY = key;
    }

    /** Configure the private key from PEM bytes (PKCS#8 or PKCS#1 RSA). */
    public static void configurePrivateKeyFromPem(byte[] pemBytes) throws GeneralSecurityException {
        if (pemBytes == null) throw new NullPointerException("pemBytes");
        DEFAULT_KEY = loadPrivateKeyPem(pemBytes);
    }

    /** Configure the private key from a classpath resource (e.g. "/fingerprint_key.pem"). */
    public static void configurePrivateKeyFromClasspath(String resourcePath) throws IOException, GeneralSecurityException {
        if (resourcePath == null) throw new NullPointerException("resourcePath");
        try (InputStream in = JarFingerprinter.class.getResourceAsStream(resourcePath)) {
            if (in == null) throw new FileNotFoundException("Resource not found: " + resourcePath);
            byte[] pem = in.readAllBytes();
            configurePrivateKeyFromPem(pem);
        }
    }

    // =========================
    // Simple, smooth API
    // =========================

    /** Injects/updates META-INF/FINGERPRINT in-place using the configured key. */
    public static File inject(File jar) throws IOException, GeneralSecurityException {
        if (DEFAULT_KEY == null) {
            throw new IllegalStateException("Private key not configured. Call configurePrivateKey*(...) first");
        }
        return inject(jar, DEFAULT_KEY);
    }

    /** Injects/updates META-INF/FINGERPRINT in-place using the provided key. */
    public static File inject(File jar, PrivateKey key) throws IOException, GeneralSecurityException {
        Objects.requireNonNull(jar, "jar");
        if (!jar.isFile()) throw new FileNotFoundException("Jar does not exist: " + jar.getAbsolutePath());

        Path jarPath = jar.toPath();
        String fingerprint = calculateFingerprintFromJar(jarPath, key);

        // Write to temp, then atomically replace original
        Path tmp = Files.createTempFile(jarPath.getParent(), "fp-", ".jar");
        try {
            writeJarWithFingerprint(jarPath, tmp, fingerprint); // overwrites existing FINGERPRINT if present
            try {
                Files.move(tmp, jarPath, StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE);
            } catch (AtomicMoveNotSupportedException e) {
                Files.move(tmp, jarPath, StandardCopyOption.REPLACE_EXISTING);
            }
        } finally {
            try { Files.deleteIfExists(tmp); } catch (Exception ignored) {}
        }
        return jar;
    }

    // =========================
    // Core logic
    // =========================

    private static String calculateFingerprintFromJar(Path jarPath, PrivateKey privateKey) throws IOException, GeneralSecurityException {
        List<String> sha256Hexes = new ArrayList<>();

        try (ZipFile zip = new ZipFile(jarPath.toFile())) {
            Enumeration<? extends ZipEntry> it = zip.entries();
            while (it.hasMoreElements()) {
                ZipEntry e = it.nextElement();
                if (e.isDirectory()) continue;
                String name = e.getName().replace('\\', '/');
                if (!name.startsWith(libPath)) continue;
                if (name.endsWith("/")) continue;

                String leaf = name.substring(name.lastIndexOf('/') + 1);
                if ("Loader.class".equals(leaf)) continue;

                try (InputStream in = zip.getInputStream(e)) {
                    byte[] data = in.readAllBytes();
                    sha256Hexes.add(toHex(digest("SHA-256", data)));
                }
            }
        }

        if (sha256Hexes.isEmpty()) {
            throw new IllegalStateException("No files found under " + libPath + " to fingerprint");
        }

        // Original program Doesn't sort, removed for feature parity.
        //Collections.sort(sha256Hexes);

        String concat = String.join("", sha256Hexes);
        String resultHash = toHex(digest("SHA-384", concat.getBytes(StandardCharsets.UTF_8)));

        byte[] signature = signPssSha256(privateKey, resultHash.getBytes(StandardCharsets.UTF_8));
        String hexSig = toHex(signature);

        // Match your Python output shape 
        //Thank you habibi :3
        return resultHash + "\n" + hexSig + "\nJNT\n";
    }

    private static void writeJarWithFingerprint(Path inputJar, Path outputJar, String fingerprint) throws IOException {
        try (ZipFile zip = new ZipFile(inputJar.toFile());
             ZipOutputStream out = new ZipOutputStream(Files.newOutputStream(outputJar))) {

            // Copy everything except META-INF/FINGERPRINT
            Enumeration<? extends ZipEntry> it = zip.entries();
            while (it.hasMoreElements()) {
                ZipEntry e = it.nextElement();
                String name = e.getName().replace('\\', '/');
                if ("META-INF/FINGERPRINT".equals(name)) {
                    // skip existing; we'll replace
                    continue;
                }

                ZipEntry copy = new ZipEntry(name);
                copy.setTime(e.getTime());
                // Use deflated for broad compatibility
                copy.setMethod(ZipEntry.DEFLATED);
                out.putNextEntry(copy);
                try (InputStream in = zip.getInputStream(e)) {
                    in.transferTo(out);
                }
                out.closeEntry();
            }

            // Ensure META-INF/FINGERPRINT exists (Zip will create parent implicitly)
            ZipEntry fp = new ZipEntry("META-INF/FINGERPRINT");
            fp.setTime(System.currentTimeMillis());
            out.putNextEntry(fp);
            out.write(fingerprint.getBytes(StandardCharsets.UTF_8));
            out.closeEntry();
        }
    }

    // =========================
    // Crypto helpers
    // =========================

    private static byte[] digest(String algo, byte[] data) throws GeneralSecurityException {
        MessageDigest md = MessageDigest.getInstance(algo);
        return md.digest(data);
    }

    /** RSASSA-PSS with SHA-256, MGF1(SHA-256), saltLen=32. */
    private static byte[] signPssSha256(PrivateKey key, byte[] message) throws GeneralSecurityException {
        Signature sig = Signature.getInstance("RSASSA-PSS");
        PSSParameterSpec spec = new PSSParameterSpec(
                "SHA-256",
                "MGF1",
                MGF1ParameterSpec.SHA256,
                32, // correct way to do it as-per https://docs.oracle.com/javase/10/docs/api/java/security/spec/class-use/AlgorithmParameterSpec.html
                1   // trailer field
        );
        sig.setParameter(spec);
        sig.initSign(key);
        sig.update(message);
        return sig.sign();
    }

    private static String toHex(byte[] bytes) {
        StringBuilder sb = new StringBuilder(bytes.length * 2);
        for (byte b : bytes) sb.append(String.format("%02x", b));
        return sb.toString();
    }

    // =========================
    // PEM loading (PKCS#8 or PKCS#1 RSA)
    // =========================
    //Only key we'll be using us PKCS#8, That's what the Frontend supports.

    private static PrivateKey loadPrivateKeyPem(byte[] pemBytes) throws GeneralSecurityException {
        String pem = new String(pemBytes, StandardCharsets.US_ASCII)
                .replace("\r", "")
                .trim();

        if (pem.contains("-----BEGIN PRIVATE KEY-----")) {
            byte[] der = base64Between(pem, "-----BEGIN PRIVATE KEY-----", "-----END PRIVATE KEY-----");
            KeyFactory kf = KeyFactory.getInstance("RSA");
            return kf.generatePrivate(new PKCS8EncodedKeySpec(der));
        }

        if (pem.contains("-----BEGIN RSA PRIVATE KEY-----")) {
            byte[] der = base64Between(pem, "-----BEGIN RSA PRIVATE KEY-----", "-----END RSA PRIVATE KEY-----");
            RSAPrivateCrtKeySpec spec = parsePkcs1RsaPrivateKey(der);
            KeyFactory kf = KeyFactory.getInstance("RSA");
            return kf.generatePrivate(spec);
        }

        throw new GeneralSecurityException("Unsupported PEM format. Expect PKCS#8 or PKCS#1 RSA");
    }

    private static byte[] base64Between(String pem, String begin, String end) throws GeneralSecurityException {
        int s = pem.indexOf(begin);
        int e = pem.indexOf(end);
        if (s < 0 || e < 0 || e <= s) throw new GeneralSecurityException("PEM markers not found: " + begin + " ... " + end);
        String body = pem.substring(s + begin.length(), e).replace("\n", "").replace("\t", "").replace(" ", "");
        return Base64.getMimeDecoder().decode(body);
    }

    // Minimal DER reader for PKCS#1 RSA key
    private static RSAPrivateCrtKeySpec parsePkcs1RsaPrivateKey(byte[] der) throws GeneralSecurityException {
        DerReader r = new DerReader(der);
        r.expectSequence();
        r.readInteger(); // version
        BigInteger n  = r.readInteger();
        BigInteger e  = r.readInteger();
        BigInteger d  = r.readInteger();
        BigInteger p  = r.readInteger();
        BigInteger q  = r.readInteger();
        BigInteger dP = r.readInteger();
        BigInteger dQ = r.readInteger();
        BigInteger qI = r.readInteger();
        return new RSAPrivateCrtKeySpec(n, e, d, p, q, dP, dQ, qI);
    }

    public static void setLibraryPath(String path) {
        libPath = path;
    }

    // Tiny ASN.1 DER reader (only INTEGER + SEQUENCE + definite lengths)
    private static final class DerReader {
        private final byte[] buf;
        private int pos = 0;
        DerReader(byte[] buf) { this.buf = buf; }

        void expectSequence() throws GeneralSecurityException {
            int tag = readByte();
            if (tag != 0x30) throw new GeneralSecurityException("Expected SEQUENCE");
            int len = readLength();
            // We trust structure; not slicing for brevity
        }

        BigInteger readInteger() throws GeneralSecurityException {
            int tag = readByte();
            if (tag != 0x02) throw new GeneralSecurityException("Expected INTEGER");
            int len = readLength();
            byte[] v = readBytes(len);
            return new BigInteger(v);
        }

        private int readByte() throws GeneralSecurityException {
            if (pos >= buf.length) throw new GeneralSecurityException("Unexpected EOF");
            return buf[pos++] & 0xFF;
        }

        private byte[] readBytes(int len) throws GeneralSecurityException {
            if (pos + len > buf.length) throw new GeneralSecurityException("Unexpected EOF");
            byte[] out = Arrays.copyOfRange(buf, pos, pos + len);
            pos += len;
            return out;
        }

        private int readLength() throws GeneralSecurityException {
            int b = readByte();
            if ((b & 0x80) == 0) return b;
            int count = b & 0x7F;
            if (count == 0 || count > 4) throw new GeneralSecurityException("Invalid length");
            int len = 0;
            for (int i = 0; i < count; i++) len = (len << 8) | readByte();
            return len;
        }
    }
}
