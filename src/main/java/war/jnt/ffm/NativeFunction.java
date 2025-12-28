package war.jnt.ffm;

import java.lang.foreign.FunctionDescriptor;
import java.lang.foreign.ValueLayout;

/**
 * @author etho
 */
public class NativeFunction {
    private String name;

    private ValueLayout rLayout;
    private ValueLayout[] argLayouts;

    public NativeFunction(String name, ValueLayout rLayout, ValueLayout... argLayouts) {
        this.name = name;
        this.rLayout = rLayout;
        this.argLayouts = argLayouts;
    }

    public String getName() {
        return name;
    }

    public ValueLayout getReturnLayout() {
        return rLayout;
    }

    public ValueLayout[] getArgLayouts() {
        return argLayouts;
    }
}
