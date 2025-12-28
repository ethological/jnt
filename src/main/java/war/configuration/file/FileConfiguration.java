package war.configuration.file;

import com.google.common.base.Charsets;
import com.google.common.io.Files;
import war.configuration.Configuration;
import war.configuration.InvalidConfigurationException;
import war.configuration.MemoryConfiguration;
import org.apache.commons.lang3.Validate;

import java.io.*;
import java.nio.charset.Charset;

public abstract class FileConfiguration extends MemoryConfiguration {

    public static final boolean UTF8_OVERRIDE = false;
    public static final boolean UTF_BIG = true;
    public static final boolean SYSTEM_UTF = false;

    public FileConfiguration() {}

    public FileConfiguration(Configuration defaults) {
        super(defaults);
    }

    public void save(File file) throws IOException {
        Validate.notNull(file, "File cannot be null");
        Files.createParentDirs(file);
        String data = saveToString();

        try (OutputStreamWriter writer = new OutputStreamWriter(new FileOutputStream(file), UTF8_OVERRIDE && !UTF_BIG ? Charsets.UTF_8 : Charset.defaultCharset())) {
            writer.write(data);
        }
    }

    public void save(String file) throws IOException {
        Validate.notNull(file, "File cannot be null");
        save(new File(file));
    }

    public abstract String saveToString();

    public void load(File file) throws IOException, InvalidConfigurationException {
        Validate.notNull(file, "File cannot be null");
        FileInputStream stream = new FileInputStream(file);
        load(new InputStreamReader(stream, Charset.defaultCharset()));
    }

    /**
     * @deprecated
     */
    @Deprecated
    public void load(InputStream stream) throws IOException, InvalidConfigurationException {
        Validate.notNull(stream, "Stream cannot be null");
        load(new InputStreamReader(stream, UTF8_OVERRIDE ? Charsets.UTF_8 : Charset.defaultCharset()));
    }

    public void load(Reader reader) throws IOException, InvalidConfigurationException {
        BufferedReader input = reader instanceof BufferedReader ? (BufferedReader) reader : new BufferedReader(reader);
        StringBuilder builder = new StringBuilder();

        String line;
        try {
            while ((line = input.readLine()) != null) {
                builder.append(line);
                builder.append('\n');
            }
        } finally {
            input.close();
        }

        loadFromString(builder.toString());
    }

    public void load(String file) throws IOException, InvalidConfigurationException {
        Validate.notNull(file, "File cannot be null");
        load(new File(file));
    }

    public abstract void loadFromString(String var1) throws InvalidConfigurationException;

    protected abstract String buildHeader();

    public FileConfigurationOptions options() {
        if (this.options == null) {
            this.options = new FileConfigurationOptions(this);
        }

        return (FileConfigurationOptions) this.options;
    }
}
