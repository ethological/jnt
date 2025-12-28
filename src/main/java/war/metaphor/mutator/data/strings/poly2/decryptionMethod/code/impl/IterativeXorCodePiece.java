package war.metaphor.mutator.data.strings.poly2.decryptionMethod.code.impl;

import org.objectweb.asm.tree.*;
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

/**
 * a lot of shit related to XOR, no idea why I slapped everything in here, makes it more dynamic ig?
 */
public class IterativeXorCodePiece extends AbstractDecryptionMethodCodePiece
{
    private final boolean shouldJavaRandom;
    private final AbstractDecryptionMethodArgument javaRandomArg;
    private final int javaRandomIdx;

    private final boolean shouldIter;
    private final boolean shouldStatic;
    private final int staticValue;

    private final boolean shouldStackTraceMethod;
    private final boolean shouldStackTraceClass;
    private final boolean shouldStackTraceMethodException;
    private final boolean shouldStackTraceClassException;

    private final AbstractDecryptionMethodArgument xorArg;
    private final int xorArgIdx;

    private final boolean shouldMemeSwitch;
    private final boolean shouldMemeSwitchLoop;
    private final boolean shouldMemeSwitchXorInSwitch;
    private final int memeSwitchSize;
    private final ArrayList<Integer> memeSwitchCases;

    public IterativeXorCodePiece(final ArrayList<AbstractDecryptionMethodArgument> arguments)
    {
        final int javaRandomIdx = ThreadLocalRandom.current().nextInt(arguments.size());
        this.shouldJavaRandom = ThreadLocalRandom.current().nextBoolean();
        this.javaRandomArg = arguments.get(javaRandomIdx);
        this.javaRandomIdx = javaRandomIdx;

        this.shouldIter = ThreadLocalRandom.current().nextBoolean();
        this.shouldStatic = ThreadLocalRandom.current().nextBoolean();
        this.staticValue = ThreadLocalRandom.current().nextInt();

        this.shouldStackTraceMethod = ThreadLocalRandom.current().nextBoolean();
        this.shouldStackTraceClass = ThreadLocalRandom.current().nextBoolean();
        this.shouldStackTraceMethodException = ThreadLocalRandom.current().nextBoolean();
        this.shouldStackTraceClassException = ThreadLocalRandom.current().nextBoolean();

        final int xorArgIdx = ThreadLocalRandom.current().nextInt(arguments.size());
        this.xorArg = arguments.get(xorArgIdx);
        this.xorArgIdx = xorArgIdx;

        this.shouldMemeSwitch = ThreadLocalRandom.current().nextBoolean();
        this.shouldMemeSwitchLoop = ThreadLocalRandom.current().nextBoolean();
        this.shouldMemeSwitchXorInSwitch = ThreadLocalRandom.current().nextBoolean();
        this.memeSwitchSize = ThreadLocalRandom.current().nextInt(6, 255);
        this.memeSwitchCases = new ArrayList<>();
        for (int i = 0; i < memeSwitchSize; i++)
        {
            int value;

            do
            {
                value = ThreadLocalRandom.current().nextInt();
            }
            while (memeSwitchCases.contains(value));

            memeSwitchCases.add(value);
        }
    }

