package war.metaphor.util.builder;

import org.objectweb.asm.Opcodes;
import org.objectweb.asm.Type;
import org.objectweb.asm.tree.*;

import java.util.HashMap;
import java.util.Map;

public class InsnListBuilder implements Opcodes {

    private final InsnList list;

    public final Map<String, LabelNode> labels;

    public InsnListBuilder() {
        this.list = new InsnList();
        this.labels = new HashMap<>();
    }

    public static InsnListBuilder builder() {
        return new InsnListBuilder();
    }

    public InsnList build() {
        return list;
    }

    public LabelNode getLabel(String name) {
        return labels.computeIfAbsent(name, k -> new LabelNode());
    }

    public InsnListBuilder math(Number current, Number wanted, int size) {

        byte[] bits_current = toBits(current, size);
        byte[] bits_wanted = toBits(wanted, size);

        boolean xor = Math.random() < 0.5;

        if (xor) {
            return (size == Integer.SIZE) ?
                    constant(current.intValue() ^ wanted.intValue()).ixor() :
                    constant(current.longValue() ^ wanted.longValue()).lxor();
        } else {
            byte[] mask = new byte[size];
            for (int i = 0; i < size; i++) {
                byte b_c = bits_current[i];
                byte b_w = bits_wanted[i];
                if (b_w == 1 && b_c != 1) mask[i] = 1;
                else if (b_w == 1)
                    if (Math.random() < 0.5)
                        mask[i] = 1;
            }
            if (size == Integer.SIZE) {
                int mask_v = toInteger(mask);
                byte[] result = new byte[size];
                for (int i = 0; i < size; i++) {
                    byte b_w = bits_wanted[i];
                    byte b_c = bits_current[i];
                    if (b_w != 0) result[i] = 1;
                    else if (b_c == 0)
                        if (Math.random() < 0.5)
                            result[i] = 1;
                }
                int result_v = toInteger(result);
                return constant(mask_v).ior().constant(result_v).iand();
            } else {
                long mask_v = toLong(mask);
                byte[] result = new byte[size];
                for (int i = 0; i < size; i++) {
                    byte b_w = bits_wanted[i];
                    byte b_c = bits_current[i];
                    if (b_w != 0) result[i] = 1;
                    else if (b_c == 0)
                        if (Math.random() < 0.5)
                            result[i] = 1;
                }
                long result_v = toLong(result);
                return constant(mask_v).lor().constant(result_v).land();
            }
        }

    }

    public byte[] toBits(Number value, int size) {
        int i_v = value.intValue();
        long l_v = value.longValue();
        byte[] bits = new byte[size];
        for (int i = 0; i < size; i++) {
            if (size == Integer.SIZE) {
                bits[i] = (byte) (i_v & 1);
                i_v >>= 1;
            } else {
                bits[i] = (byte) (l_v & 1);
                l_v >>= 1;
            }
        }
        return bits;
    }

    public int toInteger(byte[] bits) {
        int value = 0;
        for (int i = bits.length - 1; i >= 0; i--) {
            value <<= 1;
            value |= bits[i];
        }
        return value;
    }

    public long toLong(byte[] bits) {
        long value = 0;
        for (int i = bits.length - 1; i >= 0; i--) {
            value <<= 1;
            value |= bits[i];
        }
        return value;
    }

    public InsnListBuilder nop() {
        list.add(new InsnNode(NOP));
        return this;
    }

    public InsnListBuilder aconst_null() {
        list.add(new InsnNode(ACONST_NULL));
        return this;
    }

    public InsnListBuilder constant(int value) {
        if (value >= -1 && value <= 5) {
            list.add(new InsnNode(ICONST_0 + value));
        } else if (value >= Byte.MIN_VALUE && value <= Byte.MAX_VALUE) {
            list.add(new IntInsnNode(BIPUSH, value));
        } else if (value >= Short.MIN_VALUE && value <= Short.MAX_VALUE) {
            list.add(new IntInsnNode(SIPUSH, value));
        } else {
            list.add(new LdcInsnNode(value));
        }
        return this;
    }

    public InsnListBuilder constant(long value) {
        if (value == 0 || value == 1) {
            list.add(new InsnNode(LCONST_0 + (int) value));
        } else {
            list.add(new LdcInsnNode(value));
        }
        return this;
    }

