package war.jnt.core.code.impl;

import org.objectweb.asm.Opcodes;
import org.objectweb.asm.tree.InsnNode;
import war.jnt.core.code.UnitContext;
import war.jnt.fusebox.impl.Internal;

public class CastUnit implements Opcodes {
    public static void process(InsnNode insn, UnitContext ctx) {
        String pop = Internal.computePop(ctx.getTracker());
        String push = Internal.computePush(ctx.getTracker());

        switch (insn.getOpcode()) {
            case L2I -> ctx.fmtAppend("\t%s.i = (jint) %s.j;\n", push, pop);
            case L2D -> ctx.fmtAppend("\t%s.d = (jdouble) %s.j;\n", push, pop);
            case L2F -> ctx.fmtAppend("\t%s.f = (jfloat) %s.j;\n", push, pop);
            case F2L -> ctx.fmtAppend("\t%s.j = (jlong) %s.f;\n", push, pop);
            case D2L -> ctx.fmtAppend("\t%s.j = (jlong) %s.d;\n", push, pop);
            case D2F -> ctx.fmtAppend("\t%s.f = (jfloat) %s.d;\n", push, pop);
            case F2D -> ctx.fmtAppend("\t%s.d = (jdouble) %s.f;\n", push, pop);
            case F2I -> ctx.fmtAppend("\t%s.i = (jint) %s.f;\n", push, pop);
            case D2I -> ctx.fmtAppend("\t%s.i = (jint) %s.d;\n", push, pop);
            case I2C -> ctx.fmtAppend("\t%s.i = (jchar) %s.i;\n", push, pop);
            case I2B -> ctx.fmtAppend("\t%s.i = (jbyte) %s.i;\n", push, pop);
            case I2S -> ctx.fmtAppend("\t%s.i = (jshort) %s.i;\n", push, pop);
            case I2L -> ctx.fmtAppend("\t%s.j = (jlong) %s.i;\n", push, pop);
            case I2F -> ctx.fmtAppend("\t%s.f = (jfloat) %s.i;\n", push, pop);
            case I2D -> ctx.fmtAppend("\t%s.d = (jdouble) %s.i;\n", push, pop);
        }
    }
}
