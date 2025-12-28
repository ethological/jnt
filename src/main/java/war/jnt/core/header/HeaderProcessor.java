package war.jnt.core.header;

import org.objectweb.asm.Opcodes;
import org.objectweb.asm.Type;
import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.tree.MethodNode;
import war.jnt.core.name.NameProcessor;
import war.jnt.fusebox.impl.Internal;
import war.jnt.fusebox.impl.TypeMaps;
import war.metaphor.tree.JClassNode;

public class HeaderProcessor {
    public static String signature(ClassNode owner, MethodNode method) {
        var builder = new StringBuilder();

        boolean isStatic = Internal.isAccess(method.access, Opcodes.ACC_STATIC);

        String methodName = NameProcessor.forMethod(owner, method);

        TypeMaps maps = new TypeMaps();
        Type rType = Type.getReturnType(method.desc);

        builder//.append("JNIEXPORT ")
                .append(maps.fromAsm(rType))
                .append(" JNICALL JNT_")
                .append(methodName)
                .append("(JNIEnv* env, ")
                .append(isStatic ? "jclass _auto" : "jobject _auto");

        Type[] argTypes = Type.getArgumentTypes(method.desc);

        if (argTypes.length > 0) {
            builder.append(", ");
        }

        int i = 0;
        int j = 0;
        while (i < argTypes.length) {
            var type = argTypes[i];
            builder
                    .append(maps.fromAsm(type))
//                    .append("jlong")
                    .append(" p")
                    .append(j);

            if (i < argTypes.length - 1) {
                builder.append(", ");
            }

            j += type.getSize();
            i++;
        }

        builder.append(")");
        return builder.toString();
    }

    public static String call(ClassNode owner, MethodNode method, String args) {
        String className = NameProcessor.forClass(owner.name);
        String methodName = NameProcessor.forMethod(owner, method);
        return "JNT_" + methodName + "(" + args + ")";
    }

    public static String forClass(JClassNode node) {
        var builder = new StringBuilder();

        builder.append(
                """
                        #include "../lib/jni.h"
                        #include "../lib/intrinsics.h"
                        #include "../lib/cache.h"
                        #include "../lib/string_obfuscation.h"
                        #include "../lib/invokedynamic.h"
                        #include "../lib/boxing.h"
                        #include <stdlib.h>
                        #include <math.h>
                        
                        """);

        for (MethodNode method : node.methods) {
            if (node.isExempt(method)) continue;
            if (Internal.disallowedTranspile(node, method)) continue;
            builder.append(signature(node, method)).append(";\n");
        }

        return builder.toString();
    }
}
