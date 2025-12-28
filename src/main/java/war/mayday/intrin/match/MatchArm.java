package war.mayday.intrin.match;

import war.mayday.intrin.MachinePrinter;
import war.mayday.intrin.expr.Expression;

// this will definitely shit the bed
public class MatchArm {
    private final Object value;
    private Expression expression;
    private String target;

    public MatchArm(Object value) {
        this.value = value;
    }
    
    public Object getValue() {
        return value;
    }
    
    public void setAssignment(String target, Expression expression) {
        this.target = target;
        this.expression = expression;
    }
    
    public void buildBody(MachinePrinter printer) {
        if (target != null && expression != null) {
            printer.assign(target, expression);
        }
    }
}
