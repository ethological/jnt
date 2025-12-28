package war.jnt.exhaust.compiler;

import war.configuration.ConfigurationSection;

public interface ICompiler {
    void run(ConfigurationSection cfg, String dir);
}
