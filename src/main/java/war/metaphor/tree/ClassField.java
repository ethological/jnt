package war.metaphor.tree;

import org.objectweb.asm.tree.FieldNode;

public class ClassField extends ClassMember<FieldNode> {

    public ClassField(JClassNode classNode, FieldNode member) {
        super(classNode, member);
    }

    public static ClassField of(JClassNode classNode, FieldNode member) {
        return new ClassField(classNode, member);
    }

    public boolean isAssignableFrom(ClassField other) {
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
        return obj instanceof ClassField && obj.toString().equals(toString());
    }

    @Override
    public String toString() {
        return classNode.name + "." + member.name + member.desc;
    }
}
