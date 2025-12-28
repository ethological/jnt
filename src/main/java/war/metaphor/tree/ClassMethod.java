package war.metaphor.tree;

import org.objectweb.asm.tree.MethodNode;

public class ClassMethod extends ClassMember<MethodNode> {

    public ClassMethod(JClassNode classNode, MethodNode member) {
        super(classNode, member);
    }

    public static ClassMethod of(JClassNode classNode, MethodNode member) {
        return new ClassMethod(classNode, member);
    }

    public boolean isAssignableFrom(ClassMethod other) {
        return this.member.name.equals(other.member.name) && this.member.desc.equals(other.member.desc);
    }

    @Override
    public String getName() {
        return member.name;
    }

    @Override
    public String getDesc() {
        return member.desc;
    }

    @Override
    public int getAccess() {
        return member.access;
    }

    @Override
    public int hashCode() {
        return toString().hashCode();
    }

    @Override
    public boolean equals(Object obj) {
        return obj instanceof ClassMethod && obj.toString().equals(toString());
    }

    @Override
    public String toString() {
        return classNode.name + "." + member.name + member.desc;
    }

}
