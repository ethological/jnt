package war.metaphor.engine.modules;

import org.objectweb.asm.tree.InsnList;
import war.metaphor.engine.Context;

public interface Module {

    void run(Context context);

    Class<? extends Module> inverse();

    String getSourceInstructions(Context context);

    InsnList getInstructions(Context context);
}
