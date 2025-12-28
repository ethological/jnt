package war.metaphor.analysis.values;

import war.metaphor.util.asm.TypeChecker;
import org.objectweb.asm.Type;
import org.objectweb.asm.tree.analysis.BasicValue;

public abstract class SimpleValue extends BasicValue {

    public SimpleValue(Type type) {
        super(type);
    }

    public abstract boolean canMerge(BasicValue v, TypeChecker typeChecker);

    @Override
    public boolean isUninitialized() {
        return this == UninitializedValue.INSTANCE;
    }

    public abstract SimpleValue copy();
}
