package war.jnt.core.code.impl;

import org.objectweb.asm.Opcodes;
import org.objectweb.asm.tree.JumpInsnNode;
import war.jnt.core.code.UnitContext;
import war.jnt.core.vm.TempJumpVM;
import war.jnt.fusebox.impl.Internal;

public class JumpUnit implements Opcodes {
    private static final String[] OPERATIONS = {
            "==", "!=", "<", ">=", ">", "<="
    };

    public static void process(JumpInsnNode insn, UnitContext ctx, TempJumpVM tjvm) {
        String targetBlock = BlockUnit.resolveBlock(insn.label);

        switch (insn.getOpcode()) {
            case GOTO -> {
                ctx.fmtAppend("\tgoto %s;\n", targetBlock);
            }
            case IFEQ, IFNE, IFLT, IFGE, IFGT, IFLE -> {
                String computed = Internal.computePop(ctx.getTracker());
                String operation = OPERATIONS[insn.getOpcode() - IFEQ];

                ctx.fmtAppend("\tif (%s.i %s 0) goto %s;", computed, operation, targetBlock);
            }
            case IF_ICMPEQ, IF_ICMPNE, IF_ICMPLT, IF_ICMPGE, IF_ICMPGT, IF_ICMPLE -> {
                String opA = Internal.computePop(ctx.getTracker());
                String opB = Internal.computePop(ctx.getTracker());
                String operation = OPERATIONS[insn.getOpcode() - IF_ICMPEQ];

                ctx.fmtAppend("\tif (%s.i %s %s.i) goto %s;\n", opB, operation, opA, targetBlock);
            }
            case IF_ACMPEQ -> {
                String objA = Internal.computePop(ctx.getTracker());
                String objB = Internal.computePop(ctx.getTracker());

                tjvm.makeValue(24);
                ctx.fmtAppend("\tif(((jboolean (*)(JNIEnv *, jobject, jobject)) (*((void **)*env + *(volatile int *)&output)))(env, %s.l, %s.l)) goto %s;\n", objB, objA, targetBlock);
            }
            case IF_ACMPNE -> {
                String objA = Internal.computePop(ctx.getTracker());
                String objB = Internal.computePop(ctx.getTracker());

                tjvm.makeValue(24);
                ctx.fmtAppend("\tif(!((jboolean (*)(JNIEnv *, jobject, jobject)) (*((void **)*env + *(volatile int *)&output)))(env, %s.l, %s.l)) goto %s;\n", objB, objA, targetBlock);
            }
            case IFNULL, IFNONNULL -> {
                String obj = Internal.computePop(ctx.getTracker());
                String operation = insn.getOpcode() == IFNONNULL ? "!=" : "==";

                ctx.fmtAppend("\tif (%s.l %s NULL) goto %s;\n", obj, operation, targetBlock);
            }
        }
    }
}
