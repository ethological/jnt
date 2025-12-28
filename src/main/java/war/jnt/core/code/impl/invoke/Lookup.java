package war.jnt.core.code.impl.invoke;

public class Lookup {
    private final String lookupVar, classVar;

    public Lookup(String lookupVar, String classVar) {
        this.lookupVar = lookupVar;
        this.classVar = classVar;
    }

    public String getLookupVar() {
        return lookupVar;
    }

    public String getClassVar() {
        return classVar;
    }

    @Override
    public int hashCode() {
        return lookupVar.hashCode() + classVar.hashCode();
    }
}