    public InsnListBuilder constant(float value) {
        if (value == 0.0f || value == 1.0f || value == 2.0f) {
            list.add(new InsnNode(FCONST_0 + (int) value));
        } else {
            list.add(new LdcInsnNode(value));
        }
        return this;
    }

    public InsnListBuilder constant(double value) {
        if (value == 0.0 || value == 1.0) {
            list.add(new InsnNode(DCONST_0 + (int) value));
        } else {
            list.add(new LdcInsnNode(value));
        }
        return this;
    }

    public InsnListBuilder constant(String value) {
        list.add(new LdcInsnNode(value));
        return this;
    }

    public InsnListBuilder load(Type type, int var) {
        list.add(new VarInsnNode(type.getOpcode(ILOAD), var));
        return this;
    }

    public InsnListBuilder iload(int var) {
        list.add(new VarInsnNode(ILOAD, var));
        return this;
    }

    public InsnListBuilder lload(int var) {
        list.add(new VarInsnNode(LLOAD, var));
        return this;
    }

    public InsnListBuilder fload(int var) {
        list.add(new VarInsnNode(FLOAD, var));
        return this;
    }

    public InsnListBuilder dload(int var) {
        list.add(new VarInsnNode(DLOAD, var));
        return this;
    }

    public InsnListBuilder aload(int var) {
        list.add(new VarInsnNode(ALOAD, var));
        return this;
    }

    public InsnListBuilder store(Type type, int var) {
        list.add(new VarInsnNode(type.getOpcode(ISTORE), var));
        return this;
    }

    public InsnListBuilder istore(int var) {
        list.add(new VarInsnNode(ISTORE, var));
        return this;
    }

    public InsnListBuilder lstore(int var) {
        list.add(new VarInsnNode(LSTORE, var));
        return this;
    }

    public InsnListBuilder fstore(int var) {
        list.add(new VarInsnNode(FSTORE, var));
        return this;
    }

    public InsnListBuilder dstore(int var) {
        list.add(new VarInsnNode(DSTORE, var));
        return this;
    }

    public InsnListBuilder astore(int var) {
        list.add(new VarInsnNode(ASTORE, var));
        return this;
    }

    public InsnListBuilder iaload() {
        list.add(new InsnNode(IALOAD));
        return this;
    }

    public InsnListBuilder laload() {
        list.add(new InsnNode(LALOAD));
        return this;
    }

    public InsnListBuilder faload() {
        list.add(new InsnNode(FALOAD));
        return this;
    }

    public InsnListBuilder daload() {
        list.add(new InsnNode(DALOAD));
        return this;
    }

    public InsnListBuilder aaload() {
        list.add(new InsnNode(AALOAD));
        return this;
    }

    public InsnListBuilder baload() {
        list.add(new InsnNode(BALOAD));
        return this;
    }

    public InsnListBuilder caload() {
        list.add(new InsnNode(CALOAD));
        return this;
    }

    public InsnListBuilder saload() {
        list.add(new InsnNode(SALOAD));
        return this;
    }

    public InsnListBuilder iastore() {
        list.add(new InsnNode(IASTORE));
        return this;
    }

    public InsnListBuilder lastore() {
        list.add(new InsnNode(LASTORE));
        return this;
    }

    public InsnListBuilder fastore() {
        list.add(new InsnNode(FASTORE));
        return this;
    }

    public InsnListBuilder dastore() {
        list.add(new InsnNode(DASTORE));
        return this;
    }

    public InsnListBuilder aastore() {
        list.add(new InsnNode(AASTORE));
        return this;
    }

    public InsnListBuilder bastore() {
        list.add(new InsnNode(BASTORE));
        return this;
    }

    public InsnListBuilder castore() {
        list.add(new InsnNode(CASTORE));
        return this;
    }

    public InsnListBuilder sastore() {
        list.add(new InsnNode(SASTORE));
        return this;
    }

    public InsnListBuilder pop() {
        list.add(new InsnNode(POP));
        return this;
    }

    public InsnListBuilder pop2() {
        list.add(new InsnNode(POP2));
        return this;
    }

