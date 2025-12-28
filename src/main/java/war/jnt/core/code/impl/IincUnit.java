package war.jnt.core.code.impl;

import org.objectweb.asm.Type;
import org.objectweb.asm.tree.IincInsnNode;
import org.objectweb.asm.tree.MethodNode;
import war.jnt.core.code.UnitContext;
import war.jnt.fusebox.impl.Internal;

import java.lang.reflect.Modifier;

public class IincUnit {
    public static void process(IincInsnNode iinc, UnitContext ctx, MethodNode method) {
        if (Internal.isParam(method, iinc.var)) {
            ctx.fmtAppend("\tp%d += %d;\n", Modifier.isStatic(method.access) ? iinc.var : (iinc.var - 1), iinc.incr);
        } else {
            ctx.fmtAppend("\tlocals[%d].i += %d;\n", iinc.var, iinc.incr);
        }
    }
}
