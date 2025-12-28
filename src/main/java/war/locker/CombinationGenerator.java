package war.locker;

import lombok.SneakyThrows;
import org.benf.cfr.reader.api.CfrDriver;
import org.benf.cfr.reader.api.OutputSinkFactory;
import org.benf.cfr.reader.api.SinkReturns;
import war.Entrypoint;
import war.configuration.file.YamlConfiguration;
import war.jnt.dash.Level;
import war.jnt.dash.Logger;
import war.jnt.dash.Origin;
import war.metaphor.base.ObfuscatorContext;
import war.metaphor.tree.Hierarchy;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.*;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Consumer;

public class CombinationGenerator {

    public static void main(String[] args) {

        Logger logger = Logger.INSTANCE;

        File comboConfig = new File("src/test/resources/combo.yml");

        Map<String, List<String>> mutators = new HashMap<>();

        String root = "mutators.metaphor.transformers.";

        mutators.put("inlining", List.of("inlining"));
        mutators.put("access-unify", List.of("access-unify"));
        mutators.put("field-initialize", List.of("field-initialize"));
        mutators.put("renamer", List.of("renamer.method",
                "renamer.class",
                "renamer.field"));
        mutators.put("string-light", List.of("string.light"));
        mutators.put("string-poly", List.of("string.poly"));
        mutators.put("flow", List.of("flow.flattening", "flow.shuffle", "flow.break"));
        mutators.put("number", List.of("number.table"));
        mutators.put("salt", List.of("number.salt"));
        mutators.put("strip", List.of("strip"));

        File combosDir = new File("combos");
        if (!combosDir.exists()) {
            if (!combosDir.mkdirs()) {
                throw new RuntimeException("Failed to create combos directory: " + combosDir.getAbsolutePath());
            }
        }

        List<String> mutatorNames = new ArrayList<>(mutators.keySet());
        Collections.sort(mutatorNames);

        List<File> configs = new ArrayList<>();

        int n = mutatorNames.size();

        for (int mask = 0; mask < (1 << n); mask++) {
            YamlConfiguration configCopy = YamlConfiguration.loadConfiguration(comboConfig);

            List<String> selectedMutators = new ArrayList<>();
            for (int i = 0; i < n; i++) {
                if ((mask & (1 << i)) != 0) {
                    selectedMutators.add(mutatorNames.get(i));
                }
            }

            StringBuilder pathBuilder = new StringBuilder("combos");
            for (String mutator : selectedMutators) {
                pathBuilder.append(File.separator).append(mutator);
            }
            File comboFolder = new File(pathBuilder.toString());
            if (!comboFolder.exists()) if (!comboFolder.mkdirs()) {
                throw new RuntimeException("Failed to create combo folder: " + comboFolder.getAbsolutePath());
            }

            for (String mutator : selectedMutators) {
                List<String> keys = mutators.get(mutator);
                for (String key : keys) {
                    configCopy.set(root + key + ".enabled", true);
                }
            }

            File metaphorOutput = new File(comboFolder, "metaphor-temp.jar");
            configCopy.set("output", metaphorOutput.getAbsolutePath());

            File outputFile = new File(comboFolder, "config.yml");
            try {
                configCopy.save(outputFile);
                configs.add(outputFile);
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }

        System.out.println("Running " + configs.size() + " combinations...");

        int total = configs.size();
        int count = 0;

        final int BAR_WIDTH = 50;

        for (File config : configs) {
            count++;

            int percent = (count * 100) / total;

            int filled = (percent * BAR_WIDTH) / 100;
            int empty  = BAR_WIDTH - filled;

            String bar = String.format("[%s%s] %3d%%", "=".repeat(Math.max(0, filled)), " ".repeat(Math.max(0, empty)), percent);

            logger.rlog(Level.NONE, Origin.EXHAUST, bar);

            String decompiled = runConfig(config);
            File outputFile = new File(config.getParentFile(), "decompiled.txt");
            try {
                Files.writeString(outputFile.toPath(), decompiled);
                Files.deleteIfExists(config.toPath());
                Files.deleteIfExists(new File(config.getParentFile(), "metaphor-temp.jar").toPath());
            } catch (IOException e) {
                throw new RuntimeException("Failed to write decompiled output to " + outputFile.getAbsolutePath(), e);
            }
        }

        String bar = String.format("[%s] 100%%", "=".repeat(BAR_WIDTH));
        logger.rlog(Level.NONE, Origin.EXHAUST, bar + "\n");

    }

    @SneakyThrows
    private static String runConfig(File config) {

        ObfuscatorContext.INSTANCE = null;
        Hierarchy.INSTANCE = null;

        System.gc();

        Entrypoint.main(new String[] {
                "--config", config.getAbsolutePath(),
                "--metaphor", "true",
                "--logger", "false"
        });

        AtomicReference<String> decompiledCode = new AtomicReference<>("");

        OutputSinkFactory sink = new OutputSinkFactory() {
            @Override
            public List<SinkClass> getSupportedSinks(SinkType sinkType, Collection<SinkClass> collection) {
                if (sinkType == SinkType.JAVA && collection.contains(SinkClass.DECOMPILED)) {
                    return Arrays.asList(SinkClass.DECOMPILED, SinkClass.STRING);
                } else {
                    return Collections.singletonList(SinkClass.STRING);
                }
            }

            final Consumer<SinkReturns.Decompiled> dumpDecompiled = d -> decompiledCode.set(d.getJava());

            @Override
            public <T> Sink<T> getSink(SinkType sinkType, SinkClass sinkClass) {
                if (sinkType == SinkType.JAVA && sinkClass == SinkClass.DECOMPILED) {
                    return x -> dumpDecompiled.accept((SinkReturns.Decompiled) x);
                }
                return ignore -> {};
            }
        };

        CfrDriver driver = new CfrDriver.Builder()
                .withOutputSink(sink)
                .build();

        driver.analyse(Collections.singletonList(YamlConfiguration.loadConfiguration(config).getString("output")));

        return decompiledCode.get();
    }

}
