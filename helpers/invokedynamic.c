#include "invokedynamic.h"

static jobjectArray* static_arguments;
static jclass kls_Class_glob;
static jmethodID mid_getCL_glob;
static jclass kls_MethodHandles_glob;
static jclass kls_Lookup_glob;
static jmethodID mid_lookup_glob;
static jmethodID mid_privateLookupIn_glob;
static jmethodID mid_findStatic_glob;
static jmethodID mid_findVirtual_glob;
static jmethodID mid_findSpecial_glob;
static jmethodID mid_findConstructor_glob;
static jmethodID mid_findGetter_glob;
static jmethodID mid_findSetter_glob;
static jmethodID mid_findStaticGetter_glob;
static jmethodID mid_findStaticSetter_glob;
static jclass kls_CallSite_glob;
static jmethodID mid_makeSite_glob;
static jmethodID mid_dynamicInvoker_glob;

static jobject get_class_loader(JNIEnv* env, jclass cls) {
    if (!kls_Class_glob) {
        jclass t = (*env)->FindClass(env, "java/lang/Class");
        if (!t) return 0;
        kls_Class_glob = (*env)->NewGlobalRef(env, t);
        (*env)->DeleteLocalRef(env, t);
        mid_getCL_glob = (*env)->GetMethodID(env, kls_Class_glob, "getClassLoader", "()Ljava/lang/ClassLoader;");
        if (!mid_getCL_glob) return 0;
    }
    return (*env)->CallObjectMethod(env, cls, mid_getCL_glob);
}

jobject make_method_type(JNIEnv* env, const char* desc, jclass caller) {
    //printf("make_method_type: %s\n", desc);
    jclass kls_MethodType = (*env)->FindClass(env, "java/lang/invoke/MethodType");
    if (!kls_MethodType) return 0;
    jmethodID mid_fromDesc = (*env)->GetStaticMethodID(env, kls_MethodType, "fromMethodDescriptorString", "(Ljava/lang/String;Ljava/lang/ClassLoader;)Ljava/lang/invoke/MethodType;");
    if (!mid_fromDesc) { (*env)->DeleteLocalRef(env, kls_MethodType); return 0; }
    jstring j_desc = (*env)->NewStringUTF(env, desc);
    if (!j_desc) { (*env)->DeleteLocalRef(env, kls_MethodType); return 0; }
    jobject cl = get_class_loader(env, caller);
    //printf("make_method_type: class loader %p\n", cl);
    jobject mt = (*env)->CallStaticObjectMethod(env, kls_MethodType, mid_fromDesc, j_desc, cl);
    //printf("make_method_type: method type %p\n", mt);
    if (cl) (*env)->DeleteLocalRef(env, cl);
    (*env)->DeleteLocalRef(env, j_desc);
    (*env)->DeleteLocalRef(env, kls_MethodType);
    if ((*env)->ExceptionCheck(env)) { (*env)->ExceptionDescribe(env); return 0; }
    return mt;
}

