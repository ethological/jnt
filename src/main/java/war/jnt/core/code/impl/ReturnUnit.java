package war.jnt.core.code.impl;

import org.objectweb.asm.Opcodes;
import org.objectweb.asm.tree.InsnNode;
import war.jnt.core.code.UnitContext;
import war.jnt.fusebox.impl.Internal;

public class ReturnUnit implements Opcodes {
    public static void process(InsnNode insn, UnitContext ctx) {
        switch (insn.getOpcode()) {
            case IRETURN -> {
                String computed = Internal.computePop(ctx.getTracker());
                ctx.getBuilder().append(String.format("\treturn %s.i;\n", computed));
            }
            case DRETURN -> {
                String computed = Internal.computePop(ctx.getTracker());
                ctx.getBuilder().append(String.format("\treturn %s.d;\n", computed));
            }
            case FRETURN -> {
                String computed = Internal.computePop(ctx.getTracker());
                ctx.getBuilder().append(String.format("\treturn %s.f;\n", computed));
            }
            case LRETURN -> {
                String computed = Internal.computePop(ctx.getTracker());
                ctx.getBuilder().append(String.format("\treturn %s.j;\n", computed));
            }
            case ARETURN -> {
                String computed = Internal.computePop(ctx.getTracker());
                ctx.getBuilder().append(String.format("\treturn %s.l;\n", computed));
            }
            case RETURN -> {
                ctx.getBuilder().append("\treturn;\n");
            }
        }
    }
}
