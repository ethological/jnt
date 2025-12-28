package war.metaphor.gens.structs;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.Setter;
import org.objectweb.asm.Type;

@Getter
@RequiredArgsConstructor
public class GValue {

    private final Type type;
    private final Object value;

    public boolean isNullConstant;

    @Setter
    private boolean disallowRemove = false;

    public static GValue nullType() {
        GValue v = new GValue(null, null);
        v.isNullConstant = true;
        return v;
    }

    public static GValue of(int value) {
        return new GValue(Type.INT_TYPE, value);
    }

    public static GValue of(long value) {
        return new GValue(Type.LONG_TYPE, value);
    }

    public static GValue of(float value) {
        return new GValue(Type.FLOAT_TYPE, value);
    }

    public static GValue of(double value) {
        return new GValue(Type.DOUBLE_TYPE, value);
    }

    public static GValue of(String value) {
        return new GValue(Type.getType(String.class), value);
    }

    public static GValue of(Type type, Object value) {
        return new GValue(type, value);
    }

    public static GValue of(Type type) {
        return new GValue(type, null);
    }

    public int getSize() {
        return type == null ? 1 : type.getSize();
    }

    public boolean isTop() {
        if (this instanceof GTopValue) return true;
        return type == Type.VOID_TYPE && value != null && value.equals(-1);
    }

    public boolean isValueKnown() {
        return value != null;
    }

    @Override
    public String toString() {
        return "V(" + type + ")";
    }
}
