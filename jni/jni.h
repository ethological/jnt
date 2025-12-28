#pragma once

#ifndef _JAVASOFT_JNI_H_
#define _JAVASOFT_JNI_H_

#include <stdio.h>
#include <stdarg.h>

/* jni_md.h contains the machine-dependent typedefs for jbyte, jint
   and jlong */

#if defined(_MSC_VER)
    #define finline __forceinline
#elif defined(__GNUC__) || defined(__clang__)
    #define finline inline __attribute__((always_inline))
#else
    #define finline inline
#endif

#if defined(_MSC_VER)
    #define noinline __declspec(noinline)
#elif defined(__GNUC__) || defined(__clang__)
    #define noinline __attribute__((noinline))
#else
    #define noinline
#endif

//#define UNIQUE_SECTION(name) __attribute__((section(name))) noinline

// begin inlined jni_md.h
#if defined(WIN32) || defined(_WIN32) || defined(__WIN32__) || defined(__NT__)
    #ifndef JNIEXPORT
        #define JNIEXPORT __declspec(dllexport)
    #endif
    #define JNIIMPORT __declspec(dllimport)
    #define JNICALL __stdcall

    // 'long' is always 32 bit on windows so this matches what jdk expects
    typedef unsigned long juint;
    typedef long jint;
    typedef unsigned __int64 julong;
    typedef __int64 jlong;
    typedef signed char jbyte;
#else
    #ifndef __has_attribute
        #define __has_attribute(x) 0
    #endif

    #ifndef JNIEXPORT
        #if (defined(__GNUC__) && ((__GNUC__ > 4) || (__GNUC__ == 4) && (__GNUC_MINOR__ > 2))) || __has_attribute(visibility)
            #ifdef ARM
                #define JNIEXPORT     __attribute__((externally_visible,visibility("default")))
            #else
                #define JNIEXPORT     __attribute__((visibility("default")))
            #endif
        #else
            #define JNIEXPORT
        #endif
    #endif

    #if (defined(__GNUC__) && ((__GNUC__ > 4) || (__GNUC__ == 4) && (__GNUC_MINOR__ > 2))) || __has_attribute(visibility)
        #ifdef ARM
            #define JNIIMPORT     __attribute__((externally_visible,visibility("default")))
        #else
            #define JNIIMPORT     __attribute__((visibility("default")))
        #endif
    #else
        #define JNIIMPORT
    #endif

    #define JNICALL

    typedef unsigned int juint;
    typedef int jint;
    #ifdef _LP64
    typedef unsigned long julong;
    typedef long jlong;
    #else
    typedef unsigned long long julong;
    typedef long long jlong;
    #endif

    typedef signed char jbyte;
#endif
// end inlined jni_md.h

