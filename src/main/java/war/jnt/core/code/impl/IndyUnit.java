package war.jnt.core.code.impl;

import lombok.SneakyThrows;
import org.objectweb.asm.Handle;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.Type;
import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.tree.InvokeDynamicInsnNode;
import war.jnt.cache.Cache;
import war.jnt.cache.struct.CachedIndyArgs;
import war.jnt.core.Processor;
import war.jnt.core.code.UnitContext;
import war.jnt.core.header.Header;
import war.jnt.core.source.Source;
import war.jnt.fusebox.impl.Internal;
import war.jnt.fusebox.impl.VariableManager;
import war.jnt.obfuscation.MutatedString;
import war.jnt.obfuscation.StringLookup;

import java.nio.file.Files;
import java.nio.file.Path;

public class IndyUnit implements Opcodes {

    private static final StringBuilder cacheInitialisationCode = new StringBuilder();

    public static void process(InvokeDynamicInsnNode node, VariableManager varMan, UnitContext ctx) {
        int oldSize = Cache.Companion.cachedIndyArgs();
        int argumentsIndex = Cache.Companion.request_indy_args(new CachedIndyArgs(node.bsmArgs));

        if (argumentsIndex == oldSize)
            cacheArgs(oldSize, node.bsmArgs, ctx);

        ClassNode classNode = ctx.getClassNode();

        int idx = Cache.Companion.request_klass(classNode.name);

        String var = varMan.newBuffer();
        String bmVar = varMan.newBuffer();
        String typeVar = varMan.newBuffer();
        String callerClassVar = varMan.newBuffer();
        String nameVar = varMan.newBuffer();

        MutatedString name = new MutatedString(node.name);

//        ctx.fmtAppend("\tprintf(\"Creating invoke dynamic site for %%s.%%s\\n\\n\", \"%s\", \"%s\");\n",
//                node.bsm.owner, node.bsm.name + node.bsm.descriptor);

        int caller_idx = Cache.Companion.request_klass(classNode.name);

        ctx.append(name.compute(nameVar, true));

        ctx.fmtAppend("\tjobject %s = " + makeHandleConstant(classNode.name, node.bsm) + ";\n", bmVar);

//        ctx.fmtAppend("\tprintf(\"Bootstrap method: %%p\\n\\n\", %s);\n", bmVar);

        ctx.fmtAppend("\tjobject %s = make_method_type(env, \"%s\", request_klass(env, %d));\n", typeVar, node.desc, caller_idx);

//        ctx.fmtAppend("\tprintf(\"Method type: %%p\\n\\n\", %s);\n", typeVar);

        ctx.fmtAppend("\tjclass %s = request_klass(env, %d);\n",
                callerClassVar, idx);

//        ctx.fmtAppend("\tprintf(\"Caller class: %%p\\n\\n\", %s);\n", callerClassVar);

        ctx.fmtAppend("\tjobject %s = make_site(env, %s, %s, %d, %s, %d, %s);\n",
                var, bmVar, nameVar, node.name.length(), typeVar, argumentsIndex, callerClassVar);

        ctx.fmtAppend("\t(*env)->DeleteLocalRef(env, %s);\n", bmVar);
        ctx.fmtAppend("\t(*env)->DeleteLocalRef(env, %s);\n", typeVar);

//        ctx.fmtAppend("\tprintf(\"Created invoke dynamic site: %%p\\n\", %s);\n", var);

        Type[] args = Type.getArgumentTypes(node.desc);
        Type returnType = Type.getReturnType(node.desc);
//        int argc = args.length;

        int Object_idx = Cache.Companion.request_klass("java/lang/Object");

        ctx.fmtAppend("\t{\n");
        ctx.fmtAppend("\t\tjclass kls_obj = request_klass(env, %d);\n", Object_idx);
        ctx.fmtAppend("\t\tjobjectArray mh_args = (*env)->NewObjectArray(env, %d, kls_obj, NULL);\n",
                args.length);

//        ctx.fmtAppend("\t\tprintf(\"Allocating %d slots for invoke dynamic arguments\\n\");\n", args.length);

        for (int i = args.length - 1; i >= 0; i--) {

            String slot = Internal.computePop(ctx.tracker);
            Type t = args[i];

            switch (t.getSort()) {
                case Type.INT -> ctx.fmtAppend("\t{ jobject tmp = make_object_int(env, %s.i); "
                        + "(*env)->SetObjectArrayElement(env, mh_args, %d, tmp); "
                        + "(*env)->DeleteLocalRef(env, tmp); }\n", slot, i);
                case Type.LONG -> ctx.fmtAppend("\t{ jobject tmp = make_object_long(env, %s.j); "
                        + "(*env)->SetObjectArrayElement(env, mh_args, %d, tmp); "
                        + "(*env)->DeleteLocalRef(env, tmp); }\n", slot, i);
                case Type.FLOAT -> ctx.fmtAppend("\t{ jobject tmp = make_object_float(env, %s.f); "
                        + "(*env)->SetObjectArrayElement(env, mh_args, %d, tmp); "
                        + "(*env)->DeleteLocalRef(env, tmp); }\n", slot, i);
                case Type.DOUBLE -> ctx.fmtAppend("\t{ jobject tmp = make_object_double(env, %s.d); "
                        + "(*env)->SetObjectArrayElement(env, mh_args, %d, tmp); "
                        + "(*env)->DeleteLocalRef(env, tmp); }\n", slot, i);
                case Type.BOOLEAN -> ctx.fmtAppend("\t{ jobject tmp = make_object_boolean(env, %s.z); "
                        + "(*env)->SetObjectArrayElement(env, mh_args, %d, tmp); "
                        + "(*env)->DeleteLocalRef(env, tmp); }\n", slot, i);
                case Type.CHAR -> ctx.fmtAppend("\t{ jobject tmp = make_object_char(env, %s.c); "
                        + "(*env)->SetObjectArrayElement(env, mh_args, %d, tmp); "
                        + "(*env)->DeleteLocalRef(env, tmp); }\n", slot, i);
                case Type.SHORT -> ctx.fmtAppend("\t{ jobject tmp = make_object_short(env, %s.s); "
                        + "(*env)->SetObjectArrayElement(env, mh_args, %d, tmp); "
                        + "(*env)->DeleteLocalRef(env, tmp); }\n", slot, i);
                case Type.BYTE -> ctx.fmtAppend("\t{ jobject tmp = make_object_byte(env, %s.b); "
                        + "(*env)->SetObjectArrayElement(env, mh_args, %d, tmp); "
                        + "(*env)->DeleteLocalRef(env, tmp); }\n", slot, i);
                default -> ctx.fmtAppend("\t(*env)->SetObjectArrayElement(env, mh_args, %d, %s.l);\n", i, slot);
            }
        }

//        ctx.fmtAppend("\t\t(*env)->DeleteLocalRef(env, kls_obj);\n");

//        ctx.fmtAppend("\t\tprintf(\"Invoke dynamic arguments cached: %%p\\n\", mh_args);\n");

        ctx.fmtAppend("\t\tjobject mh_result = invoke_with_arguments(env, %s, mh_args);\n", var);

//        ctx.fmtAppend("\t\tprintf(\"Invoke dynamic result: %%p\\n\", mh_result);\n");

        switch (returnType.getSort()) {
            case Type.VOID -> {}
            case Type.BOOLEAN -> ctx.fmtAppend(
                    "\t\t%s%s = unbox_boolean(env, mh_result);\n",
                    Internal.computePush(ctx.tracker), Internal.resolveType(returnType)
            );
            case Type.BYTE -> ctx.fmtAppend(
                    "\t\t%s%s = unbox_byte(env, mh_result);\n",
                    Internal.computePush(ctx.tracker), Internal.resolveType(returnType)
            );
            case Type.CHAR -> ctx.fmtAppend(
                    "\t\t%s%s = unbox_char(env, mh_result);\n",
                    Internal.computePush(ctx.tracker), Internal.resolveType(returnType)
            );
            case Type.SHORT -> ctx.fmtAppend(
                    "\t\t%s%s = unbox_short(env, mh_result);\n",
                    Internal.computePush(ctx.tracker), Internal.resolveType(returnType)
            );
            case Type.INT -> ctx.fmtAppend(
                    "\t\t%s%s = unbox_int(env, mh_result);\n",
                    Internal.computePush(ctx.tracker), Internal.resolveType(returnType)
            );
            case Type.LONG -> ctx.fmtAppend(
                    "\t\t%s%s = unbox_long(env, mh_result);\n",
                    Internal.computePush(ctx.tracker), Internal.resolveType(returnType)
            );
            case Type.FLOAT -> ctx.fmtAppend(
                    "\t\t%s%s = unbox_float(env, mh_result);\n",
                    Internal.computePush(ctx.tracker), Internal.resolveType(returnType)
            );
            case Type.DOUBLE -> ctx.fmtAppend(
                    "\t\t%s%s = unbox_double(env, mh_result);\n",
                    Internal.computePush(ctx.tracker), Internal.resolveType(returnType)
            );
            case Type.OBJECT, Type.ARRAY -> ctx.fmtAppend(
                    "\t\t%s%s = mh_result;\n",
                    Internal.computePush(ctx.tracker), Internal.resolveType(returnType)
            );
        }
        ctx.fmtAppend("\t\t(*env)->DeleteLocalRef(env, mh_args);\n");
        ctx.fmtAppend("\t}\n");

    }

