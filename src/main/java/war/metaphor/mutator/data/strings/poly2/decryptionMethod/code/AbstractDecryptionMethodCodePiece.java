package war.metaphor.mutator.data.strings.poly2.decryptionMethod.code;

import org.objectweb.asm.Opcodes;
import org.objectweb.asm.tree.InsnList;
import war.metaphor.mutator.data.strings.poly2.decryptionMethod.args.AbstractDecryptionMethodArgument;
import war.metaphor.util.Pair;

public abstract class AbstractDecryptionMethodCodePiece implements Opcodes
{
    public abstract InsnList getDecryptionCode(final int byteArrayIdx);

    public abstract byte[] encrypt(final Pair<AbstractDecryptionMethodArgument, Object>[] args,
                                   final byte[] stringBytes,
                                   final String methodName,
                                   final String className);
}
