package war.jnt.core.loader;

import org.objectweb.asm.Type;
import org.objectweb.asm.tree.MethodNode;
import war.configuration.ConfigurationSection;
import war.jnt.core.Processor;
import war.jnt.core.header.Header;
import war.jnt.core.name.NameProcessor;
import war.jnt.core.source.Source;
import war.jnt.fusebox.impl.Internal;
import war.jnt.fusebox.impl.TypeMaps;
import war.jnt.obfuscation.StringLookup;

import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * One big piece of badly written code that tries its best to work
 *
 * @author jan
 */
public class NativeLoader {
    public void make(Processor processor, ConfigurationSection config) {
        TypeMaps maps = new TypeMaps();
        StringBuilder builder = new StringBuilder();

        builder.append("""
                #include "war_jnt_Loader.h"
                
                """);

        processor.getClasses().forEach(classNode -> {
            if (classNode.isExempt()) return;
            String clazzName = NameProcessor.forClass(classNode.name);
            builder.append(String.format("// %s\nJNTNativeMethod %s_methods[] = {\n", classNode.name, clazzName));

            List<MethodNode> shuffledMethods = new ArrayList<>(classNode.methods);
            Collections.shuffle(shuffledMethods);

            shuffledMethods.forEach(methodNode -> {
                if (classNode.isExempt(methodNode)) return;
                if (Internal.disallowedTranspile(classNode, methodNode)) return;
                builder.append(String.format("\t{%s, %s, %d, %s, %d, (%s*)%s},\n", Modifier.isStatic(methodNode.access) ? 1 : 0,
                        StringLookup.toUTF16(methodNode.name), methodNode.name.length(),
                        StringLookup.toUTF16(methodNode.desc), methodNode.desc.length(),
                        maps.fromAsm(Type.getReturnType(methodNode.desc)), String.format("JNT_%s", NameProcessor.forMethod(classNode, methodNode))));
            });

            builder.append("};\n");
        });

        builder.append("""              
                struct NativeClassTable nativeClasses[] = {
                """);

        processor.getClasses().forEach(classNode -> {
            if (classNode.isExempt()) return;
            String clazzName = NameProcessor.forClass(classNode.name);
            int numMethods = 0;
            for (MethodNode method : classNode.methods) {
                if (classNode.isExempt(method)) continue;
                if (Internal.disallowedTranspile(classNode, method)) continue;
                numMethods++;
            }
            String className = classNode.name.replace('/', '.');
            builder.append(String.format("\t{%s, %d, %s_methods, %d},\n", StringLookup.toUTF16(className), className.length(), clazzName, numMethods));
        });

        builder.append("};\n");

        builder.append("""
                
                static size_t post_init_flag = 0;
                
                JNIEXPORT void JNICALL Java_war_jnt_Loader_init(JNIEnv *env, jclass auto_c, jclass initMe) {
                    if (!post_init_flag) post_init(env, auto_c);
                    jclass objectClass = (*env)->GetObjectClass(env, initMe);
                    jmethodID getNameMethod = (*env)->GetMethodID(env, objectClass, "getName", "()Ljava/lang/String;");
                    jstring name = (jstring) (*env)->CallObjectMethod(env, initMe, getNameMethod);

                    if ((*env)->ExceptionCheck(env)) {
                        (*env)->ExceptionClear(env);
                        (*env)->DeleteLocalRef(env, objectClass);
                        return;
                    }

                    const char* nameChars = (*env)->GetStringUTFChars(env, name, NULL);
                    int numClasses = sizeof(nativeClasses) / sizeof(nativeClasses[0]);
                    for (int i = 0; i < numClasses; i++) {
                        char* className = jstr_to_utf8(nativeClasses[i].className, nativeClasses[i].classNameLength);
                        if (strcmp(nameChars, className) == 0) {
                            free(className);
                            jnt_RegisterNatives(env, initMe, nativeClasses[i].methods, nativeClasses[i].methodCount);
                            break;
                        }
                        free(className);
                    }
                    (*env)->ReleaseStringUTFChars(env, name, nameChars);
                    (*env)->DeleteLocalRef(env, name);
                    (*env)->DeleteLocalRef(env, objectClass);
                    return;
                }
                """);

        builder.append("""
                
                static void dummy_fn(JNIEnv* env, jclass cls) {
                    (void)env; (void)cls;
                }
                
                uintptr_t jnt_NativeAddrOffset = 0;

                void JNICALL post_init(JNIEnv* env, jclass cls) {
                
                    init_primitives(env);
                    init_boxing_classes(env);
                    ensure_indy_cache(env);

                    JNINativeMethod methods[] = {
                        { "guard", "()V", (void*)dummy_fn }
                    };
                    (*env)->RegisterNatives(env, cls, methods, 1);
                
                    jmethodID mid = (*env)->GetStaticMethodID(env, cls, "guard", "()V");
                    volatile uintptr_t** method_entry_ptr = (uintptr_t**)mid;
                    volatile uintptr_t* method_table = *method_entry_ptr;
                
                    const size_t scan_limit = 256;
                    size_t found_offset = (size_t)-1;
                
                    for (size_t i = 0; i < scan_limit; i++) {
                        uintptr_t val = method_table[i];
                        if (val == (uintptr_t) dummy_fn) {
                            found_offset = i * sizeof(uintptr_t);
                            break;
                        }
                    }
                
                    jnt_NativeAddrOffset = found_offset;
                    post_init_flag = 1;
                    return;
                }
                """);

        String libPath = config.getString("jnt-path", "war/jnt");

        String output = builder.toString();
        output = output.replace("_war_jnt_Loader", "_" + NameProcessor.encode(libPath + "/Loader", NameProcessor.EncoderType.JNI));

        processor.getSources().add(new Source("war_jnt_Loader.c", output));

        StringBuilder header = new StringBuilder().append("""
                #include "lib/jni.h"
                #include "lib/invokedynamic.h"
                #include "lib/boxing.h"
                #include <string.h>
                """);
        processor.getClasses().forEach(classNode -> {
            if (classNode.isExempt()) return;
            String clazzName = NameProcessor.forClass(classNode.name);
            header.append(String.format("#include \"classes/%s.h\"\n", clazzName));
        });
        header.append("""
                
                struct NativeClassTable {
                    jchar* className;
                    int classNameLength;
                    JNTNativeMethod* methods;
                    int methodCount;
                };
                
                JNIEXPORT void JNICALL Java_war_jnt_Loader_init(JNIEnv* env, jclass auto_c, jclass initMe);
                void JNICALL post_init(JNIEnv* env, jclass cls);
                """);

        output = header.toString();
        output = output.replace("_war_jnt_Loader", "_" + NameProcessor.encode(libPath + "/Loader", NameProcessor.EncoderType.JNI));
        processor.getHeaders().add(new Header("war_jnt_Loader.h", output));
    }
}
