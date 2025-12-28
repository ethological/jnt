package war.metaphor.util.builder;

import org.objectweb.asm.tree.InsnList;
import org.objectweb.asm.tree.LabelNode;
import org.objectweb.asm.tree.MethodNode;
import org.objectweb.asm.tree.TryCatchBlockNode;

import java.util.ArrayList;
import java.util.Arrays;

public class MethodBuilder {

    private final MethodNode method;
    private InsnListBuilder builder;

    public static MethodBuilder create() {
        return new MethodBuilder();
    }

    private MethodBuilder() {
        method = new MethodNode();
        method.tryCatchBlocks = new ArrayList<>();
    }

    public MethodBuilder withName(String name) {
        method.name = name;
        return this;
    }

    public MethodBuilder withDesc(String desc) {
        method.desc = desc;
        return this;
    }

    public MethodBuilder withAccess(int access) {
        method.access = access;
        return this;
    }

    public MethodBuilder withSignature(String signature) {
        method.signature = signature;
        return this;
    }

    public MethodBuilder withExceptions(String... exceptions) {
        method.exceptions.addAll(Arrays.asList(exceptions));
        return this;
    }

    public MethodBuilder withInstructionBuilder(InsnListBuilder builder) {
        this.builder = builder;
        return withInstructions(builder.build());
    }

    public MethodBuilder withInstructions(InsnList list) {
        method.instructions = list;
        return this;
    }

    public MethodBuilder withTrap(String type, String start, String end, String handler) {
        return withTrap(type, builder.getLabel(start), builder.getLabel(end), builder.getLabel(handler));
    }

    public MethodBuilder withTrap(String type, LabelNode start, LabelNode end, LabelNode handler) {
        method.tryCatchBlocks.add(new TryCatchBlockNode(start, end, handler, type));
        return this;
    }

    public MethodNode build() {
        return method;
    }


}
