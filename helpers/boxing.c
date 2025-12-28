#include "boxing.h"

static jclass kls_Integer;
static jmethodID mid_Integer_valueOf;
static jmethodID mid_Integer_intValue;

static jclass kls_Long;
static jmethodID mid_Long_valueOf;
static jmethodID mid_Long_longValue;

static jclass kls_Character;
static jmethodID mid_Character_valueOf;
static jmethodID mid_Character_charValue;

static jclass kls_Short;
static jmethodID mid_Short_valueOf;
static jmethodID mid_Short_shortValue;

static jclass kls_Byte;
static jmethodID mid_Byte_valueOf;
static jmethodID mid_Byte_byteValue;

static jclass kls_Boolean;
static jmethodID mid_Boolean_valueOf;
static jmethodID mid_Boolean_booleanValue;

static jclass kls_Float;
static jmethodID mid_Float_valueOf;
static jmethodID mid_Float_floatValue;

static jclass kls_Double;
static jmethodID mid_Double_valueOf;
static jmethodID mid_Double_doubleValue;

void init_boxing_classes(JNIEnv* env) {
    #define CACHE_BOXING(wrapper, sigBox, nameUnbox, sigUnbox, cls, midBox, midUnbox) do { \
        jclass t = (*env)->FindClass(env, "java/lang/" wrapper); \
        if (!t) return; \
        cls = (*env)->NewGlobalRef(env, t); \
        (*env)->DeleteLocalRef(env, t); \
        midBox = (*env)->GetStaticMethodID(env, cls, "valueOf", sigBox); \
        if (!midBox) return; \
        midUnbox = (*env)->GetMethodID(env, cls, nameUnbox, sigUnbox); \
        if (!midUnbox) return; \
    } while (0)

    CACHE_BOXING("Integer", "(I)Ljava/lang/Integer;", "intValue", "()I", kls_Integer, mid_Integer_valueOf, mid_Integer_intValue);
    CACHE_BOXING("Long", "(J)Ljava/lang/Long;", "longValue", "()J", kls_Long, mid_Long_valueOf, mid_Long_longValue);
    CACHE_BOXING("Character", "(C)Ljava/lang/Character;", "charValue", "()C", kls_Character, mid_Character_valueOf, mid_Character_charValue);
    CACHE_BOXING("Short", "(S)Ljava/lang/Short;", "shortValue", "()S", kls_Short, mid_Short_valueOf, mid_Short_shortValue);
    CACHE_BOXING("Byte", "(B)Ljava/lang/Byte;", "byteValue", "()B", kls_Byte, mid_Byte_valueOf, mid_Byte_byteValue);
    CACHE_BOXING("Boolean", "(Z)Ljava/lang/Boolean;", "booleanValue", "()Z", kls_Boolean, mid_Boolean_valueOf, mid_Boolean_booleanValue);
    CACHE_BOXING("Float", "(F)Ljava/lang/Float;", "floatValue", "()F", kls_Float, mid_Float_valueOf, mid_Float_floatValue);
    CACHE_BOXING("Double", "(D)Ljava/lang/Double;", "doubleValue", "()D", kls_Double, mid_Double_valueOf, mid_Double_doubleValue);

    #undef CACHE_BOXING
}

jobject make_object_int(JNIEnv* env, jint v) { return (*env)->CallStaticObjectMethod(env, kls_Integer, mid_Integer_valueOf, v); }
jobject make_object_long(JNIEnv* env, jlong v) { return (*env)->CallStaticObjectMethod(env, kls_Long, mid_Long_valueOf, v); }
jobject make_object_char(JNIEnv* env, jchar v) { return (*env)->CallStaticObjectMethod(env, kls_Character, mid_Character_valueOf, v); }
jobject make_object_short(JNIEnv* env, jshort v) { return (*env)->CallStaticObjectMethod(env, kls_Short, mid_Short_valueOf, v); }
jobject make_object_byte(JNIEnv* env, jbyte v) { return (*env)->CallStaticObjectMethod(env, kls_Byte, mid_Byte_valueOf, v); }
jobject make_object_boolean(JNIEnv* env, jboolean v) { return (*env)->CallStaticObjectMethod(env, kls_Boolean, mid_Boolean_valueOf, v); }
jobject make_object_float(JNIEnv* env, jfloat v) { return (*env)->CallStaticObjectMethod(env, kls_Float, mid_Float_valueOf, v); }
jobject make_object_double(JNIEnv* env, jdouble v) { return (*env)->CallStaticObjectMethod(env, kls_Double, mid_Double_valueOf, v); }

jint unbox_int(JNIEnv* env, jobject o) { return (*env)->CallIntMethod(env, o, mid_Integer_intValue); }
jlong unbox_long(JNIEnv* env, jobject o) { return (*env)->CallLongMethod(env, o, mid_Long_longValue); }
jchar unbox_char(JNIEnv* env, jobject o) { return (*env)->CallCharMethod(env, o, mid_Character_charValue); }
jshort unbox_short(JNIEnv* env, jobject o) { return (*env)->CallShortMethod(env, o, mid_Short_shortValue); }
jbyte unbox_byte(JNIEnv* env, jobject o) { return (*env)->CallByteMethod(env, o, mid_Byte_byteValue); }
jboolean unbox_boolean(JNIEnv* env, jobject o) { return (*env)->CallBooleanMethod(env, o, mid_Boolean_booleanValue); }
jfloat unbox_float(JNIEnv* env, jobject o) { return (*env)->CallFloatMethod(env, o, mid_Float_floatValue); }
jdouble unbox_double(JNIEnv* env, jobject o) { return (*env)->CallDoubleMethod(env, o, mid_Double_doubleValue); }
