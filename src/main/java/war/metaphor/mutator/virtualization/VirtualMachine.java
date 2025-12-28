package war.metaphor.mutator.virtualization;

import lombok.Getter;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.tree.*;
import war.metaphor.util.asm.BytecodeUtil;

import java.security.SecureRandom;

public class VirtualMachine implements Opcodes {
    @Getter
    private final LabelNode rangeStart;
    @Getter
    private final LabelNode rangeEnd;

    private final HandlerRange push8Range = new HandlerRange();
    private final HandlerRange push16Range = new HandlerRange();
    private final HandlerRange push32Range = new HandlerRange();

    private static final SecureRandom rand = new SecureRandom();

    private final byte[] operands;

    private final VirtualMachineInterface vmi;

    public static final int PUSH8;
    public static final int PUSH16;
    public static final int PUSH32;

    static {
        PUSH8 = rand.nextInt();
        PUSH16 = rand.nextInt();
        PUSH32 = rand.nextInt();
    }

    public VirtualMachine(VirtualMachineInterface vmi, byte[] operands) {
        this.vmi = vmi;
        this.operands = operands;
        this.rangeStart = new LabelNode();
        this.rangeEnd = new LabelNode();
    }

    public InsnList generate() {
        var list = new InsnList();

        // stack init
        list.add(new IntInsnNode(SIPUSH, 256));
        list.add(new IntInsnNode(NEWARRAY, T_INT));
        list.add(new VarInsnNode(ASTORE, vmi.stackLocal()));
        list.add(new InsnNode(ICONST_M1));
        list.add(new VarInsnNode(ISTORE, vmi.pointerLocal()));

        list.add(new VarInsnNode(ILOAD, vmi.opcodeLocal()));
        list.add(new LookupSwitchInsnNode(
                push32Range.getStart(),
                new int[]{
                        PUSH8, PUSH16, PUSH32
                },
                new LabelNode[]{
                        push8Range.getStart(),
                        push16Range.getStart(),
                        push32Range.getStart()
                }
        ));

        list.add(rangeStart);
        list.add(push8Range.getStart());
        list.add(new VarInsnNode(ALOAD, vmi.stackLocal()));
        list.add(new IincInsnNode(vmi.pointerLocal(), 1));
        list.add(new VarInsnNode(ILOAD, vmi.pointerLocal()));
        list.add(new IntInsnNode(BIPUSH, operands[0]));
        list.add(new InsnNode(IASTORE));
        list.add(new JumpInsnNode(GOTO, rangeEnd));
        list.add(push8Range.getEnd());

        int m16 = (operands[0] << 8) | operands[1];

        list.add(push16Range.getStart());
        list.add(new VarInsnNode(ALOAD, vmi.stackLocal()));
        list.add(new IincInsnNode(vmi.pointerLocal(), 1));
        list.add(new VarInsnNode(ILOAD, vmi.pointerLocal()));
        list.add(new IntInsnNode(SIPUSH, m16));
        list.add(new InsnNode(IASTORE));
        list.add(new JumpInsnNode(GOTO, rangeEnd));
        list.add(push16Range.getEnd());

        int m32 = (operands[0] << 24) |
                ((operands[1] & 0xFF) << 16) |
                ((operands[2] & 0xFF) << 8) |
                (operands[3] & 0xFF);

        list.add(push32Range.getStart());
        list.add(new VarInsnNode(ALOAD, vmi.stackLocal()));
        list.add(new IincInsnNode(vmi.pointerLocal(), 1));
        list.add(new VarInsnNode(ILOAD, vmi.pointerLocal()));
        list.add(new LdcInsnNode(m32));
        list.add(new InsnNode(IASTORE));
        list.add(push32Range.getEnd());

        list.add(rangeEnd);
        list.add(new VarInsnNode(ALOAD, vmi.stackLocal()));
        list.add(new VarInsnNode(ILOAD, vmi.pointerLocal()));
        list.add(new InsnNode(IALOAD));

        return list;
    }
}
