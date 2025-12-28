package war.metaphor.mutator.data.strings.polymorphic.math.impl;

import org.objectweb.asm.Opcodes;
import org.objectweb.asm.tree.*;
import war.metaphor.mutator.data.strings.polymorphic.math.IPolymorphicMath;
import war.metaphor.util.asm.BytecodeUtil;
import war.metaphor.util.interfaces.IRandom;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

/**
 * not broken anymore since forever basically
 * @author jan
 */
@Deprecated
public class LookupSwitchXor implements IPolymorphicMath, IRandom {
    private final Map<Integer, Integer> map = new HashMap<>();

    public LookupSwitchXor() {
        for (int i = 0; i < RANDOM.nextInt(5) + 2; i++) {
            map.put(i, RANDOM.nextInt());
        }
    }

    @Override
    public int apply(int input) {
        int idx = input;
        if (idx < 0) {
            idx = -idx;
        }

        return map.get(idx % map.size()) ^ input;
    }

    @Override
    public InsnList dump() {
        InsnList list = new InsnList();
        ArrayList<LabelNode> labels = new ArrayList<>();
        LabelNode dflt = new LabelNode();
        LabelNode start = new LabelNode();
        for (int i = 0; i < map.size(); i++) {
            labels.add(new LabelNode());
        }

        // punjabi pattern matching bypass
        if (RANDOM.nextBoolean()) {
            list.add(new InsnNode(Opcodes.DUP));
            list.add(new InsnNode(Opcodes.DUP2));
        } else {
            list.add(new InsnNode(Opcodes.DUP2));
            list.add(new InsnNode(Opcodes.DUP));
        }
        list.add(new JumpInsnNode(Opcodes.IFGE, start));
        list.add(new InsnNode(Opcodes.INEG));
        list.add(start);
        list.add(BytecodeUtil.makeInteger(map.size()));
        list.add(new InsnNode(Opcodes.IREM));
        list.add(new LookupSwitchInsnNode(
                dflt,
                map.keySet().stream().mapToInt(i -> i).toArray(),
                labels.toArray(new LabelNode[0])
        ));
        for (int i = 0; i < map.size(); i++) {
            list.add(labels.get(i));
            list.add(BytecodeUtil.makeInteger(map.get(i)));
            list.add(new InsnNode(Opcodes.IXOR));
            list.add(new JumpInsnNode(Opcodes.GOTO, dflt));
        }
        list.add(dflt);
        list.add(new InsnNode(Opcodes.IXOR));

        return list;
    }
}
