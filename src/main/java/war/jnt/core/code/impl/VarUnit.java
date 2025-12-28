package war.jnt.core.code.impl;

import org.objectweb.asm.Opcodes;
import org.objectweb.asm.Type;
import org.objectweb.asm.tree.MethodNode;
import org.objectweb.asm.tree.VarInsnNode;
import war.jnt.core.code.UnitContext;
import war.jnt.fusebox.impl.Internal;

import java.lang.reflect.Modifier;

// j2cc - cuz im too stupid for this
public class VarUnit implements Opcodes {

    public static void process(VarInsnNode insn, UnitContext ctx, MethodNode method) {
        boolean isParam = Internal.isParam(method, insn.var);

        Type[] argTypes = Type.getArgumentTypes(method.desc);
        int baseArgSizes = (Type.getArgumentsAndReturnSizes(method.desc) >> 2) - 1;

        Type[] mappedArgSlots = new Type[baseArgSizes];

        int idx = 0;
        for (Type argType : argTypes) {
            mappedArgSlots[idx] = argType;
            idx += argType.getSize();
        }

        switch (insn.getOpcode()) {
            case ILOAD, FLOAD, DLOAD, LLOAD, ALOAD -> {
                String computed = Internal.computePush(ctx.getTracker());
                char c = "ijfdl".charAt(insn.getOpcode() - ILOAD);

                if (isParam) {
                    if (insn.var == 0 && !Modifier.isStatic(method.access)) {
                        ctx.fmtAppend("\t%s.%s = _auto;\n", computed, c);
                    } else {
                        ctx.fmtAppend("\t%s.%s = (%s) p%d;\n", computed, c, Internal.mapTypeFromChar(c), Modifier.isStatic(method.access) ? insn.var : (insn.var - 1));
                    }
                } else {
                    ctx.fmtAppend("\t%s.%s = locals[%d].%s;\n", computed, c, insn.var, c);
                }
            }
            case ISTORE, FSTORE, DSTORE, LSTORE, ASTORE -> {
                String computed = Internal.computePop(ctx.getTracker());
                char c = "ijfdl".charAt(insn.getOpcode() - ISTORE);

                if (isParam) {
//                    int idx1 = insn.var - (Modifier.isStatic(method.access) ? 0 : 1);

//                    Type argumentType = mappedArgSlots[idx1];
//                    String s = jTypeMap.getOrDefault(argumentType, argumentType.getSort() == Type.ARRAY ? "jobjectArray" : "jobject");

                    ctx.fmtAppend("\tp%d = %s.%s;\n", Modifier.isStatic(method.access) ? insn.var : (insn.var - 1), computed, c);
                } else {
                    ctx.fmtAppend("\tlocals[%d].%s = %s.%s;\n", insn.var, c, computed, c);
                }
            }
        }
    }
}
