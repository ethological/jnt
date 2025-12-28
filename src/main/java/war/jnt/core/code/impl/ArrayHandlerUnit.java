package war.jnt.core.code.impl;

import org.objectweb.asm.Opcodes;
import org.objectweb.asm.Type;
import org.objectweb.asm.tree.InsnNode;
import war.jnt.cache.Cache;
import war.jnt.core.code.UnitContext;
import war.jnt.core.vm.TempJumpVM;
import war.jnt.fusebox.impl.Internal;
import war.jnt.innercache.InnerCache;

public class ArrayHandlerUnit implements Opcodes {
    public static void process(final InnerCache ic, InsnNode insn, UnitContext ctx, TempJumpVM tjvm) {
        String npe = ic.FindClass("java/lang/NullPointerException");
        String aob = ic.FindClass("java/lang/ArrayIndexOutOfBoundsException");

        switch (insn.getOpcode()) {
            case AALOAD -> {
                String idx = Internal.computePop(ctx.getTracker());
                String arr = Internal.computePop(ctx.getTracker());

                String out = Internal.computePush(ctx.getTracker());

                ctx.fmtAppend("\tif (%s.l == NULL) { (*env)->ThrowNew(env, " + npe +  ", \"array is null\"); goto %s; }\n", arr, ctx.handlerLabel);
                ctx.fmtAppend("\tif (%s.i < 0 || %s.i >= (*env)->GetArrayLength(env, %s.l)) { (*env)->ThrowNew(env, " + aob + ", \"array index out of bounds\"); goto %s; }\n", idx, idx, arr, ctx.handlerLabel);

                tjvm.makeValue(173);
                ctx.fmtAppend("""
                        \t%s.l = ((jobject (*)(JNIEnv *, jobjectArray, jsize)) (*((void **)*env + *(volatile int *)&output)))(env, %s.l, %s.i);
                        """,
                        out, arr, idx);
            }
            case AASTORE -> {
                // void SetObjectArrayElement(JNIEnv *env, jobjectArray array, jsize index, jobject value);

                String val = Internal.computePop(ctx.getTracker());
                String idx = Internal.computePop(ctx.getTracker());
                String arr = Internal.computePop(ctx.getTracker());

                ctx.fmtAppend("\tif (%s.l == NULL) { (*env)->ThrowNew(env, %s, \"array is null\"); goto %s; }\n", arr, npe, ctx.handlerLabel);
                ctx.fmtAppend("\tif (%s.i < 0 || %s.i >= (*env)->GetArrayLength(env, %s.l)) { (*env)->ThrowNew(env, %s, \"array index out of bounds\"); goto %s; }\n", idx, idx, arr, aob, ctx.handlerLabel);

                tjvm.makeValue(174);
                ctx.fmtAppend("""
                        \t((jobject (*)(JNIEnv *, jobjectArray, jsize, jobject)) (*((void **)*env + *(volatile int *)&output)))(env, %s.l, %s.i, %s.l);
                        """,
                        arr, idx, val);
            }
            case IALOAD, BALOAD, CALOAD, SALOAD,
                    LALOAD, FALOAD, DALOAD -> {

                Type type = Internal.fromOpcode(insn.getOpcode());
                String field = Internal.resolveType(type);
                CallInfo getInfo = switch (type.getSort()) {
                    case Type.BOOLEAN -> new CallInfo("jboolean", 183);
                    case Type.BYTE -> new CallInfo("jbyte", 184);
                    case Type.CHAR -> new CallInfo("jchar", 185);
                    case Type.SHORT -> new CallInfo("jshort", 186);
                    case Type.INT -> new CallInfo("jint", 187);
                    case Type.LONG -> new CallInfo("jlong", 188);
                    case Type.FLOAT -> new CallInfo("jfloat", 189);
                    case Type.DOUBLE -> new CallInfo("jdouble", 190);
                    default -> throw new IllegalStateException("Unexpected value: " + type.getSort());
                };
                CallInfo releaseInfo = switch (type.getSort()) {
                    case Type.BOOLEAN -> new CallInfo("jboolean", 191);
                    case Type.BYTE -> new CallInfo("jbyte", 192);
                    case Type.CHAR -> new CallInfo("jchar", 193);
                    case Type.SHORT -> new CallInfo("jshort", 194);
                    case Type.INT -> new CallInfo("jint", 195);
                    case Type.LONG -> new CallInfo("jlong", 196);
                    case Type.FLOAT -> new CallInfo("jfloat", 197);
                    case Type.DOUBLE -> new CallInfo("jdouble", 198);
                    default -> throw new IllegalStateException("Unexpected value: " + type.getSort());
                };

                String idx = Internal.computePop(ctx.getTracker());
                String arr = Internal.computePop(ctx.getTracker());

                String out = Internal.computePush(ctx.getTracker());

                ctx.fmtAppend("\tif (%s.l == NULL) { (*env)->ThrowNew(env, %s, \"array is null\"); goto %s; }\n", arr, npe, ctx.handlerLabel);
                ctx.fmtAppend("\tif (%s.i < 0 || %s.i >= (*env)->GetArrayLength(env, %s.l)) { (*env)->ThrowNew(env, %s, \"array index out of bounds\"); goto %s; }\n", idx, idx, arr, aob, ctx.handlerLabel);

                ctx.fmtAppend("\t{\n");

                ctx.fmtAppend("\t\tjobject __temp = %s.l;\n", arr);
                tjvm.makeValue(getInfo.idx);
                ctx.fmtAppend("\t\tvoid* __array = ((%s * (*)(JNIEnv *, %sArray, jboolean *)) (*((void **)*env + *(volatile int *)&output)))(env, %s.l, NULL);\n", getInfo.type, getInfo.type, arr);
                ctx.fmtAppend("\t\t%s%s = ((j%s*)__array)[%s.i];\n", out, field, type.getClassName().toLowerCase(), idx);
                tjvm.makeValue(releaseInfo.idx);
                ctx.fmtAppend("\t\t((void (*)(JNIEnv *, %sArray, %s *, jint)) (*((void **)*env + *(volatile int *)&output)))(env, __temp, __array, 0);\n", releaseInfo.type, releaseInfo.type);

                ctx.fmtAppend("\t}\n");
            }
            case IASTORE, BASTORE, CASTORE, SASTORE,
                    LASTORE, FASTORE, DASTORE -> {

                Type type = Internal.fromOpcode(insn.getOpcode());
                String field = Internal.resolveType(type);
                CallInfo cinfo = switch (type.getSort()) {
                    case Type.BOOLEAN -> new CallInfo("jboolean", 207);
                    case Type.BYTE -> new CallInfo("jbyte", 208);
                    case Type.CHAR -> new CallInfo("jchar", 209);
                    case Type.SHORT -> new CallInfo("jshort", 210);
                    case Type.INT -> new CallInfo("jint", 211);
                    case Type.LONG -> new CallInfo("jlong", 212);
                    case Type.FLOAT -> new CallInfo("jfloat", 213);
                    case Type.DOUBLE -> new CallInfo("jdouble", 214);
                    default -> throw new IllegalStateException("Unexpected value: " + type.getSort());
                };

                // void Set<PrimitiveType>ArrayRegion(JNIEnv *env, ArrayType array, jsize start, jsize len, const NativeType *buf);

                String val = Internal.computePop(ctx.getTracker());
                String idx = Internal.computePop(ctx.getTracker());
                String arr = Internal.computePop(ctx.getTracker());

                ctx.fmtAppend("\tif (%s.l == NULL) { (*env)->ThrowNew(env, %s, \"array is null\"); goto %s; }\n", arr, npe, ctx.handlerLabel);
                ctx.fmtAppend("\tif (%s.i < 0 || %s.i >= (*env)->GetArrayLength(env, %s.l)) { (*env)->ThrowNew(env, %s, \"array index out of bounds\"); goto %s; }\n", idx, idx, arr, aob, ctx.handlerLabel);

                tjvm.makeValue(cinfo.idx);

                ctx.fmtAppend("""
                        \t((jint (*)(JNIEnv *, %sArray, jsize, jsize, %s *)) (*((void **)*env + *(volatile int *)&output)))(env, %s.l, %s.i, 1, &%s%s);
                        """,
                        cinfo.type, cinfo.type, arr, idx, val, field);
            }
        }
    }

    public record CallInfo(String type, int idx) {}
}
