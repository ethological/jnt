package war.metaphor.mutator.flow.opaque;

import org.objectweb.asm.Opcodes;
import org.objectweb.asm.tree.MethodNode;
import war.metaphor.analysis.graph.Block;
import war.metaphor.analysis.graph.ControlFlowGraph;

public abstract class Opaque implements Opcodes {

    public abstract void handle(MethodNode method, Block block, int predicateLocal);
}
