package war.metaphor.gens.nodes;

import war.metaphor.gens.structs.GContext;
import org.objectweb.asm.tree.InsnList;

import java.util.concurrent.ThreadLocalRandom;

public interface Node extends NodeType {

    ThreadLocalRandom rand = ThreadLocalRandom.current();

    default int minStackHeight() {
        return 0;
    }

    default boolean canGenerate(GContext ctx) {
        return true;
    }

    int type();

    InsnList generate(GContext ctx);

    default boolean isValid(GContext ctx) {
        if (ctx.stackHeight < minStackHeight()) return false;
        return canGenerate(ctx);
    }

}