    public InsnListBuilder dup() {
        list.add(new InsnNode(DUP));
        return this;
    }

    public InsnListBuilder dup_x1() {
        list.add(new InsnNode(DUP_X1));
        return this;
    }

    public InsnListBuilder dup_x2() {
        list.add(new InsnNode(DUP_X2));
        return this;
    }

    public InsnListBuilder dup2() {
        list.add(new InsnNode(DUP2));
        return this;
    }

    public InsnListBuilder dup2_x1() {
        list.add(new InsnNode(DUP2_X1));
        return this;
    }

    public InsnListBuilder dup2_x2() {
        list.add(new InsnNode(DUP2_X2));
        return this;
    }

    public InsnListBuilder swap() {
        list.add(new InsnNode(SWAP));
        return this;
    }

    public InsnListBuilder iadd() {
        list.add(new InsnNode(IADD));
        return this;
    }

    public InsnListBuilder ladd() {
        list.add(new InsnNode(LADD));
        return this;
    }

    public InsnListBuilder fadd() {
        list.add(new InsnNode(FADD));
        return this;
    }

    public InsnListBuilder dadd() {
        list.add(new InsnNode(DADD));
        return this;
    }

    public InsnListBuilder isub() {
        list.add(new InsnNode(ISUB));
        return this;
    }

    public InsnListBuilder lsub() {
        list.add(new InsnNode(LSUB));
        return this;
    }

    public InsnListBuilder fsub() {
        list.add(new InsnNode(FSUB));
        return this;
    }

    public InsnListBuilder dsub() {
        list.add(new InsnNode(DSUB));
        return this;
    }

    public InsnListBuilder imul() {
        list.add(new InsnNode(IMUL));
        return this;
    }

    public InsnListBuilder lmul() {
        list.add(new InsnNode(LMUL));
        return this;
    }

    public InsnListBuilder fmul() {
        list.add(new InsnNode(FMUL));
        return this;
    }

    public InsnListBuilder dmul() {
        list.add(new InsnNode(DMUL));
        return this;
    }

    public InsnListBuilder idiv() {
        list.add(new InsnNode(IDIV));
        return this;
    }

    public InsnListBuilder ldiv() {
        list.add(new InsnNode(LDIV));
        return this;
    }

    public InsnListBuilder fdiv() {
        list.add(new InsnNode(FDIV));
        return this;
    }

    public InsnListBuilder ddiv() {
        list.add(new InsnNode(DDIV));
        return this;
    }

    public InsnListBuilder irem() {
        list.add(new InsnNode(IREM));
        return this;
    }

    public InsnListBuilder lrem() {
        list.add(new InsnNode(LREM));
        return this;
    }

    public InsnListBuilder frem() {
        list.add(new InsnNode(FREM));
        return this;
    }

    public InsnListBuilder drem() {
        list.add(new InsnNode(DREM));
        return this;
    }

    public InsnListBuilder ineg() {
        list.add(new InsnNode(INEG));
        return this;
    }

    public InsnListBuilder lneg() {
        list.add(new InsnNode(LNEG));
        return this;
    }

    public InsnListBuilder fneg() {
        list.add(new InsnNode(FNEG));
        return this;
    }

    public InsnListBuilder dneg() {
        list.add(new InsnNode(DNEG));
        return this;
    }

    public InsnListBuilder ishl() {
        list.add(new InsnNode(ISHL));
        return this;
    }

    public InsnListBuilder lshl() {
        list.add(new InsnNode(LSHL));
        return this;
    }

    public InsnListBuilder ishr() {
        list.add(new InsnNode(ISHR));
        return this;
    }

    public InsnListBuilder lshr() {
        list.add(new InsnNode(LSHR));
        return this;
    }

    public InsnListBuilder iushr() {
        list.add(new InsnNode(IUSHR));
        return this;
    }

    public InsnListBuilder lushr() {
        list.add(new InsnNode(LUSHR));
        return this;
    }

    public InsnListBuilder iand() {
        list.add(new InsnNode(IAND));
        return this;
    }

    public InsnListBuilder land() {
        list.add(new InsnNode(LAND));
        return this;
    }

    public InsnListBuilder ior() {
        list.add(new InsnNode(IOR));
        return this;
    }

