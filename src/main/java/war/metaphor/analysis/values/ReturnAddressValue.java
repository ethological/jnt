package war.metaphor.analysis.values;

import war.metaphor.util.asm.TypeChecker;
import org.objectweb.asm.tree.analysis.BasicValue;

public class ReturnAddressValue extends SimpleValue {

    public ReturnAddressValue() {
        super(null);
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
        return new ReturnAddressValue();
    }
}