#ifdef __cplusplus
extern "C" {
#endif

/*
 * JNI Types
 */

#ifndef JNI_TYPES_ALREADY_DEFINED_IN_JNI_MD_H

typedef unsigned char   jboolean;
typedef unsigned short  jchar;
typedef short           jshort;
typedef float           jfloat;
typedef double          jdouble;

typedef jint            jsize;

typedef void* jobject;
typedef jobject jclass;
typedef jobject jstring;
typedef jobject jthrowable;
typedef jobject jarray;
typedef jobject jbooleanArray;
typedef jobject jbyteArray;
typedef jobject jcharArray;
typedef jobject jshortArray;
typedef jobject jintArray;
typedef jobject jlongArray;
typedef jobject jfloatArray;
typedef jobject jdoubleArray;
typedef jobject jobjectArray;

//#ifdef __cplusplus

//class _jobject {};
//class _jclass : public _jobject {};
//class _jthrowable : public _jobject {};
//class _jstring : public _jobject {};
//class _jarray : public _jobject {};
//class _jbooleanArray : public _jarray {};
//class _jbyteArray : public _jarray {};
//class _jcharArray : public _jarray {};
//class _jshortArray : public _jarray {};
//class _jintArray : public _jarray {};
//class _jlongArray : public _jarray {};
//class _jfloatArray : public _jarray {};
//class _jdoubleArray : public _jarray {};
//class _jobjectArray : public _jarray {};
//
//typedef _jobject *jobject;
//typedef _jclass *jclass;
//typedef _jthrowable *jthrowable;
//typedef _jstring *jstring;
//typedef _jarray *jarray;
//typedef _jbooleanArray *jbooleanArray;
//typedef _jbyteArray *jbyteArray;
//typedef _jcharArray *jcharArray;
//typedef _jshortArray *jshortArray;
//typedef _jintArray *jintArray;
//typedef _jlongArray *jlongArray;
//typedef _jfloatArray *jfloatArray;
//typedef _jdoubleArray *jdoubleArray;
//typedef _jobjectArray *jobjectArray;
//
//#else
//
//struct _jobject;
//
//typedef struct _jobject *jobject;
//typedef jobject jclass;
//typedef jobject jthrowable;
//typedef jobject jstring;
//typedef jobject jarray;
//typedef jarray jbooleanArray;
//typedef jarray jbyteArray;
//typedef jarray jcharArray;
//typedef jarray jshortArray;
//typedef jarray jintArray;
//typedef jarray jlongArray;
//typedef jarray jfloatArray;
//typedef jarray jdoubleArray;
//typedef jarray jobjectArray;
//
//#endif

typedef jobject jweak;

typedef union jvalue {
    jboolean z;
    jbyte    b;
    jchar    c;
    jshort   s;
    jint     i;
    jlong    j;
    jfloat   f;
    jdouble  d;
    jobject  l;
} jvalue;

struct _jfieldID;
typedef struct _jfieldID *jfieldID;

struct _jmethodID;
typedef struct _jmethodID *jmethodID;

/* Return values from jobjectRefType */
typedef enum _jobjectType {
     JNIInvalidRefType    = 0,
     JNILocalRefType      = 1,
     JNIGlobalRefType     = 2,
     JNIWeakGlobalRefType = 3
} jobjectRefType;


#endif /* JNI_TYPES_ALREADY_DEFINED_IN_JNI_MD_H */

/*
 * jboolean constants
 */

#define JNI_FALSE 0
#define JNI_TRUE 1

/*
 * possible return values for JNI functions.
 */

#define JNI_OK           0                 /* success */
#define JNI_ERR          (-1)              /* unknown error */
#define JNI_EDETACHED    (-2)              /* thread detached from the VM */
#define JNI_EVERSION     (-3)              /* JNI version error */
#define JNI_ENOMEM       (-4)              /* not enough memory */
#define JNI_EEXIST       (-5)              /* VM already created */
#define JNI_EINVAL       (-6)              /* invalid arguments */

/*
 * used in ReleaseScalarArrayElements
 */

#define JNI_COMMIT 1
#define JNI_ABORT 2

/*
 * used in RegisterNatives to describe native method name, signature,
 * and function pointer.
 */

typedef struct {
    char *name;
    char *signature;
    void *fnPtr;
} JNINativeMethod;

/*
 * JNI Native Method Interface.
 */

struct JNINativeInterface_;

struct JNIEnv_;

#ifdef __cplusplus
typedef JNIEnv_ JNIEnv;
#else
typedef const struct JNINativeInterface_ *JNIEnv;
#endif

/*
 * JNI Invocation Interface.
 */

struct JNIInvokeInterface_;

struct JavaVM_;

#ifdef __cplusplus
typedef JavaVM_ JavaVM;
#else
typedef const struct JNIInvokeInterface_ *JavaVM;
#endif

struct JNINativeInterface_ {
    void *reserved0;
    void *reserved1;
    void *reserved2;

    void *reserved3;
    jint (JNICALL *GetVersion)(JNIEnv *env);

    jclass (JNICALL *DefineClass)
      (JNIEnv *env, const char *name, jobject loader, const jbyte *buf,
       jsize len);
    jclass (JNICALL *FindClass)
      (JNIEnv *env, const char *name);

    jmethodID (JNICALL *FromReflectedMethod)
      (JNIEnv *env, jobject method);
    jfieldID (JNICALL *FromReflectedField)
      (JNIEnv *env, jobject field);

    jobject (JNICALL *ToReflectedMethod)
      (JNIEnv *env, jclass cls, jmethodID methodID, jboolean isStatic);

    jclass (JNICALL *GetSuperclass)
      (JNIEnv *env, jclass sub);
    jboolean (JNICALL *IsAssignableFrom)
      (JNIEnv *env, jclass sub, jclass sup);

    jobject (JNICALL *ToReflectedField)
      (JNIEnv *env, jclass cls, jfieldID fieldID, jboolean isStatic);

    jint (JNICALL *Throw)
      (JNIEnv *env, jthrowable obj);
    jint (JNICALL *ThrowNew)
      (JNIEnv *env, jclass clazz, const char *msg);
    jthrowable (JNICALL *ExceptionOccurred)
      (JNIEnv *env);
    void (JNICALL *ExceptionDescribe)
      (JNIEnv *env);
    void (JNICALL *ExceptionClear)
      (JNIEnv *env);
    void (JNICALL *FatalError)
      (JNIEnv *env, const char *msg);

    jint (JNICALL *PushLocalFrame)
      (JNIEnv *env, jint capacity);
    jobject (JNICALL *PopLocalFrame)
      (JNIEnv *env, jobject result);

    jobject (JNICALL *NewGlobalRef)
      (JNIEnv *env, jobject lobj);
    void (JNICALL *DeleteGlobalRef)
      (JNIEnv *env, jobject gref);
    void (JNICALL *DeleteLocalRef)
      (JNIEnv *env, jobject obj);
    jboolean (JNICALL *IsSameObject)
      (JNIEnv *env, jobject obj1, jobject obj2);
    jobject (JNICALL *NewLocalRef)
      (JNIEnv *env, jobject ref);
    jint (JNICALL *EnsureLocalCapacity)
      (JNIEnv *env, jint capacity);

    jobject (JNICALL *AllocObject)
      (JNIEnv *env, jclass clazz);
    jobject (JNICALL *NewObject)
      (JNIEnv *env, jclass clazz, jmethodID methodID, ...);
    jobject (JNICALL *NewObjectV)
      (JNIEnv *env, jclass clazz, jmethodID methodID, va_list args);
    jobject (JNICALL *NewObjectA)
      (JNIEnv *env, jclass clazz, jmethodID methodID, const jvalue *args);

    jclass (JNICALL *GetObjectClass)
      (JNIEnv *env, jobject obj);
    jboolean (JNICALL *IsInstanceOf)
      (JNIEnv *env, jobject obj, jclass clazz);

    jmethodID (JNICALL *GetMethodID)
      (JNIEnv *env, jclass clazz, const char *name, const char *sig);

    jobject (JNICALL *CallObjectMethod)
      (JNIEnv *env, jobject obj, jmethodID methodID, ...);
    jobject (JNICALL *CallObjectMethodV)
      (JNIEnv *env, jobject obj, jmethodID methodID, va_list args);
    jobject (JNICALL *CallObjectMethodA)
      (JNIEnv *env, jobject obj, jmethodID methodID, const jvalue * args);

    jboolean (JNICALL *CallBooleanMethod)
      (JNIEnv *env, jobject obj, jmethodID methodID, ...);
    jboolean (JNICALL *CallBooleanMethodV)
      (JNIEnv *env, jobject obj, jmethodID methodID, va_list args);
    jboolean (JNICALL *CallBooleanMethodA)
      (JNIEnv *env, jobject obj, jmethodID methodID, const jvalue * args);

    jbyte (JNICALL *CallByteMethod)
      (JNIEnv *env, jobject obj, jmethodID methodID, ...);
    jbyte (JNICALL *CallByteMethodV)
      (JNIEnv *env, jobject obj, jmethodID methodID, va_list args);
    jbyte (JNICALL *CallByteMethodA)
      (JNIEnv *env, jobject obj, jmethodID methodID, const jvalue *args);

    jchar (JNICALL *CallCharMethod)
      (JNIEnv *env, jobject obj, jmethodID methodID, ...);
    jchar (JNICALL *CallCharMethodV)
      (JNIEnv *env, jobject obj, jmethodID methodID, va_list args);
    jchar (JNICALL *CallCharMethodA)
      (JNIEnv *env, jobject obj, jmethodID methodID, const jvalue *args);

    jshort (JNICALL *CallShortMethod)
      (JNIEnv *env, jobject obj, jmethodID methodID, ...);
    jshort (JNICALL *CallShortMethodV)
      (JNIEnv *env, jobject obj, jmethodID methodID, va_list args);
    jshort (JNICALL *CallShortMethodA)
      (JNIEnv *env, jobject obj, jmethodID methodID, const jvalue *args);

    jint (JNICALL *CallIntMethod)
      (JNIEnv *env, jobject obj, jmethodID methodID, ...);
    jint (JNICALL *CallIntMethodV)
      (JNIEnv *env, jobject obj, jmethodID methodID, va_list args);
    jint (JNICALL *CallIntMethodA)
      (JNIEnv *env, jobject obj, jmethodID methodID, const jvalue *args);

    jlong (JNICALL *CallLongMethod)
      (JNIEnv *env, jobject obj, jmethodID methodID, ...);
    jlong (JNICALL *CallLongMethodV)
      (JNIEnv *env, jobject obj, jmethodID methodID, va_list args);
    jlong (JNICALL *CallLongMethodA)
      (JNIEnv *env, jobject obj, jmethodID methodID, const jvalue *args);

    jfloat (JNICALL *CallFloatMethod)
      (JNIEnv *env, jobject obj, jmethodID methodID, ...);
    jfloat (JNICALL *CallFloatMethodV)
      (JNIEnv *env, jobject obj, jmethodID methodID, va_list args);
    jfloat (JNICALL *CallFloatMethodA)
      (JNIEnv *env, jobject obj, jmethodID methodID, const jvalue *args);

    jdouble (JNICALL *CallDoubleMethod)
      (JNIEnv *env, jobject obj, jmethodID methodID, ...);
    jdouble (JNICALL *CallDoubleMethodV)
      (JNIEnv *env, jobject obj, jmethodID methodID, va_list args);
    jdouble (JNICALL *CallDoubleMethodA)
      (JNIEnv *env, jobject obj, jmethodID methodID, const jvalue *args);

    void (JNICALL *CallVoidMethod)
      (JNIEnv *env, jobject obj, jmethodID methodID, ...);
    void (JNICALL *CallVoidMethodV)
      (JNIEnv *env, jobject obj, jmethodID methodID, va_list args);
    void (JNICALL *CallVoidMethodA)
      (JNIEnv *env, jobject obj, jmethodID methodID, const jvalue * args);

    jobject (JNICALL *CallNonvirtualObjectMethod)
      (JNIEnv *env, jobject obj, jclass clazz, jmethodID methodID, ...);
    jobject (JNICALL *CallNonvirtualObjectMethodV)
      (JNIEnv *env, jobject obj, jclass clazz, jmethodID methodID,
       va_list args);
    jobject (JNICALL *CallNonvirtualObjectMethodA)
      (JNIEnv *env, jobject obj, jclass clazz, jmethodID methodID,
       const jvalue * args);

    jboolean (JNICALL *CallNonvirtualBooleanMethod)
      (JNIEnv *env, jobject obj, jclass clazz, jmethodID methodID, ...);
    jboolean (JNICALL *CallNonvirtualBooleanMethodV)
      (JNIEnv *env, jobject obj, jclass clazz, jmethodID methodID,
       va_list args);
    jboolean (JNICALL *CallNonvirtualBooleanMethodA)
      (JNIEnv *env, jobject obj, jclass clazz, jmethodID methodID,
       const jvalue * args);

    jbyte (JNICALL *CallNonvirtualByteMethod)
      (JNIEnv *env, jobject obj, jclass clazz, jmethodID methodID, ...);
    jbyte (JNICALL *CallNonvirtualByteMethodV)
      (JNIEnv *env, jobject obj, jclass clazz, jmethodID methodID,
       va_list args);
    jbyte (JNICALL *CallNonvirtualByteMethodA)
      (JNIEnv *env, jobject obj, jclass clazz, jmethodID methodID,
       const jvalue *args);

    jchar (JNICALL *CallNonvirtualCharMethod)
      (JNIEnv *env, jobject obj, jclass clazz, jmethodID methodID, ...);
    jchar (JNICALL *CallNonvirtualCharMethodV)
      (JNIEnv *env, jobject obj, jclass clazz, jmethodID methodID,
       va_list args);
    jchar (JNICALL *CallNonvirtualCharMethodA)
      (JNIEnv *env, jobject obj, jclass clazz, jmethodID methodID,
       const jvalue *args);

    jshort (JNICALL *CallNonvirtualShortMethod)
      (JNIEnv *env, jobject obj, jclass clazz, jmethodID methodID, ...);
    jshort (JNICALL *CallNonvirtualShortMethodV)
      (JNIEnv *env, jobject obj, jclass clazz, jmethodID methodID,
       va_list args);
    jshort (JNICALL *CallNonvirtualShortMethodA)
      (JNIEnv *env, jobject obj, jclass clazz, jmethodID methodID,
       const jvalue *args);

    jint (JNICALL *CallNonvirtualIntMethod)
      (JNIEnv *env, jobject obj, jclass clazz, jmethodID methodID, ...);
    jint (JNICALL *CallNonvirtualIntMethodV)
      (JNIEnv *env, jobject obj, jclass clazz, jmethodID methodID,
       va_list args);
    jint (JNICALL *CallNonvirtualIntMethodA)
      (JNIEnv *env, jobject obj, jclass clazz, jmethodID methodID,
       const jvalue *args);

    jlong (JNICALL *CallNonvirtualLongMethod)
      (JNIEnv *env, jobject obj, jclass clazz, jmethodID methodID, ...);
    jlong (JNICALL *CallNonvirtualLongMethodV)
      (JNIEnv *env, jobject obj, jclass clazz, jmethodID methodID,
       va_list args);
    jlong (JNICALL *CallNonvirtualLongMethodA)
      (JNIEnv *env, jobject obj, jclass clazz, jmethodID methodID,
       const jvalue *args);

    jfloat (JNICALL *CallNonvirtualFloatMethod)
      (JNIEnv *env, jobject obj, jclass clazz, jmethodID methodID, ...);
    jfloat (JNICALL *CallNonvirtualFloatMethodV)
      (JNIEnv *env, jobject obj, jclass clazz, jmethodID methodID,
       va_list args);
    jfloat (JNICALL *CallNonvirtualFloatMethodA)
      (JNIEnv *env, jobject obj, jclass clazz, jmethodID methodID,
       const jvalue *args);

    jdouble (JNICALL *CallNonvirtualDoubleMethod)
      (JNIEnv *env, jobject obj, jclass clazz, jmethodID methodID, ...);
    jdouble (JNICALL *CallNonvirtualDoubleMethodV)
      (JNIEnv *env, jobject obj, jclass clazz, jmethodID methodID,
       va_list args);
    jdouble (JNICALL *CallNonvirtualDoubleMethodA)
      (JNIEnv *env, jobject obj, jclass clazz, jmethodID methodID,
       const jvalue *args);

    void (JNICALL *CallNonvirtualVoidMethod)
      (JNIEnv *env, jobject obj, jclass clazz, jmethodID methodID, ...);
    void (JNICALL *CallNonvirtualVoidMethodV)
      (JNIEnv *env, jobject obj, jclass clazz, jmethodID methodID,
       va_list args);
    void (JNICALL *CallNonvirtualVoidMethodA)
      (JNIEnv *env, jobject obj, jclass clazz, jmethodID methodID,
       const jvalue * args);

    jfieldID (JNICALL *GetFieldID)
      (JNIEnv *env, jclass clazz, const char *name, const char *sig);

    jobject (JNICALL *GetObjectField)
      (JNIEnv *env, jobject obj, jfieldID fieldID);
    jboolean (JNICALL *GetBooleanField)
      (JNIEnv *env, jobject obj, jfieldID fieldID);
    jbyte (JNICALL *GetByteField)
      (JNIEnv *env, jobject obj, jfieldID fieldID);
    jchar (JNICALL *GetCharField)
      (JNIEnv *env, jobject obj, jfieldID fieldID);
    jshort (JNICALL *GetShortField)
      (JNIEnv *env, jobject obj, jfieldID fieldID);
    jint (JNICALL *GetIntField)
      (JNIEnv *env, jobject obj, jfieldID fieldID);
    jlong (JNICALL *GetLongField)
      (JNIEnv *env, jobject obj, jfieldID fieldID);
    jfloat (JNICALL *GetFloatField)
      (JNIEnv *env, jobject obj, jfieldID fieldID);
    jdouble (JNICALL *GetDoubleField)
      (JNIEnv *env, jobject obj, jfieldID fieldID);

    void (JNICALL *SetObjectField)
      (JNIEnv *env, jobject obj, jfieldID fieldID, jobject val);
    void (JNICALL *SetBooleanField)
      (JNIEnv *env, jobject obj, jfieldID fieldID, jboolean val);
    void (JNICALL *SetByteField)
      (JNIEnv *env, jobject obj, jfieldID fieldID, jbyte val);
    void (JNICALL *SetCharField)
      (JNIEnv *env, jobject obj, jfieldID fieldID, jchar val);
    void (JNICALL *SetShortField)
      (JNIEnv *env, jobject obj, jfieldID fieldID, jshort val);
    void (JNICALL *SetIntField)
      (JNIEnv *env, jobject obj, jfieldID fieldID, jint val);
    void (JNICALL *SetLongField)
      (JNIEnv *env, jobject obj, jfieldID fieldID, jlong val);
    void (JNICALL *SetFloatField)
      (JNIEnv *env, jobject obj, jfieldID fieldID, jfloat val);
    void (JNICALL *SetDoubleField)
      (JNIEnv *env, jobject obj, jfieldID fieldID, jdouble val);

    jmethodID (JNICALL *GetStaticMethodID)
      (JNIEnv *env, jclass clazz, const char *name, const char *sig);

    jobject (JNICALL *CallStaticObjectMethod)
      (JNIEnv *env, jclass clazz, jmethodID methodID, ...);
    jobject (JNICALL *CallStaticObjectMethodV)
      (JNIEnv *env, jclass clazz, jmethodID methodID, va_list args);
    jobject (JNICALL *CallStaticObjectMethodA)
      (JNIEnv *env, jclass clazz, jmethodID methodID, const jvalue *args);

    jboolean (JNICALL *CallStaticBooleanMethod)
      (JNIEnv *env, jclass clazz, jmethodID methodID, ...);
    jboolean (JNICALL *CallStaticBooleanMethodV)
      (JNIEnv *env, jclass clazz, jmethodID methodID, va_list args);
    jboolean (JNICALL *CallStaticBooleanMethodA)
      (JNIEnv *env, jclass clazz, jmethodID methodID, const jvalue *args);

    jbyte (JNICALL *CallStaticByteMethod)
      (JNIEnv *env, jclass clazz, jmethodID methodID, ...);
    jbyte (JNICALL *CallStaticByteMethodV)
      (JNIEnv *env, jclass clazz, jmethodID methodID, va_list args);
    jbyte (JNICALL *CallStaticByteMethodA)
      (JNIEnv *env, jclass clazz, jmethodID methodID, const jvalue *args);

    jchar (JNICALL *CallStaticCharMethod)
      (JNIEnv *env, jclass clazz, jmethodID methodID, ...);
    jchar (JNICALL *CallStaticCharMethodV)
      (JNIEnv *env, jclass clazz, jmethodID methodID, va_list args);
    jchar (JNICALL *CallStaticCharMethodA)
      (JNIEnv *env, jclass clazz, jmethodID methodID, const jvalue *args);

    jshort (JNICALL *CallStaticShortMethod)
      (JNIEnv *env, jclass clazz, jmethodID methodID, ...);
    jshort (JNICALL *CallStaticShortMethodV)
      (JNIEnv *env, jclass clazz, jmethodID methodID, va_list args);
    jshort (JNICALL *CallStaticShortMethodA)
      (JNIEnv *env, jclass clazz, jmethodID methodID, const jvalue *args);

    jint (JNICALL *CallStaticIntMethod)
      (JNIEnv *env, jclass clazz, jmethodID methodID, ...);
    jint (JNICALL *CallStaticIntMethodV)
      (JNIEnv *env, jclass clazz, jmethodID methodID, va_list args);
    jint (JNICALL *CallStaticIntMethodA)
      (JNIEnv *env, jclass clazz, jmethodID methodID, const jvalue *args);

    jlong (JNICALL *CallStaticLongMethod)
      (JNIEnv *env, jclass clazz, jmethodID methodID, ...);
    jlong (JNICALL *CallStaticLongMethodV)
      (JNIEnv *env, jclass clazz, jmethodID methodID, va_list args);
    jlong (JNICALL *CallStaticLongMethodA)
      (JNIEnv *env, jclass clazz, jmethodID methodID, const jvalue *args);

    jfloat (JNICALL *CallStaticFloatMethod)
      (JNIEnv *env, jclass clazz, jmethodID methodID, ...);
    jfloat (JNICALL *CallStaticFloatMethodV)
      (JNIEnv *env, jclass clazz, jmethodID methodID, va_list args);
    jfloat (JNICALL *CallStaticFloatMethodA)
      (JNIEnv *env, jclass clazz, jmethodID methodID, const jvalue *args);

    jdouble (JNICALL *CallStaticDoubleMethod)
      (JNIEnv *env, jclass clazz, jmethodID methodID, ...);
    jdouble (JNICALL *CallStaticDoubleMethodV)
      (JNIEnv *env, jclass clazz, jmethodID methodID, va_list args);
    jdouble (JNICALL *CallStaticDoubleMethodA)
      (JNIEnv *env, jclass clazz, jmethodID methodID, const jvalue *args);

    void (JNICALL *CallStaticVoidMethod)
      (JNIEnv *env, jclass cls, jmethodID methodID, ...);
    void (JNICALL *CallStaticVoidMethodV)
      (JNIEnv *env, jclass cls, jmethodID methodID, va_list args);
    void (JNICALL *CallStaticVoidMethodA)
      (JNIEnv *env, jclass cls, jmethodID methodID, const jvalue * args);

    jfieldID (JNICALL *GetStaticFieldID)
      (JNIEnv *env, jclass clazz, const char *name, const char *sig);
    jobject (JNICALL *GetStaticObjectField)
      (JNIEnv *env, jclass clazz, jfieldID fieldID);
    jboolean (JNICALL *GetStaticBooleanField)
      (JNIEnv *env, jclass clazz, jfieldID fieldID);
    jbyte (JNICALL *GetStaticByteField)
      (JNIEnv *env, jclass clazz, jfieldID fieldID);
    jchar (JNICALL *GetStaticCharField)
      (JNIEnv *env, jclass clazz, jfieldID fieldID);
    jshort (JNICALL *GetStaticShortField)
      (JNIEnv *env, jclass clazz, jfieldID fieldID);
    jint (JNICALL *GetStaticIntField)
      (JNIEnv *env, jclass clazz, jfieldID fieldID);
    jlong (JNICALL *GetStaticLongField)
      (JNIEnv *env, jclass clazz, jfieldID fieldID);
    jfloat (JNICALL *GetStaticFloatField)
      (JNIEnv *env, jclass clazz, jfieldID fieldID);
    jdouble (JNICALL *GetStaticDoubleField)
      (JNIEnv *env, jclass clazz, jfieldID fieldID);

    void (JNICALL *SetStaticObjectField)
      (JNIEnv *env, jclass clazz, jfieldID fieldID, jobject value);
    void (JNICALL *SetStaticBooleanField)
      (JNIEnv *env, jclass clazz, jfieldID fieldID, jboolean value);
    void (JNICALL *SetStaticByteField)
      (JNIEnv *env, jclass clazz, jfieldID fieldID, jbyte value);
    void (JNICALL *SetStaticCharField)
      (JNIEnv *env, jclass clazz, jfieldID fieldID, jchar value);
    void (JNICALL *SetStaticShortField)
      (JNIEnv *env, jclass clazz, jfieldID fieldID, jshort value);
    void (JNICALL *SetStaticIntField)
      (JNIEnv *env, jclass clazz, jfieldID fieldID, jint value);
    void (JNICALL *SetStaticLongField)
      (JNIEnv *env, jclass clazz, jfieldID fieldID, jlong value);
    void (JNICALL *SetStaticFloatField)
      (JNIEnv *env, jclass clazz, jfieldID fieldID, jfloat value);
    void (JNICALL *SetStaticDoubleField)
      (JNIEnv *env, jclass clazz, jfieldID fieldID, jdouble value);

    jstring (JNICALL *NewString)
      (JNIEnv *env, const jchar *unicode, jsize len);
    jsize (JNICALL *GetStringLength)
      (JNIEnv *env, jstring str);
    const jchar *(JNICALL *GetStringChars)
      (JNIEnv *env, jstring str, jboolean *isCopy);
    void (JNICALL *ReleaseStringChars)
      (JNIEnv *env, jstring str, const jchar *chars);

    jstring (JNICALL *NewStringUTF)
      (JNIEnv *env, const char *utf);
    jsize (JNICALL *GetStringUTFLength)
      (JNIEnv *env, jstring str);
    const char* (JNICALL *GetStringUTFChars)
      (JNIEnv *env, jstring str, jboolean *isCopy);
    void (JNICALL *ReleaseStringUTFChars)
      (JNIEnv *env, jstring str, const char* chars);


    jsize (JNICALL *GetArrayLength)
      (JNIEnv *env, jarray array);

    jobjectArray (JNICALL *NewObjectArray)
      (JNIEnv *env, jsize len, jclass clazz, jobject init);
    jobject (JNICALL *GetObjectArrayElement)
      (JNIEnv *env, jobjectArray array, jsize index);
    void (JNICALL *SetObjectArrayElement)
      (JNIEnv *env, jobjectArray array, jsize index, jobject val);

    jbooleanArray (JNICALL *NewBooleanArray)
      (JNIEnv *env, jsize len);
    jbyteArray (JNICALL *NewByteArray)
      (JNIEnv *env, jsize len);
    jcharArray (JNICALL *NewCharArray)
      (JNIEnv *env, jsize len);
    jshortArray (JNICALL *NewShortArray)
      (JNIEnv *env, jsize len);
    jintArray (JNICALL *NewIntArray)
      (JNIEnv *env, jsize len);
    jlongArray (JNICALL *NewLongArray)
      (JNIEnv *env, jsize len);
    jfloatArray (JNICALL *NewFloatArray)
      (JNIEnv *env, jsize len);
    jdoubleArray (JNICALL *NewDoubleArray)
      (JNIEnv *env, jsize len);

    jboolean * (JNICALL *GetBooleanArrayElements)
      (JNIEnv *env, jbooleanArray array, jboolean *isCopy);
    jbyte * (JNICALL *GetByteArrayElements)
      (JNIEnv *env, jbyteArray array, jboolean *isCopy);
    jchar * (JNICALL *GetCharArrayElements)
      (JNIEnv *env, jcharArray array, jboolean *isCopy);
    jshort * (JNICALL *GetShortArrayElements)
      (JNIEnv *env, jshortArray array, jboolean *isCopy);
    jint * (JNICALL *GetIntArrayElements)
      (JNIEnv *env, jintArray array, jboolean *isCopy);
    jlong * (JNICALL *GetLongArrayElements)
      (JNIEnv *env, jlongArray array, jboolean *isCopy);
    jfloat * (JNICALL *GetFloatArrayElements)
      (JNIEnv *env, jfloatArray array, jboolean *isCopy);
    jdouble * (JNICALL *GetDoubleArrayElements)
      (JNIEnv *env, jdoubleArray array, jboolean *isCopy);

    void (JNICALL *ReleaseBooleanArrayElements)
      (JNIEnv *env, jbooleanArray array, jboolean *elems, jint mode);
    void (JNICALL *ReleaseByteArrayElements)
      (JNIEnv *env, jbyteArray array, jbyte *elems, jint mode);
    void (JNICALL *ReleaseCharArrayElements)
      (JNIEnv *env, jcharArray array, jchar *elems, jint mode);
    void (JNICALL *ReleaseShortArrayElements)
      (JNIEnv *env, jshortArray array, jshort *elems, jint mode);
    void (JNICALL *ReleaseIntArrayElements)
      (JNIEnv *env, jintArray array, jint *elems, jint mode);
    void (JNICALL *ReleaseLongArrayElements)
      (JNIEnv *env, jlongArray array, jlong *elems, jint mode);
    void (JNICALL *ReleaseFloatArrayElements)
      (JNIEnv *env, jfloatArray array, jfloat *elems, jint mode);
    void (JNICALL *ReleaseDoubleArrayElements)
      (JNIEnv *env, jdoubleArray array, jdouble *elems, jint mode);

    void (JNICALL *GetBooleanArrayRegion)
      (JNIEnv *env, jbooleanArray array, jsize start, jsize l, jboolean *buf);
    void (JNICALL *GetByteArrayRegion)
      (JNIEnv *env, jbyteArray array, jsize start, jsize len, jbyte *buf);
    void (JNICALL *GetCharArrayRegion)
      (JNIEnv *env, jcharArray array, jsize start, jsize len, jchar *buf);
    void (JNICALL *GetShortArrayRegion)
      (JNIEnv *env, jshortArray array, jsize start, jsize len, jshort *buf);
    void (JNICALL *GetIntArrayRegion)
      (JNIEnv *env, jintArray array, jsize start, jsize len, jint *buf);
    void (JNICALL *GetLongArrayRegion)
      (JNIEnv *env, jlongArray array, jsize start, jsize len, jlong *buf);
    void (JNICALL *GetFloatArrayRegion)
      (JNIEnv *env, jfloatArray array, jsize start, jsize len, jfloat *buf);
    void (JNICALL *GetDoubleArrayRegion)
      (JNIEnv *env, jdoubleArray array, jsize start, jsize len, jdouble *buf);

    void (JNICALL *SetBooleanArrayRegion)
      (JNIEnv *env, jbooleanArray array, jsize start, jsize l, const jboolean *buf);
    void (JNICALL *SetByteArrayRegion)
      (JNIEnv *env, jbyteArray array, jsize start, jsize len, const jbyte *buf);
    void (JNICALL *SetCharArrayRegion)
      (JNIEnv *env, jcharArray array, jsize start, jsize len, const jchar *buf);
    void (JNICALL *SetShortArrayRegion)
      (JNIEnv *env, jshortArray array, jsize start, jsize len, const jshort *buf);
    void (JNICALL *SetIntArrayRegion)
      (JNIEnv *env, jintArray array, jsize start, jsize len, const jint *buf);
    void (JNICALL *SetLongArrayRegion)
      (JNIEnv *env, jlongArray array, jsize start, jsize len, const jlong *buf);
    void (JNICALL *SetFloatArrayRegion)
      (JNIEnv *env, jfloatArray array, jsize start, jsize len, const jfloat *buf);
    void (JNICALL *SetDoubleArrayRegion)
      (JNIEnv *env, jdoubleArray array, jsize start, jsize len, const jdouble *buf);

    jint (JNICALL *RegisterNatives)
      (JNIEnv *env, jclass clazz, const JNINativeMethod *methods,
       jint nMethods);
    jint (JNICALL *UnregisterNatives)
      (JNIEnv *env, jclass clazz);

    jint (JNICALL *MonitorEnter)
      (JNIEnv *env, jobject obj);
    jint (JNICALL *MonitorExit)
      (JNIEnv *env, jobject obj);

    jint (JNICALL *GetJavaVM)
      (JNIEnv *env, JavaVM **vm);

    void (JNICALL *GetStringRegion)
      (JNIEnv *env, jstring str, jsize start, jsize len, jchar *buf);
    void (JNICALL *GetStringUTFRegion)
      (JNIEnv *env, jstring str, jsize start, jsize len, char *buf);

    void * (JNICALL *GetPrimitiveArrayCritical)
      (JNIEnv *env, jarray array, jboolean *isCopy);
    void (JNICALL *ReleasePrimitiveArrayCritical)
      (JNIEnv *env, jarray array, void *carray, jint mode);

    const jchar * (JNICALL *GetStringCritical)
      (JNIEnv *env, jstring string, jboolean *isCopy);
    void (JNICALL *ReleaseStringCritical)
      (JNIEnv *env, jstring string, const jchar *cstring);

    jweak (JNICALL *NewWeakGlobalRef)
       (JNIEnv *env, jobject obj);
    void (JNICALL *DeleteWeakGlobalRef)
       (JNIEnv *env, jweak ref);

    jboolean (JNICALL *ExceptionCheck)
       (JNIEnv *env);

    jobject (JNICALL *NewDirectByteBuffer)
       (JNIEnv* env, void* address, jlong capacity);
    void* (JNICALL *GetDirectBufferAddress)
       (JNIEnv* env, jobject buf);
    jlong (JNICALL *GetDirectBufferCapacity)
       (JNIEnv* env, jobject buf);

    /* New JNI 1.6 Features */

    jobjectRefType (JNICALL *GetObjectRefType)
        (JNIEnv* env, jobject obj);

    /* Module Features */

    jobject (JNICALL *GetModule)
       (JNIEnv* env, jclass clazz);
};

/*
 * We use inlined functions for C++ so that programmers can write:
 *
 *    env->FindClass("java/lang/String")
 *
 * in C++ rather than:
 *
 *    (*env)->FindClass(env, "java/lang/String")
 *
 * in C.
 */

struct JNIEnv_ {
    const struct JNINativeInterface_ *functions;
#ifdef __cplusplus

    jint GetVersion() {
        return functions->GetVersion(this);
    }
    jclass DefineClass(const char *name, jobject loader, const jbyte *buf,
                       jsize len) {
        return functions->DefineClass(this, name, loader, buf, len);
    }
    jclass FindClass(const char *name) {
        return functions->FindClass(this, name);
    }
    jmethodID FromReflectedMethod(jobject method) {
        return functions->FromReflectedMethod(this,method);
    }
    jfieldID FromReflectedField(jobject field) {
        return functions->FromReflectedField(this,field);
    }

    jobject ToReflectedMethod(jclass cls, jmethodID methodID, jboolean isStatic) {
        return functions->ToReflectedMethod(this, cls, methodID, isStatic);
    }

    jclass GetSuperclass(jclass sub) {
        return functions->GetSuperclass(this, sub);
    }
    jboolean IsAssignableFrom(jclass sub, jclass sup) {
        return functions->IsAssignableFrom(this, sub, sup);
    }

    jobject ToReflectedField(jclass cls, jfieldID fieldID, jboolean isStatic) {
        return functions->ToReflectedField(this,cls,fieldID,isStatic);
    }

    jint Throw(jthrowable obj) {
        return functions->Throw(this, obj);
    }
    jint ThrowNew(jclass clazz, const char *msg) {
        return functions->ThrowNew(this, clazz, msg);
    }
    jthrowable ExceptionOccurred() {
        return functions->ExceptionOccurred(this);
    }
    void ExceptionDescribe() {
        functions->ExceptionDescribe(this);
    }
    void ExceptionClear() {
        functions->ExceptionClear(this);
    }
    void FatalError(const char *msg) {
        functions->FatalError(this, msg);
    }

    jint PushLocalFrame(jint capacity) {
        return functions->PushLocalFrame(this,capacity);
    }
    jobject PopLocalFrame(jobject result) {
        return functions->PopLocalFrame(this,result);
    }

    jobject NewGlobalRef(jobject lobj) {
        return functions->NewGlobalRef(this,lobj);
    }
    void DeleteGlobalRef(jobject gref) {
        functions->DeleteGlobalRef(this,gref);
    }
    void DeleteLocalRef(jobject obj) {
        functions->DeleteLocalRef(this, obj);
    }

    jboolean IsSameObject(jobject obj1, jobject obj2) {
        return functions->IsSameObject(this,obj1,obj2);
    }

    jobject NewLocalRef(jobject ref) {
        return functions->NewLocalRef(this,ref);
    }
    jint EnsureLocalCapacity(jint capacity) {
        return functions->EnsureLocalCapacity(this,capacity);
    }

    jobject AllocObject(jclass clazz) {
        return functions->AllocObject(this,clazz);
    }
    jobject NewObject(jclass clazz, jmethodID methodID, ...) {
        va_list args;
        jobject result;
        va_start(args, methodID);
        result = functions->NewObjectV(this,clazz,methodID,args);
        va_end(args);
        return result;
    }
    jobject NewObjectV(jclass clazz, jmethodID methodID,
                       va_list args) {
        return functions->NewObjectV(this,clazz,methodID,args);
    }
    jobject NewObjectA(jclass clazz, jmethodID methodID,
                       const jvalue *args) {
        return functions->NewObjectA(this,clazz,methodID,args);
    }

    jclass GetObjectClass(jobject obj) {
        return functions->GetObjectClass(this,obj);
    }
    jboolean IsInstanceOf(jobject obj, jclass clazz) {
        return functions->IsInstanceOf(this,obj,clazz);
    }

    jmethodID GetMethodID(jclass clazz, const char *name,
                          const char *sig) {
        return functions->GetMethodID(this,clazz,name,sig);
    }

    jobject CallObjectMethod(jobject obj, jmethodID methodID, ...) {
        va_list args;
        jobject result;
        va_start(args,methodID);
        result = functions->CallObjectMethodV(this,obj,methodID,args);
        va_end(args);
        return result;
    }
    jobject CallObjectMethodV(jobject obj, jmethodID methodID,
                        va_list args) {
        return functions->CallObjectMethodV(this,obj,methodID,args);
    }
    jobject CallObjectMethodA(jobject obj, jmethodID methodID,
                        const jvalue * args) {
        return functions->CallObjectMethodA(this,obj,methodID,args);
    }

    jboolean CallBooleanMethod(jobject obj,
                               jmethodID methodID, ...) {
        va_list args;
        jboolean result;
        va_start(args,methodID);
        result = functions->CallBooleanMethodV(this,obj,methodID,args);
        va_end(args);
        return result;
    }
    jboolean CallBooleanMethodV(jobject obj, jmethodID methodID,
                                va_list args) {
        return functions->CallBooleanMethodV(this,obj,methodID,args);
    }
    jboolean CallBooleanMethodA(jobject obj, jmethodID methodID,
                                const jvalue * args) {
        return functions->CallBooleanMethodA(this,obj,methodID, args);
    }

    jbyte CallByteMethod(jobject obj, jmethodID methodID, ...) {
        va_list args;
        jbyte result;
        va_start(args,methodID);
        result = functions->CallByteMethodV(this,obj,methodID,args);
        va_end(args);
        return result;
    }
    jbyte CallByteMethodV(jobject obj, jmethodID methodID,
                          va_list args) {
        return functions->CallByteMethodV(this,obj,methodID,args);
    }
    jbyte CallByteMethodA(jobject obj, jmethodID methodID,
                          const jvalue * args) {
        return functions->CallByteMethodA(this,obj,methodID,args);
    }

    jchar CallCharMethod(jobject obj, jmethodID methodID, ...) {
        va_list args;
        jchar result;
        va_start(args,methodID);
        result = functions->CallCharMethodV(this,obj,methodID,args);
        va_end(args);
        return result;
    }
    jchar CallCharMethodV(jobject obj, jmethodID methodID,
                          va_list args) {
        return functions->CallCharMethodV(this,obj,methodID,args);
    }
    jchar CallCharMethodA(jobject obj, jmethodID methodID,
                          const jvalue * args) {
        return functions->CallCharMethodA(this,obj,methodID,args);
    }

    jshort CallShortMethod(jobject obj, jmethodID methodID, ...) {
        va_list args;
        jshort result;
        va_start(args,methodID);
        result = functions->CallShortMethodV(this,obj,methodID,args);
        va_end(args);
        return result;
    }
    jshort CallShortMethodV(jobject obj, jmethodID methodID,
                            va_list args) {
        return functions->CallShortMethodV(this,obj,methodID,args);
    }
    jshort CallShortMethodA(jobject obj, jmethodID methodID,
                            const jvalue * args) {
        return functions->CallShortMethodA(this,obj,methodID,args);
    }

    jint CallIntMethod(jobject obj, jmethodID methodID, ...) {
        va_list args;
        jint result;
        va_start(args,methodID);
        result = functions->CallIntMethodV(this,obj,methodID,args);
        va_end(args);
        return result;
    }
    jint CallIntMethodV(jobject obj, jmethodID methodID,
                        va_list args) {
        return functions->CallIntMethodV(this,obj,methodID,args);
    }
    jint CallIntMethodA(jobject obj, jmethodID methodID,
                        const jvalue * args) {
        return functions->CallIntMethodA(this,obj,methodID,args);
    }

    jlong CallLongMethod(jobject obj, jmethodID methodID, ...) {
        va_list args;
        jlong result;
        va_start(args,methodID);
        result = functions->CallLongMethodV(this,obj,methodID,args);
        va_end(args);
        return result;
    }
    jlong CallLongMethodV(jobject obj, jmethodID methodID,
                          va_list args) {
        return functions->CallLongMethodV(this,obj,methodID,args);
    }
    jlong CallLongMethodA(jobject obj, jmethodID methodID,
                          const jvalue * args) {
        return functions->CallLongMethodA(this,obj,methodID,args);
    }

    jfloat CallFloatMethod(jobject obj, jmethodID methodID, ...) {
        va_list args;
        jfloat result;
        va_start(args,methodID);
        result = functions->CallFloatMethodV(this,obj,methodID,args);
        va_end(args);
        return result;
    }
    jfloat CallFloatMethodV(jobject obj, jmethodID methodID,
                            va_list args) {
        return functions->CallFloatMethodV(this,obj,methodID,args);
    }
    jfloat CallFloatMethodA(jobject obj, jmethodID methodID,
                            const jvalue * args) {
        return functions->CallFloatMethodA(this,obj,methodID,args);
    }

    jdouble CallDoubleMethod(jobject obj, jmethodID methodID, ...) {
        va_list args;
        jdouble result;
        va_start(args,methodID);
        result = functions->CallDoubleMethodV(this,obj,methodID,args);
        va_end(args);
        return result;
    }
    jdouble CallDoubleMethodV(jobject obj, jmethodID methodID,
                        va_list args) {
        return functions->CallDoubleMethodV(this,obj,methodID,args);
    }
    jdouble CallDoubleMethodA(jobject obj, jmethodID methodID,
                        const jvalue * args) {
        return functions->CallDoubleMethodA(this,obj,methodID,args);
    }

    void CallVoidMethod(jobject obj, jmethodID methodID, ...) {
        va_list args;
        va_start(args,methodID);
        functions->CallVoidMethodV(this,obj,methodID,args);
        va_end(args);
    }
    void CallVoidMethodV(jobject obj, jmethodID methodID,
                         va_list args) {
        functions->CallVoidMethodV(this,obj,methodID,args);
    }
    void CallVoidMethodA(jobject obj, jmethodID methodID,
                         const jvalue * args) {
        functions->CallVoidMethodA(this,obj,methodID,args);
    }

    jobject CallNonvirtualObjectMethod(jobject obj, jclass clazz,
                                       jmethodID methodID, ...) {
        va_list args;
        jobject result;
        va_start(args,methodID);
        result = functions->CallNonvirtualObjectMethodV(this,obj,clazz,
                                                        methodID,args);
        va_end(args);
        return result;
    }
    jobject CallNonvirtualObjectMethodV(jobject obj, jclass clazz,
                                        jmethodID methodID, va_list args) {
        return functions->CallNonvirtualObjectMethodV(this,obj,clazz,
                                                      methodID,args);
    }
    jobject CallNonvirtualObjectMethodA(jobject obj, jclass clazz,
                                        jmethodID methodID, const jvalue * args) {
        return functions->CallNonvirtualObjectMethodA(this,obj,clazz,
                                                      methodID,args);
    }

    jboolean CallNonvirtualBooleanMethod(jobject obj, jclass clazz,
                                         jmethodID methodID, ...) {
        va_list args;
        jboolean result;
        va_start(args,methodID);
        result = functions->CallNonvirtualBooleanMethodV(this,obj,clazz,
                                                         methodID,args);
        va_end(args);
        return result;
    }
    jboolean CallNonvirtualBooleanMethodV(jobject obj, jclass clazz,
                                          jmethodID methodID, va_list args) {
        return functions->CallNonvirtualBooleanMethodV(this,obj,clazz,
                                                       methodID,args);
    }
    jboolean CallNonvirtualBooleanMethodA(jobject obj, jclass clazz,
                                          jmethodID methodID, const jvalue * args) {
        return functions->CallNonvirtualBooleanMethodA(this,obj,clazz,
                                                       methodID, args);
    }

    jbyte CallNonvirtualByteMethod(jobject obj, jclass clazz,
                                   jmethodID methodID, ...) {
        va_list args;
        jbyte result;
        va_start(args,methodID);
        result = functions->CallNonvirtualByteMethodV(this,obj,clazz,
                                                      methodID,args);
        va_end(args);
        return result;
    }
    jbyte CallNonvirtualByteMethodV(jobject obj, jclass clazz,
                                    jmethodID methodID, va_list args) {
        return functions->CallNonvirtualByteMethodV(this,obj,clazz,
                                                    methodID,args);
    }
    jbyte CallNonvirtualByteMethodA(jobject obj, jclass clazz,
                                    jmethodID methodID, const jvalue * args) {
        return functions->CallNonvirtualByteMethodA(this,obj,clazz,
                                                    methodID,args);
    }

    jchar CallNonvirtualCharMethod(jobject obj, jclass clazz,
                                   jmethodID methodID, ...) {
        va_list args;
        jchar result;
        va_start(args,methodID);
        result = functions->CallNonvirtualCharMethodV(this,obj,clazz,
                                                      methodID,args);
        va_end(args);
        return result;
    }
    jchar CallNonvirtualCharMethodV(jobject obj, jclass clazz,
                                    jmethodID methodID, va_list args) {
        return functions->CallNonvirtualCharMethodV(this,obj,clazz,
                                                    methodID,args);
    }
    jchar CallNonvirtualCharMethodA(jobject obj, jclass clazz,
                                    jmethodID methodID, const jvalue * args) {
        return functions->CallNonvirtualCharMethodA(this,obj,clazz,
                                                    methodID,args);
    }

    jshort CallNonvirtualShortMethod(jobject obj, jclass clazz,
                                     jmethodID methodID, ...) {
        va_list args;
        jshort result;
        va_start(args,methodID);
        result = functions->CallNonvirtualShortMethodV(this,obj,clazz,
                                                       methodID,args);
        va_end(args);
        return result;
    }
    jshort CallNonvirtualShortMethodV(jobject obj, jclass clazz,
                                      jmethodID methodID, va_list args) {
        return functions->CallNonvirtualShortMethodV(this,obj,clazz,
                                                     methodID,args);
    }
    jshort CallNonvirtualShortMethodA(jobject obj, jclass clazz,
                                      jmethodID methodID, const jvalue * args) {
        return functions->CallNonvirtualShortMethodA(this,obj,clazz,
                                                     methodID,args);
    }

    jint CallNonvirtualIntMethod(jobject obj, jclass clazz,
                                 jmethodID methodID, ...) {
        va_list args;
        jint result;
        va_start(args,methodID);
        result = functions->CallNonvirtualIntMethodV(this,obj,clazz,
                                                     methodID,args);
        va_end(args);
        return result;
    }
    jint CallNonvirtualIntMethodV(jobject obj, jclass clazz,
                                  jmethodID methodID, va_list args) {
        return functions->CallNonvirtualIntMethodV(this,obj,clazz,
                                                   methodID,args);
    }
    jint CallNonvirtualIntMethodA(jobject obj, jclass clazz,
                                  jmethodID methodID, const jvalue * args) {
        return functions->CallNonvirtualIntMethodA(this,obj,clazz,
                                                   methodID,args);
    }

    jlong CallNonvirtualLongMethod(jobject obj, jclass clazz,
                                   jmethodID methodID, ...) {
        va_list args;
        jlong result;
        va_start(args,methodID);
        result = functions->CallNonvirtualLongMethodV(this,obj,clazz,
                                                      methodID,args);
        va_end(args);
        return result;
    }
    jlong CallNonvirtualLongMethodV(jobject obj, jclass clazz,
                                    jmethodID methodID, va_list args) {
        return functions->CallNonvirtualLongMethodV(this,obj,clazz,
                                                    methodID,args);
    }
    jlong CallNonvirtualLongMethodA(jobject obj, jclass clazz,
                                    jmethodID methodID, const jvalue * args) {
        return functions->CallNonvirtualLongMethodA(this,obj,clazz,
                                                    methodID,args);
    }

    jfloat CallNonvirtualFloatMethod(jobject obj, jclass clazz,
                                     jmethodID methodID, ...) {
        va_list args;
        jfloat result;
        va_start(args,methodID);
        result = functions->CallNonvirtualFloatMethodV(this,obj,clazz,
                                                       methodID,args);
        va_end(args);
        return result;
    }
    jfloat CallNonvirtualFloatMethodV(jobject obj, jclass clazz,
                                      jmethodID methodID,
                                      va_list args) {
        return functions->CallNonvirtualFloatMethodV(this,obj,clazz,
                                                     methodID,args);
    }
    jfloat CallNonvirtualFloatMethodA(jobject obj, jclass clazz,
                                      jmethodID methodID,
                                      const jvalue * args) {
        return functions->CallNonvirtualFloatMethodA(this,obj,clazz,
                                                     methodID,args);
    }

    jdouble CallNonvirtualDoubleMethod(jobject obj, jclass clazz,
                                       jmethodID methodID, ...) {
        va_list args;
        jdouble result;
        va_start(args,methodID);
        result = functions->CallNonvirtualDoubleMethodV(this,obj,clazz,
                                                        methodID,args);
        va_end(args);
        return result;
    }
    jdouble CallNonvirtualDoubleMethodV(jobject obj, jclass clazz,
                                        jmethodID methodID,
                                        va_list args) {
        return functions->CallNonvirtualDoubleMethodV(this,obj,clazz,
                                                      methodID,args);
    }
    jdouble CallNonvirtualDoubleMethodA(jobject obj, jclass clazz,
                                        jmethodID methodID,
                                        const jvalue * args) {
        return functions->CallNonvirtualDoubleMethodA(this,obj,clazz,
                                                      methodID,args);
    }

    void CallNonvirtualVoidMethod(jobject obj, jclass clazz,
                                  jmethodID methodID, ...) {
        va_list args;
        va_start(args,methodID);
        functions->CallNonvirtualVoidMethodV(this,obj,clazz,methodID,args);
        va_end(args);
    }
    void CallNonvirtualVoidMethodV(jobject obj, jclass clazz,
                                   jmethodID methodID,
                                   va_list args) {
        functions->CallNonvirtualVoidMethodV(this,obj,clazz,methodID,args);
    }
    void CallNonvirtualVoidMethodA(jobject obj, jclass clazz,
                                   jmethodID methodID,
                                   const jvalue * args) {
        functions->CallNonvirtualVoidMethodA(this,obj,clazz,methodID,args);
    }

    jfieldID GetFieldID(jclass clazz, const char *name,
                        const char *sig) {
        return functions->GetFieldID(this,clazz,name,sig);
    }

    jobject GetObjectField(jobject obj, jfieldID fieldID) {
        return functions->GetObjectField(this,obj,fieldID);
    }
    jboolean GetBooleanField(jobject obj, jfieldID fieldID) {
        return functions->GetBooleanField(this,obj,fieldID);
    }
    jbyte GetByteField(jobject obj, jfieldID fieldID) {
        return functions->GetByteField(this,obj,fieldID);
    }
    jchar GetCharField(jobject obj, jfieldID fieldID) {
        return functions->GetCharField(this,obj,fieldID);
    }
    jshort GetShortField(jobject obj, jfieldID fieldID) {
        return functions->GetShortField(this,obj,fieldID);
    }
    jint GetIntField(jobject obj, jfieldID fieldID) {
        return functions->GetIntField(this,obj,fieldID);
    }
    jlong GetLongField(jobject obj, jfieldID fieldID) {
        return functions->GetLongField(this,obj,fieldID);
    }
    jfloat GetFloatField(jobject obj, jfieldID fieldID) {
        return functions->GetFloatField(this,obj,fieldID);
    }
    jdouble GetDoubleField(jobject obj, jfieldID fieldID) {
        return functions->GetDoubleField(this,obj,fieldID);
    }

    void SetObjectField(jobject obj, jfieldID fieldID, jobject val) {
        functions->SetObjectField(this,obj,fieldID,val);
    }
    void SetBooleanField(jobject obj, jfieldID fieldID,
                         jboolean val) {
        functions->SetBooleanField(this,obj,fieldID,val);
    }
    void SetByteField(jobject obj, jfieldID fieldID,
                      jbyte val) {
        functions->SetByteField(this,obj,fieldID,val);
    }
    void SetCharField(jobject obj, jfieldID fieldID,
                      jchar val) {
        functions->SetCharField(this,obj,fieldID,val);
    }
    void SetShortField(jobject obj, jfieldID fieldID,
                       jshort val) {
        functions->SetShortField(this,obj,fieldID,val);
    }
    void SetIntField(jobject obj, jfieldID fieldID,
                     jint val) {
        functions->SetIntField(this,obj,fieldID,val);
    }
    void SetLongField(jobject obj, jfieldID fieldID,
                      jlong val) {
        functions->SetLongField(this,obj,fieldID,val);
    }
    void SetFloatField(jobject obj, jfieldID fieldID,
                       jfloat val) {
        functions->SetFloatField(this,obj,fieldID,val);
    }
    void SetDoubleField(jobject obj, jfieldID fieldID,
                        jdouble val) {
        functions->SetDoubleField(this,obj,fieldID,val);
    }

    jmethodID GetStaticMethodID(jclass clazz, const char *name,
                                const char *sig) {
        return functions->GetStaticMethodID(this,clazz,name,sig);
    }

    jobject CallStaticObjectMethod(jclass clazz, jmethodID methodID,
                             ...) {
        va_list args;
        jobject result;
        va_start(args,methodID);
        result = functions->CallStaticObjectMethodV(this,clazz,methodID,args);
        va_end(args);
        return result;
    }
    jobject CallStaticObjectMethodV(jclass clazz, jmethodID methodID,
                              va_list args) {
        return functions->CallStaticObjectMethodV(this,clazz,methodID,args);
    }
    jobject CallStaticObjectMethodA(jclass clazz, jmethodID methodID,
                              const jvalue *args) {
        return functions->CallStaticObjectMethodA(this,clazz,methodID,args);
    }

    jboolean CallStaticBooleanMethod(jclass clazz,
                                     jmethodID methodID, ...) {
        va_list args;
        jboolean result;
        va_start(args,methodID);
        result = functions->CallStaticBooleanMethodV(this,clazz,methodID,args);
        va_end(args);
        return result;
    }
    jboolean CallStaticBooleanMethodV(jclass clazz,
                                      jmethodID methodID, va_list args) {
        return functions->CallStaticBooleanMethodV(this,clazz,methodID,args);
    }
    jboolean CallStaticBooleanMethodA(jclass clazz,
                                      jmethodID methodID, const jvalue *args) {
        return functions->CallStaticBooleanMethodA(this,clazz,methodID,args);
    }

    jbyte CallStaticByteMethod(jclass clazz,
                               jmethodID methodID, ...) {
        va_list args;
        jbyte result;
        va_start(args,methodID);
        result = functions->CallStaticByteMethodV(this,clazz,methodID,args);
        va_end(args);
        return result;
    }
    jbyte CallStaticByteMethodV(jclass clazz,
                                jmethodID methodID, va_list args) {
        return functions->CallStaticByteMethodV(this,clazz,methodID,args);
    }
    jbyte CallStaticByteMethodA(jclass clazz,
                                jmethodID methodID, const jvalue *args) {
        return functions->CallStaticByteMethodA(this,clazz,methodID,args);
    }

    jchar CallStaticCharMethod(jclass clazz,
                               jmethodID methodID, ...) {
        va_list args;
        jchar result;
        va_start(args,methodID);
        result = functions->CallStaticCharMethodV(this,clazz,methodID,args);
        va_end(args);
        return result;
    }
    jchar CallStaticCharMethodV(jclass clazz,
                                jmethodID methodID, va_list args) {
        return functions->CallStaticCharMethodV(this,clazz,methodID,args);
    }
    jchar CallStaticCharMethodA(jclass clazz,
                                jmethodID methodID, const jvalue *args) {
        return functions->CallStaticCharMethodA(this,clazz,methodID,args);
    }

    jshort CallStaticShortMethod(jclass clazz,
                                 jmethodID methodID, ...) {
        va_list args;
        jshort result;
        va_start(args,methodID);
        result = functions->CallStaticShortMethodV(this,clazz,methodID,args);
        va_end(args);
        return result;
    }
    jshort CallStaticShortMethodV(jclass clazz,
                                  jmethodID methodID, va_list args) {
        return functions->CallStaticShortMethodV(this,clazz,methodID,args);
    }
    jshort CallStaticShortMethodA(jclass clazz,
                                  jmethodID methodID, const jvalue *args) {
        return functions->CallStaticShortMethodA(this,clazz,methodID,args);
    }

    jint CallStaticIntMethod(jclass clazz,
                             jmethodID methodID, ...) {
        va_list args;
        jint result;
        va_start(args,methodID);
        result = functions->CallStaticIntMethodV(this,clazz,methodID,args);
        va_end(args);
        return result;
    }
    jint CallStaticIntMethodV(jclass clazz,
                              jmethodID methodID, va_list args) {
        return functions->CallStaticIntMethodV(this,clazz,methodID,args);
    }
    jint CallStaticIntMethodA(jclass clazz,
                              jmethodID methodID, const jvalue *args) {
        return functions->CallStaticIntMethodA(this,clazz,methodID,args);
    }

    jlong CallStaticLongMethod(jclass clazz,
                               jmethodID methodID, ...) {
        va_list args;
        jlong result;
        va_start(args,methodID);
        result = functions->CallStaticLongMethodV(this,clazz,methodID,args);
        va_end(args);
        return result;
    }
    jlong CallStaticLongMethodV(jclass clazz,
                                jmethodID methodID, va_list args) {
        return functions->CallStaticLongMethodV(this,clazz,methodID,args);
    }
    jlong CallStaticLongMethodA(jclass clazz,
                                jmethodID methodID, const jvalue *args) {
        return functions->CallStaticLongMethodA(this,clazz,methodID,args);
    }

    jfloat CallStaticFloatMethod(jclass clazz,
                                 jmethodID methodID, ...) {
        va_list args;
        jfloat result;
        va_start(args,methodID);
        result = functions->CallStaticFloatMethodV(this,clazz,methodID,args);
        va_end(args);
        return result;
    }
    jfloat CallStaticFloatMethodV(jclass clazz,
                                  jmethodID methodID, va_list args) {
        return functions->CallStaticFloatMethodV(this,clazz,methodID,args);
    }
    jfloat CallStaticFloatMethodA(jclass clazz,
                                  jmethodID methodID, const jvalue *args) {
        return functions->CallStaticFloatMethodA(this,clazz,methodID,args);
    }

    jdouble CallStaticDoubleMethod(jclass clazz,
                                   jmethodID methodID, ...) {
        va_list args;
        jdouble result;
        va_start(args,methodID);
        result = functions->CallStaticDoubleMethodV(this,clazz,methodID,args);
        va_end(args);
        return result;
    }
    jdouble CallStaticDoubleMethodV(jclass clazz,
                                    jmethodID methodID, va_list args) {
        return functions->CallStaticDoubleMethodV(this,clazz,methodID,args);
    }
    jdouble CallStaticDoubleMethodA(jclass clazz,
                                    jmethodID methodID, const jvalue *args) {
        return functions->CallStaticDoubleMethodA(this,clazz,methodID,args);
    }

    void CallStaticVoidMethod(jclass cls, jmethodID methodID, ...) {
        va_list args;
        va_start(args,methodID);
        functions->CallStaticVoidMethodV(this,cls,methodID,args);
        va_end(args);
    }
    void CallStaticVoidMethodV(jclass cls, jmethodID methodID,
                               va_list args) {
        functions->CallStaticVoidMethodV(this,cls,methodID,args);
    }
    void CallStaticVoidMethodA(jclass cls, jmethodID methodID,
                               const jvalue * args) {
        functions->CallStaticVoidMethodA(this,cls,methodID,args);
    }

    jfieldID GetStaticFieldID(jclass clazz, const char *name,
                              const char *sig) {
        return functions->GetStaticFieldID(this,clazz,name,sig);
    }
    jobject GetStaticObjectField(jclass clazz, jfieldID fieldID) {
        return functions->GetStaticObjectField(this,clazz,fieldID);
    }
    jboolean GetStaticBooleanField(jclass clazz, jfieldID fieldID) {
        return functions->GetStaticBooleanField(this,clazz,fieldID);
    }
    jbyte GetStaticByteField(jclass clazz, jfieldID fieldID) {
        return functions->GetStaticByteField(this,clazz,fieldID);
    }
    jchar GetStaticCharField(jclass clazz, jfieldID fieldID) {
        return functions->GetStaticCharField(this,clazz,fieldID);
    }
    jshort GetStaticShortField(jclass clazz, jfieldID fieldID) {
        return functions->GetStaticShortField(this,clazz,fieldID);
    }
    jint GetStaticIntField(jclass clazz, jfieldID fieldID) {
        return functions->GetStaticIntField(this,clazz,fieldID);
    }
    jlong GetStaticLongField(jclass clazz, jfieldID fieldID) {
        return functions->GetStaticLongField(this,clazz,fieldID);
    }
    jfloat GetStaticFloatField(jclass clazz, jfieldID fieldID) {
        return functions->GetStaticFloatField(this,clazz,fieldID);
    }
    jdouble GetStaticDoubleField(jclass clazz, jfieldID fieldID) {
        return functions->GetStaticDoubleField(this,clazz,fieldID);
    }

    void SetStaticObjectField(jclass clazz, jfieldID fieldID,
                        jobject value) {
      functions->SetStaticObjectField(this,clazz,fieldID,value);
    }
    void SetStaticBooleanField(jclass clazz, jfieldID fieldID,
                        jboolean value) {
      functions->SetStaticBooleanField(this,clazz,fieldID,value);
    }
    void SetStaticByteField(jclass clazz, jfieldID fieldID,
                        jbyte value) {
      functions->SetStaticByteField(this,clazz,fieldID,value);
    }
    void SetStaticCharField(jclass clazz, jfieldID fieldID,
                        jchar value) {
      functions->SetStaticCharField(this,clazz,fieldID,value);
    }
    void SetStaticShortField(jclass clazz, jfieldID fieldID,
                        jshort value) {
      functions->SetStaticShortField(this,clazz,fieldID,value);
    }
    void SetStaticIntField(jclass clazz, jfieldID fieldID,
                        jint value) {
      functions->SetStaticIntField(this,clazz,fieldID,value);
    }
    void SetStaticLongField(jclass clazz, jfieldID fieldID,
                        jlong value) {
      functions->SetStaticLongField(this,clazz,fieldID,value);
    }
    void SetStaticFloatField(jclass clazz, jfieldID fieldID,
                        jfloat value) {
      functions->SetStaticFloatField(this,clazz,fieldID,value);
    }
    void SetStaticDoubleField(jclass clazz, jfieldID fieldID,
                        jdouble value) {
      functions->SetStaticDoubleField(this,clazz,fieldID,value);
    }

    jstring NewString(const jchar *unicode, jsize len) {
        return functions->NewString(this,unicode,len);
    }
    jsize GetStringLength(jstring str) {
        return functions->GetStringLength(this,str);
    }
    const jchar *GetStringChars(jstring str, jboolean *isCopy) {
        return functions->GetStringChars(this,str,isCopy);
    }
    void ReleaseStringChars(jstring str, const jchar *chars) {
        functions->ReleaseStringChars(this,str,chars);
    }

    jstring NewStringUTF(const char *utf) {
        return functions->NewStringUTF(this,utf);
    }
    jsize GetStringUTFLength(jstring str) {
        return functions->GetStringUTFLength(this,str);
    }
    const char* GetStringUTFChars(jstring str, jboolean *isCopy) {
        return functions->GetStringUTFChars(this,str,isCopy);
    }
    void ReleaseStringUTFChars(jstring str, const char* chars) {
        functions->ReleaseStringUTFChars(this,str,chars);
    }

    jsize GetArrayLength(jarray array) {
        return functions->GetArrayLength(this,array);
    }

    jobjectArray NewObjectArray(jsize len, jclass clazz,
                                jobject init) {
        return functions->NewObjectArray(this,len,clazz,init);
    }
    jobject GetObjectArrayElement(jobjectArray array, jsize index) {
        return functions->GetObjectArrayElement(this,array,index);
    }
    void SetObjectArrayElement(jobjectArray array, jsize index,
                               jobject val) {
        functions->SetObjectArrayElement(this,array,index,val);
    }

    jbooleanArray NewBooleanArray(jsize len) {
        return functions->NewBooleanArray(this,len);
    }
    jbyteArray NewByteArray(jsize len) {
        return functions->NewByteArray(this,len);
    }
    jcharArray NewCharArray(jsize len) {
        return functions->NewCharArray(this,len);
    }
    jshortArray NewShortArray(jsize len) {
        return functions->NewShortArray(this,len);
    }
    jintArray NewIntArray(jsize len) {
        return functions->NewIntArray(this,len);
    }
    jlongArray NewLongArray(jsize len) {
        return functions->NewLongArray(this,len);
    }
    jfloatArray NewFloatArray(jsize len) {
        return functions->NewFloatArray(this,len);
    }
    jdoubleArray NewDoubleArray(jsize len) {
        return functions->NewDoubleArray(this,len);
    }

    jboolean * GetBooleanArrayElements(jbooleanArray array, jboolean *isCopy) {
        return functions->GetBooleanArrayElements(this,array,isCopy);
    }
    jbyte * GetByteArrayElements(jbyteArray array, jboolean *isCopy) {
        return functions->GetByteArrayElements(this,array,isCopy);
    }
    jchar * GetCharArrayElements(jcharArray array, jboolean *isCopy) {
        return functions->GetCharArrayElements(this,array,isCopy);
    }
    jshort * GetShortArrayElements(jshortArray array, jboolean *isCopy) {
        return functions->GetShortArrayElements(this,array,isCopy);
    }
    jint * GetIntArrayElements(jintArray array, jboolean *isCopy) {
        return functions->GetIntArrayElements(this,array,isCopy);
    }
    jlong * GetLongArrayElements(jlongArray array, jboolean *isCopy) {
        return functions->GetLongArrayElements(this,array,isCopy);
    }
    jfloat * GetFloatArrayElements(jfloatArray array, jboolean *isCopy) {
        return functions->GetFloatArrayElements(this,array,isCopy);
    }
    jdouble * GetDoubleArrayElements(jdoubleArray array, jboolean *isCopy) {
        return functions->GetDoubleArrayElements(this,array,isCopy);
    }

    void ReleaseBooleanArrayElements(jbooleanArray array,
                                     jboolean *elems,
                                     jint mode) {
        functions->ReleaseBooleanArrayElements(this,array,elems,mode);
    }
    void ReleaseByteArrayElements(jbyteArray array,
                                  jbyte *elems,
                                  jint mode) {
        functions->ReleaseByteArrayElements(this,array,elems,mode);
    }
    void ReleaseCharArrayElements(jcharArray array,
                                  jchar *elems,
                                  jint mode) {
        functions->ReleaseCharArrayElements(this,array,elems,mode);
    }
    void ReleaseShortArrayElements(jshortArray array,
                                   jshort *elems,
                                   jint mode) {
        functions->ReleaseShortArrayElements(this,array,elems,mode);
    }
    void ReleaseIntArrayElements(jintArray array,
                                 jint *elems,
                                 jint mode) {
        functions->ReleaseIntArrayElements(this,array,elems,mode);
    }
    void ReleaseLongArrayElements(jlongArray array,
                                  jlong *elems,
                                  jint mode) {
        functions->ReleaseLongArrayElements(this,array,elems,mode);
    }
    void ReleaseFloatArrayElements(jfloatArray array,
                                   jfloat *elems,
                                   jint mode) {
        functions->ReleaseFloatArrayElements(this,array,elems,mode);
    }
    void ReleaseDoubleArrayElements(jdoubleArray array,
                                    jdouble *elems,
                                    jint mode) {
        functions->ReleaseDoubleArrayElements(this,array,elems,mode);
    }

    void GetBooleanArrayRegion(jbooleanArray array,
                               jsize start, jsize len, jboolean *buf) {
        functions->GetBooleanArrayRegion(this,array,start,len,buf);
    }
    void GetByteArrayRegion(jbyteArray array,
                            jsize start, jsize len, jbyte *buf) {
        functions->GetByteArrayRegion(this,array,start,len,buf);
    }
    void GetCharArrayRegion(jcharArray array,
                            jsize start, jsize len, jchar *buf) {
        functions->GetCharArrayRegion(this,array,start,len,buf);
    }
    void GetShortArrayRegion(jshortArray array,
                             jsize start, jsize len, jshort *buf) {
        functions->GetShortArrayRegion(this,array,start,len,buf);
    }
    void GetIntArrayRegion(jintArray array,
                           jsize start, jsize len, jint *buf) {
        functions->GetIntArrayRegion(this,array,start,len,buf);
    }
    void GetLongArrayRegion(jlongArray array,
                            jsize start, jsize len, jlong *buf) {
        functions->GetLongArrayRegion(this,array,start,len,buf);
    }
    void GetFloatArrayRegion(jfloatArray array,
                             jsize start, jsize len, jfloat *buf) {
        functions->GetFloatArrayRegion(this,array,start,len,buf);
    }
    void GetDoubleArrayRegion(jdoubleArray array,
                              jsize start, jsize len, jdouble *buf) {
        functions->GetDoubleArrayRegion(this,array,start,len,buf);
    }

    void SetBooleanArrayRegion(jbooleanArray array, jsize start, jsize len,
                               const jboolean *buf) {
        functions->SetBooleanArrayRegion(this,array,start,len,buf);
    }
    void SetByteArrayRegion(jbyteArray array, jsize start, jsize len,
                            const jbyte *buf) {
        functions->SetByteArrayRegion(this,array,start,len,buf);
    }
    void SetCharArrayRegion(jcharArray array, jsize start, jsize len,
                            const jchar *buf) {
        functions->SetCharArrayRegion(this,array,start,len,buf);
    }
    void SetShortArrayRegion(jshortArray array, jsize start, jsize len,
                             const jshort *buf) {
        functions->SetShortArrayRegion(this,array,start,len,buf);
    }
    void SetIntArrayRegion(jintArray array, jsize start, jsize len,
                           const jint *buf) {
        functions->SetIntArrayRegion(this,array,start,len,buf);
    }
    void SetLongArrayRegion(jlongArray array, jsize start, jsize len,
                            const jlong *buf) {
        functions->SetLongArrayRegion(this,array,start,len,buf);
    }
    void SetFloatArrayRegion(jfloatArray array, jsize start, jsize len,
                             const jfloat *buf) {
        functions->SetFloatArrayRegion(this,array,start,len,buf);
    }
    void SetDoubleArrayRegion(jdoubleArray array, jsize start, jsize len,
                              const jdouble *buf) {
        functions->SetDoubleArrayRegion(this,array,start,len,buf);
    }

    jint RegisterNatives(jclass clazz, const JNINativeMethod *methods,
                         jint nMethods) {
        return functions->RegisterNatives(this,clazz,methods,nMethods);
    }
    jint UnregisterNatives(jclass clazz) {
        return functions->UnregisterNatives(this,clazz);
    }

    jint MonitorEnter(jobject obj) {
        return functions->MonitorEnter(this,obj);
    }
    jint MonitorExit(jobject obj) {
        return functions->MonitorExit(this,obj);
    }

    jint GetJavaVM(JavaVM **vm) {
        return functions->GetJavaVM(this,vm);
    }

    void GetStringRegion(jstring str, jsize start, jsize len, jchar *buf) {
        functions->GetStringRegion(this,str,start,len,buf);
    }
    void GetStringUTFRegion(jstring str, jsize start, jsize len, char *buf) {
        functions->GetStringUTFRegion(this,str,start,len,buf);
    }

    void * GetPrimitiveArrayCritical(jarray array, jboolean *isCopy) {
        return functions->GetPrimitiveArrayCritical(this,array,isCopy);
    }
    void ReleasePrimitiveArrayCritical(jarray array, void *carray, jint mode) {
        functions->ReleasePrimitiveArrayCritical(this,array,carray,mode);
    }

    const jchar * GetStringCritical(jstring string, jboolean *isCopy) {
        return functions->GetStringCritical(this,string,isCopy);
    }
    void ReleaseStringCritical(jstring string, const jchar *cstring) {
        functions->ReleaseStringCritical(this,string,cstring);
    }

    jweak NewWeakGlobalRef(jobject obj) {
        return functions->NewWeakGlobalRef(this,obj);
    }
    void DeleteWeakGlobalRef(jweak ref) {
        functions->DeleteWeakGlobalRef(this,ref);
    }

    jboolean ExceptionCheck() {
        return functions->ExceptionCheck(this);
    }

    jobject NewDirectByteBuffer(void* address, jlong capacity) {
        return functions->NewDirectByteBuffer(this, address, capacity);
    }
    void* GetDirectBufferAddress(jobject buf) {
        return functions->GetDirectBufferAddress(this, buf);
    }
    jlong GetDirectBufferCapacity(jobject buf) {
        return functions->GetDirectBufferCapacity(this, buf);
    }
    jobjectRefType GetObjectRefType(jobject obj) {
        return functions->GetObjectRefType(this, obj);
    }

    /* Module Features */

    jobject GetModule(jclass clazz) {
        return functions->GetModule(this, clazz);
    }

#endif /* __cplusplus */
};

typedef struct JavaVMOption {
    char *optionString;
    void *extraInfo;
} JavaVMOption;

typedef struct JavaVMInitArgs {
    jint version;

    jint nOptions;
    JavaVMOption *options;
    jboolean ignoreUnrecognized;
} JavaVMInitArgs;

typedef struct JavaVMAttachArgs {
    jint version;

    char *name;
    jobject group;
} JavaVMAttachArgs;

/* These will be VM-specific. */

#define JDK1_2
#define JDK1_4

/* End VM-specific. */

struct JNIInvokeInterface_ {
    void *reserved0;
    void *reserved1;
    void *reserved2;

    jint (JNICALL *DestroyJavaVM)(JavaVM *vm);

    jint (JNICALL *AttachCurrentThread)(JavaVM *vm, void **penv, void *args);

    jint (JNICALL *DetachCurrentThread)(JavaVM *vm);

    jint (JNICALL *GetEnv)(JavaVM *vm, void **penv, jint version);

    jint (JNICALL *AttachCurrentThreadAsDaemon)(JavaVM *vm, void **penv, void *args);
};

struct JavaVM_ {
    const struct JNIInvokeInterface_ *functions;
#ifdef __cplusplus

    jint DestroyJavaVM() {
        return functions->DestroyJavaVM(this);
    }
    jint AttachCurrentThread(void **penv, void *args) {
        return functions->AttachCurrentThread(this, penv, args);
    }
    jint DetachCurrentThread() {
        return functions->DetachCurrentThread(this);
    }

    jint GetEnv(void **penv, jint version) {
        return functions->GetEnv(this, penv, version);
    }
    jint AttachCurrentThreadAsDaemon(void **penv, void *args) {
        return functions->AttachCurrentThreadAsDaemon(this, penv, args);
    }
#endif
};

#ifdef _JNI_IMPLEMENTATION_
#define _JNI_IMPORT_OR_EXPORT_ JNIEXPORT
#else
#define _JNI_IMPORT_OR_EXPORT_
#endif
_JNI_IMPORT_OR_EXPORT_ jint JNICALL
JNI_GetDefaultJavaVMInitArgs(void *args);

_JNI_IMPORT_OR_EXPORT_ jint JNICALL
JNI_CreateJavaVM(JavaVM **pvm, void **penv, void *args);

_JNI_IMPORT_OR_EXPORT_ jint JNICALL
JNI_GetCreatedJavaVMs(JavaVM **, jsize, jsize *);

/* Defined by native libraries. */
JNIEXPORT jint JNICALL
JNI_OnLoad(JavaVM *vm, void *reserved);

JNIEXPORT void JNICALL
JNI_OnUnload(JavaVM *vm, void *reserved);

#define JNI_VERSION_1_1 0x00010001
#define JNI_VERSION_1_2 0x00010002
#define JNI_VERSION_1_4 0x00010004
#define JNI_VERSION_1_6 0x00010006
#define JNI_VERSION_1_8 0x00010008
#define JNI_VERSION_9   0x00090000
#define JNI_VERSION_10  0x000a0000

#ifdef __cplusplus
} /* extern "C" */
#endif /* __cplusplus */

