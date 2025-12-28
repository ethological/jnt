package war.metaphor.mutator.misc;

import org.objectweb.asm.tree.MethodNode;
import war.configuration.ConfigurationSection;
import war.jnt.annotate.Level;
import war.jnt.annotate.Stability;
import war.jnt.utility.mapping.Mapping;
import war.jnt.utility.mapping.impl.MemberIdentity;
import war.metaphor.base.ObfuscatorContext;
import war.metaphor.mutator.MappingMutator;
import war.metaphor.tree.ClassMethod;
import war.metaphor.tree.Hierarchy;
import war.metaphor.tree.JClassNode;
import war.metaphor.util.Dictionary;
import war.metaphor.util.Purpose;

import java.util.Base64;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

@Stability(Level.HIGH)
public class MethodRenameMutator extends MappingMutator {

    public MethodRenameMutator(ObfuscatorContext base, ConfigurationSection config) {
        super(base, config);
    }

    @Override
    public void run(ObfuscatorContext base) {
        Map<String, String> mapping = new HashMap<>();

        for (JClassNode classNode : base.getClasses()) {
            if (classNode.isExempt()) continue;

            Set<JClassNode> classTree = Hierarchy.INSTANCE.getClassHierarchy(classNode);

            classTree.add(classNode);

            for (MethodNode method : classNode.methods) {
                if (classNode.isExempt(method)
                        || (method.name.equals("main") && method.desc.equals("([Ljava/lang/String;)V"))
                        || (classNode.isEnum() && method.name.equals("values"))
                        || (classNode.isEnum() && method.name.equals("valueOf"))
                        || method.name.equals("<init>")
                        || method.name.equals("<clinit>")
                        || (method.signature != null && method.signature.equals("bsm::jnt:excluded"))
                ) {
                    continue;
                }

                ClassMethod self = ClassMethod.of(classNode, method);

                Set<ClassMethod> methodTree = Hierarchy.INSTANCE.getMethodHierarchy(self);

                if (!canRenameMethod(methodTree))
                    continue;

                String newName = null;

                for (JClassNode node : classTree) {
                    String id = node.name + "." + method.name + method.desc;
                    if (mapping.containsKey(id)) {
                        newName = mapping.get(id);
                    }
                }

                if (newName == null) {
                    newName = Dictionary.gen(1, Purpose.METHOD);
                }

                for (JClassNode node : classTree) {
                    if (node.isLibrary()) continue;
                    String id = node.name + "." + method.name + method.desc;
                    mapping.put(id, newName);
                }

                if (method.signature == null || !method.signature.startsWith("pass::jnt")) {
                    method.signature = "pass::jnt:" + Base64.getEncoder().encodeToString(method.name.getBytes());
                }

                base.getRepository().add(new Mapping(
                        new MemberIdentity(".m_same " + classNode.name, method.name, ""),
                        new MemberIdentity(classNode.name, newName, "")
                ));
            }
        }

        map(base, mapping);
    }
}
