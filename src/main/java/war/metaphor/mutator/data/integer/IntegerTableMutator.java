package war.metaphor.mutator.data.integer;

import org.objectweb.asm.Label;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.tree.*;
import war.configuration.ConfigurationSection;
import war.jnt.annotate.Level;
import war.jnt.annotate.Stability;
import war.metaphor.base.ObfuscatorContext;
import war.metaphor.mutator.Mutator;
import war.metaphor.tree.JClassNode;
import war.metaphor.util.Dictionary;
import war.metaphor.util.Purpose;
import war.metaphor.util.asm.BytecodeUtil;

import java.io.ByteArrayOutputStream;
import java.lang.reflect.Modifier;
import java.util.*;

/**
 * @author etho
 * An integer mutator based on the one used by Zelix KlassMaster.
 */
@Stability(Level.LOW)
public class IntegerTableMutator extends Mutator {

    public IntegerTableMutator(ObfuscatorContext base, ConfigurationSection config) {
        super(base, config);
    }

    @Override
    public void run(ObfuscatorContext base) {
        for (JClassNode node : base.getClasses()) {
            if (node.isExempt()) continue;
            if (node.isInterface()) continue;

            String fieldName = Dictionary.gen(1, Purpose.FIELD);

            Map<Integer, Integer> valueToIndex = new HashMap<>();
            List<Integer> values = new ArrayList<>();

            for (MethodNode method : node.methods) {
                if (Modifier.isAbstract(method.access)) continue;
                if (node.isExempt(method)) continue;
                for (AbstractInsnNode instruction : method.instructions) {
                    if (!BytecodeUtil.isInteger(instruction)) continue;
                    int value = BytecodeUtil.getInteger(instruction);
                    valueToIndex.put(value, values.size());
                    values.add(value);
                }
            }

            if (values.isEmpty()) continue;

            for (MethodNode method : node.methods) {
                if (Modifier.isAbstract(method.access))
                    continue;
                if (node.isExempt(method))
                    continue;

                int size = BytecodeUtil.leeway(method);

                InsnList list = new InsnList();
                for (AbstractInsnNode instruction : method.instructions) {
                    if (size < 30000)
                        break;

                    if (BytecodeUtil.isInteger(instruction)) {
                        int value = BytecodeUtil.getInteger(instruction);
                        int index = valueToIndex.get(value);

                        list.add(new FieldInsnNode(GETSTATIC, node.name, fieldName, "[I"));
                        list.add(BytecodeUtil.makeInteger(index));
                        list.add(new InsnNode(IALOAD));

                        method.instructions.insertBefore(instruction, list);
                        method.instructions.remove(instruction);
                        list.clear();
                    }

                    size = BytecodeUtil.leeway(method);
                }
            }

            node.fields.add(new FieldNode(ACC_PRIVATE | ACC_STATIC, fieldName, "[I", null, null));

            MethodNode init = node.getStaticInit();

            AbstractInsnNode first = init.instructions.firstInsn;
            init.instructions.insertBefore(first, makeDec(node, fieldName, makeConstant(values)));
        }
    }

    private String makeConstant(List<Integer> values) {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();

        for (int v : values) {
            byte[] bytes = spreadInteger(v);
            try {
                baos.write(bytes);
            } catch (Exception _) {
                /* */
            }
        }

        byte[] bytes = baos.toByteArray();
        return Base64.getEncoder().encodeToString(bytes);
    }

