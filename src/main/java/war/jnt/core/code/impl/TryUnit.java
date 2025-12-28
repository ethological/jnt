package war.jnt.core.code.impl;

import org.objectweb.asm.Opcodes;
import org.objectweb.asm.Type;
import org.objectweb.asm.tree.AbstractInsnNode;
import org.objectweb.asm.tree.MethodInsnNode;
import org.objectweb.asm.tree.MethodNode;
import org.objectweb.asm.tree.analysis.BasicValue;
import org.objectweb.asm.tree.analysis.Frame;
import war.metaphor.base.ObfuscatorContext;
import war.metaphor.tree.ClassMethod;
import war.metaphor.tree.JClassNode;

import java.util.HashSet;
import java.util.Map;
import java.util.Set;

public class TryUnit implements Opcodes {

    public static Set<String> canThrow(AbstractInsnNode insn, Map<AbstractInsnNode, Frame<BasicValue>> frames) {
        return canThrow(insn, frames, new HashSet<>());
    }

    private static Set<String> canThrow(AbstractInsnNode insn, Map<AbstractInsnNode, Frame<BasicValue>> frames, Set<ClassMethod> visited) {
        Set<String> throwableTypes = new HashSet<>();
        Frame<BasicValue> frame = frames.get(insn);
        if (frame == null) return throwableTypes;

        switch (insn.getOpcode()) {
            case ATHROW -> {
                BasicValue value = frame.getStack(frame.getStackSize() - 1);
                Type type = value.getType();
                if (type == null || "null".equals(type.getInternalName())) {
                    throwableTypes.add("java/lang/NullPointerException");
                } else {
                    throwableTypes.add(type.getInternalName());
                }
            }

            case NEW, MULTIANEWARRAY, CHECKCAST, INSTANCEOF, INVOKEDYNAMIC -> throwableTypes.add("*");

            case INVOKEINTERFACE, INVOKESPECIAL, INVOKESTATIC, INVOKEVIRTUAL -> {

                throwableTypes.add("java/lang/NullPointerException");

                MethodInsnNode methodInsn = (MethodInsnNode) insn;
                JClassNode node = ObfuscatorContext.INSTANCE.loadClass(methodInsn.owner);
                if (node == null) {
                    throwableTypes.add("*");
                    break;
                }

                MethodNode method = node.getMethod(methodInsn.name, methodInsn.desc);
                if (method == null) {
                    throwableTypes.add("*");
                    break;
                }

                ClassMethod member = new ClassMethod(node, method);
                if (!visited.add(member)) break;

                throwableTypes.addAll(method.exceptions);
                for (AbstractInsnNode instruction : method.instructions) {
                    throwableTypes.addAll(canThrow(instruction, frames, visited));
                }
            }

            case IREM, DREM, LREM, FREM, IDIV, DDIV, LDIV, FDIV ->
                    throwableTypes.add("java/lang/ArithmeticException");

            case IALOAD, LALOAD, FALOAD, DALOAD, AALOAD, BALOAD, CALOAD, SALOAD,
                 IASTORE, LASTORE, FASTORE, DASTORE, AASTORE, BASTORE, CASTORE, SASTORE -> {
                throwableTypes.add("java/lang/NullPointerException");
                throwableTypes.add("java/lang/ArrayIndexOutOfBoundsException");
                throwableTypes.add("java/lang/ArrayStoreException");
            }

            case GETFIELD, PUTFIELD -> {
                throwableTypes.add("java/lang/NullPointerException");
                throwableTypes.add("java/lang/IncompatibleClassChangeError");
            }

            case MONITORENTER, MONITOREXIT, ARRAYLENGTH -> {
                    throwableTypes.add("java/lang/NullPointerException");
                    throwableTypes.add("java/lang/IllegalMonitorStateException");
            }

            case NEWARRAY, ANEWARRAY ->
                    throwableTypes.add("java/lang/NegativeArraySizeException");

        }

        return throwableTypes;
    }


}

