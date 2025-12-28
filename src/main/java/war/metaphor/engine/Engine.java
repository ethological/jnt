package war.metaphor.engine;

import lombok.Getter;
import lombok.SneakyThrows;
import org.objectweb.asm.tree.InsnList;
import war.metaphor.engine.modules.Module;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.Stack;

@Getter
public class Engine {

    private final Context context;
    private final List<Module> modules;
    private final List<Module> reverse;

    private final List<Integer> keys;

    public Engine() {
        this.modules = new ArrayList<>();
        this.reverse = new ArrayList<>();
        this.keys = new ArrayList<>();
        this.context = new Context();
        this.context.stack = new Stack<>();
    }

    @SneakyThrows
    public void addModule(Class<? extends Module> module) {
        Module instance = module.getDeclaredConstructor().newInstance();
        modules.add(instance);
        reverse.add(instance.inverse().getDeclaredConstructor().newInstance());
    }

    public void randomise(int mask) {
        Random rand = new Random();
        for (int i = 0; i < modules.size(); i++)
            keys.add(rand.nextInt(mask));
    }

    public String getSourceInstructions() {
        StringBuilder sb = new StringBuilder();
        for (int i = reverse.size() - 1; i >= 0; i--) {
            Module module = reverse.get(i);
            context.pushStack(keys.get(i));
            sb.append("\t\t").append(context.varName).append(" = ")
                    .append(module.getSourceInstructions(context)).append(";\n");
        }
        context.clear();
        return sb.toString();
    }

    public InsnList getInstructions() {
        InsnList instructions = new InsnList();
        for (int i = reverse.size() - 1; i >= 0; i--) {
            Module module = reverse.get(i);
            context.pushStack(keys.get(i));
            instructions.add(module.getInstructions(context));
        }
        context.clear();
        return instructions;
    }

    public InsnList getForwardInstructions() {
        InsnList instructions = new InsnList();
        for (int i = 0; i < modules.size(); i++) {
            Module module = modules.get(i);
            context.pushStack(keys.get(i));
            instructions.add(module.getInstructions(context));
        }
        context.clear();
        return instructions;
    }

    @SneakyThrows
    public int run(int input) {
        context.pushStack(input);
        for (int i = 0; i < modules.size(); i++) {
            Module module = modules.get(i);
            context.pushStack(keys.get(i));
            module.run(context);
        }
        int value = context.popStack();
        context.clear();
        return value;
    }

    @SneakyThrows
    public int inverse(int input) {
        context.pushStack(input);
        for (int i = reverse.size() - 1; i >= 0; i--) {
            Module module = reverse.get(i);
            context.pushStack(keys.get(i));
            module.run(context);
        }
        int value = context.popStack();
        context.clear();
        return value;
    }

}
