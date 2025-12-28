package war.jnt.core.code;

import lombok.Getter;
import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.tree.MethodNode;
import war.jnt.fusebox.impl.ArgumentManager;
import war.jnt.fusebox.impl.FieldManager;
import war.jnt.stack.StackTracker;

@Getter
public class UnitContext {

    private final StringBuilder builder;
    public final StackTracker tracker;
    private final FieldManager fieldMan = new FieldManager();
    private final ArgumentManager argMan = new ArgumentManager();

    public final ClassNode classNode;
    public final MethodNode methodNode;

    public String handlerLabel = null;

    private final boolean DEBUG;

    public UnitContext(StringBuilder builder, StackTracker tracker,
                       ClassNode classNode, MethodNode methodNode) {
        this(builder, tracker, classNode, methodNode, true);
    }

    public UnitContext(StringBuilder builder, StackTracker tracker,
                       ClassNode classNode, MethodNode methodNode, boolean debug) {
        this.builder = builder;
        this.tracker = tracker;
        this.classNode = classNode;
        this.methodNode = methodNode;
        this.DEBUG = debug;
    }

    private String escapeForCString(String str) {
        return str.replace("\\", "\\\\")
                .replace("\"", "\\\"")
                .replace("\n", "\\n")
                .replace("\r", "\\r")
                .replace("\t", "\\t");
    }

    public void insert(String str) {
        if (DEBUG) {
            builder.append("printf(\"");
            builder.append(escapeForCString(str));
            builder.append("\\n\");\n");
        }
        builder.insert(0, str);
    }

    public void fmtAppend(String str, Object... objs) {
        if (DEBUG) {
            builder.append("\tprintf(\"");
            builder.append(escapeForCString(String.format(str, objs)));
            builder.append("\\n\");\n");
        }

        builder.append(String.format(str, objs));
    }

    public ArgumentManager getArgManager() {
        return argMan;
    }

    public FieldManager getFieldManager() {
        return fieldMan;
    }

    public void append(Object obj) {
        if (DEBUG) {
            builder.append("\tprintf(\"");
            builder.append(escapeForCString(obj.toString()));
            builder.append("\\n\");\n");
        }
        builder.append(obj);
    }
}
