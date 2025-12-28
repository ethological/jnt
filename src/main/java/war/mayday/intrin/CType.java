package war.mayday.intrin;

public enum CType {
    INT("int"),
    FLOAT("float"),
    DOUBLE("double"),
    LONG("long"),
    UCHAR("unsigned char"),
    CHAR_PTR("char*");

    String name;

    CType(String name) {
        this.name = name;
    }

    @Override
    public String toString() {
        return name;
    }
}
