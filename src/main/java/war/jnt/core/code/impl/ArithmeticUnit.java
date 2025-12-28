package war.jnt.core.code.impl;

import org.objectweb.asm.Opcodes;
import org.objectweb.asm.tree.InsnNode;
import war.jnt.cache.Cache;
import war.jnt.core.code.UnitContext;
import war.jnt.core.vm.EnumVMOperation;
import war.jnt.core.vm.TempJumpVM;
import war.jnt.fusebox.impl.Internal;
import war.jnt.innercache.InnerCache;

public class ArithmeticUnit implements Opcodes {
    public static void process(final InnerCache ic, InsnNode insn, UnitContext ctx, TempJumpVM tjvm) {
        String ae = ic.FindClass("java/lang/ArithmeticException");

        switch (insn.getOpcode()) {
            case IREM, IDIV -> {
                ctx.fmtAppend("\tif (stack[%s].i == 0) { (*env)->ThrowNew(env, %s, \"integer division by zero\"); goto %s; }\n",
                        ctx.getTracker().dump(), ae, ctx.handlerLabel);
            }
            case LREM, LDIV -> {
                ctx.fmtAppend("\tif (stack[%s].j == 0) { (*env)->ThrowNew(env, %s, \"long division by zero\"); goto %s; }\n",
                        ctx.getTracker().dump(), ae, ctx.handlerLabel);
            }
        }
        switch (insn.getOpcode()) {
            case IADD -> {
                String computedA = Internal.computePop(ctx.getTracker());
                String computedB = Internal.computePop(ctx.getTracker());
                String computedPush = Internal.computePush(ctx.getTracker());

                ctx.getBuilder().append(tjvm.getCode(computedA, computedB, computedPush, EnumVMOperation.ADD));
            }
            case ISUB -> {
                String computedA = Internal.computePop(ctx.getTracker());
                String computedB = Internal.computePop(ctx.getTracker());
                String computedPush = Internal.computePush(ctx.getTracker());

                ctx.getBuilder().append(tjvm.getCode(computedA, computedB, computedPush, EnumVMOperation.SUBTRACT));
            }
            case IDIV -> {
                String computedA = Internal.computePop(ctx.getTracker());
                String computedB = Internal.computePop(ctx.getTracker());
                String computedPush = Internal.computePush(ctx.getTracker());

                ctx.getBuilder().append(tjvm.getCode(computedA, computedB, computedPush, EnumVMOperation.DIVIDE));
            }
            case IMUL -> {
                String computedA = Internal.computePop(ctx.getTracker());
                String computedB = Internal.computePop(ctx.getTracker());
                String computedPush = Internal.computePush(ctx.getTracker());

                ctx.getBuilder().append(tjvm.getCode(computedA, computedB, computedPush, EnumVMOperation.MULTIPLY));
            }
            case ISHL -> {
                String computedA = Internal.computePop(ctx.getTracker());
                String computedB = Internal.computePop(ctx.getTracker());
                String computedPush = Internal.computePush(ctx.getTracker());

                ctx.getBuilder().append(tjvm.getCode(computedA, computedB, computedPush, EnumVMOperation.SHIFT_LEFT));
            }
            case DREM -> {
                String computedA = Internal.computePop(ctx.getTracker());
                String computedB = Internal.computePop(ctx.getTracker());
                String computedPush = Internal.computePush(ctx.getTracker());

                ctx.fmtAppend("\t%s.d = fmod(%s.d, %s.d);\n", computedPush, computedB, computedA);
            }
            case FREM -> {
                String computedA = Internal.computePop(ctx.getTracker());
                String computedB = Internal.computePop(ctx.getTracker());
                String computedPush = Internal.computePush(ctx.getTracker());

                ctx.fmtAppend("\t%s.f = fmod(%s.f, %s.f);\n", computedPush, computedB, computedA);
            }
            case IREM -> {
                String computedA = Internal.computePop(ctx.getTracker());
                String computedB = Internal.computePop(ctx.getTracker());
                String computedPush = Internal.computePush(ctx.getTracker());

                ctx.getBuilder().append(tjvm.getCode(computedA, computedB, computedPush, EnumVMOperation.REMAINDER));
            }
            case INEG -> {
                String computedA = Internal.computePop(ctx.getTracker());
                String computedPush = Internal.computePush(ctx.getTracker());

                ctx.getBuilder().append(String.format("\t%s.i = -%s.i;\n", computedPush, computedA));
            }
            case IXOR -> {
                String computedA = Internal.computePop(ctx.getTracker());
                String computedB = Internal.computePop(ctx.getTracker());
                String computedPush = Internal.computePush(ctx.getTracker());

                ctx.getBuilder().append(tjvm.getCode(computedA, computedB, computedPush, EnumVMOperation.XOR));
            }
            case ISHR -> {
                String computedA = Internal.computePop(ctx.getTracker());
                String computedB = Internal.computePop(ctx.getTracker());
                String computedPush = Internal.computePush(ctx.getTracker());

                ctx.getBuilder().append(tjvm.getCode(computedA, computedB, computedPush, EnumVMOperation.SHIFT_RIGHT));
            }
            case IAND -> {
                String computedA = Internal.computePop(ctx.getTracker());
                String computedB = Internal.computePop(ctx.getTracker());
                String computedPush = Internal.computePush(ctx.getTracker());

                ctx.getBuilder().append(tjvm.getCode(computedA, computedB, computedPush, EnumVMOperation.AND));
            }
            case IOR -> {
                String computedA = Internal.computePop(ctx.getTracker());
                String computedB = Internal.computePop(ctx.getTracker());
                String computedPush = Internal.computePush(ctx.getTracker());

                ctx.getBuilder().append(tjvm.getCode(computedA, computedB, computedPush, EnumVMOperation.OR));
            }
            case IUSHR -> {
                String computedA = Internal.computePop(ctx.getTracker());
                String computedB = Internal.computePop(ctx.getTracker());
                String computedPush = Internal.computePush(ctx.getTracker());

                ctx.getBuilder().append(tjvm.getCode(computedA, computedB, computedPush, EnumVMOperation.USHIFT_RIGHT));
            }
            case FADD -> {
                String computedA = Internal.computePop(ctx.getTracker());
                String computedB = Internal.computePop(ctx.getTracker());
                String computedPush = Internal.computePush(ctx.getTracker());

                ctx.getBuilder().append(String.format("\t%s.f = %s.f + %s.f;\n", computedPush, computedB, computedA));
            }
            case FSUB -> {
                String computedA = Internal.computePop(ctx.getTracker());
                String computedB = Internal.computePop(ctx.getTracker());
                String computedPush = Internal.computePush(ctx.getTracker());

                ctx.getBuilder().append(String.format("\t%s.f = %s.f - %s.f;\n", computedPush, computedB, computedA));
            }
            case FDIV -> {
                String computedA = Internal.computePop(ctx.getTracker());
                String computedB = Internal.computePop(ctx.getTracker());
                String computedPush = Internal.computePush(ctx.getTracker());

                ctx.getBuilder().append(String.format("\t%s.f = %s.f / %s.f;\n", computedPush, computedB, computedA));
            }
            case FMUL -> {
                String computedA = Internal.computePop(ctx.getTracker());
                String computedB = Internal.computePop(ctx.getTracker());
                String computedPush = Internal.computePush(ctx.getTracker());

                ctx.getBuilder().append(String.format("\t%s.f = %s.f * %s.f;\n", computedPush, computedB, computedA));
            }
            case FNEG -> {
                String computedA = Internal.computePop(ctx.getTracker());
                String computedPush = Internal.computePush(ctx.getTracker());

                ctx.getBuilder().append(String.format("\t%s.f = -%s.f;\n", computedPush, computedA));

            }
            case DADD -> {
                String computedA = Internal.computePop(ctx.getTracker());
                String computedB = Internal.computePop(ctx.getTracker());
                String computedPush = Internal.computePush(ctx.getTracker());

                ctx.getBuilder().append(String.format("\t%s.d = %s.d + %s.d;\n", computedPush, computedB, computedA));
            }
            case DSUB -> {
                String computedA = Internal.computePop(ctx.getTracker());
                String computedB = Internal.computePop(ctx.getTracker());
                String computedPush = Internal.computePush(ctx.getTracker());

                ctx.getBuilder().append(String.format("\t%s.d = %s.d - %s.d;\n", computedPush, computedB, computedA));
            }
            case DMUL -> {
                String computedA = Internal.computePop(ctx.getTracker());
                String computedB = Internal.computePop(ctx.getTracker());
                String computedPush = Internal.computePush(ctx.getTracker());

                ctx.getBuilder().append(String.format("\t%s.d = %s.d * %s.d;\n", computedPush, computedB, computedA));
            }
            case DDIV -> {
                String computedA = Internal.computePop(ctx.getTracker());
                String computedB = Internal.computePop(ctx.getTracker());
                String computedPush = Internal.computePush(ctx.getTracker());

                ctx.getBuilder().append(String.format("\t%s.d = %s.d / %s.d;\n", computedPush, computedB, computedA));
            }
            case DNEG -> {
                String computedA = Internal.computePop(ctx.getTracker());
                String computedPush = Internal.computePush(ctx.getTracker());

                ctx.getBuilder().append(String.format("\t%s.d = -%s.d;\n", computedPush, computedA));
            }
            case LADD -> {
                String computedA = Internal.computePop(ctx.getTracker());
                String computedB = Internal.computePop(ctx.getTracker());
                String computedPush = Internal.computePush(ctx.getTracker());

                ctx.getBuilder().append(String.format("\t%s.j = ((julong)%s.j) + %s.j;\n", computedPush, computedB, computedA));
            }
            case LSUB -> {
                String computedA = Internal.computePop(ctx.getTracker());
                String computedB = Internal.computePop(ctx.getTracker());
                String computedPush = Internal.computePush(ctx.getTracker());

                ctx.getBuilder().append(String.format("\t%s.j = ((julong)%s.j) - %s.j;\n", computedPush, computedB, computedA));
            }
            case LDIV -> {
                String computedA = Internal.computePop(ctx.getTracker());
                String computedB = Internal.computePop(ctx.getTracker());
                String computedPush = Internal.computePush(ctx.getTracker());

                ctx.getBuilder().append(String.format("\t%s.j = ((julong)%s.j) / %s.j;\n", computedPush, computedB, computedA));
            }
            case LMUL -> {
                String computedA = Internal.computePop(ctx.getTracker());
                String computedB = Internal.computePop(ctx.getTracker());
                String computedPush = Internal.computePush(ctx.getTracker());

                ctx.getBuilder().append(String.format("\t%s.j = ((julong)%s.j) * %s.j;\n", computedPush, computedB, computedA));
            }
            case LNEG -> {
                String computedA = Internal.computePop(ctx.getTracker());
                String computedPush = Internal.computePush(ctx.getTracker());

                ctx.getBuilder().append(String.format("\t%s.j = -%s.j;\n", computedPush, computedA));
            }
            case LXOR -> {
                String computedA = Internal.computePop(ctx.getTracker());
                String computedB = Internal.computePop(ctx.getTracker());
                String computedPush = Internal.computePush(ctx.getTracker());

                ctx.fmtAppend("\t%s.j = ((julong)%s.j) ^ %s.j;\n", computedPush, computedB, computedA);
            }
            case LAND -> {
                String computedA = Internal.computePop(ctx.getTracker());
                String computedB = Internal.computePop(ctx.getTracker());
                String computedPush = Internal.computePush(ctx.getTracker());

                ctx.fmtAppend("\t%s.j = ((julong)%s.j) & %s.j;\n", computedPush, computedB, computedA);
            }
            case LOR -> {
                String computedA = Internal.computePop(ctx.getTracker());
                String computedB = Internal.computePop(ctx.getTracker());
                String computedPush = Internal.computePush(ctx.getTracker());

                ctx.fmtAppend("\t%s.j = %s.j | %s.j;\n", computedPush, computedB, computedA);
            }
            case LREM -> {
                String computedA = Internal.computePop(ctx.getTracker());
                String computedB = Internal.computePop(ctx.getTracker());
                String computedPush = Internal.computePush(ctx.getTracker());

                ctx.fmtAppend("\t%s.j = %s.j %% %s.j;\n", computedPush, computedB, computedA);
            }
            case LSHL -> {
                String computedA = Internal.computePop(ctx.getTracker());
                String computedB = Internal.computePop(ctx.getTracker());
                String computedPush = Internal.computePush(ctx.getTracker());
                ctx.fmtAppend("\t%s.j = (jlong)(((julong)%s.j) << (%s.i & 63));\n",
                        computedPush, computedB, computedA);
            }
            case LSHR -> {
                String computedA = Internal.computePop(ctx.getTracker());
                String computedB = Internal.computePop(ctx.getTracker());
                String computedPush = Internal.computePush(ctx.getTracker());

                ctx.fmtAppend("\t%s.j = (jlong)((((julong)%s.j) >> (%s.i & 63)) | ((%s.j < 0 && (%s.i & 63)) ? (~(julong)0 << (64 - (%s.i & 63))) : 0));\n",
                        computedPush, computedB, computedA, computedB, computedA, computedA);
            }
            case LUSHR -> {
                String computedA = Internal.computePop(ctx.getTracker());
                String computedB = Internal.computePop(ctx.getTracker());
                String computedPush = Internal.computePush(ctx.getTracker());

                ctx.fmtAppend("\t%s.j = (((julong)%s.j)) >> (%s.i & 63);\n", computedPush, computedB, computedA);
            }
            case DCMPL -> {
                String computedA = Internal.computePop(ctx.getTracker());
                String computedB = Internal.computePop(ctx.getTracker());
                String computedPush = Internal.computePush(ctx.getTracker());

                ctx.fmtAppend("/* DCMPL */\n");
                ctx.fmtAppend("""
                    \tif (isnan(%s.d) || isnan(%s.d)) {
                    \t\t%s.i = -1;
                    \t} else if (%s.d > %s.d) {
                    \t\t%s.i = 1;
                    \t} else if (%s.d < %s.d) {
                    \t\t%s.i = -1;
                    \t} else {
                    \t\t%s.i = 0;
                    \t}
                    """,
                        computedA, computedB, computedPush,
                        computedB, computedA, computedPush,
                        computedB, computedA, computedPush,
                        computedPush
                );
            }

            case DCMPG -> {
                String computedA = Internal.computePop(ctx.getTracker());
                String computedB = Internal.computePop(ctx.getTracker());
                String computedPush = Internal.computePush(ctx.getTracker());

                ctx.fmtAppend("/* DCMPG */\n");
                ctx.fmtAppend("""
                    \tif (isnan(%s.d) || isnan(%s.d)) {
                    \t\t%s.i = 1;
                    \t} else if (%s.d > %s.d) {
                    \t\t%s.i = 1;
                    \t} else if (%s.d < %s.d) {
                    \t\t%s.i = -1;
                    \t} else {
                    \t\t%s.i = 0;
                    \t}
                    """,
                        computedA, computedB, computedPush,
                        computedB, computedA, computedPush,
                        computedB, computedA, computedPush,
                        computedPush
                );
            }

            case LCMP -> {
                String computedA = Internal.computePop(ctx.getTracker());
                String computedB = Internal.computePop(ctx.getTracker());
                String computedPush = Internal.computePush(ctx.getTracker());

                ctx.fmtAppend("/* LCMP */\n");
                ctx.fmtAppend("""
                    \t%s.i = (%s.j > %s.j) ? 1
                    \t\t: (%s.j < %s.j) ? -1
                    \t\t: 0;
                    """,
                        computedPush, computedB, computedA,
                        computedB, computedA
                );
            }

            case FCMPL -> {
                String computedA = Internal.computePop(ctx.getTracker());
                String computedB = Internal.computePop(ctx.getTracker());
                String computedPush = Internal.computePush(ctx.getTracker());

                ctx.fmtAppend("/* FCMPL */\n");
                ctx.fmtAppend("""
                    \tif (isnan(%s.f) || isnan(%s.f)) {
                    \t\t%s.i = -1;
                    \t} else if (%s.f > %s.f) {
                    \t\t%s.i = 1;
                    \t} else if (%s.f < %s.f) {
                    \t\t%s.i = -1;
                    \t} else {
                    \t\t%s.i = 0;
                    \t}
                    """,
                        computedA, computedB, computedPush,
                        computedB, computedA, computedPush,
                        computedB, computedA, computedPush,
                        computedPush
                );
            }

            case FCMPG -> {
                String computedA = Internal.computePop(ctx.getTracker());
                String computedB = Internal.computePop(ctx.getTracker());
                String computedPush = Internal.computePush(ctx.getTracker());

                ctx.fmtAppend("/* FCMPG */\n");
                ctx.fmtAppend("""
                    \tif (isnan(%s.f) || isnan(%s.f)) {
                    \t\t%s.i = 1;
                    \t} else if (%s.f > %s.f) {
                    \t\t%s.i = 1;
                    \t} else if (%s.f < %s.f) {
                    \t\t%s.i = -1;
                    \t} else {
                    \t\t%s.i = 0;
                    \t}
                    """,
                        computedA, computedB, computedPush,
                        computedB, computedA, computedPush,
                        computedB, computedA, computedPush,
                        computedPush
                );
            }
            case ARRAYLENGTH -> {
                String popped = Internal.computePop(ctx.getTracker());
                String pushed = Internal.computePush(ctx.getTracker());

                final String npe = ic.FindClass("java/lang/NullPointerException");

                ctx.fmtAppend("\tif (%s.l == NULL) { (*env)->ThrowNew(env, %s, \"null array for arraylength\"); goto %s; }\n",
                        popped, npe, ctx.handlerLabel);

                ctx.fmtAppend("""
                        \t%s.i = (*env)->GetArrayLength(env, (jarray) %s.l);
                        """,
                        pushed, popped);
            }
            default -> throw new IllegalStateException("Unexpected value: " + insn.getOpcode());
        }
    }
}
