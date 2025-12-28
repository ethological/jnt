package war.mayday.impl.sim;

public enum SIMMode {
    ADD("+"),
    SUB("-"),
    DIV("/"),
    MUL("*"),
    REMAINDER("%"),
    SHIFT_LEFT("<<"),
    SHIFT_RIGHT(">>"),
    USHIFT_RIGHT(">>>"),
    AND("&"),
    OR("|"),
    XOR("^");
    
    private final String operator;
    
    SIMMode(String operator) {
        this.operator = operator;
    }
    
    public String getOperator() {
        return operator;
    }
}
