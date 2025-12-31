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
        String key = owner + '.' + name + descriptor;
        String remappedName = this.map(key);
        
        // If not found, walk up the full class hierarchy (superclasses AND interfaces)
        // to find where the field is actually defined
        if (remappedName == null) {
            Set<String> visited = new HashSet<>();
            Deque<String> queue = new ArrayDeque<>();
            queue.add(owner);
            
            while (!queue.isEmpty() && remappedName == null) {
                String current = queue.poll();
                if (current == null || !visited.add(current)) continue;
                
                JClassNode node = ObfuscatorContext.INSTANCE.loadClass(current);
                if (node == null) continue;
                
                if (node.superName != null && !visited.contains(node.superName)) {
                    key = node.superName + '.' + name + descriptor;
                    remappedName = this.map(key);
                    if (remappedName == null) {
                        queue.add(node.superName);
                    }
                }
                
                // Check all interfaces (this is where Imports.mc lives (example in my case))
                if (remappedName == null && node.interfaces != null) {
                    for (String iface : node.interfaces) {
                        if (iface != null && !visited.contains(iface)) {
                            key = iface + '.' + name + descriptor;
                            remappedName = this.map(key);
                            if (remappedName != null) break;
                            queue.add(iface);
                        }
                    }
                }
            }
        }
        
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