#endif /* !_JAVASOFT_JNI_H_ */

/*
 * Copyright (c) 2003, 2011, Oracle and/or its affiliates. All rights reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 * This code is free software; you can redistribute it and/or modify it
 * under the terms of the GNU General Public License version 2 only, as
 * published by the Free Software Foundation.  Oracle designates this
 * particular file as subject to the "Classpath" exception as provided
 * by Oracle in the LICENSE file that accompanied this code.
 *
 * This code is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or
 * FITNESS FOR A PARTICULAR PURPOSE.  See the GNU General Public License
 * version 2 for more details (a copy is included in the LICENSE file that
 * accompanied this code).
 *
 * You should have received a copy of the GNU General Public License version
 * 2 along with this work; if not, write to the Free Software Foundation,
 * Inc., 51 Franklin St, Fifth Floor, Boston, MA 02110-1301 USA.
 *
 * Please contact Oracle, 500 Oracle Parkway, Redwood Shores, CA 94065 USA
 * or visit www.oracle.com if you need additional information or have any
 * questions.
 */

    /* AUTOMATICALLY GENERATED FILE - DO NOT EDIT */


    /* Include file for the Java(tm) Virtual Machine Tool Interface */

#ifndef _JAVA_JVMTI_H_
#define _JAVA_JVMTI_H_

