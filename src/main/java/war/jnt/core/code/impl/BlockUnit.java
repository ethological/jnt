package war.jnt.core.code.impl;

import org.objectweb.asm.tree.AbstractInsnNode;
import org.objectweb.asm.tree.JumpInsnNode;
import org.objectweb.asm.tree.LabelNode;
import org.objectweb.asm.tree.LdcInsnNode;
import org.objectweb.asm.tree.analysis.BasicValue;
import org.objectweb.asm.tree.analysis.Frame;
import war.jnt.core.code.UnitContext;

import java.util.Map;

public class BlockUnit {

    public static String resolveBlock(LabelNode insn) {
        return "b" + insn.index;
    }

    public static void process(LabelNode insn, Map<AbstractInsnNode, Frame<BasicValue>> frames, UnitContext ctx) {
        ctx.getBuilder().append(resolveBlock(insn)).append(":\n");
        if (insn.nextInsn == null) return;
        Frame<BasicValue> frame = frames.get(insn.nextInsn);
        if (frame == null) return;
        ctx.getTracker().setSp(frame.getStackSize() - 1);
    }
}
