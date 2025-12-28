package war.metaphor.mutator.runtime;

import sun.misc.Unsafe;

import java.lang.invoke.MethodType;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.Base64;

/**
 * @author etho
 * Unsafe, my beloved.
 */
public final class RuntimePatcher {

    private static final byte XOR_MAGIC = (byte) 0xA5;

    public static void apply(Class<?> owner) {
        try {
            Object cp = getConstantPool(owner);
            if (cp == null) return;

            java.lang.reflect.Method mSize = cp.getClass().getDeclaredMethod("getSize");
            java.lang.reflect.Method mTag = cp.getClass().getDeclaredMethod("getTagAt", int.class);
            java.lang.reflect.Method mUtf8 = cp.getClass().getDeclaredMethod("getUtf8At", int.class);
            mSize.setAccessible(true);
            mTag.setAccessible(true);
            mUtf8.setAccessible(true);

            int sz = (int) mSize.invoke(cp);
            Base64.Decoder dec = Base64.getDecoder();
            Unsafe unsafe = getUnsafe();
            if (unsafe == null) return;

            for (int i = 1; i < sz; i++) {
                int tag = (int) mTag.invoke(cp, i);
                if (tag != 1) continue; // CONSTANT_Utf8
                String s = (String) mUtf8.invoke(cp, i);
                int sep = s.indexOf('\0');
                if (sep == -1) continue;

                String key = s.substring(0, sep); // methodName+desc
                byte[] enc = dec.decode(s.substring(sep + 1));
                xor(enc);

                // anonymous patch class using reflection to avoid compile-time dependency
                Class<?> patch;
                try {
                    java.lang.reflect.Method mDef = Unsafe.class.getDeclaredMethod("defineAnonymousClass", Class.class, byte[].class, Object[].class);
                    mDef.setAccessible(true);
                    patch = (Class<?>) mDef.invoke(unsafe, owner, enc, null);
                } catch (NoSuchMethodException nf) {
                    // Fallback: JDK 15+ may expose defineAnonymousClass on jdk.internal.misc.Unsafe; ignore if not found
                    return;
                }

                // locate target and patch method pointer
                patchMethodPointer(owner, patch, key, unsafe);
            }
        } catch (Throwable ignored) {
        }
    }

    private static Object getConstantPool(Class<?> cls) {
        try {
            Method m = Class.class.getDeclaredMethod("getConstantPool");
            m.setAccessible(true);
            return m.invoke(cls);
        } catch (Throwable t) {
            return null;
        }
    }

    private static void xor(byte[] data) {
        for (int i = 0; i < data.length; i++) {
            data[i] ^= XOR_MAGIC;
        }
    }

    private static Unsafe getUnsafe() {
        try {
            Field f = Unsafe.class.getDeclaredField("theUnsafe");
            f.setAccessible(true);
            return (Unsafe) f.get(null);
        } catch (Throwable t) {
            return null;
        }
    }

    private static void patchMethodPointer(Class<?> owner, Class<?> patch, String key, Unsafe unsafe) {
        try {
            int idx = key.indexOf('(');
            String name = key.substring(0, idx);
            String desc = key.substring(idx);

            MethodType mt = MethodType.fromMethodDescriptorString(desc, owner.getClassLoader());
            Method target = owner.getDeclaredMethod(name, mt.parameterArray());
            Method source = patch.getDeclaredMethod(name, mt.parameterArray());
            target.setAccessible(true);
            source.setAccessible(true);

            // stupidity incarnate
            String[] fields = {"artMethod", "methodAccessor", "root", "vm_extra_data"};
            for (String fName : fields) {
                Field fld;
                try {
                    fld = Method.class.getDeclaredField(fName);
                } catch (NoSuchFieldException ignored) {
                    continue;
                }
                fld.setAccessible(true);
                long off = unsafe.objectFieldOffset(fld);
                long ptr = unsafe.getLong(source, off);
                unsafe.putLong(target, off, ptr);
                return;
            }
        } catch (Throwable ignored) {
        }
    }

    // No instantiation
    private RuntimePatcher() {
    }
} 