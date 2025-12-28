package war.jnt.core.code.impl;

import org.objectweb.asm.Opcodes;
import org.objectweb.asm.Type;
import org.objectweb.asm.tree.IntInsnNode;
import war.jnt.core.code.UnitContext;
import war.jnt.fusebox.impl.Internal;
import war.jnt.obfuscation.MutatedNumeric;

public class PushUnit implements Opcodes {
    private static final String[] arrays = {
            "NewBooleanArray",
            "NewCharArray",
            "NewFloatArray",
            "NewDoubleArray",
            "NewByteArray",
            "NewShortArray",
            "NewIntArray",
            "NewLongArray",
    };

    public static void process(IntInsnNode insn, UnitContext ctx) {

        if (insn.getOpcode() == NEWARRAY) {
            processNewArray(insn, ctx);
            return;
        }

        String computed = Internal.computePush(ctx.getTracker());

        var mutator = new MutatedNumeric(computed, insn.operand, Type.INT_TYPE);
        ctx.append(mutator.get());
    }

    private static void processNewArray(IntInsnNode insn, UnitContext ctx) {
        String arrayType = arrays[insn.operand - T_BOOLEAN];

        String len = Internal.computePop(ctx.getTracker());
        String push = Internal.computePush(ctx.getTracker());

        ctx.fmtAppend("""
                \t%s.l = (*env)->%s(env, %s.i);
                """,
                push, arrayType, len);
    }
}
