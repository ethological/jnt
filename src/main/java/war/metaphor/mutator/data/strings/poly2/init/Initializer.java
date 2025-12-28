package war.metaphor.mutator.data.strings.poly2.init;

import org.objectweb.asm.Opcodes;
import org.objectweb.asm.tree.*;
import war.jnt.base64.Base64;
import war.metaphor.mutator.data.strings.poly2.decryptionMethod.DecryptionMethod;
import war.metaphor.mutator.data.strings.poly2.decryptionMethod.args.AbstractDecryptionMethodArgument;
import war.metaphor.util.Pair;
import war.metaphor.util.asm.BytecodeUtil;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Map;

/**
 * omg wow not simple initializer :flushed:
 * @author Jan
 */
public class Initializer implements Opcodes
{
    public final InsnList code;

    public Initializer(final DecryptionMethod parent)
    {
        code = new InsnList();

        final ArrayList<byte[]> encrypted = new ArrayList<>();
        for (final Map.Entry<String, Pair<AbstractDecryptionMethodArgument, Object>[]> entry : parent.cachedStrings.entrySet())
        {
            encrypted.add(parent.encrypt(entry));
        }

        int targetLength = 0;
        for (final byte[] sBytes : encrypted)
        {
            // string length + length byte high + length byte low
            targetLength += sBytes.length + 2;
        }

        final byte[] bytes = new byte[targetLength];

        int idx = 0;
        for (final byte[] sBytes : encrypted)
        {
            final int len = sBytes.length ^ parent.initXorKey;
            bytes[idx++] = (byte) (len >> 8);
            bytes[idx++] = (byte) (len & 0xFF);
            for (final byte sByte : sBytes)
            {
                bytes[idx++] = sByte;
            }
        }

        final String encoded = new String(Base64.encode(bytes));

        code.add(new LdcInsnNode(encoded));
        code.add(new FieldInsnNode(GETSTATIC, "java/nio/charset/StandardCharsets", "UTF_8", "Ljava/nio/charset/Charset;"));
        code.add(new MethodInsnNode(INVOKEVIRTUAL, "java/lang/String", "getBytes", "(Ljava/nio/charset/Charset;)[B"));
        code.add(new MethodInsnNode(INVOKESTATIC, "war/jnt/base64/Base64", "decode", "([B)[B"));
        code.add(new FieldInsnNode(PUTSTATIC, parent.parent.name, parent.initField.name, parent.initField.desc));

        code.add(BytecodeUtil.makeInteger(parent.cachedStrings.size()));
        code.add(new TypeInsnNode(ANEWARRAY, "[B"));
        code.add(new FieldInsnNode(PUTSTATIC, parent.parent.name, parent.cacheField.name, parent.cacheField.desc));
    }
}
