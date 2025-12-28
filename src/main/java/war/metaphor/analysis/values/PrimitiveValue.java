package war.metaphor.analysis.values;

import war.metaphor.util.asm.TypeChecker;
import org.objectweb.asm.Type;
import org.objectweb.asm.tree.analysis.BasicValue;
import war.metaphor.util.asm.TypeUtil;

public class PrimitiveValue extends SimpleValue {

    public PrimitiveValue(Type type) {
        super(type);
    }

    public static PrimitiveValue of(Type type) {
        return new PrimitiveValue(type);
    }

    @Override
    public boolean canMerge(BasicValue v, TypeChecker typeChecker) {
        if (v == this)
            return true;
        else if (!(v instanceof PrimitiveValue))
            return false;
        return getType().equals(v.getType()) || ((PrimitiveValue) v).isPromotion(this);
    }

    private boolean isPromotion(PrimitiveValue v) {
        int i1 = TypeUtil.getPromotionIndex(getType().getSort());
        int i2 = TypeUtil.getPromotionIndex(v.getType().getSort());
        return i1 >= i2;
    }

    @Override
    public SimpleValue copy() {
        return of(getType());
    }
}
