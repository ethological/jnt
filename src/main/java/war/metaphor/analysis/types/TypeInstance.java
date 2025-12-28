package war.metaphor.analysis.types;

import org.objectweb.asm.Type;

public class TypeInstance extends Type {

    private final boolean initialized;

    public TypeInstance(Type type, boolean initialized) {
        super(type.sort, type.valueBuffer, type.valueBegin, type.valueEnd);
        this.initialized = initialized;
    }

    @Override
    public int hashCode() {
        return super.hashCode() * 31 + (initialized ? 1 : 0);
    }

    @Override
    public boolean equals(Object object) {
        if (!(object instanceof TypeInstance type))
            return false;
        return super.equals(type) && initialized == type.initialized;
    }

}
