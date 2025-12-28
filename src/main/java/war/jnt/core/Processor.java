package war.jnt.core;

import lombok.Getter;
import war.configuration.ConfigurationSection;
import war.jar.JarReader;
import war.jnt.cache.CacheLookup;
import war.jnt.core.code.impl.IndyUnit;
import war.jnt.core.header.Header;
import war.jnt.core.header.HeaderProcessor;
import war.jnt.core.loader.NativeLoader;
import war.jnt.core.name.NameProcessor;
import war.jnt.core.source.Source;
import war.jnt.core.source.SourceProcessor;
import war.jnt.dash.Level;
import war.jnt.dash.Logger;
import war.jnt.dash.Origin;
import war.jnt.exhaust.Exhaust;
import war.jnt.obfuscation.StringLookup;
import war.metaphor.processor.FilterProcessor;
import war.metaphor.tree.JClassNode;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

@Getter
public class Processor {

    public static Processor INSTANCE;

    private final List<Header> headers = new ArrayList<>();
    private final List<Source> sources = new ArrayList<>();

    public Set<JClassNode> classes;

    public Processor() {
        INSTANCE = this;
    }

    public void process(JarReader ctx, NativeLoader nativeLoader, ConfigurationSection config, String path) {
        this.classes = ctx.getClasses();

        ConfigurationSection rootSection = config.getConfigurationSection("mutators.jnt");
        if (rootSection == null)
            throw new RuntimeException("Failed to load mutators.jnt section");

        var filter = new FilterProcessor(rootSection);
        filter.process(classes, null);

        int total = classes.size();
        int current = 0;
        final int BAR_WIDTH = 40;
        Logger.INSTANCE.logln(Level.INFO, Origin.EXHAUST, "Generating C source files...");

        for (var node : classes) {
            current++;
            if (current % 50 == 0 || current == total) {
                int percent = (int) ((current * 100.0) / total);
                int filled = (percent * BAR_WIDTH) / 100;
                String bar = String.format("Generating [%s%s] %3d%%",
                        "=".repeat(filled), " ".repeat(BAR_WIDTH - filled), percent);
                Logger.INSTANCE.rlog(Level.INFO, Origin.EXHAUST, bar);
            }

            if (node.isExempt()) continue;

            String source = SourceProcessor.forClass(node, config);
            String header = HeaderProcessor.forClass(node);
            sources.add(new Source("classes/" + NameProcessor.forClass(node.name) + ".c", source));
            headers.add(new Header("classes/" + NameProcessor.forClass(node.name) + ".h", header));
        }

        Logger.INSTANCE.rlog(Level.INFO, Origin.EXHAUST, String.format("Generating [%s] 100%%\n", "=".repeat(BAR_WIDTH)));

        nativeLoader.make(this, config);
        CacheLookup.make(this);
        StringLookup.make(this);
        IndyUnit.make(this);
        write(path);
    }


    private void write(String path) {
        for (var header : headers) {
            byte[] data = header.getBuffer().toString().getBytes();
            Exhaust.write(header.getName(), data, path);
            header.getBuffer().setLength(0);
        }
        for (var src : sources) {
            byte[] data = src.getBuffer().toString().getBytes();
            Exhaust.write(src.getName(), data, path);
            src.getBuffer().setLength(0);
        }
    }

    public void clear() {
        headers.clear();
        sources.clear();
        classes = null;
    }
}
