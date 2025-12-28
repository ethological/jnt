package war.locker;

import java.io.*;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.jar.JarEntry;
import java.util.jar.JarInputStream;
import java.util.jar.JarOutputStream;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

public class LFSplitter {

    public static void main(String[] args) throws IOException {
        if (args.length < 1) {
            System.out.println("Usage: java LFSplitter <jar-path>");
            return;
        }

        File originalJar = new File(args[0]);
        if (!originalJar.exists()) {
            System.err.println("JAR file not found: " + args[0]);
            return;
        }

        // Load all native paths
        String[] natives = {
                "dev/krypton/jnt3/x86_64-windows",
                "dev/krypton/jnt3/x86_64-linux-gnu",
                "dev/krypton/jnt3/x86_64-macos",
//                "war/jnt/aarch64-linux",
                "dev/krypton/jnt3/aarch64-macos",
//                "war/jnt/aarch64-windows"
        };

        // Load original JAR entries into memory
        Map<String, byte[]> jarEntries = new LinkedHashMap<>();
        try (JarInputStream jis = new JarInputStream(new FileInputStream(originalJar))) {
            JarEntry entry;
            while ((entry = jis.getNextJarEntry()) != null) {
                ByteArrayOutputStream baos = new ByteArrayOutputStream();
                byte[] buffer = new byte[4096];
                int read;
                while ((read = jis.read(buffer)) != -1) {
                    baos.write(buffer, 0, read);
                }
                jarEntries.put(entry.getName(), baos.toByteArray());
            }
        }

        String outputZipName = new File(originalJar.getParentFile(), originalJar.getName().replace(".jar", "-split.zip")).getAbsolutePath();
        ZipOutputStream zos = new ZipOutputStream(new FileOutputStream(outputZipName));
        zos.setLevel(9);

        for (String nativePath : natives) {
            String nativeId = nativePath.replace("dev/krypton/jnt3/", "");
            File outputJar = new File(originalJar.getParentFile(), originalJar.getName().replace(".jar", "-" + nativeId + ".jar"));

            try (JarOutputStream jos = new JarOutputStream(new FileOutputStream(outputJar))) {
                jos.setLevel(9);
                for (Map.Entry<String, byte[]> e : jarEntries.entrySet()) {
                    String entryName = e.getKey();

                    if (!entryName.equals(nativePath)) {
                        boolean notOurs = false;
                        for (String s : natives)
                            if (s.equals(entryName)) {
                                notOurs = true;
                                break;
                            }
                        if (notOurs) {
                            continue;
                        }
                    }


                    JarEntry newEntry = new JarEntry(entryName);
                    jos.putNextEntry(newEntry);
                    jos.write(e.getValue());
                    jos.closeEntry();
                }
            }

            FileInputStream fis = new FileInputStream(outputJar);

            zos.putNextEntry(new ZipEntry(outputJar.getName()));
            byte[] buffer = new byte[4096];
            int bytesRead;
            while ((bytesRead = fis.read(buffer)) != -1) {
                zos.write(buffer, 0, bytesRead);
            }
            zos.closeEntry();
            fis.close();

            System.out.println("Created: " + outputJar.getName());
        }

        zos.close();
    }
}
