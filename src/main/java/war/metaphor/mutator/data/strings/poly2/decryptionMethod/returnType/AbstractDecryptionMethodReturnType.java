package war.metaphor.mutator.data.strings.poly2.decryptionMethod.returnType;

import org.objectweb.asm.Opcodes;
import org.objectweb.asm.tree.InsnList;

public abstract class AbstractDecryptionMethodReturnType implements Opcodes
{
    /**
     * byte[] -> *return object*
     */
    public abstract InsnList getPackingCode();

    /**
     * String -> *return object*
     */
    public abstract InsnList getCachePackingCode();

    /**
     * *return object* -> String
     */
    public abstract InsnList getUnpackingCode();

    public abstract String getDescriptor();
}
