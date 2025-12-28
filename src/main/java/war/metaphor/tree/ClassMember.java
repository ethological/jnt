package war.metaphor.tree;

import lombok.Getter;

@Getter
public abstract class ClassMember<T> {

    protected JClassNode classNode;
    protected T member;

    public ClassMember(JClassNode classNode, T member) {
        this.classNode = classNode;
        this.member = member;
    }

    public abstract int getAccess();
    public abstract String getName();
    public abstract String getDesc();
}
