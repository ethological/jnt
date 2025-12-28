package war.jnt.core.code.impl;

import org.objectweb.asm.Opcodes;
import org.objectweb.asm.tree.InsnNode;
import war.jnt.cache.Cache;
import war.jnt.core.code.UnitContext;
import war.jnt.core.vm.TempJumpVM;
import war.jnt.fusebox.impl.Internal;
import war.jnt.innercache.InnerCache;
import war.metaphor.util.interfaces.IRandom;

public class ThrowUnit implements Opcodes, IRandom {

    public static void process(final InnerCache ic, InsnNode insn, UnitContext ctx, TempJumpVM tjvm) {
        String npe = ic.FindClass("java/lang/NullPointerException");

        ctx.fmtAppend("\t{\n");
        ctx.fmtAppend("\t\tjthrowable thrown = (jthrowable) %s.l;\n", Internal.computePop(ctx.getTracker()));
        ctx.fmtAppend("\t\tif (thrown == NULL) {\n");
        tjvm.makeValue(14);
        ctx.fmtAppend("\t\t\t((jint (*)(JNIEnv *, jclass, const char *)) (*((void **)*env + *(volatile int *)&output)))(env, " + npe + ", \"NullPointerException\");\n");
        ctx.fmtAppend("\t\t} else {\n");
        tjvm.makeValue(13);
        ctx.fmtAppend("\t\t\t((jint (*)(JNIEnv *, jthrowable)) (*((void **)*env + *(volatile int *)&output)))(env, thrown);\n");
        ctx.fmtAppend("\t\t}\n");
        ctx.fmtAppend("\t}\n");
    }
}
