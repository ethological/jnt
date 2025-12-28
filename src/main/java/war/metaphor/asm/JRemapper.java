package war.metaphor.asm;

import org.objectweb.asm.Type;
import org.objectweb.asm.commons.Remapper;

import java.util.Map;

public class JRemapper extends Remapper {

    private final Map<String, String> mapping;

    public JRemapper(Map<String, String> mapping) {
        this.mapping = mapping;
    }

    @Override
    public String mapMethodDesc(String methodDescriptor) {
        try {
            return super.mapMethodDesc(methodDescriptor);
        } catch (Exception e) {
            return methodDescriptor;
        }
    }

    @Override
    public String mapSignature(String signature, boolean typeSignature) {
        try {
            return super.mapSignature(signature, typeSignature);
        } catch (Exception e) {
            return signature;
        }
    }

    public String mapMethodName(String owner, String name, String descriptor) {
        String remappedName = this.map(owner + '.' + name + descriptor);
        return remappedName == null ? name : remappedName;
    }

    public String mapAnnotationAttributeName(String descriptor, String name) {
        descriptor = Type.getType(descriptor).getInternalName();
        String remappedName = this.softMap(descriptor + '.' + name);
        return remappedName == null ? name : remappedName;
    }

    public String mapFieldName(String owner, String name, String descriptor) {
        String remappedName = this.map(owner + '.' + name + descriptor);
        return remappedName == null ? name : remappedName;
    }

    public String map(String key) {
        return this.mapping.get(key);
    }

    @Override
    public String mapDesc(String descriptor) {
        try {
            return super.mapDesc(descriptor);
        } catch (Exception e) {
            return descriptor;
        }
    }

    private String softMap(String s) {
        for (String s1 : this.mapping.keySet()) {
            if (s1.contains(s)) {
                return this.mapping.get(s1);
            }
        }
        return null;
    }
}