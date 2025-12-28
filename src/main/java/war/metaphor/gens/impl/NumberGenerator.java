package war.metaphor.gens.impl;

import org.objectweb.asm.Type;
import org.objectweb.asm.tree.InsnList;
import war.metaphor.gens.AbstractGenerator;
import war.metaphor.gens.nodes.Node;
import war.metaphor.gens.nodes.NodeType;
import war.metaphor.gens.nodes.constants.IntNode;
import war.metaphor.gens.nodes.constants.LongNode;
import war.metaphor.gens.nodes.math.*;
import war.metaphor.gens.nodes.stack.*;
import war.metaphor.gens.structs.GValue;
import war.metaphor.util.builder.InsnListBuilder;

public class NumberGenerator extends AbstractGenerator {

    private final Number end;

    private final Type required;

    public NumberGenerator(Number end, Type required) {
        this.end = end;
        this.required = required;

        if (required == Type.INT_TYPE)
            registerNode(new IntNode(), 3);
        else
            registerNode(new LongNode(), 3);

        registerNode(new DupNode(), 1);
        registerNode(new DupWideNode(), 1);
        registerNode(new DupX1Node(), 1);
        registerNode(new DupX2Node(), 1);
        registerNode(new DupWideX1Node(), 1);
        registerNode(new DupWideX2Node(), 1);
        registerNode(new SwapNode(), 1);

        registerNode(new XorNode(), 3);
        registerNode(new AndNode(), 3);
        registerNode(new OrNode(), 3);
        registerNode(new AddNode(), 3);
        registerNode(new SubNode(), 3);
        registerNode(new MulNode(), 3);
        registerNode(new LShiftNode(), 3);
        registerNode(new RShiftNode(), 3);
        registerNode(new URShiftNode(), 3);

    }

    @Override
    public InsnList generate(int maxCode, int maxStack) {

        InsnList code = new InsnList();

        while (code.size() < maxCode) {
            Node node = node();
            if (node.isValid(ctx)) {
                code.add(node.generate(ctx));
            }
        }

        while (true) {

            if (ctx.stackHeight == required.getSize()) {
                GValue v = ctx.peekStack();
                if (v.isTop())
                    v = ctx.peekStack(1);
                if (v.getType() == required)
                    break;
            }

            Node node = node();
            if (node.isValid(ctx)) {
                boolean cond = ctx.stackHeight > required.getSize() && (!NodeType.isPop(node) && !NodeType.isConv(node));
                if (cond) continue;
                code.add(node.generate(ctx));
            }
        }

        GValue v = ctx.popStack();
        if (v.isTop())
            v = ctx.popStack();

        if (required == Type.INT_TYPE) {
            int stackValue = (int) v.getValue();
            code.add(InsnListBuilder
                    .builder()
                    .math(stackValue, end, Integer.SIZE)
                    .build());
        } else {
            long stackValue = (long) v.getValue();
            code.add(InsnListBuilder
                    .builder()
                    .math(stackValue, end, Long.SIZE)
                    .build());
        }

        return code;
    }
}