#ifdef __cplusplus
extern "C" {
#endif

enum {
    JVMTI_VERSION_1   = 0x30010000,
    JVMTI_VERSION_1_0 = 0x30010000,
    JVMTI_VERSION_1_1 = 0x30010100,
    JVMTI_VERSION_1_2 = 0x30010200,

    JVMTI_VERSION = 0x30000000 + (1 * 0x10000) + (2 * 0x100) + 1  /* version: 1.2.1 */
};

JNIEXPORT jint JNICALL
Agent_OnLoad(JavaVM *vm, char *options, void *reserved);

JNIEXPORT jint JNICALL
Agent_OnAttach(JavaVM* vm, char* options, void* reserved);

JNIEXPORT void JNICALL
Agent_OnUnload(JavaVM *vm);

    /* Forward declaration of the environment */

struct _jvmtiEnv;

struct jvmtiInterface_1_;

#ifdef __cplusplus
typedef _jvmtiEnv jvmtiEnv;
#else
typedef const struct jvmtiInterface_1_ *jvmtiEnv;
#endif /* __cplusplus */

/* Derived Base Types */

typedef jobject jthread;
typedef jobject jthreadGroup;
typedef jlong jlocation;
struct _jrawMonitorID;
typedef struct _jrawMonitorID *jrawMonitorID;
typedef struct JNINativeInterface_ jniNativeInterface;

    /* Constants */


    /* Thread State Flags */

enum {
    JVMTI_THREAD_STATE_ALIVE = 0x0001,
    JVMTI_THREAD_STATE_TERMINATED = 0x0002,
    JVMTI_THREAD_STATE_RUNNABLE = 0x0004,
    JVMTI_THREAD_STATE_BLOCKED_ON_MONITOR_ENTER = 0x0400,
    JVMTI_THREAD_STATE_WAITING = 0x0080,
    JVMTI_THREAD_STATE_WAITING_INDEFINITELY = 0x0010,
    JVMTI_THREAD_STATE_WAITING_WITH_TIMEOUT = 0x0020,
    JVMTI_THREAD_STATE_SLEEPING = 0x0040,
    JVMTI_THREAD_STATE_IN_OBJECT_WAIT = 0x0100,
    JVMTI_THREAD_STATE_PARKED = 0x0200,
    JVMTI_THREAD_STATE_SUSPENDED = 0x100000,
    JVMTI_THREAD_STATE_INTERRUPTED = 0x200000,
    JVMTI_THREAD_STATE_IN_NATIVE = 0x400000,
    JVMTI_THREAD_STATE_VENDOR_1 = 0x10000000,
    JVMTI_THREAD_STATE_VENDOR_2 = 0x20000000,
    JVMTI_THREAD_STATE_VENDOR_3 = 0x40000000
};

    /* java.lang.Thread.State Conversion Masks */

enum {
    JVMTI_JAVA_LANG_THREAD_STATE_MASK = JVMTI_THREAD_STATE_TERMINATED | JVMTI_THREAD_STATE_ALIVE | JVMTI_THREAD_STATE_RUNNABLE | JVMTI_THREAD_STATE_BLOCKED_ON_MONITOR_ENTER | JVMTI_THREAD_STATE_WAITING | JVMTI_THREAD_STATE_WAITING_INDEFINITELY | JVMTI_THREAD_STATE_WAITING_WITH_TIMEOUT,
    JVMTI_JAVA_LANG_THREAD_STATE_NEW = 0,
    JVMTI_JAVA_LANG_THREAD_STATE_TERMINATED = JVMTI_THREAD_STATE_TERMINATED,
    JVMTI_JAVA_LANG_THREAD_STATE_RUNNABLE = JVMTI_THREAD_STATE_ALIVE | JVMTI_THREAD_STATE_RUNNABLE,
    JVMTI_JAVA_LANG_THREAD_STATE_BLOCKED = JVMTI_THREAD_STATE_ALIVE | JVMTI_THREAD_STATE_BLOCKED_ON_MONITOR_ENTER,
    JVMTI_JAVA_LANG_THREAD_STATE_WAITING = JVMTI_THREAD_STATE_ALIVE | JVMTI_THREAD_STATE_WAITING | JVMTI_THREAD_STATE_WAITING_INDEFINITELY,
    JVMTI_JAVA_LANG_THREAD_STATE_TIMED_WAITING = JVMTI_THREAD_STATE_ALIVE | JVMTI_THREAD_STATE_WAITING | JVMTI_THREAD_STATE_WAITING_WITH_TIMEOUT
};

    /* Thread Priority Constants */

enum {
    JVMTI_THREAD_MIN_PRIORITY = 1,
    JVMTI_THREAD_NORM_PRIORITY = 5,
    JVMTI_THREAD_MAX_PRIORITY = 10
};

    /* Heap Filter Flags */

enum {
    JVMTI_HEAP_FILTER_TAGGED = 0x4,
    JVMTI_HEAP_FILTER_UNTAGGED = 0x8,
    JVMTI_HEAP_FILTER_CLASS_TAGGED = 0x10,
    JVMTI_HEAP_FILTER_CLASS_UNTAGGED = 0x20
};

    /* Heap Visit Control Flags */

enum {
    JVMTI_VISIT_OBJECTS = 0x100,
    JVMTI_VISIT_ABORT = 0x8000
};

    /* Heap Reference Enumeration */

typedef enum {
    JVMTI_HEAP_REFERENCE_CLASS = 1,
    JVMTI_HEAP_REFERENCE_FIELD = 2,
    JVMTI_HEAP_REFERENCE_ARRAY_ELEMENT = 3,
    JVMTI_HEAP_REFERENCE_CLASS_LOADER = 4,
    JVMTI_HEAP_REFERENCE_SIGNERS = 5,
    JVMTI_HEAP_REFERENCE_PROTECTION_DOMAIN = 6,
    JVMTI_HEAP_REFERENCE_INTERFACE = 7,
    JVMTI_HEAP_REFERENCE_STATIC_FIELD = 8,
    JVMTI_HEAP_REFERENCE_CONSTANT_POOL = 9,
    JVMTI_HEAP_REFERENCE_SUPERCLASS = 10,
    JVMTI_HEAP_REFERENCE_JNI_GLOBAL = 21,
    JVMTI_HEAP_REFERENCE_SYSTEM_CLASS = 22,
    JVMTI_HEAP_REFERENCE_MONITOR = 23,
    JVMTI_HEAP_REFERENCE_STACK_LOCAL = 24,
    JVMTI_HEAP_REFERENCE_JNI_LOCAL = 25,
    JVMTI_HEAP_REFERENCE_THREAD = 26,
    JVMTI_HEAP_REFERENCE_OTHER = 27
} jvmtiHeapReferenceKind;

    /* Primitive Type Enumeration */

typedef enum {
    JVMTI_PRIMITIVE_TYPE_BOOLEAN = 90,
    JVMTI_PRIMITIVE_TYPE_BYTE = 66,
    JVMTI_PRIMITIVE_TYPE_CHAR = 67,
    JVMTI_PRIMITIVE_TYPE_SHORT = 83,
    JVMTI_PRIMITIVE_TYPE_INT = 73,
    JVMTI_PRIMITIVE_TYPE_LONG = 74,
    JVMTI_PRIMITIVE_TYPE_FLOAT = 70,
    JVMTI_PRIMITIVE_TYPE_DOUBLE = 68
} jvmtiPrimitiveType;

    /* Heap Object Filter Enumeration */

typedef enum {
    JVMTI_HEAP_OBJECT_TAGGED = 1,
    JVMTI_HEAP_OBJECT_UNTAGGED = 2,
    JVMTI_HEAP_OBJECT_EITHER = 3
} jvmtiHeapObjectFilter;

    /* Heap Root Kind Enumeration */

typedef enum {
    JVMTI_HEAP_ROOT_JNI_GLOBAL = 1,
    JVMTI_HEAP_ROOT_SYSTEM_CLASS = 2,
    JVMTI_HEAP_ROOT_MONITOR = 3,
    JVMTI_HEAP_ROOT_STACK_LOCAL = 4,
    JVMTI_HEAP_ROOT_JNI_LOCAL = 5,
    JVMTI_HEAP_ROOT_THREAD = 6,
    JVMTI_HEAP_ROOT_OTHER = 7
} jvmtiHeapRootKind;

    /* Object Reference Enumeration */

typedef enum {
    JVMTI_REFERENCE_CLASS = 1,
    JVMTI_REFERENCE_FIELD = 2,
    JVMTI_REFERENCE_ARRAY_ELEMENT = 3,
    JVMTI_REFERENCE_CLASS_LOADER = 4,
    JVMTI_REFERENCE_SIGNERS = 5,
    JVMTI_REFERENCE_PROTECTION_DOMAIN = 6,
    JVMTI_REFERENCE_INTERFACE = 7,
    JVMTI_REFERENCE_STATIC_FIELD = 8,
    JVMTI_REFERENCE_CONSTANT_POOL = 9
} jvmtiObjectReferenceKind;

    /* Iteration Control Enumeration */

typedef enum {
    JVMTI_ITERATION_CONTINUE = 1,
    JVMTI_ITERATION_IGNORE = 2,
    JVMTI_ITERATION_ABORT = 0
} jvmtiIterationControl;

    /* Class Status Flags */

enum {
    JVMTI_CLASS_STATUS_VERIFIED = 1,
    JVMTI_CLASS_STATUS_PREPARED = 2,
    JVMTI_CLASS_STATUS_INITIALIZED = 4,
    JVMTI_CLASS_STATUS_ERROR = 8,
    JVMTI_CLASS_STATUS_ARRAY = 16,
    JVMTI_CLASS_STATUS_PRIMITIVE = 32
};

    /* Event Enable/Disable */

typedef enum {
    JVMTI_ENABLE = 1,
    JVMTI_DISABLE = 0
} jvmtiEventMode;

    /* Extension Function/Event Parameter Types */

typedef enum {
    JVMTI_TYPE_JBYTE = 101,
    JVMTI_TYPE_JCHAR = 102,
    JVMTI_TYPE_JSHORT = 103,
    JVMTI_TYPE_JINT = 104,
    JVMTI_TYPE_JLONG = 105,
    JVMTI_TYPE_JFLOAT = 106,
    JVMTI_TYPE_JDOUBLE = 107,
    JVMTI_TYPE_JBOOLEAN = 108,
    JVMTI_TYPE_JOBJECT = 109,
    JVMTI_TYPE_JTHREAD = 110,
    JVMTI_TYPE_JCLASS = 111,
    JVMTI_TYPE_JVALUE = 112,
    JVMTI_TYPE_JFIELDID = 113,
    JVMTI_TYPE_JMETHODID = 114,
    JVMTI_TYPE_CCHAR = 115,
    JVMTI_TYPE_CVOID = 116,
    JVMTI_TYPE_JNIENV = 117
} jvmtiParamTypes;

    /* Extension Function/Event Parameter Kinds */

typedef enum {
    JVMTI_KIND_IN = 91,
    JVMTI_KIND_IN_PTR = 92,
    JVMTI_KIND_IN_BUF = 93,
    JVMTI_KIND_ALLOC_BUF = 94,
    JVMTI_KIND_ALLOC_ALLOC_BUF = 95,
    JVMTI_KIND_OUT = 96,
    JVMTI_KIND_OUT_BUF = 97
} jvmtiParamKind;

    /* Timer Kinds */

typedef enum {
    JVMTI_TIMER_USER_CPU = 30,
    JVMTI_TIMER_TOTAL_CPU = 31,
    JVMTI_TIMER_ELAPSED = 32
} jvmtiTimerKind;

    /* Phases of execution */

typedef enum {
    JVMTI_PHASE_ONLOAD = 1,
    JVMTI_PHASE_PRIMORDIAL = 2,
    JVMTI_PHASE_START = 6,
    JVMTI_PHASE_LIVE = 4,
    JVMTI_PHASE_DEAD = 8
} jvmtiPhase;

    /* Version Interface Types */

enum {
    JVMTI_VERSION_INTERFACE_JNI = 0x00000000,
    JVMTI_VERSION_INTERFACE_JVMTI = 0x30000000
};

    /* Version Masks */

enum {
    JVMTI_VERSION_MASK_INTERFACE_TYPE = 0x70000000,
    JVMTI_VERSION_MASK_MAJOR = 0x0FFF0000,
    JVMTI_VERSION_MASK_MINOR = 0x0000FF00,
    JVMTI_VERSION_MASK_MICRO = 0x000000FF
};

    /* Version Shifts */

enum {
    JVMTI_VERSION_SHIFT_MAJOR = 16,
    JVMTI_VERSION_SHIFT_MINOR = 8,
    JVMTI_VERSION_SHIFT_MICRO = 0
};

    /* Verbose Flag Enumeration */

typedef enum {
    JVMTI_VERBOSE_OTHER = 0,
    JVMTI_VERBOSE_GC = 1,
    JVMTI_VERBOSE_CLASS = 2,
    JVMTI_VERBOSE_JNI = 4
} jvmtiVerboseFlag;

    /* JLocation Format Enumeration */

typedef enum {
    JVMTI_JLOCATION_JVMBCI = 1,
    JVMTI_JLOCATION_MACHINEPC = 2,
    JVMTI_JLOCATION_OTHER = 0
} jvmtiJlocationFormat;

    /* Resource Exhaustion Flags */

enum {
    JVMTI_RESOURCE_EXHAUSTED_OOM_ERROR = 0x0001,
    JVMTI_RESOURCE_EXHAUSTED_JAVA_HEAP = 0x0002,
    JVMTI_RESOURCE_EXHAUSTED_THREADS = 0x0004
};

    /* Errors */

typedef enum {
    JVMTI_ERROR_NONE = 0,
    JVMTI_ERROR_INVALID_THREAD = 10,
    JVMTI_ERROR_INVALID_THREAD_GROUP = 11,
    JVMTI_ERROR_INVALID_PRIORITY = 12,
    JVMTI_ERROR_THREAD_NOT_SUSPENDED = 13,
    JVMTI_ERROR_THREAD_SUSPENDED = 14,
    JVMTI_ERROR_THREAD_NOT_ALIVE = 15,
    JVMTI_ERROR_INVALID_OBJECT = 20,
    JVMTI_ERROR_INVALID_CLASS = 21,
    JVMTI_ERROR_CLASS_NOT_PREPARED = 22,
    JVMTI_ERROR_INVALID_METHODID = 23,
    JVMTI_ERROR_INVALID_LOCATION = 24,
    JVMTI_ERROR_INVALID_FIELDID = 25,
    JVMTI_ERROR_NO_MORE_FRAMES = 31,
    JVMTI_ERROR_OPAQUE_FRAME = 32,
    JVMTI_ERROR_TYPE_MISMATCH = 34,
    JVMTI_ERROR_INVALID_SLOT = 35,
    JVMTI_ERROR_DUPLICATE = 40,
    JVMTI_ERROR_NOT_FOUND = 41,
    JVMTI_ERROR_INVALID_MONITOR = 50,
    JVMTI_ERROR_NOT_MONITOR_OWNER = 51,
    JVMTI_ERROR_INTERRUPT = 52,
    JVMTI_ERROR_INVALID_CLASS_FORMAT = 60,
    JVMTI_ERROR_CIRCULAR_CLASS_DEFINITION = 61,
    JVMTI_ERROR_FAILS_VERIFICATION = 62,
    JVMTI_ERROR_UNSUPPORTED_REDEFINITION_METHOD_ADDED = 63,
    JVMTI_ERROR_UNSUPPORTED_REDEFINITION_SCHEMA_CHANGED = 64,
    JVMTI_ERROR_INVALID_TYPESTATE = 65,
    JVMTI_ERROR_UNSUPPORTED_REDEFINITION_HIERARCHY_CHANGED = 66,
    JVMTI_ERROR_UNSUPPORTED_REDEFINITION_METHOD_DELETED = 67,
    JVMTI_ERROR_UNSUPPORTED_VERSION = 68,
    JVMTI_ERROR_NAMES_DONT_MATCH = 69,
    JVMTI_ERROR_UNSUPPORTED_REDEFINITION_CLASS_MODIFIERS_CHANGED = 70,
    JVMTI_ERROR_UNSUPPORTED_REDEFINITION_METHOD_MODIFIERS_CHANGED = 71,
    JVMTI_ERROR_UNMODIFIABLE_CLASS = 79,
    JVMTI_ERROR_NOT_AVAILABLE = 98,
    JVMTI_ERROR_MUST_POSSESS_CAPABILITY = 99,
    JVMTI_ERROR_NULL_POINTER = 100,
    JVMTI_ERROR_ABSENT_INFORMATION = 101,
    JVMTI_ERROR_INVALID_EVENT_TYPE = 102,
    JVMTI_ERROR_ILLEGAL_ARGUMENT = 103,
    JVMTI_ERROR_NATIVE_METHOD = 104,
    JVMTI_ERROR_CLASS_LOADER_UNSUPPORTED = 106,
    JVMTI_ERROR_OUT_OF_MEMORY = 110,
    JVMTI_ERROR_ACCESS_DENIED = 111,
    JVMTI_ERROR_WRONG_PHASE = 112,
    JVMTI_ERROR_INTERNAL = 113,
    JVMTI_ERROR_UNATTACHED_THREAD = 115,
    JVMTI_ERROR_INVALID_ENVIRONMENT = 116,
    JVMTI_ERROR_MAX = 116
} jvmtiError;

    /* Event IDs */

typedef enum {
    JVMTI_MIN_EVENT_TYPE_VAL = 50,
    JVMTI_EVENT_VM_INIT = 50,
    JVMTI_EVENT_VM_DEATH = 51,
    JVMTI_EVENT_THREAD_START = 52,
    JVMTI_EVENT_THREAD_END = 53,
    JVMTI_EVENT_CLASS_FILE_LOAD_HOOK = 54,
    JVMTI_EVENT_CLASS_LOAD = 55,
    JVMTI_EVENT_CLASS_PREPARE = 56,
    JVMTI_EVENT_VM_START = 57,
    JVMTI_EVENT_EXCEPTION = 58,
    JVMTI_EVENT_EXCEPTION_CATCH = 59,
    JVMTI_EVENT_SINGLE_STEP = 60,
    JVMTI_EVENT_FRAME_POP = 61,
    JVMTI_EVENT_BREAKPOINT = 62,
    JVMTI_EVENT_FIELD_ACCESS = 63,
    JVMTI_EVENT_FIELD_MODIFICATION = 64,
    JVMTI_EVENT_METHOD_ENTRY = 65,
    JVMTI_EVENT_METHOD_EXIT = 66,
    JVMTI_EVENT_NATIVE_METHOD_BIND = 67,
    JVMTI_EVENT_COMPILED_METHOD_LOAD = 68,
    JVMTI_EVENT_COMPILED_METHOD_UNLOAD = 69,
    JVMTI_EVENT_DYNAMIC_CODE_GENERATED = 70,
    JVMTI_EVENT_DATA_DUMP_REQUEST = 71,
    JVMTI_EVENT_MONITOR_WAIT = 73,
    JVMTI_EVENT_MONITOR_WAITED = 74,
    JVMTI_EVENT_MONITOR_CONTENDED_ENTER = 75,
    JVMTI_EVENT_MONITOR_CONTENDED_ENTERED = 76,
    JVMTI_EVENT_RESOURCE_EXHAUSTED = 80,
    JVMTI_EVENT_GARBAGE_COLLECTION_START = 81,
    JVMTI_EVENT_GARBAGE_COLLECTION_FINISH = 82,
    JVMTI_EVENT_OBJECT_FREE = 83,
    JVMTI_EVENT_VM_OBJECT_ALLOC = 84,
    JVMTI_MAX_EVENT_TYPE_VAL = 84
} jvmtiEvent;


    /* Pre-Declarations */
struct _jvmtiThreadInfo;
typedef struct _jvmtiThreadInfo jvmtiThreadInfo;
struct _jvmtiMonitorStackDepthInfo;
typedef struct _jvmtiMonitorStackDepthInfo jvmtiMonitorStackDepthInfo;
struct _jvmtiThreadGroupInfo;
typedef struct _jvmtiThreadGroupInfo jvmtiThreadGroupInfo;
struct _jvmtiFrameInfo;
typedef struct _jvmtiFrameInfo jvmtiFrameInfo;
struct _jvmtiStackInfo;
typedef struct _jvmtiStackInfo jvmtiStackInfo;
struct _jvmtiHeapReferenceInfoField;
typedef struct _jvmtiHeapReferenceInfoField jvmtiHeapReferenceInfoField;
struct _jvmtiHeapReferenceInfoArray;
typedef struct _jvmtiHeapReferenceInfoArray jvmtiHeapReferenceInfoArray;
struct _jvmtiHeapReferenceInfoConstantPool;
typedef struct _jvmtiHeapReferenceInfoConstantPool jvmtiHeapReferenceInfoConstantPool;
struct _jvmtiHeapReferenceInfoStackLocal;
typedef struct _jvmtiHeapReferenceInfoStackLocal jvmtiHeapReferenceInfoStackLocal;
struct _jvmtiHeapReferenceInfoJniLocal;
typedef struct _jvmtiHeapReferenceInfoJniLocal jvmtiHeapReferenceInfoJniLocal;
struct _jvmtiHeapReferenceInfoReserved;
typedef struct _jvmtiHeapReferenceInfoReserved jvmtiHeapReferenceInfoReserved;
union _jvmtiHeapReferenceInfo;
typedef union _jvmtiHeapReferenceInfo jvmtiHeapReferenceInfo;
struct _jvmtiHeapCallbacks;
typedef struct _jvmtiHeapCallbacks jvmtiHeapCallbacks;
struct _jvmtiClassDefinition;
typedef struct _jvmtiClassDefinition jvmtiClassDefinition;
struct _jvmtiMonitorUsage;
typedef struct _jvmtiMonitorUsage jvmtiMonitorUsage;
struct _jvmtiLineNumberEntry;
typedef struct _jvmtiLineNumberEntry jvmtiLineNumberEntry;
struct _jvmtiLocalVariableEntry;
typedef struct _jvmtiLocalVariableEntry jvmtiLocalVariableEntry;
struct _jvmtiParamInfo;
typedef struct _jvmtiParamInfo jvmtiParamInfo;
struct _jvmtiExtensionFunctionInfo;
typedef struct _jvmtiExtensionFunctionInfo jvmtiExtensionFunctionInfo;
struct _jvmtiExtensionEventInfo;
typedef struct _jvmtiExtensionEventInfo jvmtiExtensionEventInfo;
struct _jvmtiTimerInfo;
typedef struct _jvmtiTimerInfo jvmtiTimerInfo;
struct _jvmtiAddrLocationMap;
typedef struct _jvmtiAddrLocationMap jvmtiAddrLocationMap;

    /* Function Types */

typedef void (JNICALL *jvmtiStartFunction)
    (jvmtiEnv* jvmti_env, JNIEnv* jni_env, void* arg);

typedef jint (JNICALL *jvmtiHeapIterationCallback)
    (jlong class_tag, jlong size, jlong* tag_ptr, jint length, void* user_data);

typedef jint (JNICALL *jvmtiHeapReferenceCallback)
    (jvmtiHeapReferenceKind reference_kind, const jvmtiHeapReferenceInfo* reference_info, jlong class_tag, jlong referrer_class_tag, jlong size, jlong* tag_ptr, jlong* referrer_tag_ptr, jint length, void* user_data);

typedef jint (JNICALL *jvmtiPrimitiveFieldCallback)
    (jvmtiHeapReferenceKind kind, const jvmtiHeapReferenceInfo* info, jlong object_class_tag, jlong* object_tag_ptr, jvalue value, jvmtiPrimitiveType value_type, void* user_data);

typedef jint (JNICALL *jvmtiArrayPrimitiveValueCallback)
    (jlong class_tag, jlong size, jlong* tag_ptr, jint element_count, jvmtiPrimitiveType element_type, const void* elements, void* user_data);

typedef jint (JNICALL *jvmtiStringPrimitiveValueCallback)
    (jlong class_tag, jlong size, jlong* tag_ptr, const jchar* value, jint value_length, void* user_data);

typedef jint (JNICALL *jvmtiReservedCallback)
    ();

typedef jvmtiIterationControl (JNICALL *jvmtiHeapObjectCallback)
    (jlong class_tag, jlong size, jlong* tag_ptr, void* user_data);

typedef jvmtiIterationControl (JNICALL *jvmtiHeapRootCallback)
    (jvmtiHeapRootKind root_kind, jlong class_tag, jlong size, jlong* tag_ptr, void* user_data);

typedef jvmtiIterationControl (JNICALL *jvmtiStackReferenceCallback)
    (jvmtiHeapRootKind root_kind, jlong class_tag, jlong size, jlong* tag_ptr, jlong thread_tag, jint depth, jmethodID method, jint slot, void* user_data);

typedef jvmtiIterationControl (JNICALL *jvmtiObjectReferenceCallback)
    (jvmtiObjectReferenceKind reference_kind, jlong class_tag, jlong size, jlong* tag_ptr, jlong referrer_tag, jint referrer_index, void* user_data);

typedef jvmtiError (JNICALL *jvmtiExtensionFunction)
    (jvmtiEnv* jvmti_env,  ...);

typedef void (JNICALL *jvmtiExtensionEvent)
    (jvmtiEnv* jvmti_env,  ...);


    /* Structure Types */
struct _jvmtiThreadInfo {
    char* name;
    jint priority;
    jboolean is_daemon;
    jthreadGroup thread_group;
    jobject context_class_loader;
};
struct _jvmtiMonitorStackDepthInfo {
    jobject monitor;
    jint stack_depth;
};
struct _jvmtiThreadGroupInfo {
    jthreadGroup parent;
    char* name;
    jint max_priority;
    jboolean is_daemon;
};
struct _jvmtiFrameInfo {
    jmethodID method;
    jlocation location;
};
struct _jvmtiStackInfo {
    jthread thread;
    jint state;
    jvmtiFrameInfo* frame_buffer;
    jint frame_count;
};
struct _jvmtiHeapReferenceInfoField {
    jint index;
};
struct _jvmtiHeapReferenceInfoArray {
    jint index;
};
struct _jvmtiHeapReferenceInfoConstantPool {
    jint index;
};
struct _jvmtiHeapReferenceInfoStackLocal {
    jlong thread_tag;
    jlong thread_id;
    jint depth;
    jmethodID method;
    jlocation location;
    jint slot;
};
struct _jvmtiHeapReferenceInfoJniLocal {
    jlong thread_tag;
    jlong thread_id;
    jint depth;
    jmethodID method;
};
struct _jvmtiHeapReferenceInfoReserved {
    jlong reserved1;
    jlong reserved2;
    jlong reserved3;
    jlong reserved4;
    jlong reserved5;
    jlong reserved6;
    jlong reserved7;
    jlong reserved8;
};
union _jvmtiHeapReferenceInfo {
    jvmtiHeapReferenceInfoField field;
    jvmtiHeapReferenceInfoArray array;
    jvmtiHeapReferenceInfoConstantPool constant_pool;
    jvmtiHeapReferenceInfoStackLocal stack_local;
    jvmtiHeapReferenceInfoJniLocal jni_local;
    jvmtiHeapReferenceInfoReserved other;
};
struct _jvmtiHeapCallbacks {
    jvmtiHeapIterationCallback heap_iteration_callback;
    jvmtiHeapReferenceCallback heap_reference_callback;
    jvmtiPrimitiveFieldCallback primitive_field_callback;
    jvmtiArrayPrimitiveValueCallback array_primitive_value_callback;
    jvmtiStringPrimitiveValueCallback string_primitive_value_callback;
    jvmtiReservedCallback reserved5;
    jvmtiReservedCallback reserved6;
    jvmtiReservedCallback reserved7;
    jvmtiReservedCallback reserved8;
    jvmtiReservedCallback reserved9;
    jvmtiReservedCallback reserved10;
    jvmtiReservedCallback reserved11;
    jvmtiReservedCallback reserved12;
    jvmtiReservedCallback reserved13;
    jvmtiReservedCallback reserved14;
    jvmtiReservedCallback reserved15;
};
struct _jvmtiClassDefinition {
    jclass klass;
    jint class_byte_count;
    const unsigned char* class_bytes;
};
struct _jvmtiMonitorUsage {
    jthread owner;
    jint entry_count;
    jint waiter_count;
    jthread* waiters;
    jint notify_waiter_count;
    jthread* notify_waiters;
};
struct _jvmtiLineNumberEntry {
    jlocation start_location;
    jint line_number;
};
struct _jvmtiLocalVariableEntry {
    jlocation start_location;
    jint length;
    char* name;
    char* signature;
    char* generic_signature;
    jint slot;
};
struct _jvmtiParamInfo {
    char* name;
    jvmtiParamKind kind;
    jvmtiParamTypes base_type;
    jboolean null_ok;
};
struct _jvmtiExtensionFunctionInfo {
    jvmtiExtensionFunction func;
    char* id;
    char* short_description;
    jint param_count;
    jvmtiParamInfo* params;
    jint error_count;
    jvmtiError* errors;
};
struct _jvmtiExtensionEventInfo {
    jint extension_event_index;
    char* id;
    char* short_description;
    jint param_count;
    jvmtiParamInfo* params;
};
struct _jvmtiTimerInfo {
    jlong max_value;
    jboolean may_skip_forward;
    jboolean may_skip_backward;
    jvmtiTimerKind kind;
    jlong reserved1;
    jlong reserved2;
};
struct _jvmtiAddrLocationMap {
    const void* start_address;
    jlocation location;
};

typedef struct {
    unsigned int can_tag_objects : 1;
    unsigned int can_generate_field_modification_events : 1;
    unsigned int can_generate_field_access_events : 1;
    unsigned int can_get_bytecodes : 1;
    unsigned int can_get_synthetic_attribute : 1;
    unsigned int can_get_owned_monitor_info : 1;
    unsigned int can_get_current_contended_monitor : 1;
    unsigned int can_get_monitor_info : 1;
    unsigned int can_pop_frame : 1;
    unsigned int can_redefine_classes : 1;
    unsigned int can_signal_thread : 1;
    unsigned int can_get_source_file_name : 1;
    unsigned int can_get_line_numbers : 1;
    unsigned int can_get_source_debug_extension : 1;
    unsigned int can_access_local_variables : 1;
    unsigned int can_maintain_original_method_order : 1;
    unsigned int can_generate_single_step_events : 1;
    unsigned int can_generate_exception_events : 1;
    unsigned int can_generate_frame_pop_events : 1;
    unsigned int can_generate_breakpoint_events : 1;
    unsigned int can_suspend : 1;
    unsigned int can_redefine_any_class : 1;
    unsigned int can_get_current_thread_cpu_time : 1;
    unsigned int can_get_thread_cpu_time : 1;
    unsigned int can_generate_method_entry_events : 1;
    unsigned int can_generate_method_exit_events : 1;
    unsigned int can_generate_all_class_hook_events : 1;
    unsigned int can_generate_compiled_method_load_events : 1;
    unsigned int can_generate_monitor_events : 1;
    unsigned int can_generate_vm_object_alloc_events : 1;
    unsigned int can_generate_native_method_bind_events : 1;
    unsigned int can_generate_garbage_collection_events : 1;
    unsigned int can_generate_object_free_events : 1;
    unsigned int can_force_early_return : 1;
    unsigned int can_get_owned_monitor_stack_depth_info : 1;
    unsigned int can_get_constant_pool : 1;
    unsigned int can_set_native_method_prefix : 1;
    unsigned int can_retransform_classes : 1;
    unsigned int can_retransform_any_class : 1;
    unsigned int can_generate_resource_exhaustion_heap_events : 1;
    unsigned int can_generate_resource_exhaustion_threads_events : 1;
    unsigned int : 7;
    unsigned int : 16;
    unsigned int : 16;
    unsigned int : 16;
    unsigned int : 16;
    unsigned int : 16;
} jvmtiCapabilities;


    /* Event Definitions */

typedef void (JNICALL *jvmtiEventReserved)(void);


typedef void (JNICALL *jvmtiEventBreakpoint)
    (jvmtiEnv *jvmti_env,
     JNIEnv* jni_env,
     jthread thread,
     jmethodID method,
     jlocation location);

typedef void (JNICALL *jvmtiEventClassFileLoadHook)
    (jvmtiEnv *jvmti_env,
     JNIEnv* jni_env,
     jclass class_being_redefined,
     jobject loader,
     const char* name,
     jobject protection_domain,
     jint class_data_len,
     const unsigned char* class_data,
     jint* new_class_data_len,
     unsigned char** new_class_data);

typedef void (JNICALL *jvmtiEventClassLoad)
    (jvmtiEnv *jvmti_env,
     JNIEnv* jni_env,
     jthread thread,
     jclass klass);

typedef void (JNICALL *jvmtiEventClassPrepare)
    (jvmtiEnv *jvmti_env,
     JNIEnv* jni_env,
     jthread thread,
     jclass klass);

typedef void (JNICALL *jvmtiEventCompiledMethodLoad)
    (jvmtiEnv *jvmti_env,
     jmethodID method,
     jint code_size,
     const void* code_addr,
     jint map_length,
     const jvmtiAddrLocationMap* map,
     const void* compile_info);

typedef void (JNICALL *jvmtiEventCompiledMethodUnload)
    (jvmtiEnv *jvmti_env,
     jmethodID method,
     const void* code_addr);

typedef void (JNICALL *jvmtiEventDataDumpRequest)
    (jvmtiEnv *jvmti_env);

typedef void (JNICALL *jvmtiEventDynamicCodeGenerated)
    (jvmtiEnv *jvmti_env,
     const char* name,
     const void* address,
     jint length);

typedef void (JNICALL *jvmtiEventException)
    (jvmtiEnv *jvmti_env,
     JNIEnv* jni_env,
     jthread thread,
     jmethodID method,
     jlocation location,
     jobject exception,
     jmethodID catch_method,
     jlocation catch_location);

typedef void (JNICALL *jvmtiEventExceptionCatch)
    (jvmtiEnv *jvmti_env,
     JNIEnv* jni_env,
     jthread thread,
     jmethodID method,
     jlocation location,
     jobject exception);

typedef void (JNICALL *jvmtiEventFieldAccess)
    (jvmtiEnv *jvmti_env,
     JNIEnv* jni_env,
     jthread thread,
     jmethodID method,
     jlocation location,
     jclass field_klass,
     jobject object,
     jfieldID field);

typedef void (JNICALL *jvmtiEventFieldModification)
    (jvmtiEnv *jvmti_env,
     JNIEnv* jni_env,
     jthread thread,
     jmethodID method,
     jlocation location,
     jclass field_klass,
     jobject object,
     jfieldID field,
     char signature_type,
     jvalue new_value);

typedef void (JNICALL *jvmtiEventFramePop)
    (jvmtiEnv *jvmti_env,
     JNIEnv* jni_env,
     jthread thread,
     jmethodID method,
     jboolean was_popped_by_exception);

typedef void (JNICALL *jvmtiEventGarbageCollectionFinish)
    (jvmtiEnv *jvmti_env);

typedef void (JNICALL *jvmtiEventGarbageCollectionStart)
    (jvmtiEnv *jvmti_env);

typedef void (JNICALL *jvmtiEventMethodEntry)
    (jvmtiEnv *jvmti_env,
     JNIEnv* jni_env,
     jthread thread,
     jmethodID method);

typedef void (JNICALL *jvmtiEventMethodExit)
    (jvmtiEnv *jvmti_env,
     JNIEnv* jni_env,
     jthread thread,
     jmethodID method,
     jboolean was_popped_by_exception,
     jvalue return_value);

typedef void (JNICALL *jvmtiEventMonitorContendedEnter)
    (jvmtiEnv *jvmti_env,
     JNIEnv* jni_env,
     jthread thread,
     jobject object);

typedef void (JNICALL *jvmtiEventMonitorContendedEntered)
    (jvmtiEnv *jvmti_env,
     JNIEnv* jni_env,
     jthread thread,
     jobject object);

typedef void (JNICALL *jvmtiEventMonitorWait)
    (jvmtiEnv *jvmti_env,
     JNIEnv* jni_env,
     jthread thread,
     jobject object,
     jlong timeout);

typedef void (JNICALL *jvmtiEventMonitorWaited)
    (jvmtiEnv *jvmti_env,
     JNIEnv* jni_env,
     jthread thread,
     jobject object,
     jboolean timed_out);

typedef void (JNICALL *jvmtiEventNativeMethodBind)
    (jvmtiEnv *jvmti_env,
     JNIEnv* jni_env,
     jthread thread,
     jmethodID method,
     void* address,
     void** new_address_ptr);

typedef void (JNICALL *jvmtiEventObjectFree)
    (jvmtiEnv *jvmti_env,
     jlong tag);

typedef void (JNICALL *jvmtiEventResourceExhausted)
    (jvmtiEnv *jvmti_env,
     JNIEnv* jni_env,
     jint flags,
     const void* reserved,
     const char* description);

typedef void (JNICALL *jvmtiEventSingleStep)
    (jvmtiEnv *jvmti_env,
     JNIEnv* jni_env,
     jthread thread,
     jmethodID method,
     jlocation location);

typedef void (JNICALL *jvmtiEventThreadEnd)
    (jvmtiEnv *jvmti_env,
     JNIEnv* jni_env,
     jthread thread);

typedef void (JNICALL *jvmtiEventThreadStart)
    (jvmtiEnv *jvmti_env,
     JNIEnv* jni_env,
     jthread thread);

typedef void (JNICALL *jvmtiEventVMDeath)
    (jvmtiEnv *jvmti_env,
     JNIEnv* jni_env);

typedef void (JNICALL *jvmtiEventVMInit)
    (jvmtiEnv *jvmti_env,
     JNIEnv* jni_env,
     jthread thread);

typedef void (JNICALL *jvmtiEventVMObjectAlloc)
    (jvmtiEnv *jvmti_env,
     JNIEnv* jni_env,
     jthread thread,
     jobject object,
     jclass object_klass,
     jlong size);

typedef void (JNICALL *jvmtiEventVMStart)
    (jvmtiEnv *jvmti_env,
     JNIEnv* jni_env);

    /* Event Callback Structure */

typedef struct {
                              /*   50 : VM Initialization Event */
    jvmtiEventVMInit VMInit;
                              /*   51 : VM Death Event */
    jvmtiEventVMDeath VMDeath;
                              /*   52 : Thread Start */
    jvmtiEventThreadStart ThreadStart;
                              /*   53 : Thread End */
    jvmtiEventThreadEnd ThreadEnd;
                              /*   54 : Class File Load Hook */
    jvmtiEventClassFileLoadHook ClassFileLoadHook;
                              /*   55 : Class Load */
    jvmtiEventClassLoad ClassLoad;
                              /*   56 : Class Prepare */
    jvmtiEventClassPrepare ClassPrepare;
                              /*   57 : VM Start Event */
    jvmtiEventVMStart VMStart;
                              /*   58 : Exception */
    jvmtiEventException Exception;
                              /*   59 : Exception Catch */
    jvmtiEventExceptionCatch ExceptionCatch;
                              /*   60 : Single Step */
    jvmtiEventSingleStep SingleStep;
                              /*   61 : Frame Pop */
    jvmtiEventFramePop FramePop;
                              /*   62 : Breakpoint */
    jvmtiEventBreakpoint Breakpoint;
                              /*   63 : Field Access */
    jvmtiEventFieldAccess FieldAccess;
                              /*   64 : Field Modification */
    jvmtiEventFieldModification FieldModification;
                              /*   65 : Method Entry */
    jvmtiEventMethodEntry MethodEntry;
                              /*   66 : Method Exit */
    jvmtiEventMethodExit MethodExit;
                              /*   67 : Native Method Bind */
    jvmtiEventNativeMethodBind NativeMethodBind;
                              /*   68 : Compiled Method Load */
    jvmtiEventCompiledMethodLoad CompiledMethodLoad;
                              /*   69 : Compiled Method Unload */
    jvmtiEventCompiledMethodUnload CompiledMethodUnload;
                              /*   70 : Dynamic Code Generated */
    jvmtiEventDynamicCodeGenerated DynamicCodeGenerated;
                              /*   71 : Data Dump Request */
    jvmtiEventDataDumpRequest DataDumpRequest;
                              /*   72 */
    jvmtiEventReserved reserved72;
                              /*   73 : Monitor Wait */
    jvmtiEventMonitorWait MonitorWait;
                              /*   74 : Monitor Waited */
    jvmtiEventMonitorWaited MonitorWaited;
                              /*   75 : Monitor Contended Enter */
    jvmtiEventMonitorContendedEnter MonitorContendedEnter;
                              /*   76 : Monitor Contended Entered */
    jvmtiEventMonitorContendedEntered MonitorContendedEntered;
                              /*   77 */
    jvmtiEventReserved reserved77;
                              /*   78 */
    jvmtiEventReserved reserved78;
                              /*   79 */
    jvmtiEventReserved reserved79;
                              /*   80 : Resource Exhausted */
    jvmtiEventResourceExhausted ResourceExhausted;
                              /*   81 : Garbage Collection Start */
    jvmtiEventGarbageCollectionStart GarbageCollectionStart;
                              /*   82 : Garbage Collection Finish */
    jvmtiEventGarbageCollectionFinish GarbageCollectionFinish;
                              /*   83 : Object Free */
    jvmtiEventObjectFree ObjectFree;
                              /*   84 : VM Object Allocation */
    jvmtiEventVMObjectAlloc VMObjectAlloc;
} jvmtiEventCallbacks;


    /* Function Interface */

typedef struct jvmtiInterface_1_ {

  /*   1 :  RESERVED */
  void *reserved1;

  /*   2 : Set Event Notification Mode */
  jvmtiError (JNICALL *SetEventNotificationMode) (jvmtiEnv* env,
    jvmtiEventMode mode,
    jvmtiEvent event_type,
    jthread event_thread,
     ...);

  /*   3 :  RESERVED */
  void *reserved3;

  /*   4 : Get All Threads */
  jvmtiError (JNICALL *GetAllThreads) (jvmtiEnv* env,
    jint* threads_count_ptr,
    jthread** threads_ptr);

  /*   5 : Suspend Thread */
  jvmtiError (JNICALL *SuspendThread) (jvmtiEnv* env,
    jthread thread);

  /*   6 : Resume Thread */
  jvmtiError (JNICALL *ResumeThread) (jvmtiEnv* env,
    jthread thread);

  /*   7 : Stop Thread */
  jvmtiError (JNICALL *StopThread) (jvmtiEnv* env,
    jthread thread,
    jobject exception);

  /*   8 : Interrupt Thread */
  jvmtiError (JNICALL *InterruptThread) (jvmtiEnv* env,
    jthread thread);

  /*   9 : Get Thread Info */
  jvmtiError (JNICALL *GetThreadInfo) (jvmtiEnv* env,
    jthread thread,
    jvmtiThreadInfo* info_ptr);

  /*   10 : Get Owned Monitor Info */
  jvmtiError (JNICALL *GetOwnedMonitorInfo) (jvmtiEnv* env,
    jthread thread,
    jint* owned_monitor_count_ptr,
    jobject** owned_monitors_ptr);

  /*   11 : Get Current Contended Monitor */
  jvmtiError (JNICALL *GetCurrentContendedMonitor) (jvmtiEnv* env,
    jthread thread,
    jobject* monitor_ptr);

  /*   12 : Run Agent Thread */
  jvmtiError (JNICALL *RunAgentThread) (jvmtiEnv* env,
    jthread thread,
    jvmtiStartFunction proc,
    const void* arg,
    jint priority);

  /*   13 : Get Top Thread Groups */
  jvmtiError (JNICALL *GetTopThreadGroups) (jvmtiEnv* env,
    jint* group_count_ptr,
    jthreadGroup** groups_ptr);

  /*   14 : Get Thread Group Info */
  jvmtiError (JNICALL *GetThreadGroupInfo) (jvmtiEnv* env,
    jthreadGroup group,
    jvmtiThreadGroupInfo* info_ptr);

  /*   15 : Get Thread Group Children */
  jvmtiError (JNICALL *GetThreadGroupChildren) (jvmtiEnv* env,
    jthreadGroup group,
    jint* thread_count_ptr,
    jthread** threads_ptr,
    jint* group_count_ptr,
    jthreadGroup** groups_ptr);

  /*   16 : Get Frame Count */
  jvmtiError (JNICALL *GetFrameCount) (jvmtiEnv* env,
    jthread thread,
    jint* count_ptr);

  /*   17 : Get Thread State */
  jvmtiError (JNICALL *GetThreadState) (jvmtiEnv* env,
    jthread thread,
    jint* thread_state_ptr);

  /*   18 : Get Current Thread */
  jvmtiError (JNICALL *GetCurrentThread) (jvmtiEnv* env,
    jthread* thread_ptr);

  /*   19 : Get Frame Location */
  jvmtiError (JNICALL *GetFrameLocation) (jvmtiEnv* env,
    jthread thread,
    jint depth,
    jmethodID* method_ptr,
    jlocation* location_ptr);

  /*   20 : Notify Frame Pop */
  jvmtiError (JNICALL *NotifyFramePop) (jvmtiEnv* env,
    jthread thread,
    jint depth);

  /*   21 : Get Local Variable - Object */
  jvmtiError (JNICALL *GetLocalObject) (jvmtiEnv* env,
    jthread thread,
    jint depth,
    jint slot,
    jobject* value_ptr);

  /*   22 : Get Local Variable - Int */
  jvmtiError (JNICALL *GetLocalInt) (jvmtiEnv* env,
    jthread thread,
    jint depth,
    jint slot,
    jint* value_ptr);

  /*   23 : Get Local Variable - Long */
  jvmtiError (JNICALL *GetLocalLong) (jvmtiEnv* env,
    jthread thread,
    jint depth,
    jint slot,
    jlong* value_ptr);

  /*   24 : Get Local Variable - Float */
  jvmtiError (JNICALL *GetLocalFloat) (jvmtiEnv* env,
    jthread thread,
    jint depth,
    jint slot,
    jfloat* value_ptr);

  /*   25 : Get Local Variable - Double */
  jvmtiError (JNICALL *GetLocalDouble) (jvmtiEnv* env,
    jthread thread,
    jint depth,
    jint slot,
    jdouble* value_ptr);

  /*   26 : Set Local Variable - Object */
  jvmtiError (JNICALL *SetLocalObject) (jvmtiEnv* env,
    jthread thread,
    jint depth,
    jint slot,
    jobject value);

  /*   27 : Set Local Variable - Int */
  jvmtiError (JNICALL *SetLocalInt) (jvmtiEnv* env,
    jthread thread,
    jint depth,
    jint slot,
    jint value);

  /*   28 : Set Local Variable - Long */
  jvmtiError (JNICALL *SetLocalLong) (jvmtiEnv* env,
    jthread thread,
    jint depth,
    jint slot,
    jlong value);

  /*   29 : Set Local Variable - Float */
  jvmtiError (JNICALL *SetLocalFloat) (jvmtiEnv* env,
    jthread thread,
    jint depth,
    jint slot,
    jfloat value);

  /*   30 : Set Local Variable - Double */
  jvmtiError (JNICALL *SetLocalDouble) (jvmtiEnv* env,
    jthread thread,
    jint depth,
    jint slot,
    jdouble value);

  /*   31 : Create Raw Monitor */
  jvmtiError (JNICALL *CreateRawMonitor) (jvmtiEnv* env,
    const char* name,
    jrawMonitorID* monitor_ptr);

  /*   32 : Destroy Raw Monitor */
  jvmtiError (JNICALL *DestroyRawMonitor) (jvmtiEnv* env,
    jrawMonitorID monitor);

  /*   33 : Raw Monitor Enter */
  jvmtiError (JNICALL *RawMonitorEnter) (jvmtiEnv* env,
    jrawMonitorID monitor);

  /*   34 : Raw Monitor Exit */
  jvmtiError (JNICALL *RawMonitorExit) (jvmtiEnv* env,
    jrawMonitorID monitor);

  /*   35 : Raw Monitor Wait */
  jvmtiError (JNICALL *RawMonitorWait) (jvmtiEnv* env,
    jrawMonitorID monitor,
    jlong millis);

  /*   36 : Raw Monitor Notify */
  jvmtiError (JNICALL *RawMonitorNotify) (jvmtiEnv* env,
    jrawMonitorID monitor);

  /*   37 : Raw Monitor Notify All */
  jvmtiError (JNICALL *RawMonitorNotifyAll) (jvmtiEnv* env,
    jrawMonitorID monitor);

  /*   38 : Set Breakpoint */
  jvmtiError (JNICALL *SetBreakpoint) (jvmtiEnv* env,
    jmethodID method,
    jlocation location);

  /*   39 : Clear Breakpoint */
  jvmtiError (JNICALL *ClearBreakpoint) (jvmtiEnv* env,
    jmethodID method,
    jlocation location);

  /*   40 :  RESERVED */
  void *reserved40;

  /*   41 : Set Field Access Watch */
  jvmtiError (JNICALL *SetFieldAccessWatch) (jvmtiEnv* env,
    jclass klass,
    jfieldID field);

  /*   42 : Clear Field Access Watch */
  jvmtiError (JNICALL *ClearFieldAccessWatch) (jvmtiEnv* env,
    jclass klass,
    jfieldID field);

  /*   43 : Set Field Modification Watch */
  jvmtiError (JNICALL *SetFieldModificationWatch) (jvmtiEnv* env,
    jclass klass,
    jfieldID field);

  /*   44 : Clear Field Modification Watch */
  jvmtiError (JNICALL *ClearFieldModificationWatch) (jvmtiEnv* env,
    jclass klass,
    jfieldID field);

  /*   45 : Is Modifiable Class */
  jvmtiError (JNICALL *IsModifiableClass) (jvmtiEnv* env,
    jclass klass,
    jboolean* is_modifiable_class_ptr);

  /*   46 : Allocate */
  jvmtiError (JNICALL *Allocate) (jvmtiEnv* env,
    jlong size,
    unsigned char** mem_ptr);

  /*   47 : Deallocate */
  jvmtiError (JNICALL *Deallocate) (jvmtiEnv* env,
    unsigned char* mem);

  /*   48 : Get Class Signature */
  jvmtiError (JNICALL *GetClassSignature) (jvmtiEnv* env,
    jclass klass,
    char** signature_ptr,
    char** generic_ptr);

  /*   49 : Get Class Status */
  jvmtiError (JNICALL *GetClassStatus) (jvmtiEnv* env,
    jclass klass,
    jint* status_ptr);

  /*   50 : Get Source File Name */
  jvmtiError (JNICALL *GetSourceFileName) (jvmtiEnv* env,
    jclass klass,
    char** source_name_ptr);

  /*   51 : Get Class Modifiers */
  jvmtiError (JNICALL *GetClassModifiers) (jvmtiEnv* env,
    jclass klass,
    jint* modifiers_ptr);

  /*   52 : Get Class Methods */
  jvmtiError (JNICALL *GetClassMethods) (jvmtiEnv* env,
    jclass klass,
    jint* method_count_ptr,
    jmethodID** methods_ptr);

  /*   53 : Get Class Fields */
  jvmtiError (JNICALL *GetClassFields) (jvmtiEnv* env,
    jclass klass,
    jint* field_count_ptr,
    jfieldID** fields_ptr);

  /*   54 : Get Implemented Interfaces */
  jvmtiError (JNICALL *GetImplementedInterfaces) (jvmtiEnv* env,
    jclass klass,
    jint* interface_count_ptr,
    jclass** interfaces_ptr);

  /*   55 : Is Interface */
  jvmtiError (JNICALL *IsInterface) (jvmtiEnv* env,
    jclass klass,
    jboolean* is_interface_ptr);

  /*   56 : Is Array Class */
  jvmtiError (JNICALL *IsArrayClass) (jvmtiEnv* env,
    jclass klass,
    jboolean* is_array_class_ptr);

  /*   57 : Get Class Loader */
  jvmtiError (JNICALL *GetClassLoader) (jvmtiEnv* env,
    jclass klass,
    jobject* classloader_ptr);

  /*   58 : Get Object Hash Code */
  jvmtiError (JNICALL *GetObjectHashCode) (jvmtiEnv* env,
    jobject object,
    jint* hash_code_ptr);

  /*   59 : Get Object Monitor Usage */
  jvmtiError (JNICALL *GetObjectMonitorUsage) (jvmtiEnv* env,
    jobject object,
    jvmtiMonitorUsage* info_ptr);

  /*   60 : Get Field Name (and Signature) */
  jvmtiError (JNICALL *GetFieldName) (jvmtiEnv* env,
    jclass klass,
    jfieldID field,
    char** name_ptr,
    char** signature_ptr,
    char** generic_ptr);

  /*   61 : Get Field Declaring Class */
  jvmtiError (JNICALL *GetFieldDeclaringClass) (jvmtiEnv* env,
    jclass klass,
    jfieldID field,
    jclass* declaring_class_ptr);

  /*   62 : Get Field Modifiers */
  jvmtiError (JNICALL *GetFieldModifiers) (jvmtiEnv* env,
    jclass klass,
    jfieldID field,
    jint* modifiers_ptr);

  /*   63 : Is Field Synthetic */
  jvmtiError (JNICALL *IsFieldSynthetic) (jvmtiEnv* env,
    jclass klass,
    jfieldID field,
    jboolean* is_synthetic_ptr);

  /*   64 : Get Method Name (and Signature) */
  jvmtiError (JNICALL *GetMethodName) (jvmtiEnv* env,
    jmethodID method,
    char** name_ptr,
    char** signature_ptr,
    char** generic_ptr);

  /*   65 : Get Method Declaring Class */
  jvmtiError (JNICALL *GetMethodDeclaringClass) (jvmtiEnv* env,
    jmethodID method,
    jclass* declaring_class_ptr);

  /*   66 : Get Method Modifiers */
  jvmtiError (JNICALL *GetMethodModifiers) (jvmtiEnv* env,
    jmethodID method,
    jint* modifiers_ptr);

  /*   67 :  RESERVED */
  void *reserved67;

  /*   68 : Get Max Locals */
  jvmtiError (JNICALL *GetMaxLocals) (jvmtiEnv* env,
    jmethodID method,
    jint* max_ptr);

  /*   69 : Get Arguments Size */
  jvmtiError (JNICALL *GetArgumentsSize) (jvmtiEnv* env,
    jmethodID method,
    jint* size_ptr);

  /*   70 : Get Line Number Table */
  jvmtiError (JNICALL *GetLineNumberTable) (jvmtiEnv* env,
    jmethodID method,
    jint* entry_count_ptr,
    jvmtiLineNumberEntry** table_ptr);

  /*   71 : Get Method Location */
  jvmtiError (JNICALL *GetMethodLocation) (jvmtiEnv* env,
    jmethodID method,
    jlocation* start_location_ptr,
    jlocation* end_location_ptr);

  /*   72 : Get Local Variable Table */
  jvmtiError (JNICALL *GetLocalVariableTable) (jvmtiEnv* env,
    jmethodID method,
    jint* entry_count_ptr,
    jvmtiLocalVariableEntry** table_ptr);

  /*   73 : Set Native Method Prefix */
  jvmtiError (JNICALL *SetNativeMethodPrefix) (jvmtiEnv* env,
    const char* prefix);

  /*   74 : Set Native Method Prefixes */
  jvmtiError (JNICALL *SetNativeMethodPrefixes) (jvmtiEnv* env,
    jint prefix_count,
    char** prefixes);

  /*   75 : Get Bytecodes */
  jvmtiError (JNICALL *GetBytecodes) (jvmtiEnv* env,
    jmethodID method,
    jint* bytecode_count_ptr,
    unsigned char** bytecodes_ptr);

  /*   76 : Is Method Native */
  jvmtiError (JNICALL *IsMethodNative) (jvmtiEnv* env,
    jmethodID method,
    jboolean* is_native_ptr);

  /*   77 : Is Method Synthetic */
  jvmtiError (JNICALL *IsMethodSynthetic) (jvmtiEnv* env,
    jmethodID method,
    jboolean* is_synthetic_ptr);

  /*   78 : Get Loaded Classes */
  jvmtiError (JNICALL *GetLoadedClasses) (jvmtiEnv* env,
    jint* class_count_ptr,
    jclass** classes_ptr);

  /*   79 : Get Classloader Classes */
  jvmtiError (JNICALL *GetClassLoaderClasses) (jvmtiEnv* env,
    jobject initiating_loader,
    jint* class_count_ptr,
    jclass** classes_ptr);

  /*   80 : Pop Frame */
  jvmtiError (JNICALL *PopFrame) (jvmtiEnv* env,
    jthread thread);

  /*   81 : Force Early Return - Object */
  jvmtiError (JNICALL *ForceEarlyReturnObject) (jvmtiEnv* env,
    jthread thread,
    jobject value);

  /*   82 : Force Early Return - Int */
  jvmtiError (JNICALL *ForceEarlyReturnInt) (jvmtiEnv* env,
    jthread thread,
    jint value);

  /*   83 : Force Early Return - Long */
  jvmtiError (JNICALL *ForceEarlyReturnLong) (jvmtiEnv* env,
    jthread thread,
    jlong value);

  /*   84 : Force Early Return - Float */
  jvmtiError (JNICALL *ForceEarlyReturnFloat) (jvmtiEnv* env,
    jthread thread,
    jfloat value);

  /*   85 : Force Early Return - Double */
  jvmtiError (JNICALL *ForceEarlyReturnDouble) (jvmtiEnv* env,
    jthread thread,
    jdouble value);

  /*   86 : Force Early Return - Void */
  jvmtiError (JNICALL *ForceEarlyReturnVoid) (jvmtiEnv* env,
    jthread thread);

  /*   87 : Redefine Classes */
  jvmtiError (JNICALL *RedefineClasses) (jvmtiEnv* env,
    jint class_count,
    const jvmtiClassDefinition* class_definitions);

  /*   88 : Get Version Number */
  jvmtiError (JNICALL *GetVersionNumber) (jvmtiEnv* env,
    jint* version_ptr);

  /*   89 : Get Capabilities */
  jvmtiError (JNICALL *GetCapabilities) (jvmtiEnv* env,
    jvmtiCapabilities* capabilities_ptr);

  /*   90 : Get Source Debug Extension */
  jvmtiError (JNICALL *GetSourceDebugExtension) (jvmtiEnv* env,
    jclass klass,
    char** source_debug_extension_ptr);

  /*   91 : Is Method Obsolete */
  jvmtiError (JNICALL *IsMethodObsolete) (jvmtiEnv* env,
    jmethodID method,
    jboolean* is_obsolete_ptr);

  /*   92 : Suspend Thread List */
  jvmtiError (JNICALL *SuspendThreadList) (jvmtiEnv* env,
    jint request_count,
    const jthread* request_list,
    jvmtiError* results);

  /*   93 : Resume Thread List */
  jvmtiError (JNICALL *ResumeThreadList) (jvmtiEnv* env,
    jint request_count,
    const jthread* request_list,
    jvmtiError* results);

  /*   94 :  RESERVED */
  void *reserved94;

  /*   95 :  RESERVED */
  void *reserved95;

  /*   96 :  RESERVED */
  void *reserved96;

  /*   97 :  RESERVED */
  void *reserved97;

  /*   98 :  RESERVED */
  void *reserved98;

  /*   99 :  RESERVED */
  void *reserved99;

  /*   100 : Get All Stack Traces */
  jvmtiError (JNICALL *GetAllStackTraces) (jvmtiEnv* env,
    jint max_frame_count,
    jvmtiStackInfo** stack_info_ptr,
    jint* thread_count_ptr);

  /*   101 : Get Thread List Stack Traces */
  jvmtiError (JNICALL *GetThreadListStackTraces) (jvmtiEnv* env,
    jint thread_count,
    const jthread* thread_list,
    jint max_frame_count,
    jvmtiStackInfo** stack_info_ptr);

  /*   102 : Get Thread Local Storage */
  jvmtiError (JNICALL *GetThreadLocalStorage) (jvmtiEnv* env,
    jthread thread,
    void** data_ptr);

  /*   103 : Set Thread Local Storage */
  jvmtiError (JNICALL *SetThreadLocalStorage) (jvmtiEnv* env,
    jthread thread,
    const void* data);

  /*   104 : Get Stack Trace */
  jvmtiError (JNICALL *GetStackTrace) (jvmtiEnv* env,
    jthread thread,
    jint start_depth,
    jint max_frame_count,
    jvmtiFrameInfo* frame_buffer,
    jint* count_ptr);

  /*   105 :  RESERVED */
  void *reserved105;

  /*   106 : Get Tag */
  jvmtiError (JNICALL *GetTag) (jvmtiEnv* env,
    jobject object,
    jlong* tag_ptr);

  /*   107 : Set Tag */
  jvmtiError (JNICALL *SetTag) (jvmtiEnv* env,
    jobject object,
    jlong tag);

  /*   108 : Force Garbage Collection */
  jvmtiError (JNICALL *ForceGarbageCollection) (jvmtiEnv* env);

  /*   109 : Iterate Over Objects Reachable From Object */
  jvmtiError (JNICALL *IterateOverObjectsReachableFromObject) (jvmtiEnv* env,
    jobject object,
    jvmtiObjectReferenceCallback object_reference_callback,
    const void* user_data);

  /*   110 : Iterate Over Reachable Objects */
  jvmtiError (JNICALL *IterateOverReachableObjects) (jvmtiEnv* env,
    jvmtiHeapRootCallback heap_root_callback,
    jvmtiStackReferenceCallback stack_ref_callback,
    jvmtiObjectReferenceCallback object_ref_callback,
    const void* user_data);

  /*   111 : Iterate Over Heap */
  jvmtiError (JNICALL *IterateOverHeap) (jvmtiEnv* env,
    jvmtiHeapObjectFilter object_filter,
    jvmtiHeapObjectCallback heap_object_callback,
    const void* user_data);

  /*   112 : Iterate Over Instances Of Class */
  jvmtiError (JNICALL *IterateOverInstancesOfClass) (jvmtiEnv* env,
    jclass klass,
    jvmtiHeapObjectFilter object_filter,
    jvmtiHeapObjectCallback heap_object_callback,
    const void* user_data);

  /*   113 :  RESERVED */
  void *reserved113;

  /*   114 : Get Objects With Tags */
  jvmtiError (JNICALL *GetObjectsWithTags) (jvmtiEnv* env,
    jint tag_count,
    const jlong* tags,
    jint* count_ptr,
    jobject** object_result_ptr,
    jlong** tag_result_ptr);

  /*   115 : Follow References */
  jvmtiError (JNICALL *FollowReferences) (jvmtiEnv* env,
    jint heap_filter,
    jclass klass,
    jobject initial_object,
    const jvmtiHeapCallbacks* callbacks,
    const void* user_data);

  /*   116 : Iterate Through Heap */
  jvmtiError (JNICALL *IterateThroughHeap) (jvmtiEnv* env,
    jint heap_filter,
    jclass klass,
    const jvmtiHeapCallbacks* callbacks,
    const void* user_data);

  /*   117 :  RESERVED */
  void *reserved117;

  /*   118 :  RESERVED */
  void *reserved118;

  /*   119 :  RESERVED */
  void *reserved119;

  /*   120 : Set JNI Function Table */
  jvmtiError (JNICALL *SetJNIFunctionTable) (jvmtiEnv* env,
    const jniNativeInterface* function_table);

  /*   121 : Get JNI Function Table */
  jvmtiError (JNICALL *GetJNIFunctionTable) (jvmtiEnv* env,
    jniNativeInterface** function_table);

  /*   122 : Set Event Callbacks */
  jvmtiError (JNICALL *SetEventCallbacks) (jvmtiEnv* env,
    const jvmtiEventCallbacks* callbacks,
    jint size_of_callbacks);

  /*   123 : Generate Events */
  jvmtiError (JNICALL *GenerateEvents) (jvmtiEnv* env,
    jvmtiEvent event_type);

  /*   124 : Get Extension Functions */
  jvmtiError (JNICALL *GetExtensionFunctions) (jvmtiEnv* env,
    jint* extension_count_ptr,
    jvmtiExtensionFunctionInfo** extensions);

  /*   125 : Get Extension Events */
  jvmtiError (JNICALL *GetExtensionEvents) (jvmtiEnv* env,
    jint* extension_count_ptr,
    jvmtiExtensionEventInfo** extensions);

  /*   126 : Set Extension Event Callback */
  jvmtiError (JNICALL *SetExtensionEventCallback) (jvmtiEnv* env,
    jint extension_event_index,
    jvmtiExtensionEvent callback);

  /*   127 : Dispose Environment */
  jvmtiError (JNICALL *DisposeEnvironment) (jvmtiEnv* env);

  /*   128 : Get Error Name */
  jvmtiError (JNICALL *GetErrorName) (jvmtiEnv* env,
    jvmtiError error,
    char** name_ptr);

  /*   129 : Get JLocation Format */
  jvmtiError (JNICALL *GetJLocationFormat) (jvmtiEnv* env,
    jvmtiJlocationFormat* format_ptr);

  /*   130 : Get System Properties */
  jvmtiError (JNICALL *GetSystemProperties) (jvmtiEnv* env,
    jint* count_ptr,
    char*** property_ptr);

  /*   131 : Get System Property */
  jvmtiError (JNICALL *GetSystemProperty) (jvmtiEnv* env,
    const char* property,
    char** value_ptr);

  /*   132 : Set System Property */
  jvmtiError (JNICALL *SetSystemProperty) (jvmtiEnv* env,
    const char* property,
    const char* value);

  /*   133 : Get Phase */
  jvmtiError (JNICALL *GetPhase) (jvmtiEnv* env,
    jvmtiPhase* phase_ptr);

  /*   134 : Get Current Thread CPU Timer Information */
  jvmtiError (JNICALL *GetCurrentThreadCpuTimerInfo) (jvmtiEnv* env,
    jvmtiTimerInfo* info_ptr);

  /*   135 : Get Current Thread CPU Time */
  jvmtiError (JNICALL *GetCurrentThreadCpuTime) (jvmtiEnv* env,
    jlong* nanos_ptr);

  /*   136 : Get Thread CPU Timer Information */
  jvmtiError (JNICALL *GetThreadCpuTimerInfo) (jvmtiEnv* env,
    jvmtiTimerInfo* info_ptr);

  /*   137 : Get Thread CPU Time */
  jvmtiError (JNICALL *GetThreadCpuTime) (jvmtiEnv* env,
    jthread thread,
    jlong* nanos_ptr);

  /*   138 : Get Timer Information */
  jvmtiError (JNICALL *GetTimerInfo) (jvmtiEnv* env,
    jvmtiTimerInfo* info_ptr);

  /*   139 : Get Time */
  jvmtiError (JNICALL *GetTime) (jvmtiEnv* env,
    jlong* nanos_ptr);

  /*   140 : Get Potential Capabilities */
  jvmtiError (JNICALL *GetPotentialCapabilities) (jvmtiEnv* env,
    jvmtiCapabilities* capabilities_ptr);

  /*   141 :  RESERVED */
  void *reserved141;

  /*   142 : Add Capabilities */
  jvmtiError (JNICALL *AddCapabilities) (jvmtiEnv* env,
    const jvmtiCapabilities* capabilities_ptr);

  /*   143 : Relinquish Capabilities */
  jvmtiError (JNICALL *RelinquishCapabilities) (jvmtiEnv* env,
    const jvmtiCapabilities* capabilities_ptr);

  /*   144 : Get Available Processors */
  jvmtiError (JNICALL *GetAvailableProcessors) (jvmtiEnv* env,
    jint* processor_count_ptr);

  /*   145 : Get Class Version Numbers */
  jvmtiError (JNICALL *GetClassVersionNumbers) (jvmtiEnv* env,
    jclass klass,
    jint* minor_version_ptr,
    jint* major_version_ptr);

  /*   146 : Get Constant Pool */
  jvmtiError (JNICALL *GetConstantPool) (jvmtiEnv* env,
    jclass klass,
    jint* constant_pool_count_ptr,
    jint* constant_pool_byte_count_ptr,
    unsigned char** constant_pool_bytes_ptr);

  /*   147 : Get Environment Local Storage */
  jvmtiError (JNICALL *GetEnvironmentLocalStorage) (jvmtiEnv* env,
    void** data_ptr);

  /*   148 : Set Environment Local Storage */
  jvmtiError (JNICALL *SetEnvironmentLocalStorage) (jvmtiEnv* env,
    const void* data);

  /*   149 : Add To Bootstrap Class Loader Search */
  jvmtiError (JNICALL *AddToBootstrapClassLoaderSearch) (jvmtiEnv* env,
    const char* segment);

  /*   150 : Set Verbose Flag */
  jvmtiError (JNICALL *SetVerboseFlag) (jvmtiEnv* env,
    jvmtiVerboseFlag flag,
    jboolean value);

  /*   151 : Add To System Class Loader Search */
  jvmtiError (JNICALL *AddToSystemClassLoaderSearch) (jvmtiEnv* env,
    const char* segment);

  /*   152 : Retransform Classes */
  jvmtiError (JNICALL *RetransformClasses) (jvmtiEnv* env,
    jint class_count,
    const jclass* classes);

  /*   153 : Get Owned Monitor Stack Depth Info */
  jvmtiError (JNICALL *GetOwnedMonitorStackDepthInfo) (jvmtiEnv* env,
    jthread thread,
    jint* monitor_info_count_ptr,
    jvmtiMonitorStackDepthInfo** monitor_info_ptr);

  /*   154 : Get Object Size */
  jvmtiError (JNICALL *GetObjectSize) (jvmtiEnv* env,
    jobject object,
    jlong* size_ptr);

  /*   155 : Get Local Instance */
  jvmtiError (JNICALL *GetLocalInstance) (jvmtiEnv* env,
    jthread thread,
    jint depth,
    jobject* value_ptr);

} jvmtiInterface_1;

struct _jvmtiEnv {
    const struct jvmtiInterface_1_ *functions;
#ifdef __cplusplus


  jvmtiError Allocate(jlong size,
            unsigned char** mem_ptr) {
    return functions->Allocate(this, size, mem_ptr);
  }

  jvmtiError Deallocate(unsigned char* mem) {
    return functions->Deallocate(this, mem);
  }

  jvmtiError GetThreadState(jthread thread,
            jint* thread_state_ptr) {
    return functions->GetThreadState(this, thread, thread_state_ptr);
  }

  jvmtiError GetCurrentThread(jthread* thread_ptr) {
    return functions->GetCurrentThread(this, thread_ptr);
  }

  jvmtiError GetAllThreads(jint* threads_count_ptr,
            jthread** threads_ptr) {
    return functions->GetAllThreads(this, threads_count_ptr, threads_ptr);
  }

  jvmtiError SuspendThread(jthread thread) {
    return functions->SuspendThread(this, thread);
  }

  jvmtiError SuspendThreadList(jint request_count,
            const jthread* request_list,
            jvmtiError* results) {
    return functions->SuspendThreadList(this, request_count, request_list, results);
  }

  jvmtiError ResumeThread(jthread thread) {
    return functions->ResumeThread(this, thread);
  }

  jvmtiError ResumeThreadList(jint request_count,
            const jthread* request_list,
            jvmtiError* results) {
    return functions->ResumeThreadList(this, request_count, request_list, results);
  }

  jvmtiError StopThread(jthread thread,
            jobject exception) {
    return functions->StopThread(this, thread, exception);
  }

  jvmtiError InterruptThread(jthread thread) {
    return functions->InterruptThread(this, thread);
  }

  jvmtiError GetThreadInfo(jthread thread,
            jvmtiThreadInfo* info_ptr) {
    return functions->GetThreadInfo(this, thread, info_ptr);
  }

  jvmtiError GetOwnedMonitorInfo(jthread thread,
            jint* owned_monitor_count_ptr,
            jobject** owned_monitors_ptr) {
    return functions->GetOwnedMonitorInfo(this, thread, owned_monitor_count_ptr, owned_monitors_ptr);
  }

  jvmtiError GetOwnedMonitorStackDepthInfo(jthread thread,
            jint* monitor_info_count_ptr,
            jvmtiMonitorStackDepthInfo** monitor_info_ptr) {
    return functions->GetOwnedMonitorStackDepthInfo(this, thread, monitor_info_count_ptr, monitor_info_ptr);
  }

  jvmtiError GetCurrentContendedMonitor(jthread thread,
            jobject* monitor_ptr) {
    return functions->GetCurrentContendedMonitor(this, thread, monitor_ptr);
  }

  jvmtiError RunAgentThread(jthread thread,
            jvmtiStartFunction proc,
            const void* arg,
            jint priority) {
    return functions->RunAgentThread(this, thread, proc, arg, priority);
  }

  jvmtiError SetThreadLocalStorage(jthread thread,
            const void* data) {
    return functions->SetThreadLocalStorage(this, thread, data);
  }

  jvmtiError GetThreadLocalStorage(jthread thread,
            void** data_ptr) {
    return functions->GetThreadLocalStorage(this, thread, data_ptr);
  }

  jvmtiError GetTopThreadGroups(jint* group_count_ptr,
            jthreadGroup** groups_ptr) {
    return functions->GetTopThreadGroups(this, group_count_ptr, groups_ptr);
  }

  jvmtiError GetThreadGroupInfo(jthreadGroup group,
            jvmtiThreadGroupInfo* info_ptr) {
    return functions->GetThreadGroupInfo(this, group, info_ptr);
  }

  jvmtiError GetThreadGroupChildren(jthreadGroup group,
            jint* thread_count_ptr,
            jthread** threads_ptr,
            jint* group_count_ptr,
            jthreadGroup** groups_ptr) {
    return functions->GetThreadGroupChildren(this, group, thread_count_ptr, threads_ptr, group_count_ptr, groups_ptr);
  }

  jvmtiError GetStackTrace(jthread thread,
            jint start_depth,
            jint max_frame_count,
            jvmtiFrameInfo* frame_buffer,
            jint* count_ptr) {
    return functions->GetStackTrace(this, thread, start_depth, max_frame_count, frame_buffer, count_ptr);
  }

  jvmtiError GetAllStackTraces(jint max_frame_count,
            jvmtiStackInfo** stack_info_ptr,
            jint* thread_count_ptr) {
    return functions->GetAllStackTraces(this, max_frame_count, stack_info_ptr, thread_count_ptr);
  }

  jvmtiError GetThreadListStackTraces(jint thread_count,
            const jthread* thread_list,
            jint max_frame_count,
            jvmtiStackInfo** stack_info_ptr) {
    return functions->GetThreadListStackTraces(this, thread_count, thread_list, max_frame_count, stack_info_ptr);
  }

  jvmtiError GetFrameCount(jthread thread,
            jint* count_ptr) {
    return functions->GetFrameCount(this, thread, count_ptr);
  }

  jvmtiError PopFrame(jthread thread) {
    return functions->PopFrame(this, thread);
  }

  jvmtiError GetFrameLocation(jthread thread,
            jint depth,
            jmethodID* method_ptr,
            jlocation* location_ptr) {
    return functions->GetFrameLocation(this, thread, depth, method_ptr, location_ptr);
  }

  jvmtiError NotifyFramePop(jthread thread,
            jint depth) {
    return functions->NotifyFramePop(this, thread, depth);
  }

  jvmtiError ForceEarlyReturnObject(jthread thread,
            jobject value) {
    return functions->ForceEarlyReturnObject(this, thread, value);
  }

  jvmtiError ForceEarlyReturnInt(jthread thread,
            jint value) {
    return functions->ForceEarlyReturnInt(this, thread, value);
  }

  jvmtiError ForceEarlyReturnLong(jthread thread,
            jlong value) {
    return functions->ForceEarlyReturnLong(this, thread, value);
  }

  jvmtiError ForceEarlyReturnFloat(jthread thread,
            jfloat value) {
    return functions->ForceEarlyReturnFloat(this, thread, value);
  }

  jvmtiError ForceEarlyReturnDouble(jthread thread,
            jdouble value) {
    return functions->ForceEarlyReturnDouble(this, thread, value);
  }

  jvmtiError ForceEarlyReturnVoid(jthread thread) {
    return functions->ForceEarlyReturnVoid(this, thread);
  }

  jvmtiError FollowReferences(jint heap_filter,
            jclass klass,
            jobject initial_object,
            const jvmtiHeapCallbacks* callbacks,
            const void* user_data) {
    return functions->FollowReferences(this, heap_filter, klass, initial_object, callbacks, user_data);
  }

  jvmtiError IterateThroughHeap(jint heap_filter,
            jclass klass,
            const jvmtiHeapCallbacks* callbacks,
            const void* user_data) {
    return functions->IterateThroughHeap(this, heap_filter, klass, callbacks, user_data);
  }

  jvmtiError GetTag(jobject object,
            jlong* tag_ptr) {
    return functions->GetTag(this, object, tag_ptr);
  }

  jvmtiError SetTag(jobject object,
            jlong tag) {
    return functions->SetTag(this, object, tag);
  }

  jvmtiError GetObjectsWithTags(jint tag_count,
            const jlong* tags,
            jint* count_ptr,
            jobject** object_result_ptr,
            jlong** tag_result_ptr) {
    return functions->GetObjectsWithTags(this, tag_count, tags, count_ptr, object_result_ptr, tag_result_ptr);
  }

  jvmtiError ForceGarbageCollection() {
    return functions->ForceGarbageCollection(this);
  }

  jvmtiError IterateOverObjectsReachableFromObject(jobject object,
            jvmtiObjectReferenceCallback object_reference_callback,
            const void* user_data) {
    return functions->IterateOverObjectsReachableFromObject(this, object, object_reference_callback, user_data);
  }

  jvmtiError IterateOverReachableObjects(jvmtiHeapRootCallback heap_root_callback,
            jvmtiStackReferenceCallback stack_ref_callback,
            jvmtiObjectReferenceCallback object_ref_callback,
            const void* user_data) {
    return functions->IterateOverReachableObjects(this, heap_root_callback, stack_ref_callback, object_ref_callback, user_data);
  }

  jvmtiError IterateOverHeap(jvmtiHeapObjectFilter object_filter,
            jvmtiHeapObjectCallback heap_object_callback,
            const void* user_data) {
    return functions->IterateOverHeap(this, object_filter, heap_object_callback, user_data);
  }

  jvmtiError IterateOverInstancesOfClass(jclass klass,
            jvmtiHeapObjectFilter object_filter,
            jvmtiHeapObjectCallback heap_object_callback,
            const void* user_data) {
    return functions->IterateOverInstancesOfClass(this, klass, object_filter, heap_object_callback, user_data);
  }

  jvmtiError GetLocalObject(jthread thread,
            jint depth,
            jint slot,
            jobject* value_ptr) {
    return functions->GetLocalObject(this, thread, depth, slot, value_ptr);
  }

  jvmtiError GetLocalInstance(jthread thread,
            jint depth,
            jobject* value_ptr) {
    return functions->GetLocalInstance(this, thread, depth, value_ptr);
  }

  jvmtiError GetLocalInt(jthread thread,
            jint depth,
            jint slot,
            jint* value_ptr) {
    return functions->GetLocalInt(this, thread, depth, slot, value_ptr);
  }

  jvmtiError GetLocalLong(jthread thread,
            jint depth,
            jint slot,
            jlong* value_ptr) {
    return functions->GetLocalLong(this, thread, depth, slot, value_ptr);
  }

  jvmtiError GetLocalFloat(jthread thread,
            jint depth,
            jint slot,
            jfloat* value_ptr) {
    return functions->GetLocalFloat(this, thread, depth, slot, value_ptr);
  }

  jvmtiError GetLocalDouble(jthread thread,
            jint depth,
            jint slot,
            jdouble* value_ptr) {
    return functions->GetLocalDouble(this, thread, depth, slot, value_ptr);
  }

  jvmtiError SetLocalObject(jthread thread,
            jint depth,
            jint slot,
            jobject value) {
    return functions->SetLocalObject(this, thread, depth, slot, value);
  }

  jvmtiError SetLocalInt(jthread thread,
            jint depth,
            jint slot,
            jint value) {
    return functions->SetLocalInt(this, thread, depth, slot, value);
  }

  jvmtiError SetLocalLong(jthread thread,
            jint depth,
            jint slot,
            jlong value) {
    return functions->SetLocalLong(this, thread, depth, slot, value);
  }

  jvmtiError SetLocalFloat(jthread thread,
            jint depth,
            jint slot,
            jfloat value) {
    return functions->SetLocalFloat(this, thread, depth, slot, value);
  }

  jvmtiError SetLocalDouble(jthread thread,
            jint depth,
            jint slot,
            jdouble value) {
    return functions->SetLocalDouble(this, thread, depth, slot, value);
  }

  jvmtiError SetBreakpoint(jmethodID method,
            jlocation location) {
    return functions->SetBreakpoint(this, method, location);
  }

  jvmtiError ClearBreakpoint(jmethodID method,
            jlocation location) {
    return functions->ClearBreakpoint(this, method, location);
  }

  jvmtiError SetFieldAccessWatch(jclass klass,
            jfieldID field) {
    return functions->SetFieldAccessWatch(this, klass, field);
  }

  jvmtiError ClearFieldAccessWatch(jclass klass,
            jfieldID field) {
    return functions->ClearFieldAccessWatch(this, klass, field);
  }

  jvmtiError SetFieldModificationWatch(jclass klass,
            jfieldID field) {
    return functions->SetFieldModificationWatch(this, klass, field);
  }

  jvmtiError ClearFieldModificationWatch(jclass klass,
            jfieldID field) {
    return functions->ClearFieldModificationWatch(this, klass, field);
  }

  jvmtiError GetLoadedClasses(jint* class_count_ptr,
            jclass** classes_ptr) {
    return functions->GetLoadedClasses(this, class_count_ptr, classes_ptr);
  }

  jvmtiError GetClassLoaderClasses(jobject initiating_loader,
            jint* class_count_ptr,
            jclass** classes_ptr) {
    return functions->GetClassLoaderClasses(this, initiating_loader, class_count_ptr, classes_ptr);
  }

  jvmtiError GetClassSignature(jclass klass,
            char** signature_ptr,
            char** generic_ptr) {
    return functions->GetClassSignature(this, klass, signature_ptr, generic_ptr);
  }

  jvmtiError GetClassStatus(jclass klass,
            jint* status_ptr) {
    return functions->GetClassStatus(this, klass, status_ptr);
  }

  jvmtiError GetSourceFileName(jclass klass,
            char** source_name_ptr) {
    return functions->GetSourceFileName(this, klass, source_name_ptr);
  }

  jvmtiError GetClassModifiers(jclass klass,
            jint* modifiers_ptr) {
    return functions->GetClassModifiers(this, klass, modifiers_ptr);
  }

  jvmtiError GetClassMethods(jclass klass,
            jint* method_count_ptr,
            jmethodID** methods_ptr) {
    return functions->GetClassMethods(this, klass, method_count_ptr, methods_ptr);
  }

  jvmtiError GetClassFields(jclass klass,
            jint* field_count_ptr,
            jfieldID** fields_ptr) {
    return functions->GetClassFields(this, klass, field_count_ptr, fields_ptr);
  }

  jvmtiError GetImplementedInterfaces(jclass klass,
            jint* interface_count_ptr,
            jclass** interfaces_ptr) {
    return functions->GetImplementedInterfaces(this, klass, interface_count_ptr, interfaces_ptr);
  }

  jvmtiError GetClassVersionNumbers(jclass klass,
            jint* minor_version_ptr,
            jint* major_version_ptr) {
    return functions->GetClassVersionNumbers(this, klass, minor_version_ptr, major_version_ptr);
  }

  jvmtiError GetConstantPool(jclass klass,
            jint* constant_pool_count_ptr,
            jint* constant_pool_byte_count_ptr,
            unsigned char** constant_pool_bytes_ptr) {
    return functions->GetConstantPool(this, klass, constant_pool_count_ptr, constant_pool_byte_count_ptr, constant_pool_bytes_ptr);
  }

  jvmtiError IsInterface(jclass klass,
            jboolean* is_interface_ptr) {
    return functions->IsInterface(this, klass, is_interface_ptr);
  }

  jvmtiError IsArrayClass(jclass klass,
            jboolean* is_array_class_ptr) {
    return functions->IsArrayClass(this, klass, is_array_class_ptr);
  }

  jvmtiError IsModifiableClass(jclass klass,
            jboolean* is_modifiable_class_ptr) {
    return functions->IsModifiableClass(this, klass, is_modifiable_class_ptr);
  }

  jvmtiError GetClassLoader(jclass klass,
            jobject* classloader_ptr) {
    return functions->GetClassLoader(this, klass, classloader_ptr);
  }

  jvmtiError GetSourceDebugExtension(jclass klass,
            char** source_debug_extension_ptr) {
    return functions->GetSourceDebugExtension(this, klass, source_debug_extension_ptr);
  }

  jvmtiError RetransformClasses(jint class_count,
            const jclass* classes) {
    return functions->RetransformClasses(this, class_count, classes);
  }

  jvmtiError RedefineClasses(jint class_count,
            const jvmtiClassDefinition* class_definitions) {
    return functions->RedefineClasses(this, class_count, class_definitions);
  }

  jvmtiError GetObjectSize(jobject object,
            jlong* size_ptr) {
    return functions->GetObjectSize(this, object, size_ptr);
  }

  jvmtiError GetObjectHashCode(jobject object,
            jint* hash_code_ptr) {
    return functions->GetObjectHashCode(this, object, hash_code_ptr);
  }

  jvmtiError GetObjectMonitorUsage(jobject object,
            jvmtiMonitorUsage* info_ptr) {
    return functions->GetObjectMonitorUsage(this, object, info_ptr);
  }

  jvmtiError GetFieldName(jclass klass,
            jfieldID field,
            char** name_ptr,
            char** signature_ptr,
            char** generic_ptr) {
    return functions->GetFieldName(this, klass, field, name_ptr, signature_ptr, generic_ptr);
  }

  jvmtiError GetFieldDeclaringClass(jclass klass,
            jfieldID field,
            jclass* declaring_class_ptr) {
    return functions->GetFieldDeclaringClass(this, klass, field, declaring_class_ptr);
  }

  jvmtiError GetFieldModifiers(jclass klass,
            jfieldID field,
            jint* modifiers_ptr) {
    return functions->GetFieldModifiers(this, klass, field, modifiers_ptr);
  }

  jvmtiError IsFieldSynthetic(jclass klass,
            jfieldID field,
            jboolean* is_synthetic_ptr) {
    return functions->IsFieldSynthetic(this, klass, field, is_synthetic_ptr);
  }

  jvmtiError GetMethodName(jmethodID method,
            char** name_ptr,
            char** signature_ptr,
            char** generic_ptr) {
    return functions->GetMethodName(this, method, name_ptr, signature_ptr, generic_ptr);
  }

  jvmtiError GetMethodDeclaringClass(jmethodID method,
            jclass* declaring_class_ptr) {
    return functions->GetMethodDeclaringClass(this, method, declaring_class_ptr);
  }

  jvmtiError GetMethodModifiers(jmethodID method,
            jint* modifiers_ptr) {
    return functions->GetMethodModifiers(this, method, modifiers_ptr);
  }

  jvmtiError GetMaxLocals(jmethodID method,
            jint* max_ptr) {
    return functions->GetMaxLocals(this, method, max_ptr);
  }

  jvmtiError GetArgumentsSize(jmethodID method,
            jint* size_ptr) {
    return functions->GetArgumentsSize(this, method, size_ptr);
  }

  jvmtiError GetLineNumberTable(jmethodID method,
            jint* entry_count_ptr,
            jvmtiLineNumberEntry** table_ptr) {
    return functions->GetLineNumberTable(this, method, entry_count_ptr, table_ptr);
  }

  jvmtiError GetMethodLocation(jmethodID method,
            jlocation* start_location_ptr,
            jlocation* end_location_ptr) {
    return functions->GetMethodLocation(this, method, start_location_ptr, end_location_ptr);
  }

  jvmtiError GetLocalVariableTable(jmethodID method,
            jint* entry_count_ptr,
            jvmtiLocalVariableEntry** table_ptr) {
    return functions->GetLocalVariableTable(this, method, entry_count_ptr, table_ptr);
  }

  jvmtiError GetBytecodes(jmethodID method,
            jint* bytecode_count_ptr,
            unsigned char** bytecodes_ptr) {
    return functions->GetBytecodes(this, method, bytecode_count_ptr, bytecodes_ptr);
  }

  jvmtiError IsMethodNative(jmethodID method,
            jboolean* is_native_ptr) {
    return functions->IsMethodNative(this, method, is_native_ptr);
  }

  jvmtiError IsMethodSynthetic(jmethodID method,
            jboolean* is_synthetic_ptr) {
    return functions->IsMethodSynthetic(this, method, is_synthetic_ptr);
  }

  jvmtiError IsMethodObsolete(jmethodID method,
            jboolean* is_obsolete_ptr) {
    return functions->IsMethodObsolete(this, method, is_obsolete_ptr);
  }

  jvmtiError SetNativeMethodPrefix(const char* prefix) {
    return functions->SetNativeMethodPrefix(this, prefix);
  }

  jvmtiError SetNativeMethodPrefixes(jint prefix_count,
            char** prefixes) {
    return functions->SetNativeMethodPrefixes(this, prefix_count, prefixes);
  }

  jvmtiError CreateRawMonitor(const char* name,
            jrawMonitorID* monitor_ptr) {
    return functions->CreateRawMonitor(this, name, monitor_ptr);
  }

  jvmtiError DestroyRawMonitor(jrawMonitorID monitor) {
    return functions->DestroyRawMonitor(this, monitor);
  }

  jvmtiError RawMonitorEnter(jrawMonitorID monitor) {
    return functions->RawMonitorEnter(this, monitor);
  }

  jvmtiError RawMonitorExit(jrawMonitorID monitor) {
    return functions->RawMonitorExit(this, monitor);
  }

  jvmtiError RawMonitorWait(jrawMonitorID monitor,
            jlong millis) {
    return functions->RawMonitorWait(this, monitor, millis);
  }

  jvmtiError RawMonitorNotify(jrawMonitorID monitor) {
    return functions->RawMonitorNotify(this, monitor);
  }

  jvmtiError RawMonitorNotifyAll(jrawMonitorID monitor) {
    return functions->RawMonitorNotifyAll(this, monitor);
  }

  jvmtiError SetJNIFunctionTable(const jniNativeInterface* function_table) {
    return functions->SetJNIFunctionTable(this, function_table);
  }

  jvmtiError GetJNIFunctionTable(jniNativeInterface** function_table) {
    return functions->GetJNIFunctionTable(this, function_table);
  }

  jvmtiError SetEventCallbacks(const jvmtiEventCallbacks* callbacks,
            jint size_of_callbacks) {
    return functions->SetEventCallbacks(this, callbacks, size_of_callbacks);
  }

  jvmtiError SetEventNotificationMode(jvmtiEventMode mode,
            jvmtiEvent event_type,
            jthread event_thread,
             ...) {
    return functions->SetEventNotificationMode(this, mode, event_type, event_thread);
  }

  jvmtiError GenerateEvents(jvmtiEvent event_type) {
    return functions->GenerateEvents(this, event_type);
  }

  jvmtiError GetExtensionFunctions(jint* extension_count_ptr,
            jvmtiExtensionFunctionInfo** extensions) {
    return functions->GetExtensionFunctions(this, extension_count_ptr, extensions);
  }

  jvmtiError GetExtensionEvents(jint* extension_count_ptr,
            jvmtiExtensionEventInfo** extensions) {
    return functions->GetExtensionEvents(this, extension_count_ptr, extensions);
  }

  jvmtiError SetExtensionEventCallback(jint extension_event_index,
            jvmtiExtensionEvent callback) {
    return functions->SetExtensionEventCallback(this, extension_event_index, callback);
  }

  jvmtiError GetPotentialCapabilities(jvmtiCapabilities* capabilities_ptr) {
    return functions->GetPotentialCapabilities(this, capabilities_ptr);
  }

  jvmtiError AddCapabilities(const jvmtiCapabilities* capabilities_ptr) {
    return functions->AddCapabilities(this, capabilities_ptr);
  }

  jvmtiError RelinquishCapabilities(const jvmtiCapabilities* capabilities_ptr) {
    return functions->RelinquishCapabilities(this, capabilities_ptr);
  }

  jvmtiError GetCapabilities(jvmtiCapabilities* capabilities_ptr) {
    return functions->GetCapabilities(this, capabilities_ptr);
  }

  jvmtiError GetCurrentThreadCpuTimerInfo(jvmtiTimerInfo* info_ptr) {
    return functions->GetCurrentThreadCpuTimerInfo(this, info_ptr);
  }

  jvmtiError GetCurrentThreadCpuTime(jlong* nanos_ptr) {
    return functions->GetCurrentThreadCpuTime(this, nanos_ptr);
  }

  jvmtiError GetThreadCpuTimerInfo(jvmtiTimerInfo* info_ptr) {
    return functions->GetThreadCpuTimerInfo(this, info_ptr);
  }

  jvmtiError GetThreadCpuTime(jthread thread,
            jlong* nanos_ptr) {
    return functions->GetThreadCpuTime(this, thread, nanos_ptr);
  }

  jvmtiError GetTimerInfo(jvmtiTimerInfo* info_ptr) {
    return functions->GetTimerInfo(this, info_ptr);
  }

  jvmtiError GetTime(jlong* nanos_ptr) {
    return functions->GetTime(this, nanos_ptr);
  }

  jvmtiError GetAvailableProcessors(jint* processor_count_ptr) {
    return functions->GetAvailableProcessors(this, processor_count_ptr);
  }

  jvmtiError AddToBootstrapClassLoaderSearch(const char* segment) {
    return functions->AddToBootstrapClassLoaderSearch(this, segment);
  }

  jvmtiError AddToSystemClassLoaderSearch(const char* segment) {
    return functions->AddToSystemClassLoaderSearch(this, segment);
  }

  jvmtiError GetSystemProperties(jint* count_ptr,
            char*** property_ptr) {
    return functions->GetSystemProperties(this, count_ptr, property_ptr);
  }

  jvmtiError GetSystemProperty(const char* property,
            char** value_ptr) {
    return functions->GetSystemProperty(this, property, value_ptr);
  }

  jvmtiError SetSystemProperty(const char* property,
            const char* value) {
    return functions->SetSystemProperty(this, property, value);
  }

  jvmtiError GetPhase(jvmtiPhase* phase_ptr) {
    return functions->GetPhase(this, phase_ptr);
  }

  jvmtiError DisposeEnvironment() {
    return functions->DisposeEnvironment(this);
  }

  jvmtiError SetEnvironmentLocalStorage(const void* data) {
    return functions->SetEnvironmentLocalStorage(this, data);
  }

  jvmtiError GetEnvironmentLocalStorage(void** data_ptr) {
    return functions->GetEnvironmentLocalStorage(this, data_ptr);
  }

  jvmtiError GetVersionNumber(jint* version_ptr) {
    return functions->GetVersionNumber(this, version_ptr);
  }

  jvmtiError GetErrorName(jvmtiError error,
            char** name_ptr) {
    return functions->GetErrorName(this, error, name_ptr);
  }

  jvmtiError SetVerboseFlag(jvmtiVerboseFlag flag,
            jboolean value) {
    return functions->SetVerboseFlag(this, flag, value);
  }

  jvmtiError GetJLocationFormat(jvmtiJlocationFormat* format_ptr) {
    return functions->GetJLocationFormat(this, format_ptr);
  }

#endif /* __cplusplus */
};


#ifdef __cplusplus
} /* extern "C" */
#endif /* __cplusplus */

#endif /* !_JAVA_JVMTI_H_ */


