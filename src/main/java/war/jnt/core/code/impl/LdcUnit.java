package war.jnt.core.code.impl;

import org.apache.commons.lang3.RandomUtils;
import org.objectweb.asm.Handle;
import org.objectweb.asm.Type;
import org.objectweb.asm.tree.LdcInsnNode;
import war.jnt.cache.Cache;
import war.jnt.core.code.UnitContext;
import war.jnt.core.vm.TempJumpVM;
import war.jnt.fusebox.impl.Internal;
import war.jnt.fusebox.impl.VariableManager;
import war.jnt.obfuscation.MutatedString;

public class LdcUnit {
    public static void process(LdcInsnNode insn, UnitContext ctx, VariableManager varMan, TempJumpVM tjvm) {
        String computedPush = Internal.computePush(ctx.getTracker());

        switch (insn.cst) {
            case String str -> {
                String varName = varMan.newBuffer();
                MutatedString ms = new MutatedString(str);
                ctx.append(ms.compute(varName, true));
                int xorKey = RandomUtils.nextInt();
                tjvm.makeValue(163 ^ xorKey);
                ctx.fmtAppend("\t%s.l = ((jstring (*)(JNIEnv *, jchar *, jsize)) (*((void **)*env + (*(volatile int *)&output ^ %s))))(env, %s, %d);\n", computedPush, xorKey, varName, str.length());
            }
            case Integer value -> {
                ctx.fmtAppend("\t%s.i = %s;\n", computedPush, Integer.toString(value));
//                var mutator = new MutatedNumeric(computedPush, insn.cst, Type.INT_TYPE);
//                ctx.append(mutator.get());
            }
            case Float value -> {
                ctx.fmtAppend("\t%s.f = %s;\n", computedPush, Float.isNaN(value) ? "0.0f / 0.0f" : Float.isInfinite(value)
                        ? (value > 0 ? "1.0f / 0.0f" : "-1.0f / 0.0f")
                        : Float.toString(value));
            }
            case Double value -> {
                ctx.fmtAppend("\t%s.d = %s;\n", computedPush, Double.isNaN(value) ? "0.0 / 0.0" : Double.isInfinite(value)
                        ? (value > 0 ? "1.0 / 0.0" : "-1.0 / 0.0")
                        : Double.toString(value));
            }
            case Long value -> {
                ctx.fmtAppend("\t%s.j = %s;\n", computedPush, Long.toString(value));
            }
            case Type type -> {
                if (type.getSort() == Type.ARRAY || type.getSort() == Type.OBJECT) { // just to be safe
                    int idx = Cache.Companion.request_klass(type.getInternalName());
                    ctx.fmtAppend("\t%s.l = request_klass(env, %d);\n", computedPush, idx);
                } else if (type.getSort() == Type.METHOD) {
                    //TODO: Address this, maybe with NEW NEW invokedynamic UNIT
                    throw new UnsupportedOperationException("Method Type LDCs are not supported");
                } else {
                    int idx = Cache.Companion.request_klass(type.getClassName());
                    ctx.fmtAppend("\t%s.l = request_klass(env, %d);\n", computedPush, idx);
                }
            }
            case Handle handle -> {
                ctx.fmtAppend("\t%s.l = %s;\n", computedPush, IndyUnit.makeHandleConstant(ctx.getClassNode().name, handle));
//                throw new UnsupportedOperationException("Handle LDCs are not supported");
            }
            default -> throw new IllegalStateException("Unexpected value: " + insn.cst);
        }
    }
}