    private static void cacheArgs(int index, Object[] bsmArgs, UnitContext ctx) {
        StringBuilder init = new StringBuilder();

//        init.append(String.format("\tprintf(\"Caching invoke dynamic arguments %d\\n\");\n", index));

        init.append("\tjobject args").append(index)
                .append("[").append(bsmArgs.length).append("];\n");

        for (int i = 0; i < bsmArgs.length; i++) {
            String slot = "args" + index + "[" + i + "]";
            Object value = bsmArgs[i];

//            init.append(String.format("\tprintf(\"Slot %d: %s (%s)\\n\");\n", i, value instanceof String ? "..." : value, value == null ? "null" : value.getClass().getSimpleName()));

            switch (value) {
                case Integer v -> init.append("\t{ jobject tmp = make_object_int(env, ").append(v).append(");\n")
                        .append("\t").append(slot).append(" = (*env)->NewGlobalRef(env, tmp);\n")
                        .append("\t(*env)->DeleteLocalRef(env, tmp); }\n");

                case Byte v -> init.append("\t{ jobject tmp = make_object_byte(env, ").append(v.intValue()).append(");\n")
                        .append("\t").append(slot).append(" = (*env)->NewGlobalRef(env, tmp);\n")
                        .append("\t(*env)->DeleteLocalRef(env, tmp); }\n");

                case Character v -> init.append("\t{ jobject tmp = make_object_char(env, ").append((int) v).append(");\n")
                        .append("\t").append(slot).append(" = (*env)->NewGlobalRef(env, tmp);\n")
                        .append("\t(*env)->DeleteLocalRef(env, tmp); }\n");

                case Short v -> init.append("\t{ jobject tmp = make_object_short(env, ").append(v.intValue()).append(");\n")
                        .append("\t").append(slot).append(" = (*env)->NewGlobalRef(env, tmp);\n")
                        .append("\t(*env)->DeleteLocalRef(env, tmp); }\n");

                case Boolean v -> init.append("\t{ jobject tmp = make_object_boolean(env, ").append(v ? 1 : 0).append(");\n")
                        .append("\t").append(slot).append(" = (*env)->NewGlobalRef(env, tmp);\n")
                        .append("\t(*env)->DeleteLocalRef(env, tmp); }\n");

                case Float v -> init.append("\t{ jobject tmp = make_object_float(env, ").append(v).append("f);\n")
                        .append("\t").append(slot).append(" = (*env)->NewGlobalRef(env, tmp);\n")
                        .append("\t(*env)->DeleteLocalRef(env, tmp); }\n");

                case Long v -> init.append("\t{ jobject tmp = make_object_long(env, ").append(v).append("L);\n")
                        .append("\t").append(slot).append(" = (*env)->NewGlobalRef(env, tmp);\n")
                        .append("\t(*env)->DeleteLocalRef(env, tmp); }\n");

                case Double v -> init.append("\t{ jobject tmp = make_object_double(env, ").append(v).append(");\n")
                        .append("\t").append(slot).append(" = (*env)->NewGlobalRef(env, tmp);\n")
                        .append("\t(*env)->DeleteLocalRef(env, tmp); }\n");

                case String s -> init.append("\t{ jstring tmp = (*env)->NewString(env, ")
                        .append(StringLookup.toUTF16(s)).append(", ").append(s.length()).append(");\n")
                        .append("\t").append(slot).append(" = (*env)->NewGlobalRef(env, tmp);\n")
                        .append("\t(*env)->DeleteLocalRef(env, tmp); }\n");

                case Type type -> {
                    int sort = type.getSort();
                    if (sort == Type.OBJECT) {
                        int k = Cache.Companion.request_klass(type.getInternalName());
                        init.append("\t").append(slot).append(" = request_klass(env, ").append(k).append(");\n");
                    } else if (sort == Type.METHOD) {
                        init.append("\t{ jobject tmp = make_method_type(env, \"").append(type.getDescriptor()).append("\", request_klass(env, " +
                                Cache.Companion.request_klass(ctx.getClassNode().name) + "));\n")
                                .append("\t").append(slot).append(" = (*env)->NewGlobalRef(env, tmp);\n")
                                .append("\t(*env)->DeleteLocalRef(env, tmp); }\n");
                    } else if (sort == Type.ARRAY) {
                        int k = Cache.Companion.request_klass(type.getDescriptor());
                        init.append("\t").append(slot).append(" = request_klass(env, ").append(k).append(");\n");
                    } else {
                        String prim = type.getClassName();
                        int k = Cache.Companion.request_klass(prim);
                        init.append("\t").append(slot).append(" = request_klass(env, ").append(k).append(");\n");
                    }
                }

                case Handle h -> init.append("\t{ jobject tmp = ").append(makeHandleConstant(ctx.getClassNode().name, h)).append(";\n")
                        .append("\t").append(slot).append(" = (*env)->NewGlobalRef(env, tmp);\n")
                        .append("\t(*env)->DeleteLocalRef(env, tmp);\n\t}\n");

                case null, default -> throw new IllegalArgumentException("Unsupported value: " + value);
            }

//            init.append(String.format("\tprintf(\"Slot %d cached: %s: %%p\\n\", %s);\n", i, slot, slot));
        }

        int Object_idx = Cache.Companion.request_klass("java/lang/Object");
        init.append("\t{\n")
                .append("\t\tjclass kls_obj = request_klass(env, " + Object_idx + ");\n")
//                .append("\t\tjclass kls_obj = (*env)->FindClass(env, \"java/lang/Object\");\n")
                .append("\t\tjobjectArray arr").append(index)
                .append(" = (*env)->NewObjectArray(env, ").append(bsmArgs.length)
                .append(", kls_obj, NULL);\n")
                .append("\t\tif (arr").append(index).append(") {\n");

        for (int i = 0; i < bsmArgs.length; i++) {
            init.append("\t\t\t(*env)->SetObjectArrayElement(env, arr").append(index)
                    .append(", ").append(i).append(", args").append(index).append("[").append(i).append("]);\n");
        }

        init.append("\t\t\tstatic_arguments[").append(index).append("] = (jobjectArray)")
                .append("(*env)->NewGlobalRef(env, arr").append(index).append(");\n")
                .append("\t\t\t(*env)->DeleteLocalRef(env, arr").append(index).append(");\n")
                .append("\t\t}\n")
                .append("\t}\n");

        cacheInitialisationCode.append(init);
    }

