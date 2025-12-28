package war.configuration.file;

import war.configuration.Configuration;
import war.configuration.ConfigurationSection;
import war.configuration.InvalidConfigurationException;
import org.apache.commons.lang3.Validate;
import org.yaml.snakeyaml.DumperOptions;
import org.yaml.snakeyaml.Yaml;
import org.yaml.snakeyaml.error.YAMLException;
import org.yaml.snakeyaml.representer.Representer;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.Reader;
import java.util.Iterator;
import java.util.Map;

public class YamlConfiguration extends FileConfiguration {

    private final DumperOptions yamlOptions = new DumperOptions();
    private final Representer yamlRepresenter = new YamlRepresenter();
    private final Yaml yaml;

    public YamlConfiguration() {
        this.yaml = new Yaml(new YamlConstructor(), this.yamlRepresenter, this.yamlOptions);
    }

    public static YamlConfiguration loadConfiguration(File file) {
        Validate.notNull(file, "File cannot be null");
        YamlConfiguration config = new YamlConfiguration();

        try {
            config.load(file);
        } catch (IOException | InvalidConfigurationException e) {
            System.err.println("Cannot load configuration from " + file);
            e.printStackTrace(System.err);
        }

        return config;
    }

    /**
     * @deprecated
     */
    @Deprecated
    public static YamlConfiguration loadConfiguration(InputStream stream) {
        Validate.notNull(stream, "Stream cannot be null");
        YamlConfiguration config = new YamlConfiguration();

        try {
            config.load(stream);
        } catch (IOException | InvalidConfigurationException e) {
            System.err.println("Cannot load configuration from stream");
            e.printStackTrace(System.err);
        }

        return config;
    }

    public static YamlConfiguration loadConfiguration(Reader reader) {
        Validate.notNull(reader, "Stream cannot be null");
        YamlConfiguration config = new YamlConfiguration();

        try {
            config.load(reader);
        } catch (IOException | InvalidConfigurationException e) {
            System.err.println("Cannot load configuration from stream");
            e.printStackTrace(System.err);
        }

        return config;
    }

    public String saveToString() {
        this.yamlOptions.setIndent(this.options().indent());
        this.yamlOptions.setDefaultFlowStyle(DumperOptions.FlowStyle.BLOCK);
        this.yamlOptions.setAllowUnicode(SYSTEM_UTF);
        this.yamlRepresenter.setDefaultFlowStyle(DumperOptions.FlowStyle.BLOCK);
        String header = this.buildHeader();
        String dump = this.yaml.dump(this.getValues(false));
        if (dump.equals("{}\n")) {
            dump = "";
        }

        return header + dump;
    }

    public void loadFromString(String contents) throws InvalidConfigurationException {
        Validate.notNull(contents, "Contents cannot be null");

        Map<?, ?> input;
        try {
            input = this.yaml.load(contents);
        } catch (YAMLException var4) {
            throw new InvalidConfigurationException(var4);
        } catch (ClassCastException var5) {
            throw new InvalidConfigurationException("Top level is not a Map.");
        }

        String header = this.parseHeader(contents);
        if (!header.isEmpty()) {
            this.options().header(header);
        }

        if (input != null) {
            this.convertMapsToSections(input, this);
        }
    }

    protected void convertMapsToSections(Map<?, ?> input, ConfigurationSection section) {
        for (Map.Entry<?, ?> item : input.entrySet()) {
            String key = item.getKey().toString();
            Object value = item.getValue();
            if (value instanceof Map) {
                this.convertMapsToSections((Map<?, ?>) value, section.createSection(key));
            } else {
                section.set(key, value);
            }
        }
    }

    protected String parseHeader(String input) {
        String[] lines = input.split("\r?\n", -1);
        StringBuilder result = new StringBuilder();
        boolean readingHeader = true;
        boolean foundHeader = false;

        for (int i = 0; i < lines.length && readingHeader; ++i) {
            String line = lines[i];
            if (line.startsWith("# ")) {
                if (i > 0) {
                    result.append("\n");
                }

                if (line.length() > "# ".length()) {
                    result.append(line.substring("# ".length()));
                }

                foundHeader = true;
            } else if (foundHeader && line.isEmpty()) {
                result.append("\n");
            } else if (foundHeader) {
                readingHeader = false;
            }
        }

        return result.toString();
    }

    protected String buildHeader() {
        String header = this.options().header();
        if (this.options().copyHeader()) {
            Configuration def = this.getDefaults();
            if (def instanceof FileConfiguration file) {
                String defaultsHeader = file.buildHeader();
                if (defaultsHeader != null && !defaultsHeader.isEmpty()) {
                    return defaultsHeader;
                }
            }
        }

        if (header == null) {
            return "";
        } else {
            StringBuilder builder = new StringBuilder();
            String[] lines = header.split("\r?\n", -1);
            boolean startedHeader = false;

            for (int i = lines.length - 1; i >= 0; --i) {
                builder.insert(0, "\n");
                if (startedHeader || !lines[i].isEmpty()) {
                    builder.insert(0, lines[i]);
                    builder.insert(0, "# ");
                    startedHeader = true;
                }
            }

            return builder.toString();
        }
    }

    public YamlConfigurationOptions options() {
        if (this.options == null) {
            this.options = new YamlConfigurationOptions(this);
        }

        return (YamlConfigurationOptions) this.options;
    }
}
