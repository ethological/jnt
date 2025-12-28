package war.metaphor.mutator;

import org.objectweb.asm.Opcodes;
import war.configuration.ConfigurationSection;
import war.metaphor.base.ObfuscatorContext;
import war.metaphor.util.interfaces.IRandom;

import java.util.Random;

public abstract class Mutator implements IRandom, Opcodes {

    protected final ObfuscatorContext base;
    protected final ConfigurationSection config;

    protected Random rand = new Random();

    public Mutator(ObfuscatorContext base, ConfigurationSection config) {
        this.base = base;
        this.config = config;
    }

    public boolean isEnabled() {
        return config == null || config.getBoolean("enabled", true);
    }

    public abstract void run(ObfuscatorContext base);

}
