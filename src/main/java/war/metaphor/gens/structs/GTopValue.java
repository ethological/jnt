package war.metaphor.gens.structs;

import org.objectweb.asm.Type;

public class GTopValue extends GValue {

    public static final GValue INSTANCE = new GTopValue();

    public GTopValue() {
        super(Type.VOID_TYPE, -1);
    }

    @Override
    public String toString() {
        return "TOP";
    }
}
