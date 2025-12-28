package war.configuration;

import lombok.Getter;
import org.apache.commons.lang3.Validate;

import java.util.Map;
import java.util.Map.Entry;

public class MemoryConfiguration extends MemorySection implements Configuration {

    @Getter
    protected Configuration defaults;
    protected MemoryConfigurationOptions options;

    public MemoryConfiguration() {}

    public MemoryConfiguration(Configuration defaults) {
        this.defaults = defaults;
    }

    public void addDefault(String path, Object value) {
        Validate.notNull(path, "Path may not be null");
        if (this.defaults == null) {
            this.defaults = new MemoryConfiguration();
        }

        this.defaults.set(path, value);
    }

    public void addDefaults(Map<String, Object> defaults) {
        Validate.notNull(defaults, "Defaults may not be null");

        for (Entry<String, Object> stringObjectEntry : defaults.entrySet()) {
            addDefault(stringObjectEntry.getKey(), stringObjectEntry.getValue());
        }
    }

    public void addDefaults(Configuration defaults) {
        Validate.notNull(defaults, "Defaults may not be null");
        this.addDefaults(defaults.getValues(true));
    }

    public void setDefaults(Configuration defaults) {
        Validate.notNull(defaults, "Defaults may not be null");
        this.defaults = defaults;
    }

    public ConfigurationSection getParent() {
        return null;
    }

    public MemoryConfigurationOptions options() {
        if (this.options == null) {
            this.options = new MemoryConfigurationOptions(this);
        }

        return this.options;
    }
}
