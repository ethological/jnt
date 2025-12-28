package war.metaphor.mutator.data.strings.poly2.decryptionMethod.code.impl;

import org.objectweb.asm.tree.*;
import war.jnt.crypto.Crypto;
import war.metaphor.mutator.data.strings.poly2.decryptionMethod.DecryptionMethod;
import war.metaphor.mutator.data.strings.poly2.decryptionMethod.args.AbstractDecryptionMethodArgument;
import war.metaphor.mutator.data.strings.poly2.decryptionMethod.code.AbstractDecryptionMethodCodePiece;
import war.metaphor.util.Pair;
import war.metaphor.util.asm.BytecodeUtil;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Random;
import java.util.concurrent.ThreadLocalRandom;
import java.util.stream.Stream;

public final class AesCodePiece extends AbstractDecryptionMethodCodePiece
{
    private final AbstractDecryptionMethodArgument keyArg;
    private final int keyIdx;

    public AesCodePiece(final ArrayList<AbstractDecryptionMethodArgument> arguments)
    {
        final int keyIdx = ThreadLocalRandom.current().nextInt(arguments.size());
        this.keyArg = arguments.get(keyIdx);
        this.keyIdx = keyIdx;
    }

    @Override
    public InsnList getDecryptionCode(final int byteArrayIdx)
    {
        final InsnList list = new InsnList();

        int randomIdx = -1;
        int keyArrIdx = -1;
        int ivArrIdx  = -1;

        final ArrayList<Integer> indicies = new ArrayList<>(Stream.of(1, 2, 3).toList());
        Collections.shuffle(indicies);

        for (int i = 0; i < 3; i++)
        {
            switch (i)
            {
                case 0 -> randomIdx = byteArrayIdx + indicies.get(i);
                case 1 -> keyArrIdx = byteArrayIdx + indicies.get(i);
                case 2 -> ivArrIdx = byteArrayIdx + indicies.get(i);
            }
        }

        final ArrayList<InsnList> inits = new ArrayList<>();

        final InsnList randomInit = new InsnList();

        randomInit.add(new TypeInsnNode(NEW, "java/util/Random"));
        randomInit.add(new InsnNode(DUP));
        randomInit.add(keyArg.load(keyIdx));
        randomInit.add(new InsnNode(I2L));
        randomInit.add(new MethodInsnNode(INVOKESPECIAL, "java/util/Random", "<init>", "(J)V"));
        randomInit.add(new VarInsnNode(ASTORE, randomIdx));

        final InsnList keyArrInit = new InsnList();

        keyArrInit.add(BytecodeUtil.makeInteger(16));
        keyArrInit.add(new IntInsnNode(NEWARRAY, T_BYTE));
        keyArrInit.add(new VarInsnNode(ASTORE, keyArrIdx));

        final InsnList ivArrInit = new InsnList();

        ivArrInit.add(BytecodeUtil.makeInteger(16));
        ivArrInit.add(new IntInsnNode(NEWARRAY, T_BYTE));
        ivArrInit.add(new VarInsnNode(ASTORE, ivArrIdx));

        inits.add(randomInit);
        inits.add(keyArrInit);
        inits.add(ivArrInit);

        Collections.shuffle(inits);

        for (InsnList init : inits)
        {
            list.add(init);
        }


        list.add(new VarInsnNode(ALOAD, randomIdx));
        list.add(new VarInsnNode(ALOAD, keyArrIdx));
        list.add(new MethodInsnNode(INVOKEVIRTUAL, "java/util/Random", "nextBytes", "([B)V"));

        list.add(new VarInsnNode(ALOAD, randomIdx));
        list.add(new VarInsnNode(ALOAD, ivArrIdx));
        list.add(new MethodInsnNode(INVOKEVIRTUAL, "java/util/Random", "nextBytes", "([B)V"));

        list.add(new VarInsnNode(ALOAD, byteArrayIdx));
        list.add(new VarInsnNode(ALOAD, keyArrIdx));
        list.add(new VarInsnNode(ALOAD, ivArrIdx));
        list.add(new MethodInsnNode(INVOKESTATIC, "war/jnt/crypto/Crypto", "decrypt", "([B[B[B)[B"));
        list.add(new VarInsnNode(ASTORE, byteArrayIdx));

        return list;
    }

    @Override
    public byte[] encrypt(final Pair<AbstractDecryptionMethodArgument, Object>[] args,
                          final byte[] stringBytes,
                          final String methodName,
                          final String className)
    {
        final Random r = new Random(DecryptionMethod.toInt(args[keyIdx].b));
        final byte[] key = new byte[16];
        final byte[] iv = new byte[16];
        r.nextBytes(key);
        r.nextBytes(iv);

        return Crypto.encrypt(stringBytes, key, iv);
    }
}
