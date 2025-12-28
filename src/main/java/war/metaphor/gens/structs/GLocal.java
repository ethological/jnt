package war.metaphor.gens.structs;

import org.objectweb.asm.Type;

public class GLocal extends GValue {

    public int index;

    public GLocal(Type type, Object value) {
        super(type, value);
    }

    public static GLocal of(GValue v, int index) {
        GLocal gv = new GLocal(v.getType(), v.getValue());
        gv.setDisallowRemove(v.isDisallowRemove());
        gv.index = index;
        return gv;
    }

    @Override
    public String toString() {
        return "L(" + getType() + ", " + index + ")";
    }
}
