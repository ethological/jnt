package war.metaphor.util;

import lombok.AllArgsConstructor;
import org.objectweb.asm.Type;

import java.util.LinkedList;
import java.util.List;

@AllArgsConstructor
public class Descriptor {

    private String desc;

    public static Descriptor of(String desc) {
        return new Descriptor(desc);
    }

    public static Descriptor of(Type returnType) {
        return of("()" + returnType.getDescriptor());
    }

    public Descriptor add(Type type) {
        Type[] types = Type.getArgumentTypes(desc);
        Type[] newTypes = new Type[types.length + 1];
        System.arraycopy(types, 0, newTypes, 0, types.length);
        newTypes[types.length] = type;
        desc = Type.getMethodDescriptor(Type.getReturnType(desc), newTypes);
        return this;
    }

    public Descriptor insert(Type type, int index) {
        Type[] types = Type.getArgumentTypes(desc);
        Type[] newTypes = new Type[types.length + 1];
        System.arraycopy(types, 0, newTypes, 0, index);
        newTypes[index] = type;
        System.arraycopy(types, index, newTypes, index + 1, types.length - index);
        desc = Type.getMethodDescriptor(Type.getReturnType(desc), newTypes);
        return this;
    }

    public Descriptor insert(Type type) {
        return insert(type, 0);
    }

    public Descriptor returnType(Type type) {
        desc = Type.getMethodDescriptor(type, Type.getArgumentTypes(desc));
        return this;
    }

    public Descriptor removeAll() {
        desc = Type.getMethodDescriptor(Type.getReturnType(desc));
        return this;
    }

    public List<Integer> getLast(List<Type> types, boolean isStatic) {
        int index = getLast(isStatic);
        LinkedList<Integer> cache = new LinkedList<>();
        Type[] args = Type.getArgumentTypes(desc);
        for (int i = args.length - 1; i >= 0; i--) {
            Type t = args[i];
            index -= t.getSize();
            if (types.contains(t))
                cache.addFirst(index);
            if (cache.size() == types.size())
                return cache;
        }
        return cache;
    }

    public int getLast(Type type, boolean isStatic) {
        int index = isStatic ? 0 : 1;
        int cache = -1;
        Type[] types = Type.getArgumentTypes(desc);
        for (Type t : types) {
            if (t.equals(type))
                cache = index;
            index += t.getSize();
        }
        return cache;
    }

    public int getLast(boolean isStatic) {
        Type[] types = Type.getArgumentTypes(desc);
        int index = isStatic ? 0 : 1;
        for (Type t : types) {
            index += t.getSize();
        }
        return index;
    }

    public String toString() {
        return desc;
    }

    public void remove(int i) {
        Type[] types = Type.getArgumentTypes(desc);
        if (i < 0 || i >= types.length) {
            throw new IndexOutOfBoundsException("Index " + i + " is out of bounds for descriptor: " + desc);
        }
        Type[] newTypes = new Type[types.length - 1];
        System.arraycopy(types, 0, newTypes, 0, i);
        System.arraycopy(types, i + 1, newTypes, i, types.length - i - 1);
        desc = Type.getMethodDescriptor(Type.getReturnType(desc), newTypes);
    }

    public void setAll(Type type) {
        Type[] types = Type.getArgumentTypes(desc);
        Type[] newTypes = new Type[types.length];
        for (int i = 0; i < types.length; i++) {
            newTypes[i] = type;
        }
        desc = Type.getMethodDescriptor(Type.getReturnType(desc), newTypes);
    }
}