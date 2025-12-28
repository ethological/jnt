package war.metaphor.mutator.data.strings.poly2.decryptionMethod.args;

import org.objectweb.asm.Opcodes;
import org.objectweb.asm.tree.InsnList;

public abstract class AbstractDecryptionMethodArgument implements Opcodes
{
    public abstract String getDescriptor();
    public abstract Object makeRandomValue();
    public abstract InsnList load(final int var);
}
