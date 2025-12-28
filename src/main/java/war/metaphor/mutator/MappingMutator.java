package war.metaphor.mutator;

import org.objectweb.asm.Handle;
import org.objectweb.asm.commons.ClassRemapper;
import org.objectweb.asm.tree.AnnotationNode;
import org.objectweb.asm.tree.FieldNode;
import org.objectweb.asm.tree.InvokeDynamicInsnNode;
import org.objectweb.asm.tree.MethodNode;
import war.configuration.ConfigurationSection;
import war.metaphor.asm.JRemapper;
import war.metaphor.base.ObfuscatorContext;
import war.metaphor.tree.ClassField;
import war.metaphor.tree.ClassMethod;
import war.metaphor.tree.Hierarchy;
import war.metaphor.tree.JClassNode;

import java.lang.reflect.Modifier;
import java.util.*;

public abstract class MappingMutator extends Mutator {

    public MappingMutator(ObfuscatorContext base, ConfigurationSection config) {
        super(base, config);
    }

    protected final boolean canRenameField(Set<ClassField> tree) {
        for (ClassField member : tree) {
            JClassNode node = member.getClassNode();
            FieldNode field = member.getMember();
            if (!canRenameField(node, field)) return false;
        }
        return true;
    }

    protected final boolean canRenameMethod(Set<ClassMethod> tree) {
        for (ClassMethod member : tree) {
            JClassNode node = member.getClassNode();
            MethodNode method = member.getMember();
            if (!canRenameMethod(node, method)) return false;
        }
        return true;
    }

    protected final boolean canRenameField(JClassNode node, FieldNode field) {
        return !node.isLibrary();
    }

    protected final boolean canRenameMethod(JClassNode node, MethodNode method) {
        if (node.hasAnnotation("Ljava/lang/FunctionalInterface;")) return false;
        if (method.name.startsWith("<")) return false;
        if (node.isEnum() && method.name.equals("values") && method.desc.equals("()[L" + node.name + ";"))
            return false;
        if (node.isEnum() && method.name.equals("valueOf") && method.desc.equals("(Ljava/lang/String;)L" + node.name + ";"))
            return false;
        if (Modifier.isNative(method.access)) return false;
        return !node.isLibrary();
    }

    public final void map(ObfuscatorContext base, Map<String, String> mappings) {
        Map<JClassNode, JClassNode> old2new = new HashMap<>();

        JRemapper remapper = new JRemapper(mappings);

        for (JClassNode classNode : base.getClasses()) {

            JClassNode remap = new JClassNode();

            ClassRemapper classRemapper = new ClassRemapper(remap, remapper);
            classNode.accept(classRemapper);

            remap.methods.forEach(method -> {
                method.instructions.forEach(instruction -> {
                    if (instruction instanceof InvokeDynamicInsnNode insn) {
                        Handle handle = insn.bsm;

                        String owner = handle.getOwner();
                        String name = handle.getName();
                        String desc = handle.getDesc();
                        insn.bsm = new Handle(handle.getTag(), remapper.mapType(owner), remapper.mapMethodName(owner, name, desc),
                                remapper.mapMethodDesc(desc), handle.isInterface());

                        for (int i = 0; i < insn.bsmArgs.length; i++) {
                            Object bsmArg = insn.bsmArgs[i];
                            if (bsmArg instanceof Handle) {
                                handle = (Handle) bsmArg;
                                owner = handle.getOwner();
                                name = handle.getName();
                                desc = handle.getDesc();
                                insn.bsmArgs[i] = new Handle(handle.getTag(), remapper.mapType(owner), remapper.mapMethodName(owner, name, desc),
                                        remapper.mapMethodDesc(desc), handle.isInterface());
                            }
                        }

                    }
                });
            });

            if (remap.visibleAnnotations != null) {
                for (AnnotationNode visibleAnnotation : remap.visibleAnnotations) {
                    if (visibleAnnotation.values == null) continue;
                    for (Object value : visibleAnnotation.values) {
                        if (value instanceof String[] arr) {
                            if (Arrays.stream(arr).allMatch(Objects::nonNull)) {
                                String className = arr[0];
                                String fieldName = arr[1];
                                arr[1] = remapper.mapAnnotationAttributeName(className, fieldName);
                            }
                        }
                    }
                }
            }

            remap.setRealName(classNode.getRealName());
            old2new.put(classNode, remap);
        }

        old2new.forEach(JClassNode::update);

        Hierarchy.INSTANCE.reset();
        Hierarchy.INSTANCE.ensureGraphBuilt();
    }
}