    public InsnListBuilder lor() {
        list.add(new InsnNode(LOR));
        return this;
    }

    public InsnListBuilder ixor() {
        list.add(new InsnNode(IXOR));
        return this;
    }

    public InsnListBuilder lxor() {
        list.add(new InsnNode(LXOR));
        return this;
    }

    public InsnListBuilder iinc(int var, int increment) {
        list.add(new IincInsnNode(var, increment));
        return this;
    }

    public InsnListBuilder i2l() {
        list.add(new InsnNode(I2L));
        return this;
    }

    public InsnListBuilder i2f() {
        list.add(new InsnNode(I2F));
        return this;
    }

    public InsnListBuilder i2d() {
        list.add(new InsnNode(I2D));
        return this;
    }

    public InsnListBuilder l2i() {
        list.add(new InsnNode(L2I));
        return this;
    }

    public InsnListBuilder l2f() {
        list.add(new InsnNode(L2F));
        return this;
    }

    public InsnListBuilder l2d() {
        list.add(new InsnNode(L2D));
        return this;
    }

    public InsnListBuilder f2i() {
        list.add(new InsnNode(F2I));
        return this;
    }

    public InsnListBuilder f2l() {
        list.add(new InsnNode(F2L));
        return this;
    }

    public InsnListBuilder f2d() {
        list.add(new InsnNode(F2D));
        return this;
    }

    public InsnListBuilder d2i() {
        list.add(new InsnNode(D2I));
        return this;
    }

    public InsnListBuilder d2l() {
        list.add(new InsnNode(D2L));
        return this;
    }

    public InsnListBuilder d2f() {
        list.add(new InsnNode(D2F));
        return this;
    }

    public InsnListBuilder i2b() {
        list.add(new InsnNode(I2B));
        return this;
    }

    public InsnListBuilder i2c() {
        list.add(new InsnNode(I2C));
        return this;
    }

    public InsnListBuilder i2s() {
        list.add(new InsnNode(I2S));
        return this;
    }

    public InsnListBuilder lcmp() {
        list.add(new InsnNode(LCMP));
        return this;
    }

    public InsnListBuilder fcmpl() {
        list.add(new InsnNode(FCMPL));
        return this;
    }

    public InsnListBuilder fcmpg() {
        list.add(new InsnNode(FCMPG));
        return this;
    }

    public InsnListBuilder dcmpl() {
        list.add(new InsnNode(DCMPL));
        return this;
    }

    public InsnListBuilder dcmpg() {
        list.add(new InsnNode(DCMPG));
        return this;
    }

    public InsnListBuilder ifeq(LabelNode label) {
        list.add(new JumpInsnNode(IFEQ, label));
        return this;
    }

    public InsnListBuilder ifne(LabelNode label) {
        list.add(new JumpInsnNode(IFNE, label));
        return this;
    }

    public InsnListBuilder iflt(LabelNode label) {
        list.add(new JumpInsnNode(IFLT, label));
        return this;
    }

    public InsnListBuilder ifge(LabelNode label) {
        list.add(new JumpInsnNode(IFGE, label));
        return this;
    }

    public InsnListBuilder ifgt(LabelNode label) {
        list.add(new JumpInsnNode(IFGT, label));
        return this;
    }

    public InsnListBuilder ifle(LabelNode label) {
        list.add(new JumpInsnNode(IFLE, label));
        return this;
    }

    public InsnListBuilder if_icmpeq(LabelNode label) {
        list.add(new JumpInsnNode(IF_ICMPEQ, label));
        return this;
    }

    public InsnListBuilder if_icmpne(LabelNode label) {
        list.add(new JumpInsnNode(IF_ICMPNE, label));
        return this;
    }

    public InsnListBuilder if_icmplt(LabelNode label) {
        list.add(new JumpInsnNode(IF_ICMPLT, label));
        return this;
    }

    public InsnListBuilder if_icmpge(LabelNode label) {
        list.add(new JumpInsnNode(IF_ICMPGE, label));
        return this;
    }

    public InsnListBuilder if_icmpgt(LabelNode label) {
        list.add(new JumpInsnNode(IF_ICMPGT, label));
        return this;
    }

