package war.metaphor.analysis.values;

import war.metaphor.util.asm.TypeChecker;
import org.objectweb.asm.Type;
import org.objectweb.asm.tree.analysis.BasicValue;

public class NullValue extends SimpleValue {

    public NullValue(Type type) {
        super(type);
    }

    public static NullValue of(Type type) {
        return new NullValue(type);
    }

    @Override
    public boolean canMerge(BasicValue v, TypeChecker typeChecker) {
        return v == this;
    }

    @Override
    public SimpleValue copy() {
        return of(getType());
    }
}
