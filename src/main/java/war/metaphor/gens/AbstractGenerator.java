package war.metaphor.gens;

import war.metaphor.gens.nodes.Node;
import war.metaphor.gens.nodes.NodeType;
import war.metaphor.gens.structs.GContext;
import org.objectweb.asm.tree.InsnList;
import org.objectweb.asm.tree.MethodNode;
import war.metaphor.tree.JClassNode;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public abstract class AbstractGenerator implements NodeType {

    protected final List<Node> nodes = new ArrayList<>();

    protected JClassNode classNode;
    protected MethodNode method;
    protected GContext ctx;

    public InsnList generate() {
        return generate(0, 0);
    }

    public abstract InsnList generate(int maxCode, int maxStack);

    protected void registerNode(Node node, int weight) {
        for (int i = 0; i < weight; i++) {
            nodes.add(node);
        }
    }

    public final void setup(JClassNode classNode, MethodNode method) {
        this.classNode = classNode;
        this.method = method;
        this.ctx = new GContext(method);
    }

    public final void setup() {
        this.classNode = null;
        this.method = null;
        this.ctx = new GContext();
    }

    protected Node node() {
        return nodes.get((int) (Math.random() * nodes.size()));
    }

}