    public InsnListBuilder if_icmple(LabelNode label) {
        list.add(new JumpInsnNode(IF_ICMPLE, label));
        return this;
    }

    public InsnListBuilder if_acmpeq(LabelNode label) {
        list.add(new JumpInsnNode(IF_ACMPEQ, label));
        return this;
    }

    public InsnListBuilder if_acmpne(LabelNode label) {
        list.add(new JumpInsnNode(IF_ACMPNE, label));
        return this;
    }

    public InsnListBuilder _goto(LabelNode label) {
        list.add(new JumpInsnNode(GOTO, label));
        return this;
    }

    public InsnListBuilder tableswitch(int min, int max, LabelNode dflt, LabelNode... labels) {
        list.add(new TableSwitchInsnNode(min, max, dflt, labels));
        return this;
    }

    public InsnListBuilder lookupswitch(LabelNode dflt, int[] keys, LabelNode[] labels) {
        list.add(new LookupSwitchInsnNode(dflt, keys, labels));
        return this;
    }

    public InsnListBuilder ireturn() {
        list.add(new InsnNode(IRETURN));
        return this;
    }

    public InsnListBuilder lreturn() {
        list.add(new InsnNode(LRETURN));
        return this;
    }

    public InsnListBuilder freturn() {
        list.add(new InsnNode(FRETURN));
        return this;
    }

    public InsnListBuilder dreturn() {
        list.add(new InsnNode(DRETURN));
        return this;
    }

    public InsnListBuilder areturn() {
        list.add(new InsnNode(ARETURN));
        return this;
    }

    public InsnListBuilder _return() {
        list.add(new InsnNode(RETURN));
        return this;
    }

    public InsnListBuilder getstatic(String owner, String name, String desc) {
        list.add(new FieldInsnNode(GETSTATIC, owner, name, desc));
        return this;
    }

    public InsnListBuilder putstatic(String owner, String name, String desc) {
        list.add(new FieldInsnNode(PUTSTATIC, owner, name, desc));
        return this;
    }

    public InsnListBuilder getfield(String owner, String name, String desc) {
        list.add(new FieldInsnNode(GETFIELD, owner, name, desc));
        return this;
    }

    public InsnListBuilder putfield(String owner, String name, String desc) {
        list.add(new FieldInsnNode(PUTFIELD, owner, name, desc));
        return this;
    }

    public InsnListBuilder invokevirtual(String owner, String name, String desc) {
        list.add(new MethodInsnNode(INVOKEVIRTUAL, owner, name, desc, false));
        return this;
    }

    public InsnListBuilder invokespecial(String owner, String name, String desc) {
        list.add(new MethodInsnNode(INVOKESPECIAL, owner, name, desc, false));
        return this;
    }

    public InsnListBuilder invokestatic(String owner, String name, String desc) {
        list.add(new MethodInsnNode(INVOKESTATIC, owner, name, desc, false));
        return this;
    }

    public InsnListBuilder invokeinterface(String owner, String name, String desc) {
        list.add(new MethodInsnNode(INVOKEINTERFACE, owner, name, desc, true));
        return this;
    }

    public InsnListBuilder _new(String type) {
        list.add(new TypeInsnNode(NEW, type));
        return this;
    }

    public InsnListBuilder newarray(int type) {
        list.add(new IntInsnNode(NEWARRAY, type));
        return this;
    }

    public InsnListBuilder anewarray(String type) {
        list.add(new TypeInsnNode(ANEWARRAY, type));
        return this;
    }

    public InsnListBuilder arraylength() {
        list.add(new InsnNode(ARRAYLENGTH));
        return this;
    }

    public InsnListBuilder athrow() {
        list.add(new InsnNode(ATHROW));
        return this;
    }

    public InsnListBuilder checkcast(String type) {
        list.add(new TypeInsnNode(CHECKCAST, type));
        return this;
    }

    public InsnListBuilder _instanceof(String type) {
        list.add(new TypeInsnNode(INSTANCEOF, type));
        return this;
    }

    public InsnListBuilder monitorenter() {
        list.add(new InsnNode(MONITORENTER));
        return this;
    }

    public InsnListBuilder monitorexit() {
        list.add(new InsnNode(MONITOREXIT));
        return this;
    }