static int ensure_methodhandles(JNIEnv* env) {
    if (kls_MethodHandles_glob) return 1;
    jclass t = (*env)->FindClass(env, "java/lang/invoke/MethodHandles");
    if (!t) return 0;
    kls_MethodHandles_glob = (*env)->NewGlobalRef(env, t);
    (*env)->DeleteLocalRef(env, t);
    t = (*env)->FindClass(env, "java/lang/invoke/MethodHandles$Lookup");
    if (!t) return 0;
    kls_Lookup_glob = (*env)->NewGlobalRef(env, t);
    (*env)->DeleteLocalRef(env, t);
    mid_lookup_glob = (*env)->GetStaticMethodID(env, kls_MethodHandles_glob, "lookup", "()Ljava/lang/invoke/MethodHandles$Lookup;");
    mid_privateLookupIn_glob = (*env)->GetStaticMethodID(env, kls_MethodHandles_glob, "privateLookupIn", "(Ljava/lang/Class;Ljava/lang/invoke/MethodHandles$Lookup;)Ljava/lang/invoke/MethodHandles$Lookup;");
    mid_findStatic_glob = (*env)->GetMethodID(env, kls_Lookup_glob, "findStatic", "(Ljava/lang/Class;Ljava/lang/String;Ljava/lang/invoke/MethodType;)Ljava/lang/invoke/MethodHandle;");
    mid_findVirtual_glob = (*env)->GetMethodID(env, kls_Lookup_glob, "findVirtual", "(Ljava/lang/Class;Ljava/lang/String;Ljava/lang/invoke/MethodType;)Ljava/lang/invoke/MethodHandle;");
    mid_findSpecial_glob = (*env)->GetMethodID(env, kls_Lookup_glob, "findSpecial", "(Ljava/lang/Class;Ljava/lang/String;Ljava/lang/invoke/MethodType;Ljava/lang/Class;)Ljava/lang/invoke/MethodHandle;");
    mid_findConstructor_glob = (*env)->GetMethodID(env, kls_Lookup_glob, "findConstructor", "(Ljava/lang/Class;Ljava/lang/invoke/MethodType;)Ljava/lang/invoke/MethodHandle;");
    mid_findGetter_glob = (*env)->GetMethodID(env, kls_Lookup_glob, "findGetter", "(Ljava/lang/Class;Ljava/lang/String;Ljava/lang/Class;)Ljava/lang/invoke/MethodHandle;");
    mid_findSetter_glob = (*env)->GetMethodID(env, kls_Lookup_glob, "findSetter", "(Ljava/lang/Class;Ljava/lang/String;Ljava/lang/Class;)Ljava/lang/invoke/MethodHandle;");
    mid_findStaticGetter_glob = (*env)->GetMethodID(env, kls_Lookup_glob, "findStaticGetter", "(Ljava/lang/Class;Ljava/lang/String;Ljava/lang/Class;)Ljava/lang/invoke/MethodHandle;");
    mid_findStaticSetter_glob = (*env)->GetMethodID(env, kls_Lookup_glob, "findStaticSetter", "(Ljava/lang/Class;Ljava/lang/String;Ljava/lang/Class;)Ljava/lang/invoke/MethodHandle;");
    if (!mid_lookup_glob || !mid_privateLookupIn_glob || !mid_findStatic_glob || !mid_findVirtual_glob || !mid_findSpecial_glob || !mid_findConstructor_glob || !mid_findGetter_glob || !mid_findSetter_glob || !mid_findStaticGetter_glob || !mid_findStaticSetter_glob) return 0;
    return 1;
}

jobject make_handle(JNIEnv* env, jint tag, const char* owner, const char* name, const char* desc, jclass field_type, jclass caller) {
    if ((*env)->ExceptionCheck(env)) (*env)->ExceptionDescribe(env);
    if (!ensure_methodhandles(env)) return 0;
//    printf("make_handle: tag=%d, owner=%s, name=%s, desc=%s, field_type=%p, caller=%p\n", tag, owner, name, desc, field_type, caller);
    jclass ownerCls = (*env)->FindClass(env, owner);
//    printf("make_handle: owner class %p\n", ownerCls);
    if (!ownerCls) return 0;
    jobject lookup = (*env)->CallStaticObjectMethod(env, kls_MethodHandles_glob, mid_lookup_glob);
//    printf("make_handle: base lookup %p\n", lookup);
    if ((*env)->ExceptionCheck(env) || !lookup) { (*env)->DeleteLocalRef(env, ownerCls); if ((*env)->ExceptionCheck(env)) (*env)->ExceptionDescribe(env); return 0; }

    jobject private_lookup = (*env)->CallStaticObjectMethod(env, kls_MethodHandles_glob, mid_privateLookupIn_glob, caller, lookup);
    if ((*env)->ExceptionCheck(env) || !private_lookup) { (*env)->DeleteLocalRef(env, ownerCls); (*env)->DeleteLocalRef(env, lookup); if ((*env)->ExceptionCheck(env)) (*env)->ExceptionDescribe(env); return 0; }
    (*env)->DeleteLocalRef(env, lookup);
    lookup = private_lookup;
//    printf("make_handle: private lookup %p\n", lookup);

    jobject mh = 0;

    if (tag >= 1 && tag <= 4) {
        if (!field_type) { (*env)->DeleteLocalRef(env, ownerCls); (*env)->DeleteLocalRef(env, lookup); return 0; }
        jstring jName = (*env)->NewStringUTF(env, name);
        if (!jName) { (*env)->DeleteLocalRef(env, ownerCls); (*env)->DeleteLocalRef(env, lookup); return 0; }
        if (tag == 1 || tag == 3) {
            mh = (*env)->CallObjectMethod(env, lookup, (tag == 1 ? mid_findGetter_glob : mid_findSetter_glob), ownerCls, jName, field_type);
        } else {
            mh = (*env)->CallObjectMethod(env, lookup, (tag == 2 ? mid_findStaticGetter_glob : mid_findStaticSetter_glob), ownerCls, jName, field_type);
        }
        (*env)->DeleteLocalRef(env, jName);
    } else {
        //printf("finding method type for %s\n", desc);
        jobject mt = make_method_type(env, desc, caller);
        //printf("make_handle: method type %p\n", mt);
        if (!mt) { (*env)->DeleteLocalRef(env, ownerCls); (*env)->DeleteLocalRef(env, lookup); return 0; }
        if (tag == 8) {
            mh = (*env)->CallObjectMethod(env, lookup, mid_findConstructor_glob, ownerCls, mt);
        } else {
            jstring jName = (*env)->NewStringUTF(env, name);
            if (!jName) { (*env)->DeleteLocalRef(env, mt); (*env)->DeleteLocalRef(env, ownerCls); (*env)->DeleteLocalRef(env, lookup); return 0; }
            if (tag == 6) {
                mh = (*env)->CallObjectMethod(env, lookup, mid_findStatic_glob, ownerCls, jName, mt);
            } else if (tag == 5 || tag == 9) {
                mh = (*env)->CallObjectMethod(env, lookup, mid_findVirtual_glob, ownerCls, jName, mt);
            } else if (tag == 7) {
                jclass sc = caller ? caller : ownerCls;
                mh = (*env)->CallObjectMethod(env, lookup, mid_findSpecial_glob, ownerCls, jName, mt, sc);
            }
            (*env)->DeleteLocalRef(env, jName);
        }
        (*env)->DeleteLocalRef(env, mt);
    }

    (*env)->DeleteLocalRef(env, ownerCls);
    (*env)->DeleteLocalRef(env, lookup);

    if ((*env)->ExceptionCheck(env)) { (*env)->ExceptionDescribe(env); return 0; }
    return mh;
}

