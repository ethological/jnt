#ifndef JNT_INTRINSICS
#define JNT_INTRINSICS

#include "jni.h"
#include <string.h>
#include <stdlib.h>
#include <stdint.h>
#include <stdio.h>
#include <math.h>
#include <stdbool.h>

typedef struct JNTNativeMethod {
    int isStatic;
    const jchar* name;
    int name_length;
    const jchar* desc;
    int desc_length;
    void* fn;
} JNTNativeMethod;

extern uintptr_t jnt_NativeAddrOffset;

void jnt_reset_traceback(JNIEnv* env);

void jnt_RegisterNatives(JNIEnv* env, jclass cls,
                         JNTNativeMethod* methods, jint count);
void jnt_RegisterNative(JNIEnv* env, jclass cls,
                        const char* name, const char* desc, int isStatic, void* fn);

// java.lang.Object#getClass
jclass jnt_Object_getClass(JNIEnv* env, jobject instance);


// STR =====


// java.lang.String#length
jint jnt_String_length(JNIEnv* env, jobject instance);

// java.lang.String$isEmpty
jboolean jnt_String_isEmpty(JNIEnv* env, jobject instance);

// java.lang.String#equals
// jboolean jnt_String_equals(JNIEnv* env, jobject a, jstring b)


// SYS =====

// java.lang.Runtime#halt
void jnt_Runtime_halt(JNIEnv* env, jint status);

// java.lang.System#exit
void jnt_System_exit(JNIEnv* env, jint status);


// MTH =====


// java.lang.Math$abs
jint jnt_Math_abs_int(JNIEnv* env, jint a);
jfloat jnt_Math_abs_float(JNIEnv* env, jfloat a);
jlong jnt_Math_abs_long(JNIEnv* env, jlong a);
jdouble jnt_Math_abs_double(JNIEnv* env, jdouble a);

// java.lang.Math$min
jint jnt_Math_min_int(JNIEnv* env, jint a, jint b);
jfloat jnt_Math_min_float(JNIEnv* env, jfloat a, jfloat b);
jlong jnt_Math_min_long(JNIEnv* env, jlong a, jlong b);
jdouble jnt_Math_min_double(JNIEnv* env, jdouble a, jdouble b);

// java.lang.Math$max
jint jnt_Math_max_int(JNIEnv* env, jint a, jint b);
jfloat jnt_Math_max_float(JNIEnv* env, jfloat a, jfloat b);
jlong jnt_Math_max_long(JNIEnv* env, jlong a, jlong b);
jdouble jnt_Math_max_double(JNIEnv* env, jdouble a, jdouble b);

// java.lang.Math$sqrt
jdouble jnt_Math_sqrt_double(JNIEnv* env, jdouble a);

// java.lang.Math$pow
jdouble jnt_Math_pow_double(JNIEnv* env, jdouble a, jdouble b);

// java.lang.Math#round
jint jnt_Math_round_float(JNIEnv* env, jfloat a);
jlong jnt_Math_round_double(JNIEnv* env, jdouble a);


// UTL =====


char* jstr_to_utf8(const jchar* input, int length);

#endif