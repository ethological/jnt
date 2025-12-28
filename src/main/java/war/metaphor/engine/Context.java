package war.metaphor.engine;


import java.util.Stack;

public class Context {

    public String varName;
    public String opaque;

    public Stack<Integer> stack;

    public int stackHeight;

    public void pushStack(int value) {
        stack.push(value);
        stackHeight = stack.size();
    }

    public Integer popStack() {
        int value = stack.pop();
        stackHeight = stack.size();
        return value;
    }

    public Integer peekStack() {
        return stack.peek();
    }

    public Integer peekStack(int depth) {
        return stack.elementAt(stack.size() - 1 - depth);
    }

    public void clear() {
        stack.clear();
        stackHeight = 0;
    }

}