    @Override
    public InsnList getDecryptionCode(final int byteArrayIdx)
    {
        final InsnList list = new InsnList();

        // exit loop
        final LabelNode jumpOut = new LabelNode();

        // start of the loop
        final LabelNode loop = new LabelNode();

        int iIdx = -1;
        int randomIdx = -1;
        int switchXorIdx = -1;

        final ArrayList<Integer> indicies = new ArrayList<>(Stream.of(1, 2, 3).toList());
        Collections.shuffle(indicies);

        for (int i = 0; i < 3; i++)
        {
            switch (i)
            {
                case 0 -> iIdx = byteArrayIdx + indicies.get(i);
                case 1 -> randomIdx = byteArrayIdx + indicies.get(i);
                case 2 -> switchXorIdx = byteArrayIdx + indicies.get(i);
            }
        }

        // Random r = new Random((long)seed);
        if (shouldJavaRandom)
        {
            list.add(new TypeInsnNode(NEW, "java/util/Random"));
            list.add(new InsnNode(DUP));
            list.add(javaRandomArg.load(javaRandomIdx));
            list.add(new InsnNode(I2L));
            list.add(new MethodInsnNode(INVOKESPECIAL, "java/util/Random", "<init>", "(J)V"));
            list.add(new VarInsnNode(ASTORE, randomIdx));
        }

        // int i = 0;
        list.add(BytecodeUtil.makeInteger(0));
        list.add(new VarInsnNode(ISTORE, iIdx));

        list.add(loop);

        // jump out if i >= stringBytes.length
        list.add(new VarInsnNode(ILOAD, iIdx));
        list.add(new VarInsnNode(ALOAD, byteArrayIdx));
        list.add(new InsnNode(ARRAYLENGTH));
        list.add(new JumpInsnNode(IF_ICMPGE, jumpOut));

        // stringBytes[i]
        list.add(new VarInsnNode(ALOAD, byteArrayIdx));
        list.add(new VarInsnNode(ILOAD, iIdx));
        list.add(new InsnNode(DUP2));
        list.add(new InsnNode(BALOAD));

        // ^ xorKey
        list.add(xorArg.load(xorArgIdx)); // heavy trollage

        final ArrayList<InsnList> codes = new ArrayList<>();

        if (shouldIter)
        {
            final InsnList iterCode = new InsnList();

            iterCode.add(new VarInsnNode(ILOAD, iIdx));
            iterCode.add(new InsnNode(IXOR));

            codes.add(iterCode);
        }
        if (shouldMemeSwitch)
        {
            final InsnList memeSwitchCode = new InsnList();

            final LabelNode dflt = new LabelNode();
            final LabelNode end = new LabelNode();
            final ArrayList<LabelNode> labels = new ArrayList<>();
            for (int i = 0; i < memeSwitchSize; i++)
            {
                labels.add(new LabelNode());
            }

            if (shouldMemeSwitchLoop)
            {
                memeSwitchCode.add(dflt);
            }
            memeSwitchCode.add(new VarInsnNode(ILOAD, iIdx));
            memeSwitchCode.add(BytecodeUtil.makeInteger(memeSwitchSize));
            memeSwitchCode.add(new InsnNode(IREM));
            memeSwitchCode.add(new TableSwitchInsnNode(0, memeSwitchSize - 1, dflt, labels.toArray(new LabelNode[0])));

            for (int i = 0; i < memeSwitchSize; i++)
            {
                memeSwitchCode.add(labels.get(i));
                memeSwitchCode.add(BytecodeUtil.makeInteger(memeSwitchCases.get(i)));
                if (shouldMemeSwitchXorInSwitch)
                {
                    memeSwitchCode.add(new InsnNode(IXOR));
                }
                memeSwitchCode.add(new JumpInsnNode(GOTO, end));
            }

            memeSwitchCode.add(end);
            if (!shouldMemeSwitchXorInSwitch)
            {
                memeSwitchCode.add(new InsnNode(IXOR));
            }
            if (!shouldMemeSwitchLoop)
            {
                memeSwitchCode.add(dflt);
            }

            codes.add(memeSwitchCode);
        }
        if (shouldStatic)
        {
            final InsnList staticCode = new InsnList();

            staticCode.add(BytecodeUtil.makeInteger(staticValue));
            staticCode.add(new InsnNode(IXOR));

            codes.add(staticCode);
        }
        if (shouldJavaRandom)
        {
            final InsnList javaRandomCode = new InsnList();

            javaRandomCode.add(new VarInsnNode(ALOAD, randomIdx));
            javaRandomCode.add(new MethodInsnNode(INVOKEVIRTUAL, "java/util/Random", "nextInt", "()I"));
            javaRandomCode.add(new InsnNode(IXOR));

            codes.add(javaRandomCode);
        }
        if (shouldStackTraceClass)
        {
            final InsnList stackTraceClassCode = new InsnList();
            int stackTraceIndex = ThreadLocalRandom.current().nextInt(1, 2);

            if (shouldStackTraceClassException)
            {
                final String exceptionClass = getRandomExceptionClassName();
                stackTraceIndex--;

                /*
                    NEW java/lang/Exception
                    DUP
                    INVOKESPECIAL java/lang/Exception.<init> ()V
                    INVOKEVIRTUAL java/lang/Exception.getStackTrace ()[Ljava/lang/StackTraceElement;
                 */

                stackTraceClassCode.add(new TypeInsnNode(NEW, exceptionClass));
                stackTraceClassCode.add(new InsnNode(DUP));
                stackTraceClassCode.add(new MethodInsnNode(INVOKESPECIAL, exceptionClass, "<init>", "()V"));
                stackTraceClassCode.add(new MethodInsnNode(INVOKEVIRTUAL, exceptionClass, "getStackTrace", "()[Ljava/lang/StackTraceElement;"));
            }
            else
            {
                /*
                    INVOKESTATIC java/lang/Thread.currentThread ()Ljava/lang/Thread;
                    INVOKEVIRTUAL java/lang/Thread.getStackTrace ()[Ljava/lang/StackTraceElement;
                 */

                stackTraceClassCode.add(new MethodInsnNode(INVOKESTATIC, "java/lang/Thread", "currentThread", "()Ljava/lang/Thread;"));
                stackTraceClassCode.add(new MethodInsnNode(INVOKEVIRTUAL, "java/lang/Thread", "getStackTrace", "()[Ljava/lang/StackTraceElement;"));
            }

            stackTraceClassCode.add(BytecodeUtil.makeInteger(stackTraceIndex));
            stackTraceClassCode.add(new InsnNode(AALOAD));
            stackTraceClassCode.add(new MethodInsnNode(INVOKEVIRTUAL, "java/lang/StackTraceElement", "getClassName", "()Ljava/lang/String;"));
            stackTraceClassCode.add(new MethodInsnNode(INVOKEVIRTUAL, "java/lang/String", "hashCode", "()I"));
            stackTraceClassCode.add(new InsnNode(IXOR));

            codes.add(stackTraceClassCode);
        }
        if (shouldStackTraceMethod)
        {
            final InsnList stackTraceMethodCode = new InsnList();
            int stackTraceIndex = 1;

            if (shouldStackTraceMethodException)
            {
                final String exceptionClass = getRandomExceptionClassName();
                stackTraceIndex--;

                /*
                    NEW java/lang/Exception
                    DUP
                    INVOKESPECIAL java/lang/Exception.<init> ()V
                    INVOKEVIRTUAL java/lang/Exception.getStackTrace ()[Ljava/lang/StackTraceElement;
                 */

                stackTraceMethodCode.add(new TypeInsnNode(NEW, exceptionClass));
                stackTraceMethodCode.add(new InsnNode(DUP));
                stackTraceMethodCode.add(new MethodInsnNode(INVOKESPECIAL, exceptionClass, "<init>", "()V"));
                stackTraceMethodCode.add(new MethodInsnNode(INVOKEVIRTUAL, exceptionClass, "getStackTrace", "()[Ljava/lang/StackTraceElement;"));
            }
            else
            {
                /*
                    INVOKESTATIC java/lang/Thread.currentThread ()Ljava/lang/Thread;
                    INVOKEVIRTUAL java/lang/Thread.getStackTrace ()[Ljava/lang/StackTraceElement;
                 */

                stackTraceMethodCode.add(new MethodInsnNode(INVOKESTATIC, "java/lang/Thread", "currentThread", "()Ljava/lang/Thread;"));
                stackTraceMethodCode.add(new MethodInsnNode(INVOKEVIRTUAL, "java/lang/Thread", "getStackTrace", "()[Ljava/lang/StackTraceElement;"));
            }

            stackTraceMethodCode.add(BytecodeUtil.makeInteger(stackTraceIndex));
            stackTraceMethodCode.add(new InsnNode(AALOAD));
            stackTraceMethodCode.add(new MethodInsnNode(INVOKEVIRTUAL, "java/lang/StackTraceElement", "getMethodName", "()Ljava/lang/String;"));
            stackTraceMethodCode.add(new MethodInsnNode(INVOKEVIRTUAL, "java/lang/String", "hashCode", "()I"));
            stackTraceMethodCode.add(new InsnNode(IXOR));

            codes.add(stackTraceMethodCode);
        }

        Collections.shuffle(codes);

        for (final InsnList code : codes)
        {
            list.add(code);
        }

        // probably not needed but imma keep it for completeness sake
        list.add(new InsnNode(I2B));
        list.add(new InsnNode(IXOR));
        list.add(new InsnNode(I2B));
        list.add(new InsnNode(BASTORE));

        // i++
        list.add(new IincInsnNode(iIdx, 1));

        list.add(new JumpInsnNode(GOTO, loop));

        list.add(jumpOut);

        return list;
    }