    public static String makeHandleConstant(String callerClass, Handle h) {

        String caller = String.format("request_klass(env, %d)", Cache.Companion.request_klass(callerClass));

        StringBuilder result = new StringBuilder();
        switch (h.getTag()) {
            case H_GETFIELD, H_GETSTATIC, H_PUTFIELD, H_PUTSTATIC -> {
                String request;

                Type type = Type.getType(h.getDesc());
                if (type.getSort() == Type.METHOD) {
                    type = type.getReturnType();
                }

                if (type.getSort() == Type.OBJECT) {
                    int idx = Cache.Companion.request_klass(type.getInternalName());
                    request = "request_klass(env, " + idx + ")";
                } else if (type.getSort() == Type.ARRAY) {
                    int idx = Cache.Companion.request_klass(type.getDescriptor());
                    request = "request_klass(env, " + idx + ")";
                } else {
                    int idx = Cache.Companion.request_klass(type.getClassName());
                    request = "request_klass(env, " + idx + ")";
                }

                result.append(String.format("make_handle(env, %d, \"%s\", \"%s\", \"%s\", %s, %s)",
                        h.getTag(), h.getOwner(), h.getName(), h.getDesc(), request, caller));
            }
            case H_INVOKEVIRTUAL, H_INVOKEINTERFACE, H_INVOKESTATIC, H_NEWINVOKESPECIAL, H_INVOKESPECIAL -> {


                result.append(String.format("make_handle(env, %d, \"%s\", \"%s\", \"%s\", 0, %s)",
                        h.getTag(), h.getOwner(), h.getName(), h.getDesc(), caller));

            }
            default -> throw new IllegalArgumentException("Unsupported handle tag: " + h.getTag());
        }
        return result.toString();
    }


    @SneakyThrows
    public static void make(Processor processor) {
        String output = String.format("""
                void ensure_indy_cache(JNIEnv* env) {
                    if (static_arguments) return;
                    static_arguments = malloc(sizeof(jobjectArray) * %d);
                %s
                }
                """, Cache.Companion.cachedIndyArgs(), cacheInitialisationCode);
        processor.getSources().add(new Source("lib/invokedynamic.c", Files.readString(Path.of("helpers/invokedynamic.c"))
                .replace("__CACHE__", output)));
        processor.getHeaders().add(new Header("lib/invokedynamic.h", Files.readString(Path.of("helpers/invokedynamic.h"))));
        processor.getHeaders().add(new Header("lib/boxing.h", Files.readString(Path.of("helpers/boxing.h"))));
        processor.getSources().add(new Source("lib/boxing.c", Files.readString(Path.of("helpers/boxing.c"))));
        cacheInitialisationCode.setLength(0);
    }

}
