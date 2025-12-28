package war.metaphor.mutator.integrity.mainCallCheck.math;

import org.objectweb.asm.Opcodes;
import org.objectweb.asm.tree.InsnList;

public interface IMath extends Opcodes {

    int apply(int input);

    InsnList dump();

}
