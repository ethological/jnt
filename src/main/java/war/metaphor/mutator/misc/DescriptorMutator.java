package war.metaphor.mutator.misc;

import org.objectweb.asm.Type;
import org.objectweb.asm.tree.*;
import war.configuration.ConfigurationSection;
import war.jnt.annotate.Level;
import war.jnt.annotate.Stability;
import war.metaphor.base.ObfuscatorContext;
import war.metaphor.mutator.MappingMutator;
import war.metaphor.tree.ClassField;
import war.metaphor.tree.Hierarchy;
import war.metaphor.tree.JClassNode;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;

/*
TODO:   Need proper impl of this shit, it was rushed af
 */
@Stability(Level.UNKNOWN)
public class DescriptorMutator extends MappingMutator {

    public DescriptorMutator(ObfuscatorContext base, ConfigurationSection config) {
        super(base, config);
    }

    @Override
    public void run(ObfuscatorContext base) {
        // TODO, proper renaming and stuff, also impl paramter obf
        Map<String, Type> mapping = new HashMap<>();

        for (JClassNode classNode : base.getClasses()) {
            if (classNode.isExempt()) continue;

            Set<JClassNode> classTree = Hierarchy.INSTANCE.getClassHierarchy(classNode);
            classTree.add(classNode);

            for (FieldNode field : classNode.fields) {
                if (field.value != null) continue;
                if (field.desc.length() == 1) continue;
//                String newName = Dictionary.gen(3, Purpose.FIELD);
                for (JClassNode ct : classTree) {
                    ClassField cf = new ClassField(ct, field);
                    mapping.put(cf.toString(), Type.getType(field.desc));
//                    nameMapping.put(cf.toString(), newName);
                }
            }
        }

        for (JClassNode classNode : base.getClasses()) {
            for (MethodNode method : classNode.methods) {

                for (AbstractInsnNode instruction : method.instructions) {

                    if (instruction instanceof FieldInsnNode fn) {

                        String id = fn.owner + "." + fn.name + fn.desc;
                        if (!mapping.containsKey(id)) continue;

                        Type type = Type.getType(fn.desc);
                        InsnList list = new InsnList();

                        if (instruction.getOpcode() == GETFIELD || instruction.getOpcode() == GETSTATIC) {
                            switch (type.getSort()) {
                                case Type.INT -> {
                                    list.add(new TypeInsnNode(CHECKCAST, "java/lang/Integer"));
                                    list.add(new MethodInsnNode(INVOKEVIRTUAL, "java/lang/Integer", "intValue", "()I", false));
                                }
                                case Type.LONG -> {
                                    list.add(new TypeInsnNode(CHECKCAST, "java/lang/Long"));
                                    list.add(new MethodInsnNode(INVOKEVIRTUAL, "java/lang/Long", "longValue", "()J", false));
                                }
                                case Type.FLOAT -> {
                                    list.add(new TypeInsnNode(CHECKCAST, "java/lang/Float"));
                                    list.add(new MethodInsnNode(INVOKEVIRTUAL, "java/lang/Float", "floatValue", "()F", false));
                                }
                                case Type.DOUBLE -> {
                                    list.add(new TypeInsnNode(CHECKCAST, "java/lang/Double"));
                                    list.add(new MethodInsnNode(INVOKEVIRTUAL, "java/lang/Double", "doubleValue", "()D", false));
                                }
                                case Type.BOOLEAN -> {
                                    list.add(new TypeInsnNode(CHECKCAST, "java/lang/Boolean"));
                                    list.add(new MethodInsnNode(INVOKEVIRTUAL, "java/lang/Boolean", "booleanValue", "()Z", false));
                                }
                                case Type.SHORT, Type.BYTE -> {
                                    list.add(new TypeInsnNode(CHECKCAST, "java/lang/Number"));
                                    list.add(new MethodInsnNode(INVOKEVIRTUAL, "java/lang/Number", "intValue", "()I", false));
                                }
                                case Type.CHAR -> {
                                    list.add(new TypeInsnNode(CHECKCAST, "java/lang/Character"));
                                    list.add(new MethodInsnNode(INVOKEVIRTUAL, "java/lang/Character", "charValue", "()C", false));
                                }
                                case Type.OBJECT, Type.ARRAY -> list.add(new TypeInsnNode(CHECKCAST, type.getInternalName()));
                                default -> throw new IllegalStateException("Unexpected value: " + type.getSort());
                            }
                            method.instructions.insert(instruction, list);
                        } else {
                            switch (type.getSort()) {
                                case Type.INT -> list.add(new MethodInsnNode(INVOKESTATIC, "java/lang/Integer", "valueOf", "(I)Ljava/lang/Integer;", false));
                                case Type.LONG -> list.add(new MethodInsnNode(INVOKESTATIC, "java/lang/Long", "valueOf", "(J)Ljava/lang/Long;", false));
                                case Type.FLOAT -> list.add(new MethodInsnNode(INVOKESTATIC, "java/lang/Float", "valueOf", "(F)Ljava/lang/Float;", false));
                                case Type.DOUBLE -> list.add(new MethodInsnNode(INVOKESTATIC, "java/lang/Double", "valueOf", "(D)Ljava/lang/Double;", false));
                                case Type.BOOLEAN -> list.add(new MethodInsnNode(INVOKESTATIC, "java/lang/Boolean", "valueOf", "(Z)Ljava/lang/Boolean;", false));
                                case Type.SHORT -> list.add(new MethodInsnNode(INVOKESTATIC, "java/lang/Short", "valueOf", "(S)Ljava/lang/Short;", false));
                                case Type.BYTE -> list.add(new MethodInsnNode(INVOKESTATIC, "java/lang/Byte", "valueOf", "(B)Ljava/lang/Byte;", false));
                                case Type.CHAR -> list.add(new MethodInsnNode(INVOKESTATIC, "java/lang/Character", "valueOf", "(C)Ljava/lang/Character;", false));
                            }
                            method.instructions.insertBefore(instruction, list);
                        }
                    }
                }
            }
        }

        for (JClassNode classNode : base.getClasses()) {
            for (MethodNode method : classNode.methods) {

                for (AbstractInsnNode instruction : method.instructions) {

                    if (instruction instanceof FieldInsnNode fn) {

                        String id = fn.owner + "." + fn.name + fn.desc;
                        if (!mapping.containsKey(id)) continue;

//                        fn.name = nameMapping.get(id);
                        fn.desc = "Ljava/lang/Object;"; // Default to Object type
                    }
                }
            }
        }

        for (JClassNode classNode : base.getClasses()) {
            if (classNode.isExempt()) continue;

            for (FieldNode field : classNode.fields) {
                if (field.value != null) continue;
                ClassField cf = new ClassField(classNode, field);
                if (mapping.containsKey(cf.toString())) {
//                    field.name = nameMapping.get(cf.toString());
                    field.desc = "Ljava/lang/Object;"; // Default to Object type
                }
            }
        }

        Hierarchy.INSTANCE.reset();
        Hierarchy.INSTANCE.ensureGraphBuilt();
    }
}
