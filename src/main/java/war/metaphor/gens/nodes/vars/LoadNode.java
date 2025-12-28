package war.metaphor.gens.nodes.vars;

import org.objectweb.asm.tree.InsnList;
import war.metaphor.gens.nodes.Node;
import war.metaphor.gens.structs.GContext;
import war.metaphor.gens.structs.GLocal;
import war.metaphor.gens.structs.GTopValue;
import war.metaphor.gens.structs.GValue;
import war.metaphor.util.builder.InsnListBuilder;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

public class LoadNode implements Node {

    @Override
    public int type() {
        return T_LOAD;
    }

    @Override
    public boolean canGenerate(GContext ctx) {
        return Arrays.stream(ctx.locals).anyMatch(v -> v != null && !v.isTop());
    }

    @Override
    public InsnList generate(GContext ctx) {
        int index = 0;
        List<GLocal> vars = Arrays.asList(ctx.locals.clone());
        Collections.shuffle(vars);
        for (GLocal var : vars) {
            if (var != null && !var.isTop()) {
                index = var.index;
                break;
            }
        }
        GValue var = ctx.getLocal(index);
        GValue copy = new GValue(var.getType(), var.getValue());
        ctx.pushStack(copy);
        if (var.getSize() == 2)
            ctx.pushStack(GTopValue.INSTANCE);
        return InsnListBuilder
                .builder()
                .load(var.getType(), index)
                .build();
    }
}
