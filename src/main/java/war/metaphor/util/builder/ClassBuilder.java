package war.metaphor.util.builder;

import org.objectweb.asm.tree.MethodNode;
import war.metaphor.tree.JClassNode;

public class ClassBuilder {

    private final JClassNode classNode;

    public static ClassBuilder create() {
        return new ClassBuilder();
    }

    private ClassBuilder() {
        classNode = new JClassNode();
    }

    public ClassBuilder withName(String name) {
        classNode.name = name;
        return this;
    }

    public ClassBuilder withVersion(int version) {
        classNode.version = version;
        return this;
    }

    public ClassBuilder withAccess(int access) {
        classNode.access = access;
        return this;
    }

    public ClassBuilder withSuperName(String superName) {
        classNode.superName = superName;
        return this;
    }

    public ClassBuilder withInterface(String interfaceName) {
        classNode.interfaces.add(interfaceName);
        return this;
    }

    public ClassBuilder withMethod(MethodNode method) {
        classNode.methods.add(method);
        return this;
    }

    public JClassNode build() {
        return classNode;
    }

}
