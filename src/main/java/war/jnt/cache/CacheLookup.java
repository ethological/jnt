package war.jnt.cache;

import war.jnt.cache.struct.CachedClass;
import war.jnt.cache.struct.CachedMethod;
import war.jnt.core.Processor;
import war.jnt.core.header.Header;
import war.jnt.core.source.Source;
import war.jnt.obfuscation.MutatedString;

public class CacheLookup {

    public static void make(Processor processor) {

        Cache.Companion cache = Cache.Companion;

        String header = """
                
                #ifndef JNT_CACHE
                #define JNT_CACHE
                
                #include "jni.h"
                #include <string.h>
                #include <stdint.h>
                #include <stdatomic.h>
                #include <stdlib.h>
                #include "intrinsics.h"
                #include "string_obfuscation.h"
                
                #ifndef CLASS_CACHE
                #define CLASS_CACHE 0
                #endif
                
                #ifndef METHOD_CACHE
                #define METHOD_CACHE 0
                #endif
                
                #ifndef FIELD_CACHE
                #define FIELD_CACHE 0
                #endif
                
                extern jclass klasses[CLASS_CACHE];
                extern jmethodID methods[METHOD_CACHE];
                extern jfieldID fields[FIELD_CACHE];
                
                void init_primitives(JNIEnv* env);
                
                jclass request_klass(JNIEnv* env, int idx);
                
                jmethodID request_method(JNIEnv* env, jclass owner, int idx);
                jmethodID request_virtual(JNIEnv* env, jclass owner, int idx);
                
                jfieldID request_field(JNIEnv* env, jclass owner, int idx);
                jfieldID request_ifield(JNIEnv* env, jclass owner, int idx);
                
                #endif
                
                """;

        processor.getHeaders().add(
                new Header("lib/cache.h", header)
        );

        StringBuilder source = new StringBuilder();
        source.append("""
                
                #include "cache.h"
                
                jclass klasses[CLASS_CACHE];
                jmethodID methods[METHOD_CACHE];
                jfieldID fields[FIELD_CACHE];
                
                static jclass kls_int, kls_long, kls_char, kls_short, kls_byte, kls_boolean, kls_float, kls_double, kls_void;
                
                void init_primitives(JNIEnv* env) {
                    jclass tmp;
                    jfieldID fid;
                    jobject clsObj;
                
                    #define CACHE_PRIMITIVE(wrapperName, targetVar) do {                         \\
                        tmp = (*env)->FindClass(env, "java/lang/" wrapperName);                  \\
                        if (!tmp) return;                                                       \\
                        fid = (*env)->GetStaticFieldID(env, tmp, "TYPE", "Ljava/lang/Class;");    \\
                        if (!fid) { (*env)->DeleteLocalRef(env, tmp); return; }                 \\
                        clsObj = (*env)->GetStaticObjectField(env, tmp, fid);                     \\
                        if (!clsObj) { (*env)->DeleteLocalRef(env, tmp); return; }              \\
                        targetVar = (jclass)(*env)->NewGlobalRef(env, clsObj);                    \\
                        (*env)->DeleteLocalRef(env, clsObj);                                      \\
                        (*env)->DeleteLocalRef(env, tmp);                                         \\
                    } while(0)
                
                    CACHE_PRIMITIVE("Integer", kls_int);
                    CACHE_PRIMITIVE("Long", kls_long);
                    CACHE_PRIMITIVE("Character", kls_char);
                    CACHE_PRIMITIVE("Short", kls_short);
                    CACHE_PRIMITIVE("Byte", kls_byte);
                    CACHE_PRIMITIVE("Boolean", kls_boolean);
                    CACHE_PRIMITIVE("Float", kls_float);
                    CACHE_PRIMITIVE("Double", kls_double);
                    CACHE_PRIMITIVE("Void", kls_void);
                
                    #undef CACHE_PRIMITIVE
                }
                
                jclass lookup_klass(JNIEnv* env, const char* name, int idx) {
                    jclass klass = klasses[idx];
                    if (klass) return klass;
                    jclass kls = (*env)->FindClass(env, name);
                    if ((*env)->ExceptionCheck(env)) {
                        jthrowable ex = (*env)->ExceptionOccurred(env);
                        (*env)->ExceptionDescribe(env);
                        (*env)->ExceptionClear(env);
                        (*env)->Throw(env, ex);
                    }
                    klasses[idx] = (jclass)(*env)->NewGlobalRef(env, kls);
                    (*env)->DeleteLocalRef(env, kls);
                    return klasses[idx];
                }
                
                jmethodID lookup_method(JNIEnv* env, jclass klass, const char* name, const char* desc, int idx) {
                    if (methods[idx]) return methods[idx];
                    methods[idx] = (*env)->GetStaticMethodID(env, klass, name, desc);
                    return methods[idx];
                }
                
                jmethodID lookup_virtual(JNIEnv* env, jclass klass, const char* name, const char* desc, int idx) {
                    if (methods[idx]) return methods[idx];
                    methods[idx] = (*env)->GetMethodID(env, klass, name, desc);
                    return methods[idx];
                }
                
                jfieldID lookup_field(JNIEnv* env, jclass klass, const char* name, const char* desc, int idx) {
                    if (fields[idx]) return fields[idx];
                    fields[idx] = (*env)->GetStaticFieldID(env, klass, name, desc);
                    return fields[idx];
                }
                
                jfieldID lookup_ifield(JNIEnv* env, jclass klass, const char* name, const char* desc, int idx) {
                    if (fields[idx]) return fields[idx];
                    fields[idx] = (*env)->GetFieldID(env, klass, name, desc);
                    return fields[idx];
                }
                
                """);

        source.append("jclass request_klass(JNIEnv* env, int idx) {\n");
        source.append("\tif (klasses[idx]) return klasses[idx];\n");
        source.append("\tjclass r = 0;\n");
        source.append("\tswitch (idx) {\n");
        for (CachedClass klass : cache.getKlasses()) {
            int index = cache.request_klass(klass.getName());
            MutatedString mutation = new MutatedString(klass.getName());
            source.append("\t\tcase ").append(index).append(":\n");
            source.append("\t\t\t/* ").append(klass.getName()).append(" */\n");

            switch (klass.getName()) {
                case "int" -> source.append("\t\t\tr = (klasses[").append(index).append("] = kls_int);\n");
                case "long" -> source.append("\t\t\tr = (klasses[").append(index).append("] = kls_long);\n");
                case "char" -> source.append("\t\t\tr =  (klasses[").append(index).append("] = kls_char);\n");
                case "short" -> source.append("\t\t\tr = (klasses[").append(index).append("] = kls_short);\n");
                case "byte" -> source.append("\t\t\tr = (klasses[").append(index).append("] = kls_byte);\n");
                case "boolean" -> source.append("\t\t\tr = (klasses[").append(index).append("] = kls_boolean);\n");
                case "float" -> source.append("\t\t\tr = (klasses[").append(index).append("] = kls_float);\n");
                case "double" -> source.append("\t\t\tr = (klasses[").append(index).append("] = kls_double);\n");
                case "void" -> source.append("\t\t\tr = (klasses[").append(index).append("] = kls_void);\n");
                default -> {
                    source.append("\t\t\t").append(mutation.compute("kls_" + index).replace("\n", "\n\t\t").trim());
                    source.append("\r\t\t\tr = lookup_klass(env, ").append("kls_").append(index).append(", ").append(index).append(");\n");
                    source.append("\t\t\tfree(kls_").append(index).append(");\n");
                }
            }
            source.append("\t\t\tbreak;\n");
        }
        source.append("\t\tdefault:\n");
        source.append("\t\t\treturn r;\n");
        source.append("\t}\n");
        source.append("\treturn r;\n");
        source.append("}\n\n");

        StringBuilder imethods = new StringBuilder();
        StringBuilder smethods = new StringBuilder();

        StringBuilder ifields = new StringBuilder();
        StringBuilder sfields = new StringBuilder();

        imethods.append("finline jmethodID request_virtual(JNIEnv* env, jclass owner, int idx) {\n");
        imethods.append("\tif (methods[idx]) return methods[idx];\n");
        imethods.append("\tjmethodID r = 0;\n");
        imethods.append("\tswitch (idx) {\n");
        smethods.append("finline jmethodID request_method(JNIEnv* env, jclass owner, int idx) {\n");
        smethods.append("\tif (methods[idx]) return methods[idx];\n");
        smethods.append("\tjmethodID r = 0;\n");
        smethods.append("\tswitch (idx) {\n");
        for (CachedMethod method : cache.getMethods()) {
            int index = cache.request_method(method);
            MutatedString nameMutation = new MutatedString(method.getName());
            MutatedString descMutation = new MutatedString(method.getDesc());
            if (!method.isStatic()) {
                imethods.append("\t\tcase ").append(index).append(":\n");
                imethods.append("\t\t\t/* ").append(method.getName()).append(method.getDesc()).append(" */\n");
                imethods.append("\t\t\t").append(nameMutation.compute("mtd_name_" + index).replace("\n", "\n\t\t").trim());
                imethods.append("\r\t\t\t").append(descMutation.compute("mtd_desc_" + index).replace("\n", "\n\t\t").trim());
                imethods.append("\r\t\t\tr = lookup_virtual(env, owner, ").append("mtd_name_").append(index).append(", ").append("mtd_desc_").append(index).append(", ").append(index).append(");\n");
                imethods.append("\t\t\tfree(mtd_name_").append(index).append(");\n");
                imethods.append("\t\t\tfree(mtd_desc_").append(index).append(");\n");
                imethods.append("\t\t\tbreak;\n");
            } else {
                smethods.append("\t\tcase ").append(index).append(":\n");
                smethods.append("\t\t\t/* ").append(method.getName()).append(method.getDesc()).append(" */\n");
                smethods.append("\t\t\t").append(nameMutation.compute("mtd_name_" + index).replace("\n", "\n\t\t").trim());
                smethods.append("\r\t\t\t").append(descMutation.compute("mtd_desc_" + index).replace("\n", "\n\t\t").trim());
                smethods.append("\r\t\t\tr = lookup_method(env, owner, ").append("mtd_name_").append(index).append(", ").append("mtd_desc_").append(index).append(", ").append(index).append(");\n");
                smethods.append("\t\t\tfree(mtd_name_").append(index).append(");\n");
                smethods.append("\t\t\tfree(mtd_desc_").append(index).append(");\n");
                smethods.append("\t\t\tbreak;\n");
            }
        }
        imethods.append("\t\tdefault:\n");
        imethods.append("\t\t\treturn r;\n");
        imethods.append("\t}\n");
        imethods.append("\treturn r;\n");
        imethods.append("}\n\n");
        smethods.append("\t\tdefault:\n");
        smethods.append("\t\t\treturn r;\n");
        smethods.append("\t}\n");
        smethods.append("\treturn r;\n");
        smethods.append("}\n\n");

        source.append(imethods);
        source.append(smethods);

        ifields.append("finline jfieldID request_ifield(JNIEnv* env, jclass owner, int idx) {\n");
        ifields.append("\tif (fields[idx]) return fields[idx];\n");
        ifields.append("\tjfieldID r = 0;\n");
        ifields.append("\tswitch (idx) {\n");
        sfields.append("finline jfieldID request_field(JNIEnv* env, jclass owner, int idx) {\n");
        sfields.append("\tif (fields[idx]) return fields[idx];\n");
        sfields.append("\tjfieldID r = 0;\n");
        sfields.append("\tswitch (idx) {\n");
        for (var field : cache.getFields()) {
            int index = cache.request_field(field);
            MutatedString nameMutation = new MutatedString(field.getName());
            MutatedString descMutation = new MutatedString(field.getDesc());
            if (!field.isStatic()) {
                ifields.append("\t\tcase ").append(index).append(":\n");
                ifields.append("\t\t\t").append("/* ").append(field.getName()).append(field.getDesc()).append(" */\n");
                ifields.append("\t\t\t").append(nameMutation.compute("fld_name_" + index).replace("\n", "\n\t\t").trim());
                ifields.append("\r\t\t\t").append(descMutation.compute("fld_desc_" + index).replace("\n", "\n\t\t").trim());
                ifields.append("\r\t\t\tr = lookup_ifield(env, owner, ").append("fld_name_").append(index).append(", ").append("fld_desc_").append(index).append(", ").append(index).append(");\n");
                ifields.append("\t\t\tfree(fld_name_").append(index).append(");\n");
                ifields.append("\t\t\tfree(fld_desc_").append(index).append(");\n");
                ifields.append("\t\t\tbreak;\n");
            } else {
                sfields.append("\t\tcase ").append(index).append(":\n");
                sfields.append("\t\t\t").append("/* ").append(field.getName()).append(field.getDesc()).append(" */\n");
                sfields.append("\t\t\t").append(nameMutation.compute("fld_name_" + index).replace("\n", "\n\t\t").trim());
                sfields.append("\r\t\t\t").append(descMutation.compute("fld_desc_" + index).replace("\n", "\n\t\t").trim());
                sfields.append("\r\t\t\tr = lookup_field(env, owner, ").append("fld_name_").append(index).append(", ").append("fld_desc_").append(index).append(", ").append(index).append(");\n");
                sfields.append("\t\t\tfree(fld_name_").append(index).append(");\n");
                sfields.append("\t\t\tfree(fld_desc_").append(index).append(");\n");
                sfields.append("\t\t\tbreak;\n");
            }
        }
        ifields.append("\t\tdefault:\n");
        ifields.append("\t\t\treturn r;\n");
        ifields.append("\t}\n");
        ifields.append("\treturn r;\n");
        ifields.append("}\n\n");
        sfields.append("\t\tdefault:\n");
        sfields.append("\t\t\treturn r;\n");
        sfields.append("\t}\n");
        sfields.append("\treturn r;\n");
        sfields.append("}\n\n");

        source.append(ifields);
        source.append(sfields);

        processor.getSources().add(
                new Source("lib/cache.c", source.toString())
        );

    }

}
