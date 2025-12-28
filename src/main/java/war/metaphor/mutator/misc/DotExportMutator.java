package war.metaphor.mutator.misc;

import org.objectweb.asm.tree.MethodNode;
import war.configuration.ConfigurationSection;
import war.jnt.annotate.Level;
import war.jnt.annotate.Stability;
import war.metaphor.analysis.graph.CFGExporter;
import war.metaphor.analysis.graph.ControlFlowGraph;
import war.metaphor.base.ObfuscatorContext;
import war.metaphor.mutator.Mutator;
import war.metaphor.tree.JClassNode;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Comparator;
import java.util.stream.Stream;

@Stability(Level.HIGH)
public class DotExportMutator extends Mutator {

    public DotExportMutator(ObfuscatorContext base, ConfigurationSection config) {
        super(base, config);
    }

    @Override
    public void run(ObfuscatorContext base) {
        Path outputDir = Paths.get("dot-cfg/");

//        try {
//            if (Files.exists(outputDir)) {
//                try (Stream<Path> walk = Files.walk(outputDir)) {
//                    walk.sorted(Comparator.reverseOrder())
//                            .map(Path::toFile)
//                            .forEach(File::delete);
//                }
//            }
//            Files.createDirectory(outputDir);
//        } catch (IOException e) {
//            throw new RuntimeException("Failed to reset output directory", e);
//        }

        for (JClassNode classNode : base.getClasses()) {
            if (classNode.isExempt()) continue;
            for (MethodNode method : classNode.methods) {
                if (classNode.isExempt(method)) continue;
                ControlFlowGraph cfg = new ControlFlowGraph(classNode, method);
                cfg.compute();

                Path dotFile = outputDir.resolve(classNode.name.replace('/', '_') + "_" + method.name.replace(">", "").replace("<", "") + ".dot");
                try (FileWriter writer = new FileWriter(dotFile.toFile())) {
                    CFGExporter.exportToDot(cfg, writer, true);
                } catch (IOException e) {
                    throw new RuntimeException("Failed to write DOT file", e);
                }
            }
        }
    }

}
