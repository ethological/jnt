package war.metaphor.analysis.values;

import lombok.Getter;
import org.objectweb.asm.Type;

@Getter
public class MemoryValue extends VirtualValue {

    private final Object value;

    public MemoryValue(Type type, Object value) {
        super(type);
        this.value = value;
    }

    public static MemoryValue of(Type type, Object value) {
        return new MemoryValue(type, value);
    }

    @Override
    public SimpleValue copy() {
        return of(getType(), value);
    }
}
