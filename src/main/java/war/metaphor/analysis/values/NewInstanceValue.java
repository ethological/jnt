package war.metaphor.analysis.values;

import lombok.Getter;
import lombok.Setter;
import org.objectweb.asm.Type;
import war.metaphor.analysis.types.TypeInstance;

@Setter
@Getter
public class NewInstanceValue extends VirtualValue {

    private static int ID_COUNTER = 0;
    private static int SUB_ID_COUNTER = 0;

    private boolean initialized, parameter;

    @Getter
    private int id;

    public int subId;

    public NewInstanceValue(Type type, boolean initialized, boolean parameter) {
        super(type);
        this.initialized = initialized;
        this.parameter = parameter;
        this.id = ID_COUNTER++;
        this.subId = SUB_ID_COUNTER++;
    }

    public static NewInstanceValue of(Type type, boolean initialized, boolean parameter) {
        return new NewInstanceValue(type, initialized, parameter);
    }

    public static NewInstanceValue of(Type type, boolean initialized) {
        return new NewInstanceValue(type, initialized, false);
    }

    public static NewInstanceValue of(Type type) {
        return new NewInstanceValue(type, false, false);
    }

    public static NewInstanceValue of(NewInstanceValue old) {
        NewInstanceValue v = new NewInstanceValue(old.getVirtualType(), old.isInitialized(), old.isParameter());
        v.setId(old.getId());
        return v;
    }

    public boolean isCopy(NewInstanceValue copy) {
        return id == copy.id;
    }

    @Override
    public SimpleValue copy() {
        return NewInstanceValue.of(this);
    }

    @Override
    public TypeInstance getType() {
        return new TypeInstance(super.getType(), initialized);
    }

    public Type getVirtualType() {
        return super.getType();
    }

    @Override
    public String toString() {
        return (initialized ? "I" : "U") + "(" + getType() + ")";
    }

    @Override
    public boolean equals(Object value) {
        if (value instanceof NewInstanceValue v) {
            return v.getType().equals(getType()) && v.isInitialized() == initialized && v.isParameter() == parameter;
        }
        return false;
    }
}
