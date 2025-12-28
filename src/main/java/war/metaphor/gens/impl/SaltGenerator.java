package war.metaphor.gens.impl;

import org.objectweb.asm.Type;
import org.objectweb.asm.tree.InsnList;
import war.metaphor.gens.AbstractGenerator;
import war.metaphor.gens.nodes.Node;
import war.metaphor.gens.nodes.NodeType;
import war.metaphor.gens.nodes.constants.IntNode;
import war.metaphor.gens.nodes.constants.LongNode;
import war.metaphor.gens.nodes.conv.IntToLongNode;
import war.metaphor.gens.nodes.conv.LongToIntNode;
import war.metaphor.gens.nodes.math.*;
import war.metaphor.gens.nodes.stack.*;
import war.metaphor.gens.nodes.vars.LoadNode;
import war.metaphor.gens.structs.GLocal;
import war.metaphor.gens.structs.GValue;
import war.metaphor.util.builder.InsnListBuilder;

public class SaltGenerator extends AbstractGenerator {

    private final int saltIndex;
    private final Number saltValue;
    private final Number finalValue;

    private final Type type, localType;

    private final Node loadNode;

    public SaltGenerator(int saltIndex, Number saltValue, Number finalValue, Type type, Type localType) {
        this.saltIndex = saltIndex;
        this.saltValue = saltValue;
        this.finalValue = finalValue;
        this.type = type;
        this.localType = localType;

        if (type == Type.INT_TYPE)
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

        registerNode(new LongToIntNode(), 1);
        registerNode(new IntToLongNode(), 1);

        loadNode = new LoadNode();

        registerNode(loadNode, 10);

    }

    @Override
    public InsnList generate(int maxCode, int maxStack) {

        if (localType == Type.INT_TYPE) {
            ctx.locals[saltIndex] = GLocal.of(GValue.of(saltValue.intValue()), saltIndex);
        } else {
            ctx.locals[saltIndex] = GLocal.of(GValue.of(saltValue.longValue()), saltIndex);
        }

        InsnList code = new InsnList();

        code.add(loadNode.generate(ctx));

        while (code.size() < maxCode) {
            Node node = node();
            if (node.isValid(ctx)) {
                code.add(node.generate(ctx));
            }
        }

        while (true) {

            if (ctx.stackHeight == type.getSize()) {
                GValue v = ctx.peekStack();
                if (v.isTop())
                    v = ctx.peekStack(1);
                if (v.getType() == type)
                    break;
            }

            Node node = node();
            if (node.isValid(ctx)) {
                boolean cond = ctx.stackHeight > type.getSize() && (!NodeType.isPop(node) && !NodeType.isConv(node));
                if (cond) continue;
                code.add(node.generate(ctx));
            }
        }

        GValue v = ctx.popStack();
        if (v.isTop())
            v = ctx.popStack();

        if (type == Type.INT_TYPE) {
            int stackValue = (int) v.getValue();
            code.add(InsnListBuilder
                    .builder()
                    .math(stackValue, finalValue, Integer.SIZE)
                    .build());
        } else {
            long stackValue = (long) v.getValue();
            code.add(InsnListBuilder
                    .builder()
                    .math(stackValue, finalValue, Long.SIZE)
                    .build());
        }

        return code;
    }
}
