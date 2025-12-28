package war.mayday.intrin;

import war.mayday.intrin.expr.Expression;
import war.mayday.intrin.match.MatchArm;
import war.mayday.intrin.match.SwitchStmt;

public class MachinePrinter {
    private int indentLevel = 0;
    private final StringBuilder sb = new StringBuilder();
    
    private static final String INDENT = "\t";
    private static final String NEWLINE = "\n";
    private static final String SPACE = " ";
    private static final String SEMI = ";";
    private static final String OPEN_BRACE = "{";
    private static final String CLOSE_BRACE = "}";
    
    public static final String BREAK = "break";
    
    // Append current indentation
    private void indent() {
        sb.append(INDENT.repeat(indentLevel));
    }
    
    // Add a new line with proper indentation
    public MachinePrinter newLine() {
        sb.append(NEWLINE);
        return this;
    }
    
    // Add content with indentation at start
    public MachinePrinter appendIndented(String content) {
        indent();
        sb.append(content);
        return this;
    }
    
    // Add content without indentation
    public MachinePrinter append(String content) {
        sb.append(content);
        return this;
    }
    
    // Enter a new block, increasing indentation
    public MachinePrinter enterBlock() {
        appendIndented(OPEN_BRACE).newLine();
        indentLevel++;
        return this;
    }
    
    // Exit a block, decreasing indentation
    public MachinePrinter exitBlock() {
        indentLevel--;
        appendIndented(CLOSE_BRACE).newLine();
        return this;
    }
    
    // Create a variable declaration
    public MachinePrinter varDecl(String type, String name, String value) {
        appendIndented(type)
            .append(SPACE)
            .append(name)
            .append(SPACE)
            .append("=")
            .append(SPACE)
            .append(value)
            .append(SEMI)
            .newLine();
        return this;
    }
    
    // Create a variable declaration with CType
    public MachinePrinter varDecl(CType type, String name, Object value) {
        return varDecl(type.toString(), name, value.toString());
    }
    
    // Create a label
    public MachinePrinter label(String name) {
        appendIndented(name + ":").newLine();
        return this;
    }
    
    // Create an assignment statement
    public MachinePrinter assign(String target, String value) {
        appendIndented(target)
            .append(SPACE)
            .append("=")
            .append(SPACE)
            .append(value)
            .append(SEMI)
            .newLine();
        return this;
    }
    
    // Create an assignment with an expression
    public MachinePrinter assign(String target, Expression expr) {
        return assign(target, expr.toString());
    }
    
    // Add a break statement
    public MachinePrinter breakStmt() {
        appendIndented(BREAK + SEMI).newLine();
        return this;
    }
    
    // Create a switch statement
    public MachinePrinter switchStmt(String value, Runnable bodyBuilder) {
        appendIndented("switch (" + value + ")").newLine();
        enterBlock();
        bodyBuilder.run();
        exitBlock();
        return this;
    }
    
    // Create a case statement
    public MachinePrinter caseStmt(String value, Runnable bodyBuilder) {
        appendIndented("case " + value + ":").newLine();
        enterBlock();
        bodyBuilder.run();
        exitBlock();
        return this;
    }
    
    // Build a complete switch with SwitchStmt object
    public MachinePrinter buildSwitch(SwitchStmt stmt) {
        return switchStmt(stmt.getValue(), () -> {
            for (MatchArm arm : stmt.arms) {
                String armValue = arm.getValue().toString();
                caseStmt(armValue, () -> {
                    // Execute the arm's body building operations
                    arm.buildBody(this);
                    // Add break statement
                    breakStmt();
                });
            }
        });
    }
    
    @Override
    public String toString() {
        return sb.toString();
    }
}