    public InsnListBuilder multianewarray(String desc, int dims) {
        list.add(new MultiANewArrayInsnNode(desc, dims));
        return this;
    }

    public InsnListBuilder ifnull(LabelNode label) {
        list.add(new JumpInsnNode(IFNULL, label));
        return this;
    }

    public InsnListBuilder ifnonnull(LabelNode label) {
        list.add(new JumpInsnNode(IFNONNULL, label));
        return this;
    }

    public InsnListBuilder label(LabelNode label) {
        list.add(label);
        return this;
    }

    public InsnListBuilder label(String name) {
        LabelNode label = labels.computeIfAbsent(name, k -> new LabelNode());
        list.add(label);
        return this;
    }

    public InsnListBuilder ifeq(String label) {
        return ifeq(labels.computeIfAbsent(label, k -> new LabelNode()));
    }

    public InsnListBuilder ifne(String label) {
        return ifne(labels.computeIfAbsent(label, k -> new LabelNode()));
    }

    public InsnListBuilder iflt(String label) {
        return iflt(labels.computeIfAbsent(label, k -> new LabelNode()));
    }

    public InsnListBuilder ifge(String label) {
        return ifge(labels.computeIfAbsent(label, k -> new LabelNode()));
    }

    public InsnListBuilder ifgt(String label) {
        return ifgt(labels.computeIfAbsent(label, k -> new LabelNode()));
    }

    public InsnListBuilder ifle(String label) {
        return ifle(labels.computeIfAbsent(label, k -> new LabelNode()));
    }

    public InsnListBuilder if_icmpeq(String label) {
        return if_icmpeq(labels.computeIfAbsent(label, k -> new LabelNode()));
    }

    public InsnListBuilder if_icmpne(String label) {
        return if_icmpne(labels.computeIfAbsent(label, k -> new LabelNode()));
    }

    public InsnListBuilder if_icmplt(String label) {
        return if_icmplt(labels.computeIfAbsent(label, k -> new LabelNode()));
    }

    public InsnListBuilder if_icmpge(String label) {
        return if_icmpge(labels.computeIfAbsent(label, k -> new LabelNode()));
    }

    public InsnListBuilder if_icmpgt(String label) {
        return if_icmpgt(labels.computeIfAbsent(label, k -> new LabelNode()));
    }

    public InsnListBuilder if_icmple(String label) {
        return if_icmple(labels.computeIfAbsent(label, k -> new LabelNode()));
    }

    public InsnListBuilder if_acmpeq(String label) {
        return if_acmpeq(labels.computeIfAbsent(label, k -> new LabelNode()));
    }

    public InsnListBuilder if_acmpne(String label) {
        return if_acmpne(labels.computeIfAbsent(label, k -> new LabelNode()));
    }

    public InsnListBuilder _goto(String label) {
        return _goto(labels.computeIfAbsent(label, k -> new LabelNode()));
    }

    public InsnListBuilder ifnull(String label) {
        return ifnull(labels.computeIfAbsent(label, k -> new LabelNode()));
    }

    public InsnListBuilder ifnonnull(String label) {
        return ifnonnull(labels.computeIfAbsent(label, k -> new LabelNode()));
    }

    public InsnListBuilder list(InsnList list) {
        this.list.add(list);
        return this;
    }

    public InsnListBuilder ldc(Object i) {
        list.add(new LdcInsnNode(i));
        return this;
    }

    public InsnListBuilder iconst_0() {
        list.add(new InsnNode(ICONST_0));
        return this;
    }

    public InsnListBuilder iconst_1() {
        list.add(new InsnNode(ICONST_1));
        return this;
    }

    public InsnListBuilder iconst_2() {
        list.add(new InsnNode(ICONST_2));
        return this;
    }

    public InsnListBuilder iconst_3() {
        list.add(new InsnNode(ICONST_3));
        return this;
    }

    public InsnListBuilder iconst_4() {
        list.add(new InsnNode(ICONST_4));
        return this;
    }

    public InsnListBuilder iconst_5() {
        list.add(new InsnNode(ICONST_5));
        return this;
    }

    public InsnListBuilder iconst_m1() {
        list.add(new InsnNode(ICONST_M1));
        return this;
    }
}
