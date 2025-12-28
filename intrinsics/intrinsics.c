#include "intrinsics.h"

typedef char* address;

void jnt_reset_traceback(JNIEnv* env) {
    address env_addr = (address) env;
    address anchor_addr = env_addr - 0x20;
    *(void**)anchor_addr = 0;
}

finline void jnt_RegisterNative(JNIEnv* env, jclass cls,
                                const char* name, const char* desc,
                                int isStatic, void* fn) {

    jmethodID mid = isStatic
        ? (*env)->GetStaticMethodID(env, cls, name, desc)
        : (*env)->GetMethodID(env, cls, name, desc);

    void* method = *(void**)mid;
    address* addr = (address*)((uintptr_t)method + jnt_NativeAddrOffset);
    *addr = (address)fn;
}

finline void jnt_RegisterNatives(JNIEnv* env, jclass cls,
                                 JNTNativeMethod* methods, jint count) {
    for (jint i = 0; i < count; ++i) {
        const JNTNativeMethod* m = &methods[i];
        char* c_name = jstr_to_utf8(m->name, m->name_length);
        char* c_desc = jstr_to_utf8(m->desc, m->desc_length);
        jnt_RegisterNative(env, cls, c_name, c_desc, m->isStatic, m->fn);
        free(c_name);
        free(c_desc);
    }
}

finline jclass jnt_Object_getClass(JNIEnv* env, jobject instance) {
    return (*env)->GetObjectClass(env, instance);
}

finline jint jnt_String_length(JNIEnv* env, jobject str) {
    return (*env)->GetStringLength(env, str);
}

finline jboolean jnt_String_isEmpty(JNIEnv* env, jobject instance) {
    return (*env)->GetStringLength(env, (jstring)instance) == 0;
}

// jboolean jnt_String_equals(JNIEnv* env, jobject a, jstring b) {
//    // Must be handled a little differently.
//    const char* strA = (*env)->GetStringUTFChars(env, (jstring)a, 0);
//    const char* strB = (*env)->GetStringUTFChars(env, b, 0);
//
//    jboolean result = (strcmp(strA, strB) == 0) ? JNI_TRUE : JNI_FALSE;
//
//    (*env)->ReleaseStringUTFChars(env, (jstring)a, strA);
//    (*env)->ReleaseStringUTFChars(env, b, strB);
//
//    return result;
// }

void stackframe() {
    stackframe();
}
 
finline void jnt_Runtime_halt(JNIEnv* env, jint status) {
    // HANDLE WITH CARE, compilers optimize this out
    //  (-O2, -foptimize-sibling-calls)

    stackframe(); // Too many stack frames = stack overflow = segmentation fault.
}

finline void jnt_System_exit(JNIEnv* env, jint status) {
    exit(status);
}

// Abs ===
finline jint jnt_Math_abs_int(JNIEnv* env, jint a) {
    return (a < 0) ? -a : a;
}

finline jfloat jnt_Math_abs_float(JNIEnv* env, jfloat a) {
    return (a <= 0.0) ? 0.0 - a : a;
}

finline jlong jnt_Math_abs_long(JNIEnv* env, jlong a) {
    return (a < 0) ? -a : a;
}

finline jdouble jnt_Math_abs_double(JNIEnv* env, jdouble a) {
    return (a <= 0.0) ? 0.0 - a : a;
}

// Min ===
finline jint jnt_Math_min_int(JNIEnv* env, jint a, jint b) {
    return (a < b) ? a : b;
}

finline jlong jnt_Math_min_long(JNIEnv* env, jlong a, jlong b) {
    return (a < b) ? a : b;
}

finline jfloat jnt_Math_min_float(JNIEnv* env, jfloat a, jfloat b) {
    return (a < b) ? a : b;
}

finline jdouble jnt_Math_min_double(JNIEnv* env, jdouble a, jdouble b) {
    return (a < b) ? a : b;
}

// Max ===
finline jint jnt_Math_max_int(JNIEnv* env, jint a, jint b) {
    return (a > b) ? a : b;
}

finline jlong jnt_Math_max_long(JNIEnv* env, jlong a, jlong b) {
    return (a > b) ? a : b;
}

finline jfloat jnt_Math_max_float(JNIEnv* env, jfloat a, jfloat b) {
    return (a > b) ? a : b;
}

finline jdouble jnt_Math_max_double(JNIEnv* env, jdouble a, jdouble b) {
    return (a > b) ? a : b;
}

// Sqrt ===
finline jdouble jnt_Math_sqrt_double(JNIEnv* env, jdouble a) {
    return sqrt(a);
}

// Pow ===
finline jdouble jnt_Math_pow_double(JNIEnv* env, jdouble a, jdouble b) {
    return pow(a, b);
}

// java.lang.Math#round(float)
finline jint jnt_Math_round_float(JNIEnv* env, jfloat a) {
    if (isnan(a) || isinf(a) || fabs(a) >= (1 << (24 - 1))) {
        return (jint)a;
    }

    int32_t intBits = *(int32_t*)&a;
    int32_t biasedExp = (intBits & 0x7F800000) >> (24 - 1);
    int32_t shift = (24 - 2 + 127) - biasedExp;

    if ((shift & -32) == 0) {
        int32_t r = (intBits & 0x007FFFFF) | (0x007FFFFF + 1);
        if (intBits < 0) {
            r = -r;
        }
        return ((r >> shift) + 1) >> 1;
    } else {
        return (jint)a;
    }
}

// java.lang.Math#round(double)
finline jlong jnt_Math_round_double(JNIEnv* env, jdouble a) {
    if (isnan(a) || isinf(a) || fabs(a) >= (1LL << (53 - 1))) {
        return (jlong)a;
    }

    int64_t longBits = *(int64_t*)&a;
    int64_t biasedExp = (longBits & 0x7FF0000000000000LL) >> (53 - 1);
    int64_t shift = (53 - 2 + 1023) - biasedExp;

    if ((shift & -64) == 0) {
        int64_t r = (longBits & 0x000FFFFFFFFFFFFFLL) | (0x000FFFFFFFFFFFFFLL + 1);
        if (longBits < 0) {
            r = -r;
        }
        return ((r >> shift) + 1) >> 1;
    } else {
        return (jlong)a;
    }
}

finline char* jstr_to_utf8(const jchar* input, int len) {
    if (!input || len <= 0) {
        static char empty[] = "";
        return empty;
    }

    size_t length = (size_t)len;

    size_t max_utf8 = length * 3 + 1;
    char* out = (char*)malloc(max_utf8);
    if (!out) return NULL;

    size_t out_idx = 0;

    for (size_t i = 0; i < length; i++) {
        jchar ch = input[i];

        if (ch == 0x0000) {
            out[out_idx++] = (char)0xC0;
            out[out_idx++] = (char)0x80;
        }
        else if (ch < 0x80) {
            out[out_idx++] = (char)ch;
        }
        else if (ch < 0x800) {
            out[out_idx++] = (char)(0xC0 | (ch >> 6));
            out[out_idx++] = (char)(0x80 | (ch & 0x3F));
        }
        else {
            out[out_idx++] = (char)(0xE0 | (ch >> 12));
            out[out_idx++] = (char)(0x80 | ((ch >> 6) & 0x3F));
            out[out_idx++] = (char)(0x80 | (ch & 0x3F));
        }
    }

    out[out_idx] = '\0';
    return out;
}