jobject make_site(JNIEnv* env, jobject bootstrap_method, jchar* name, jint name_length, jobject method_type, jint index, jclass caller_class) {
    if (!kls_CallSite_glob) {
        jclass t = (*env)->FindClass(env, "java/lang/invoke/CallSite");
        if (!t) return 0;
        kls_CallSite_glob = (*env)->NewGlobalRef(env, t);
        (*env)->DeleteLocalRef(env, t);
        mid_makeSite_glob = (*env)->GetStaticMethodID(env, kls_CallSite_glob, "makeSite", "(Ljava/lang/invoke/MethodHandle;Ljava/lang/String;Ljava/lang/invoke/MethodType;Ljava/lang/Object;Ljava/lang/Class;)Ljava/lang/invoke/CallSite;");
        if (!mid_makeSite_glob) return 0;
        mid_dynamicInvoker_glob = (*env)->GetMethodID(env, kls_CallSite_glob, "dynamicInvoker", "()Ljava/lang/invoke/MethodHandle;");
        if (!mid_dynamicInvoker_glob) return 0;
    }
    jstring j_name = (*env)->NewString(env, name, name_length);
//    printf("make_site: name='%.*s' method_type=%p index=%d caller_class=%p\n", name_length, name, method_type, index, caller_class);
    if (!j_name) return 0;
    jobjectArray args = static_arguments ? static_arguments[index] : 0;
//    printf("make_site: args=%p\n", args);
    jobject cs = (*env)->CallStaticObjectMethod(env, kls_CallSite_glob, mid_makeSite_glob, bootstrap_method, j_name, method_type, args, caller_class);
//    printf("make_site: call site %p\n", cs);
    (*env)->DeleteLocalRef(env, j_name);
    if ((*env)->ExceptionCheck(env) || !cs) { if ((*env)->ExceptionCheck(env)) (*env)->ExceptionDescribe(env); return 0; }
    jobject mh = (*env)->CallObjectMethod(env, cs, mid_dynamicInvoker_glob);
    (*env)->DeleteLocalRef(env, cs);
    if ((*env)->ExceptionCheck(env)) { (*env)->ExceptionDescribe(env); return 0; }
    return mh;
}

jobject invoke_with_arguments(JNIEnv* env, jobject mh, jobjectArray args) {
//    printf("invoke_with_arguments: mh=%p, args=%p\n", mh, args);
    jclass mh_cls = (*env)->GetObjectClass(env, mh);
    if (!mh_cls) return 0;
    jmethodID mid = (*env)->GetMethodID(env, mh_cls, "invokeWithArguments", "([Ljava/lang/Object;)Ljava/lang/Object;");
    jobject r = (*env)->CallObjectMethod(env, mh, mid, args);
    (*env)->DeleteLocalRef(env, mh_cls);
    return r;
}

__CACHE__