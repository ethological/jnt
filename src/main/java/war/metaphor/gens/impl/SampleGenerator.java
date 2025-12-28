package war.metaphor.gens.impl;

import war.metaphor.gens.AbstractGenerator;
import war.metaphor.gens.nodes.Node;
import war.metaphor.gens.nodes.NodeType;
import org.objectweb.asm.tree.InsnList;

public class SampleGenerator extends AbstractGenerator {

    @Override
    public InsnList generate(int maxCode, int maxStack) {

        InsnList code = new InsnList();

        while (code.size() < maxCode) {
            Node node = node();
            if (node.isValid(ctx)) {
                boolean cond = ctx.stackHeight <= maxStack;
                if (cond || NodeType.isPop(node)) {
                    code.add(node.generate(ctx));
                }
            }
        }

        while (ctx.stackHeight > 0) {
            Node node = node();
            if (node.isValid(ctx)) {
                if (NodeType.isPop(node)) {
                    code.add(node.generate(ctx));
                }
            }
        }

        return code;
    }
}
