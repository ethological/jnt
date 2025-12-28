package war.metaphor.mutator.loader;

import org.objectweb.asm.Type;
import org.objectweb.asm.tree.*;
import war.configuration.ConfigurationSection;
import war.jnt.annotate.Level;
import war.jnt.annotate.Stability;
import war.jnt.fusebox.impl.Internal;
import war.metaphor.base.ObfuscatorContext;
import war.metaphor.mutator.Mutator;
import war.metaphor.tree.JClassNode;

import java.lang.reflect.Modifier;
import java.util.concurrent.ThreadPoolExecutor;

@Stability(Level.UNKNOWN)
public class MultiNewArrayMutator extends Mutator {

    public MultiNewArrayMutator(ObfuscatorContext base, ConfigurationSection config) {
        super(base, config);
    }

    @Override
    public void run(ObfuscatorContext base) {
        for (JClassNode classNode : base.getClasses()) {
            if (classNode.isExempt()) continue;
            for (MethodNode method : classNode.methods) {
                if (Modifier.isAbstract(method.access)) continue;
                if (classNode.isExempt(method)) continue;
                if (Internal.disallowedTranspile(classNode, method)) continue;
                for (AbstractInsnNode instruction : method.instructions) {
                    if (instruction instanceof MultiANewArrayInsnNode node) {
                        InsnList list = new InsnList();
                        list.add(new LdcInsnNode(node.dims));
                        list.add(new IntInsnNode(NEWARRAY, T_INT));
                        for (int dims = node.dims - 1; dims >= 0; dims--) {
                            list.add(new InsnNode(DUP));
                            list.add(new LdcInsnNode(dims));
                            list.add(new InsnNode(DUP2_X2));
                            list.add(new InsnNode(POP2));
                            list.add(new InsnNode(DUP_X2));
                            list.add(new InsnNode(POP));
                            list.add(new InsnNode(IASTORE));
                        }

                        Type type = Type.getType(node.desc).getElementType();

                        switch (type.getSort()) {
                            case Type.INT -> list.add(new FieldInsnNode(GETSTATIC, "java/lang/Integer", "TYPE", "Ljava/lang/Class;"));
                            case Type.BOOLEAN -> list.add(new FieldInsnNode(GETSTATIC, "java/lang/Boolean", "TYPE", "Ljava/lang/Class;"));
                            case Type.BYTE -> list.add(new FieldInsnNode(GETSTATIC, "java/lang/Byte", "TYPE", "Ljava/lang/Class;"));
                            case Type.SHORT -> list.add(new FieldInsnNode(GETSTATIC, "java/lang/Short", "TYPE", "Ljava/lang/Class;"));
                            case Type.CHAR -> list.add(new FieldInsnNode(GETSTATIC, "java/lang/Character", "TYPE", "Ljava/lang/Class;"));
                            case Type.FLOAT -> list.add(new FieldInsnNode(GETSTATIC, "java/lang/Float", "TYPE", "Ljava/lang/Class;"));
                            case Type.LONG -> list.add(new FieldInsnNode(GETSTATIC, "java/lang/Long", "TYPE", "Ljava/lang/Class;"));
                            case Type.DOUBLE -> list.add(new FieldInsnNode(GETSTATIC, "java/lang/Double", "TYPE", "Ljava/lang/Class;"));
                            case Type.OBJECT, Type.ARRAY -> list.add(new LdcInsnNode(type));
                        }

                        list.add(new InsnNode(SWAP));
                        list.add(new MethodInsnNode(INVOKESTATIC, "java/lang/reflect/Array", "newInstance", "(Ljava/lang/Class;[I)Ljava/lang/Object;", false));
                        list.add(new TypeInsnNode(CHECKCAST, node.desc));
                        method.instructions.insertBefore(instruction, list);
                        method.instructions.remove(instruction);
                    }
                }
            }
        }
    }

}