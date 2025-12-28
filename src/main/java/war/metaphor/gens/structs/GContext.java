package war.metaphor.gens.structs;

import war.metaphor.tree.ClassField;
import org.objectweb.asm.tree.FieldNode;
import org.objectweb.asm.tree.MethodNode;
import war.metaphor.tree.JClassNode;

import java.util.Arrays;
import java.util.Stack;

public class GContext {

    public Stack<GValue> stack;
    public GLocal[] locals;

    public int stackHeight;

    public MethodNode method;

    public ClassField[] fields;

    public GContext(MethodNode method) {
        this.stack = new Stack<>();
        this.locals = new GLocal[method.maxLocals];
        this.method = method;
    }

    public GContext() {
        this.stack = new Stack<>();
        this.locals = new GLocal[0];
    }

    public void registerField(JClassNode node, FieldNode field) {
        fields = Arrays.copyOf(fields, fields.length + 1);
        fields[fields.length - 1] = ClassField.of(node, field);
    }

    public void pushStack(GValue value) {
        stack.push(value);
        stackHeight = stack.size();
    }

    public GValue popStack() {
        GValue value = stack.pop();
        stackHeight = stack.size();
        return value;
    }

    public GValue peekStack() {
        return stack.peek();
    }

    public GValue peekStack(int depth) {
        return stack.elementAt(stack.size() - 1 - depth);
    }

    public GLocal getLocal(int index) {
        return locals[index];
    }

    public void growLocals(int size) {
        locals = Arrays.copyOf(locals, locals.length + size);
        method.maxLocals += size;
    }

    public void clearFields() {
        fields = new ClassField[0];
    }
}
