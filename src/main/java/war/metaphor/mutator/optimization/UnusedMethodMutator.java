package war.metaphor.mutator.optimization;

import org.objectweb.asm.tree.MethodNode;
import war.configuration.ConfigurationSection;
import war.jnt.annotate.Level;
import war.jnt.annotate.Stability;
import war.metaphor.analysis.callgraph.CallGraph;
import war.metaphor.base.ObfuscatorContext;
import war.metaphor.mutator.Mutator;
import war.metaphor.tree.ClassMethod;
import war.metaphor.tree.Hierarchy;
import war.metaphor.tree.JClassNode;

import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.Set;

@Stability(Level.UNKNOWN)
public class UnusedMethodMutator extends Mutator {
    public UnusedMethodMutator(ObfuscatorContext base, ConfigurationSection config) {
        super(base, config);
    }

    @Override
    public void run(ObfuscatorContext base) {
        CallGraph callGraph = new CallGraph();
        callGraph.buildGraph();

        for (JClassNode classNode : base.getClasses()) {
            if (classNode.isExempt()) continue;
            if (classNode.isInterface()) continue;

            boolean modified;

            do {
                modified = false;

                ArrayList<MethodNode> toRemove = new ArrayList<>();

                for (MethodNode method : classNode.methods) {
                    if (Modifier.isAbstract(method.access)) continue;
                    if (classNode.isExempt(method)) continue;
                    if (method.name.contains("<")) continue;
                    if (isMain(method)) continue;
                    if ((classNode.access & ACC_ENUM) == ACC_ENUM && (method.name.equals("values") || method.name.equals("valueOf"))) continue;

                    ClassMethod classMethod = ClassMethod.of(classNode, method);

                    Set<CallGraph.CallGraphNode> nodes = callGraph.getNodes(classMethod);

                    if (!nodes.isEmpty()) continue;

                    if (Hierarchy.INSTANCE.getMethodHierarchy(classMethod).size() > 1) continue;

                    toRemove.add(method);
                    modified = true;
                    break;
                }

                classNode.methods.removeAll(toRemove);
                Hierarchy.INSTANCE.ensureGraphBuilt();
            } while(modified);
        }
    }

    private boolean isMain(MethodNode method) {
        return method.name.equals("main") && method.desc.equals("([Ljava/lang/String;)V") && Modifier.isStatic(method.access);
    }
}
