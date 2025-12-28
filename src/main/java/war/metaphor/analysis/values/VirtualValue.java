package war.metaphor.analysis.values;

import war.metaphor.util.asm.TypeChecker;
import org.objectweb.asm.Type;
import org.objectweb.asm.tree.analysis.BasicValue;

public class VirtualValue extends SimpleValue {

    public VirtualValue(Type type) {
        super(type);
    }

    public static VirtualValue of(Type type) {
        return new VirtualValue(type);
    }

    @Override
    public boolean canMerge(BasicValue v, TypeChecker typeChecker) {
        if (v == this) return true;
        else if (!(v instanceof NullValue) && v != UninitializedValue.INSTANCE && v != null) {
            if (getType() == null) {
                return v.getType() == null;
            } else {
                return getType().equals(v.getType()) || this.isParent(v.getType(), typeChecker);
            }
        } else {
            return false;
        }
    }

    @Override
    public SimpleValue copy() {
        return of(getType());
    }

    private boolean isParent(Type child, TypeChecker typeChecker) {
        Type parent = getType();
        if (parent.equals(child))
            return true;
        else if (parent.getSort() == Type.OBJECT && child.getSort() == Type.OBJECT) {
            return typeChecker.test(parent, child);
        } else
            return parent.getSort() < Type.ARRAY && child.getSort() < Type.ARRAY;
    }
}
