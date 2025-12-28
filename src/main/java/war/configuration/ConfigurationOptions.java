package war.configuration;

public class ConfigurationOptions {

    private final Configuration configuration;
    private char pathSeparator = '.';
    private boolean copyDefaults = false;

    protected ConfigurationOptions(Configuration configuration) {
        this.configuration = configuration;
    }

    public Configuration configuration() {
        return this.configuration;
    }

    public char pathSeparator() {
        return this.pathSeparator;
    }

    public ConfigurationOptions pathSeparator(char value) {
        this.pathSeparator = value;
        return this;
    }

    public boolean copyDefaults() {
        return this.copyDefaults;
    }

    public ConfigurationOptions copyDefaults(boolean value) {
        this.copyDefaults = value;
        return this;
    }
}

