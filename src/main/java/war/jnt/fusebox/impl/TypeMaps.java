package war.jnt.fusebox.impl;

import org.objectweb.asm.Type;
import war.jnt.dash.Level;
import war.jnt.dash.Logger;
import war.jnt.dash.Origin;

public class TypeMaps {
    public static final String[] types = {
            "void", "jboolean",
            "jchar", "jbyte",
            "jshort", "jint",
            "jfloat", "jlong",
            "jdouble", "jarray",
            "jobject", "jobject"
    };

    public static final String[] arrayTypes = {
            "jarray", "jbooleanArray",
            "jcharArray", "jbyteArray",
            "jshortArray", "jintArray",
            "jfloatArray", "jlongArray",
            "jdoubleArray", "jobjectArray",
            "jarray", "jarray"
    };

    public String fromAsm(Type type) {
        int dimensions = type.toString().contains("[") ? type.getDimensions() : 0;
        if (dimensions > 0) {
            Type elementType = type.getElementType();
            return arrayTypes[elementType.getSort()];
        }
        return types[type.getSort()];
    }
}
