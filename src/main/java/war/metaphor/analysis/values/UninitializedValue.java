package war.metaphor.analysis.values;

import war.metaphor.util.asm.TypeChecker;
import org.objectweb.asm.Type;
import org.objectweb.asm.tree.analysis.BasicValue;

public class UninitializedValue extends SimpleValue {

    public static final UninitializedValue INSTANCE = new UninitializedValue(null);

    public UninitializedValue(Type type) {
        super(type);
    }

    @Override
    public boolean canMerge(BasicValue v, TypeChecker typeChecker) {
        return v == this;
    }

    @Override
    public boolean isUninitialized() {
        return true;
    }

    @Override
    public SimpleValue copy() {
        return INSTANCE;
    }
}
