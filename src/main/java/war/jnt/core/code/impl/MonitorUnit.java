package war.jnt.core.code.impl;

import org.objectweb.asm.Opcodes;
import org.objectweb.asm.tree.AbstractInsnNode;
import org.objectweb.asm.tree.InsnNode;
import org.objectweb.asm.tree.LabelNode;
import org.objectweb.asm.tree.analysis.BasicValue;
import org.objectweb.asm.tree.analysis.Frame;
import war.jnt.core.code.UnitContext;
import war.jnt.fusebox.impl.Internal;

import java.util.Map;

public class MonitorUnit {

    public static void process(InsnNode insn, UnitContext ctx) {
        if (insn.getOpcode() == Opcodes.MONITORENTER) {
            ctx.fmtAppend("\t(*env)->MonitorEnter(env, %s.l);\n", Internal.computePop(ctx.getTracker()));
        } else {
            ctx.fmtAppend("\t(*env)->MonitorExit(env, %s.l);\n", Internal.computePop(ctx.getTracker()));
        }
    }
}
