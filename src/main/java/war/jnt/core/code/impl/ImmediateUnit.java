package war.jnt.core.code.impl;

import org.objectweb.asm.Opcodes;
import org.objectweb.asm.tree.InsnNode;
import org.objectweb.asm.tree.JumpInsnNode;
import war.jnt.core.code.UnitContext;
import war.jnt.fusebox.impl.Internal;

public class ImmediateUnit implements Opcodes {
    public static void process(InsnNode insn, UnitContext ctx) {
        String computed = Internal.computePush(ctx.getTracker());

        if (insn.getOpcode() == ACONST_NULL) {
            ctx.fmtAppend("\t%s.l = NULL;\n", computed);
            return;
        }

        switch (Internal.resolveConst(insn.getOpcode())) {
            case 0 -> {
                int value = Internal.fromIntConst(insn);
                ctx.getBuilder().append(String.format("\t%s.i = %d;\n", computed, value));
            }
            case 1 -> {
                float value = Internal.fromFloatConst(insn);
                ctx.getBuilder().append(String.format("\t%s.f = %f;\n", computed, value));
            }
            case 2 -> {
                double value = Internal.fromDoubleConst(insn);
                ctx.getBuilder().append(String.format("\t%s.d = %f;\n", computed, value));
            }
            case 3 -> {
                long value = Internal.fromLongConst(insn);
                ctx.getBuilder().append(String.format("\t%s.j = %d;\n", computed, value));
            }
        }
    }
}
