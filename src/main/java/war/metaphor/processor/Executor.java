package war.metaphor.processor;

import lombok.AllArgsConstructor;
import war.configuration.ConfigurationSection;
import war.jnt.annotate.Warning;
import war.jnt.dash.Ansi;
import war.jnt.dash.Level;
import war.jnt.dash.Logger;
import war.jnt.dash.Origin;
import war.metaphor.base.ObfuscatorContext;
import war.metaphor.mutator.Mutator;
import war.metaphor.tree.JClassNode;

import java.util.*;

import static war.jnt.dash.Ansi.Color.*;

@AllArgsConstructor
public class Executor {

    private final ConfigurationSection config;
    private final ObfuscatorContext context;

    private final Logger logger = Logger.INSTANCE;

    public void process(Set<JClassNode> classes, Class<? extends Mutator> mutator, String section, String path) {
        classes.forEach(JClassNode::removeExempt);

        ConfigurationSection rootSection = config.getConfigurationSection(section);
        if (rootSection == null) return;

        ConfigurationSection transformersSection = rootSection.getConfigurationSection("transformers");
        if (transformersSection == null) return;

        ConfigurationSection mutatorSection = transformersSection.getConfigurationSection(path);
        if (mutatorSection == null) return;

        Mutator instance;
        try {
            instance = mutator.getConstructor(ObfuscatorContext.class, ConfigurationSection.class).newInstance(context,
                    mutatorSection);
        } catch (Exception e) {
            throw new RuntimeException("Failed to create mutator instance", e);
        }

        try {
            if (instance.isEnabled()) {
                FilterProcessor processor = new FilterProcessor(rootSection);
                processor.process(classes, mutatorSection);

                if (mutator.isAnnotationPresent(Warning.class)) {
                    Warning w = mutator.getAnnotation(Warning.class);
                    logger.logln(Level.WARNING, Origin.METAPHOR, String.format("%s", new Ansi().c(YELLOW).s(w.value())));
                }

                logger.logln(Level.INFO, Origin.METAPHOR, String.format("Running %s...", new Ansi().c(WHITE).s(mutator.getSimpleName())));
                instance.run(context);
            }
        } catch (Exception e) {
            logger.logln(Level.ERROR, Origin.METAPHOR, String.format("Failed to run %s: %s", new Ansi().c(RED).s(mutator.getSimpleName()).r(false).c(BRIGHT_RED), e.getMessage()));
            e.printStackTrace(System.err);
        }
    }
}