    private InsnList makeDec(JClassNode jClassNode, String fieldName, String constant) {
        MethodNode mn = new MethodNode();
        mn.visitCode();

        Label label0 = new Label();
        mn.visitLabel(label0);
        mn.visitLdcInsn(constant);
        mn.visitVarInsn(Opcodes.ASTORE, 0);
        Label label1 = new Label();
        mn.visitLabel(label1);
        mn.visitLdcInsn("ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789+/");
        mn.visitVarInsn(Opcodes.ASTORE, 1);
        Label label2 = new Label();
        mn.visitLabel(label2);
        mn.visitInsn(Opcodes.ICONST_0);
        mn.visitVarInsn(Opcodes.ISTORE, 2);
        Label label3 = new Label();
        mn.visitLabel(label3);
        mn.visitVarInsn(Opcodes.ALOAD, 0);
        mn.visitLdcInsn("==");
        mn.visitMethodInsn(Opcodes.INVOKEVIRTUAL, "java/lang/String", "endsWith", "(Ljava/lang/String;)Z", false);
        Label label4 = new Label();
        mn.visitJumpInsn(Opcodes.IFEQ, label4);
        Label label5 = new Label();
        mn.visitLabel(label5);
        mn.visitInsn(Opcodes.ICONST_2);
        mn.visitVarInsn(Opcodes.ISTORE, 2);
        Label label6 = new Label();
        mn.visitLabel(label6);
        mn.visitVarInsn(Opcodes.ALOAD, 0);
        mn.visitInsn(Opcodes.ICONST_0);
        mn.visitVarInsn(Opcodes.ALOAD, 0);
        mn.visitMethodInsn(Opcodes.INVOKEVIRTUAL, "java/lang/String", "length", "()I", false);
        mn.visitInsn(Opcodes.ICONST_2);
        mn.visitInsn(Opcodes.ISUB);
        mn.visitMethodInsn(Opcodes.INVOKEVIRTUAL, "java/lang/String", "substring", "(II)Ljava/lang/String;", false);
        mn.visitVarInsn(Opcodes.ASTORE, 0);
        Label label7 = new Label();
        mn.visitJumpInsn(Opcodes.GOTO, label7);
        mn.visitLabel(label4);
        mn.visitFrame(Opcodes.F_APPEND, 3, new Object[]{"java/lang/String", "java/lang/String", Opcodes.INTEGER}, 0, null);
        mn.visitVarInsn(Opcodes.ALOAD, 0);
        mn.visitLdcInsn("=");
        mn.visitMethodInsn(Opcodes.INVOKEVIRTUAL, "java/lang/String", "endsWith", "(Ljava/lang/String;)Z", false);
        mn.visitJumpInsn(Opcodes.IFEQ, label7);
        Label label8 = new Label();
        mn.visitLabel(label8);
        mn.visitInsn(Opcodes.ICONST_1);
        mn.visitVarInsn(Opcodes.ISTORE, 2);
        Label label9 = new Label();
        mn.visitLabel(label9);
        mn.visitVarInsn(Opcodes.ALOAD, 0);
        mn.visitInsn(Opcodes.ICONST_0);
        mn.visitVarInsn(Opcodes.ALOAD, 0);
        mn.visitMethodInsn(Opcodes.INVOKEVIRTUAL, "java/lang/String", "length", "()I", false);
        mn.visitInsn(Opcodes.ICONST_1);
        mn.visitInsn(Opcodes.ISUB);
        mn.visitMethodInsn(Opcodes.INVOKEVIRTUAL, "java/lang/String", "substring", "(II)Ljava/lang/String;", false);
        mn.visitVarInsn(Opcodes.ASTORE, 0);
        mn.visitLabel(label7);
        mn.visitFrame(Opcodes.F_SAME, 0, null, 0, null);
        mn.visitTypeInsn(Opcodes.NEW, "java/lang/StringBuilder");
        mn.visitInsn(Opcodes.DUP);
        mn.visitMethodInsn(Opcodes.INVOKESPECIAL, "java/lang/StringBuilder", "<init>", "()V", false);
        mn.visitVarInsn(Opcodes.ASTORE, 3);
        Label label10 = new Label();
        mn.visitLabel(label10);
        mn.visitInsn(Opcodes.ICONST_0);
        mn.visitVarInsn(Opcodes.ISTORE, 4);
        Label label11 = new Label();
        mn.visitLabel(label11);
        mn.visitFrame(Opcodes.F_APPEND, 2, new Object[]{"java/lang/StringBuilder", Opcodes.INTEGER}, 0, null);
        mn.visitVarInsn(Opcodes.ILOAD, 4);
        mn.visitVarInsn(Opcodes.ALOAD, 0);
        mn.visitMethodInsn(Opcodes.INVOKEVIRTUAL, "java/lang/String", "length", "()I", false);
        Label label12 = new Label();
        mn.visitJumpInsn(Opcodes.IF_ICMPGE, label12);
        Label label13 = new Label();
        mn.visitLabel(label13);
        mn.visitVarInsn(Opcodes.ALOAD, 0);
        mn.visitMethodInsn(Opcodes.INVOKEVIRTUAL, "java/lang/String", "toCharArray", "()[C", false);
        mn.visitVarInsn(Opcodes.ILOAD, 4);
        mn.visitInsn(Opcodes.CALOAD);
        mn.visitVarInsn(Opcodes.ISTORE, 5);
        Label label14 = new Label();
        mn.visitLabel(label14);
        mn.visitVarInsn(Opcodes.ALOAD, 1);
        mn.visitVarInsn(Opcodes.ILOAD, 5);
        mn.visitMethodInsn(Opcodes.INVOKEVIRTUAL, "java/lang/String", "indexOf", "(I)I", false);
        mn.visitVarInsn(Opcodes.ISTORE, 6);
        Label label15 = new Label();
        mn.visitLabel(label15);
        mn.visitVarInsn(Opcodes.ALOAD, 3);
        mn.visitLdcInsn("%6s");
        mn.visitInsn(Opcodes.ICONST_1);
        mn.visitTypeInsn(Opcodes.ANEWARRAY, "java/lang/Object");
        mn.visitInsn(Opcodes.DUP);
        mn.visitInsn(Opcodes.ICONST_0);
        mn.visitVarInsn(Opcodes.ILOAD, 6);
        mn.visitMethodInsn(Opcodes.INVOKESTATIC, "java/lang/Integer", "toBinaryString", "(I)Ljava/lang/String;", false);
        mn.visitInsn(Opcodes.AASTORE);
        mn.visitMethodInsn(Opcodes.INVOKESTATIC, "java/lang/String", "format", "(Ljava/lang/String;[Ljava/lang/Object;)Ljava/lang/String;", false);
        mn.visitIntInsn(Opcodes.BIPUSH, 32);
        mn.visitIntInsn(Opcodes.BIPUSH, 48);
        mn.visitMethodInsn(Opcodes.INVOKEVIRTUAL, "java/lang/String", "replace", "(CC)Ljava/lang/String;", false);
        mn.visitMethodInsn(Opcodes.INVOKEVIRTUAL, "java/lang/StringBuilder", "append", "(Ljava/lang/String;)Ljava/lang/StringBuilder;", false);
        mn.visitInsn(Opcodes.POP);
        Label label16 = new Label();
        mn.visitLabel(label16);
        mn.visitIincInsn(4, 1);
        Label label17 = new Label();
        mn.visitLabel(label17);
        mn.visitJumpInsn(Opcodes.GOTO, label11);
        mn.visitLabel(label12);
        mn.visitFrame(Opcodes.F_SAME, 0, null, 0, null);
        mn.visitVarInsn(Opcodes.ILOAD, 2);
        Label label18 = new Label();
        mn.visitJumpInsn(Opcodes.IFLE, label18);
        Label label19 = new Label();
        mn.visitLabel(label19);
        mn.visitVarInsn(Opcodes.ALOAD, 3);
        mn.visitVarInsn(Opcodes.ALOAD, 3);
        mn.visitMethodInsn(Opcodes.INVOKEVIRTUAL, "java/lang/StringBuilder", "length", "()I", false);
        mn.visitVarInsn(Opcodes.ILOAD, 2);
        mn.visitInsn(Opcodes.ICONST_2);
        mn.visitInsn(Opcodes.IMUL);
        mn.visitInsn(Opcodes.ISUB);
        mn.visitMethodInsn(Opcodes.INVOKEVIRTUAL, "java/lang/StringBuilder", "setLength", "(I)V", false);
        mn.visitLabel(label18);
        mn.visitFrame(Opcodes.F_SAME, 0, null, 0, null);
        mn.visitVarInsn(Opcodes.ALOAD, 3);
        mn.visitMethodInsn(Opcodes.INVOKEVIRTUAL, "java/lang/StringBuilder", "length", "()I", false);
        mn.visitIntInsn(Opcodes.BIPUSH, 8);
        mn.visitInsn(Opcodes.IDIV);
        mn.visitVarInsn(Opcodes.ISTORE, 5);
        Label label20 = new Label();
        mn.visitLabel(label20);
        mn.visitVarInsn(Opcodes.ILOAD, 5);
        mn.visitIntInsn(Opcodes.NEWARRAY, T_BYTE);
        mn.visitVarInsn(Opcodes.ASTORE, 6);
        Label label21 = new Label();
        mn.visitLabel(label21);
        mn.visitInsn(Opcodes.ICONST_0);
        mn.visitVarInsn(Opcodes.ISTORE, 7);
        Label label22 = new Label();
        mn.visitLabel(label22);
        mn.visitFrame(Opcodes.F_APPEND, 3, new Object[]{Opcodes.INTEGER, "[B", Opcodes.INTEGER}, 0, null);
        mn.visitVarInsn(Opcodes.ILOAD, 7);
        mn.visitVarInsn(Opcodes.ILOAD, 5);
        Label label23 = new Label();
        mn.visitJumpInsn(Opcodes.IF_ICMPGE, label23);
        Label label24 = new Label();
        mn.visitLabel(label24);
        mn.visitVarInsn(Opcodes.ALOAD, 3);
        mn.visitVarInsn(Opcodes.ILOAD, 7);
        mn.visitIntInsn(Opcodes.BIPUSH, 8);
        mn.visitInsn(Opcodes.IMUL);
        mn.visitVarInsn(Opcodes.ILOAD, 7);
        mn.visitIntInsn(Opcodes.BIPUSH, 8);
        mn.visitInsn(Opcodes.IMUL);
        mn.visitIntInsn(Opcodes.BIPUSH, 8);
        mn.visitInsn(Opcodes.IADD);
        mn.visitMethodInsn(Opcodes.INVOKEVIRTUAL, "java/lang/StringBuilder", "substring", "(II)Ljava/lang/String;", false);
        mn.visitVarInsn(Opcodes.ASTORE, 8);
        Label label25 = new Label();
        mn.visitLabel(label25);
        mn.visitVarInsn(Opcodes.ALOAD, 6);
        mn.visitVarInsn(Opcodes.ILOAD, 7);
        mn.visitVarInsn(Opcodes.ALOAD, 8);
        mn.visitInsn(Opcodes.ICONST_2);
        mn.visitMethodInsn(Opcodes.INVOKESTATIC, "java/lang/Integer", "parseInt", "(Ljava/lang/String;I)I", false);
        mn.visitInsn(Opcodes.I2B);
        mn.visitInsn(Opcodes.BASTORE);
        Label label26 = new Label();
        mn.visitLabel(label26);
        mn.visitIincInsn(7, 1);
        mn.visitJumpInsn(Opcodes.GOTO, label22);
        mn.visitLabel(label23);
        mn.visitVarInsn(Opcodes.ALOAD, 6);
        mn.visitInsn(Opcodes.ARRAYLENGTH);
        mn.visitInsn(Opcodes.ICONST_4);
        mn.visitInsn(Opcodes.IDIV);
        mn.visitVarInsn(Opcodes.ISTORE, 7);
        Label label27 = new Label();
        mn.visitLabel(label27);
        mn.visitVarInsn(Opcodes.ILOAD, 7);
        mn.visitIntInsn(Opcodes.NEWARRAY, T_INT);
        mn.visitVarInsn(Opcodes.ASTORE, 8);
        Label label28 = new Label();
        mn.visitLabel(label28);
        mn.visitInsn(Opcodes.ICONST_0);
        mn.visitVarInsn(Opcodes.ISTORE, 9);
        Label label29 = new Label();
        mn.visitLabel(label29);
        mn.visitVarInsn(Opcodes.ILOAD, 9);
        mn.visitInsn(Opcodes.ICONST_4);
        mn.visitInsn(Opcodes.IMUL);
        mn.visitVarInsn(Opcodes.ISTORE, 11);
        Label label30 = new Label();
        mn.visitLabel(label30);
        mn.visitVarInsn(Opcodes.ALOAD, 8);
        mn.visitVarInsn(Opcodes.ILOAD, 9);
        mn.visitVarInsn(Opcodes.ALOAD, 6);
        mn.visitVarInsn(Opcodes.ILOAD, 11);
        mn.visitInsn(Opcodes.BALOAD);
        mn.visitIntInsn(Opcodes.SIPUSH, 255);
        mn.visitInsn(Opcodes.IAND);
        mn.visitIntInsn(Opcodes.BIPUSH, 24);
        mn.visitInsn(Opcodes.ISHL);
        mn.visitVarInsn(Opcodes.ALOAD, 6);
        mn.visitVarInsn(Opcodes.ILOAD, 11);
        mn.visitInsn(Opcodes.ICONST_1);
        mn.visitInsn(Opcodes.IADD);
        mn.visitInsn(Opcodes.BALOAD);
        mn.visitIntInsn(Opcodes.SIPUSH, 255);
        mn.visitInsn(Opcodes.IAND);
        mn.visitIntInsn(Opcodes.BIPUSH, 16);
        mn.visitInsn(Opcodes.ISHL);
        mn.visitInsn(Opcodes.IOR);
        mn.visitVarInsn(Opcodes.ALOAD, 6);
        mn.visitVarInsn(Opcodes.ILOAD, 11);
        mn.visitInsn(Opcodes.ICONST_2);
        mn.visitInsn(Opcodes.IADD);
        mn.visitInsn(Opcodes.BALOAD);
        mn.visitIntInsn(Opcodes.SIPUSH, 255);
        mn.visitInsn(Opcodes.IAND);
        mn.visitIntInsn(Opcodes.BIPUSH, 8);
        mn.visitInsn(Opcodes.ISHL);
        mn.visitInsn(Opcodes.IOR);
        mn.visitVarInsn(Opcodes.ALOAD, 6);
        mn.visitVarInsn(Opcodes.ILOAD, 11);
        mn.visitInsn(Opcodes.ICONST_3);
        mn.visitInsn(Opcodes.IADD);
        mn.visitInsn(Opcodes.BALOAD);
        mn.visitIntInsn(Opcodes.SIPUSH, 255);
        mn.visitInsn(Opcodes.IAND);
        mn.visitInsn(Opcodes.IOR);
        mn.visitInsn(Opcodes.DUP);
        mn.visitVarInsn(Opcodes.ISTORE, 10);
        Label label31 = new Label();
        mn.visitLabel(label31);
        mn.visitInsn(Opcodes.IASTORE);
        Label label32 = new Label();
        mn.visitLabel(label32);
        mn.visitIincInsn(9, 1);
        mn.visitVarInsn(Opcodes.ILOAD, 9);
        mn.visitVarInsn(Opcodes.ILOAD, 7);
        mn.visitJumpInsn(Opcodes.IF_ICMPLT, label29);
        Label label33 = new Label();
        mn.visitLabel(label33);
        mn.visitVarInsn(Opcodes.ALOAD, 8);
        mn.visitFieldInsn(Opcodes.PUTSTATIC, jClassNode.name, fieldName, "[I");
        Label label34 = new Label();
        mn.visitLabel(label34);
        mn.visitMaxs(6, 12);
        mn.visitEnd();

        return mn.instructions;
    }

    public static byte[] spreadInteger(int value) {
        return new byte[]{
                (byte) (value >>> 24),
                (byte) (value >>> 16),
                (byte) (value >>> 8),
                (byte) value
        };
    }
}
