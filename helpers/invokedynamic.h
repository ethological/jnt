#ifndef JNT_HELPER_INVOKEDYNAMIC
#define JNT_HELPER_INVOKEDYNAMIC

#include "jni.h"
#include "stdlib.h"
#include "cache.h"
#include "boxing.h"

jobject make_method_type(JNIEnv* env, const char* desc, jclass caller);
jobject make_handle(JNIEnv* env, jint tag, const char* owner, const char* name, const char* desc, jclass field_type, jclass caller);
void ensure_indy_cache(JNIEnv* env);
jobject make_site(JNIEnv* env, jobject bootstrap_method, jchar* name, jint name_length, jobject method_type, jint index, jclass caller_class);
jobject invoke_with_arguments(JNIEnv* env, jobject mh, jobjectArray args);

#endif
