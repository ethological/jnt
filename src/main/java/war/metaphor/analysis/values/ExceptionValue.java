package war.metaphor.analysis.values;

import org.objectweb.asm.Type;

public class ExceptionValue extends VirtualValue {

    public ExceptionValue(Type type) {
        super(type);
    }

    public static ExceptionValue of(Type type) {
        return new ExceptionValue(type);
    }

    @Override
    public SimpleValue copy() {
        return of(getType());
    }
}
