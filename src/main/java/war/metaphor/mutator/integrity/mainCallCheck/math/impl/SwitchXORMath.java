package war.metaphor.mutator.integrity.mainCallCheck.math.impl;

import org.objectweb.asm.Label;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.tree.*;
import war.metaphor.mutator.integrity.mainCallCheck.math.IMath;
import war.metaphor.util.asm.BytecodeUtil;
import war.metaphor.util.interfaces.IRandom;

import static org.objectweb.asm.Opcodes.*;

public class SwitchXORMath implements IMath, IRandom {
    private final int[] keys = new int[6];
    private final boolean idk = RANDOM.nextBoolean();

    public SwitchXORMath() {
        for (int i = 0; i < keys.length; i++) {
            keys[i] = RANDOM.nextInt();
        }
    }

    @Override
    public int apply(int input) {
        int xorVal = switch ((input > 0 ? input : -input) % 6) {
            case 0 -> keys[0];
            case 1 -> keys[1];
            case 2 -> keys[2];
            case 3 -> keys[3];
            case 4 -> keys[4];
            case 5 -> keys[5];
            default -> throw new IllegalStateException("Unexpected value: " + input % 6);
        };
        return input ^ xorVal;
    }

    @Override
    public InsnList dump() {
        InsnList list = new InsnList();

        LabelNode[] caseLabels = new LabelNode[6];
        for (int i = 0; i < 6; i++) {
            caseLabels[i] = new LabelNode();
        }
        LabelNode start = new LabelNode();
        LabelNode end = new LabelNode();

        list.add(new InsnNode(Opcodes.DUP));
        list.add(new InsnNode(Opcodes.DUP));
        if (idk) {
            list.add(new InsnNode(Opcodes.DUP));
        }
        list.add(new JumpInsnNode(Opcodes.IFGE, start));
        list.add(new InsnNode(Opcodes.INEG));
        list.add(start);
        list.add(BytecodeUtil.makeInteger(6));
        list.add(new InsnNode(IREM));
        list.add(new TableSwitchInsnNode(0, 5, end, caseLabels));

        for (int i = 0; i < 6; i++) {
            list.add(caseLabels[i]);
            list.add(BytecodeUtil.makeInteger(keys[i]));
            list.add(new InsnNode(IXOR));
            list.add(new JumpInsnNode(GOTO, end));
        }

        list.add(end);

        if (idk) {
            list.add(new InsnNode(SWAP));
            list.add(new InsnNode(POP));
        }

        return list;
    }
}
