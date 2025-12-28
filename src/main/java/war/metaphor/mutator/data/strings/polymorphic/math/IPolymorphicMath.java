package war.metaphor.mutator.data.strings.polymorphic.math;

import org.objectweb.asm.tree.InsnList;

public interface IPolymorphicMath {

    int apply(int input);

    InsnList dump();

}