    private String getRandomExceptionClassName()
    {
        return "java/lang/Exception"; // TODO: include more classes, maybe make discovery dynamic?
    }

    @Override
    public byte[] encrypt(final Pair<AbstractDecryptionMethodArgument, Object>[] args,
                          final byte[] stringBytes,
                          final String methodName,
                          final String className)
    {
        final int xorKey = DecryptionMethod.toInt(args[xorArgIdx].b);
        final Random random = new Random(DecryptionMethod.toInt(args[javaRandomIdx].b));

        for (int i = 0; i < stringBytes.length; i++)
        {
            if (shouldIter)
            {
                stringBytes[i] ^= (byte) i;
            }
            if (shouldStatic)
            {
                stringBytes[i] ^= (byte) staticValue;
            }
            if (shouldJavaRandom)
            {
                stringBytes[i] ^= (byte) random.nextInt();
            }
            if (shouldStackTraceClass)
            {
                stringBytes[i] ^= (byte) className.hashCode();
            }
            if (shouldStackTraceMethod)
            {
                stringBytes[i] ^= (byte) methodName.hashCode();
            }
            if (shouldMemeSwitch)
            {
                stringBytes[i] ^= (byte) memeSwitchCases.get(i % memeSwitchSize).intValue();
            }

            stringBytes[i] ^= (byte) xorKey;
        }

        return stringBytes;
    }
}
