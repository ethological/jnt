#ifndef JNT_HELPER_BOXING
#define JNT_HELPER_BOXING

#include "jni.h"

void init_boxing_classes(JNIEnv* env);

jobject make_object_int(JNIEnv* env, jint value);
jobject make_object_long(JNIEnv* env, jlong value);
jobject make_object_char(JNIEnv* env, jchar value);
jobject make_object_short(JNIEnv* env, jshort value);
jobject make_object_byte(JNIEnv* env, jbyte value);
jobject make_object_boolean(JNIEnv* env, jboolean value);
jobject make_object_float(JNIEnv* env, jfloat value);
jobject make_object_double(JNIEnv* env, jdouble value);

jint unbox_int(JNIEnv* env, jobject obj);
jlong unbox_long(JNIEnv* env, jobject obj);
jchar unbox_char(JNIEnv* env, jobject obj);
jshort unbox_short(JNIEnv* env, jobject obj);
jbyte unbox_byte(JNIEnv* env, jobject obj);
jboolean unbox_boolean(JNIEnv* env, jobject obj);
jfloat unbox_float(JNIEnv* env, jobject obj);
jdouble unbox_double(JNIEnv* env, jobject obj);

#endif
