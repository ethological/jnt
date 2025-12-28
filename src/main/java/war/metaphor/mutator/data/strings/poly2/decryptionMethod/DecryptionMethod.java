package war.metaphor.mutator.data.strings.poly2.decryptionMethod;

import org.objectweb.asm.Opcodes;
import org.objectweb.asm.tree.*;
import war.metaphor.mutator.data.strings.poly2.decryptionMethod.args.AbstractDecryptionMethodArgument;
import war.metaphor.mutator.data.strings.poly2.decryptionMethod.args.impl.*;
import war.metaphor.mutator.data.strings.poly2.decryptionMethod.args.impl.integer.ByteArgument;
import war.metaphor.mutator.data.strings.poly2.decryptionMethod.args.impl.integer.CacheIndexIntegerArgument;
import war.metaphor.mutator.data.strings.poly2.decryptionMethod.args.impl.integer.IntegerArgument;
import war.metaphor.mutator.data.strings.poly2.decryptionMethod.args.impl.integer.ShortArgument;
import war.metaphor.mutator.data.strings.poly2.decryptionMethod.code.AbstractDecryptionMethodCodePiece;
import war.metaphor.mutator.data.strings.poly2.decryptionMethod.code.impl.AesCodePiece;
import war.metaphor.mutator.data.strings.poly2.decryptionMethod.code.impl.IterativeXorCodePiece;
import war.metaphor.mutator.data.strings.poly2.decryptionMethod.returnType.AbstractDecryptionMethodReturnType;
import war.metaphor.mutator.data.strings.poly2.decryptionMethod.returnType.impl.ByteArrayReturnType;
import war.metaphor.mutator.data.strings.poly2.decryptionMethod.returnType.impl.ObjectReturnType;
import war.metaphor.mutator.data.strings.poly2.decryptionMethod.returnType.impl.StringReturnType;
import war.metaphor.mutator.data.strings.poly2.init.Initializer;
import war.metaphor.tree.JClassNode;
import war.metaphor.util.Dictionary;
import war.metaphor.util.Pair;
import war.metaphor.util.Purpose;
import war.metaphor.util.asm.BytecodeUtil;

import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.concurrent.ThreadLocalRandom;

/**
 * Decryption method, not much to say
 * @author Jan
 */
public final class DecryptionMethod implements Opcodes
{
    private final ArrayList<AbstractDecryptionMethodArgument> arguments;
    private final ArrayList<AbstractDecryptionMethodCodePiece> code;
    private final String methodName;
    private int cacheXorIdx;
    private int cacheVarIdx;

    public final LinkedHashMap<String, Pair<AbstractDecryptionMethodArgument, Object>[]> cachedStrings = new LinkedHashMap<>();
    public final AbstractDecryptionMethodReturnType returnType;
    public final JClassNode parent;
    public final int byteArrayIdx;
    public final FieldNode initField;
    public final FieldNode cacheField;
    public boolean needsCryptoClass;
    public final int initXorKey;

    public DecryptionMethod(final JClassNode parent)
    {
        this.parent = parent;

        // field init
        initField = makeInitField();
        cacheField = makeCacheField();

        // actual start
        methodName = makeMethodName();
        arguments = new ArrayList<>();
        code = new ArrayList<>();

        final int argumentCount = ThreadLocalRandom.current().nextInt(3, 10);

        for (int i = 0; i < argumentCount; i++)
        {
            arguments.add(makeArgument());
        }

        arguments.add(new CacheIndexIntegerArgument());

        Collections.shuffle(arguments);

        final int codeCount = ThreadLocalRandom.current().nextInt(3, 10);
        for (int i = 0; i < codeCount; i++)
        {
            code.add(makeCode());
        }

        // possibly +1 but i wanna rule out possible issues because im kinda unstable today
        byteArrayIdx = argumentCount + 2;

        do
        {
            cacheXorIdx = ThreadLocalRandom.current().nextInt(arguments.size());
        }
        while (arguments.get(cacheXorIdx) instanceof CacheIndexIntegerArgument); // gotta make REALLY sure

        for (int i = 0; i < arguments.size(); i++)
        {
            if (arguments.get(i) instanceof CacheIndexIntegerArgument)
            {
                cacheVarIdx = i;
            }
        }

        this.returnType = makeReturnType();

        this.initXorKey = ThreadLocalRandom.current().nextInt(1, (int) (Math.pow(2, 16) - 1));
    }

    private AbstractDecryptionMethodReturnType makeReturnType()
    {
        return switch (ThreadLocalRandom.current().nextInt(3))
        {
            case 0 -> new ByteArrayReturnType(byteArrayIdx);
            case 1 -> new ObjectReturnType(byteArrayIdx);
            case 2 -> new StringReturnType(byteArrayIdx);
            default -> throw new IllegalStateException("Unexpected value while making return value");
        };
    }

    private AbstractDecryptionMethodCodePiece makeCode()
    {
        return switch(ThreadLocalRandom.current().nextInt(4))
        {
            case 0, 1, 2 -> new IterativeXorCodePiece(arguments); // give this a higher chance
            case 3 -> new AesCodePiece(arguments);
            default -> throw new IllegalStateException("Unexpected value while making code");
        };
    }

    public Pair<AbstractDecryptionMethodArgument, Object>[] storeString(final String str)
    {
        if (cachedStrings.containsKey(str))
        {
            return cachedStrings.get(str);
        }

        final int cacheIdx = cachedStrings.size();
        @SuppressWarnings("unchecked")
        final Pair<AbstractDecryptionMethodArgument, Object>[] args = new Pair /* yes ik weird java typing */ [arguments.size()];

        for (int i = 0; i < args.length; i++)
        {
            args[i] = new Pair<>(arguments.get(i), arguments.get(i).makeRandomValue());
        }

        args[cacheVarIdx].b = cacheIdx ^ toInt(args[cacheXorIdx].b);

        cachedStrings.put(str, args);

        return args;
    }

    public static int toInt(Object obj)
    {
        if (obj instanceof Integer)
        {
            return (int) obj;
        }
        else if (obj instanceof String str)
        {
            return str.hashCode();
        }
        throw new IllegalArgumentException("Integer conversion case not handled: " + obj.getClass().getName());
    }

    public MethodNode toMethodNode()
    {
        final MethodNode methodNode = new MethodNode(
                makeAccess(false),
                methodName,
                makeDescriptor(),
                null,
                null
        );

        final LabelNode noCache = new LabelNode();

        methodNode.instructions.clear(); // gotta make sure :pray:

        methodNode.instructions.add(new VarInsnNode(ILOAD, cacheVarIdx));
        methodNode.instructions.add(arguments.get(cacheXorIdx).load(cacheXorIdx));
        methodNode.instructions.add(new InsnNode(IXOR));
        methodNode.instructions.add(new InsnNode(DUP)); // for decryption if needed
        methodNode.instructions.add(new InsnNode(DUP)); // cache :trol:

        methodNode.instructions.add(new FieldInsnNode(GETSTATIC, parent.name, cacheField.name, cacheField.desc));
        methodNode.instructions.add(new InsnNode(SWAP));
        methodNode.instructions.add(new InsnNode(AALOAD));

        methodNode.instructions.add(new InsnNode(DUP));
        methodNode.instructions.add(new JumpInsnNode(IFNULL, noCache));

        methodNode.instructions.add(new InsnNode(SWAP));
        methodNode.instructions.add(new InsnNode(POP)); // yeah we don't need the idx used for making the cache here lul

        // cache debug lole
/*
        methodNode.instructions.add(new FieldInsnNode(GETSTATIC, "java/lang/System", "out", "Ljava/io/PrintStream;"));
        methodNode.instructions.add(new LdcInsnNode("cached!!!"));
        methodNode.instructions.add(new MethodInsnNode(INVOKEVIRTUAL, "java/io/PrintStream", "println", "(Ljava/lang/String;)V"));
*/
        methodNode.instructions.add(returnType.getCachePackingCode());

        methodNode.instructions.add(new InsnNode(ARETURN));

        methodNode.instructions.add(noCache);

        methodNode.instructions.add(new InsnNode(POP)); // cached string -> guaranteed null in this case

        // int on stack -> string idx

        final int idxIdx = byteArrayIdx + 1; // 0
        final int iIdx = byteArrayIdx + 2;   // 1
        // byteArrayIdx                      // 2
        final int lenIdx = byteArrayIdx + 3; // 3
        // byteArrayIdx                      // 4
        final int jIdx = byteArrayIdx + 4;   // 5
        final int bIdx = byteArrayIdx + 5;   // 6 (bytes field cache)

        final LabelNode l3  = new LabelNode();
        final LabelNode l4  = new LabelNode();
        final LabelNode l6  = new LabelNode();
        final LabelNode l10 = new LabelNode();

        methodNode.instructions.add(new VarInsnNode(ISTORE, idxIdx));

        methodNode.instructions.add(BytecodeUtil.makeInteger(0));
        methodNode.instructions.add(new VarInsnNode(ISTORE, iIdx));

        methodNode.instructions.add(new FieldInsnNode(GETSTATIC, parent.name, initField.name, initField.desc));
        methodNode.instructions.add(new VarInsnNode(ASTORE, bIdx));

        methodNode.instructions.add(l3);

        methodNode.instructions.add(new VarInsnNode(ALOAD, bIdx));
        methodNode.instructions.add(new VarInsnNode(ILOAD, iIdx));
        methodNode.instructions.add(new InsnNode(BALOAD));
        methodNode.instructions.add(BytecodeUtil.makeInteger(0xFF));
        methodNode.instructions.add(new InsnNode(IAND));
        methodNode.instructions.add(BytecodeUtil.makeInteger(8));
        methodNode.instructions.add(new InsnNode(ISHL));
        methodNode.instructions.add(new VarInsnNode(ALOAD, bIdx));
        methodNode.instructions.add(new VarInsnNode(ILOAD, iIdx));
        methodNode.instructions.add(BytecodeUtil.makeInteger(1));
        methodNode.instructions.add(new InsnNode(IADD));
        methodNode.instructions.add(new InsnNode(BALOAD));
        methodNode.instructions.add(BytecodeUtil.makeInteger(0xFF));
        methodNode.instructions.add(new InsnNode(IAND));
        methodNode.instructions.add(new InsnNode(IOR));

        methodNode.instructions.add(BytecodeUtil.makeInteger(initXorKey));
        methodNode.instructions.add(new InsnNode(IXOR));

        methodNode.instructions.add(new VarInsnNode(ISTORE, lenIdx));

        methodNode.instructions.add(new VarInsnNode(ILOAD, idxIdx));
        methodNode.instructions.add(new JumpInsnNode(IFNE, l6));

        methodNode.instructions.add(new VarInsnNode(ILOAD, lenIdx));
        methodNode.instructions.add(new IntInsnNode(NEWARRAY, T_BYTE));
        methodNode.instructions.add(new VarInsnNode(ASTORE, byteArrayIdx));

        methodNode.instructions.add(BytecodeUtil.makeInteger(0));
        methodNode.instructions.add(new VarInsnNode(ISTORE, jIdx));

        methodNode.instructions.add(l10);

        methodNode.instructions.add(new VarInsnNode(ILOAD, jIdx));
        methodNode.instructions.add(new VarInsnNode(ILOAD, lenIdx));
        methodNode.instructions.add(new JumpInsnNode(IF_ICMPGE, l4));

        methodNode.instructions.add(new VarInsnNode(ALOAD, byteArrayIdx));
        methodNode.instructions.add(new VarInsnNode(ILOAD, jIdx));
        methodNode.instructions.add(new VarInsnNode(ALOAD, bIdx));
        methodNode.instructions.add(new VarInsnNode(ILOAD, iIdx));
        methodNode.instructions.add(new VarInsnNode(ILOAD, jIdx));
        methodNode.instructions.add(BytecodeUtil.makeInteger(2));
        methodNode.instructions.add(new InsnNode(IADD));
        methodNode.instructions.add(new InsnNode(IADD));
        methodNode.instructions.add(new InsnNode(BALOAD));
        methodNode.instructions.add(new InsnNode(BASTORE));

        methodNode.instructions.add(new IincInsnNode(jIdx, 1));
        methodNode.instructions.add(new JumpInsnNode(GOTO, l10));

        methodNode.instructions.add(l6);

        methodNode.instructions.add(new VarInsnNode(ILOAD, iIdx));
        methodNode.instructions.add(new VarInsnNode(ILOAD, lenIdx));
        methodNode.instructions.add(BytecodeUtil.makeInteger(2));
        methodNode.instructions.add(new InsnNode(IADD));
        methodNode.instructions.add(new InsnNode(IADD));
        methodNode.instructions.add(new VarInsnNode(ISTORE, iIdx));

        methodNode.instructions.add(new IincInsnNode(idxIdx, -1));
        methodNode.instructions.add(new JumpInsnNode(GOTO, l3));

        methodNode.instructions.add(l4);

        // end

        for (final AbstractDecryptionMethodCodePiece abstractDecryptionMethodCodePiece : code)
        {
            if (abstractDecryptionMethodCodePiece instanceof AesCodePiece)
            {
                needsCryptoClass = true;
            }
            methodNode.instructions.add(abstractDecryptionMethodCodePiece.getDecryptionCode(byteArrayIdx));
        }

        // stack: integer (cache idx)
        methodNode.instructions.add(new FieldInsnNode(GETSTATIC, parent.name, cacheField.name, cacheField.desc));
        methodNode.instructions.add(new InsnNode(SWAP));
        methodNode.instructions.add(new VarInsnNode(ALOAD, byteArrayIdx));
        methodNode.instructions.add(new InsnNode(AASTORE));

        methodNode.instructions.add(returnType.getPackingCode());

        methodNode.instructions.add(new InsnNode(ARETURN));

        return methodNode;
    }

    private String makeMethodName()
    {
        for (MethodNode method : parent.methods)
        {
            Dictionary.addUsed(method.name, Purpose.METHOD);
        }

        return Dictionary.gen(1, Purpose.METHOD);
    }

    private String makeDescriptor()
    {
        final StringBuilder sb = new StringBuilder("(");

        for (AbstractDecryptionMethodArgument argument : arguments)
        {
            sb.append(argument.getDescriptor());
        }

        sb.append(")").append(returnType.getDescriptor());

        return sb.toString();
    }

    private int makeAccess(final boolean field)
    {
        int access = ACC_STATIC;

        access += switch (ThreadLocalRandom.current().nextInt(4))
        {
            case 0 -> ACC_PUBLIC;
            case 1 -> ACC_PRIVATE;
            case 2 -> ACC_PROTECTED;
            case 3 -> 0;
            default -> throw new IllegalStateException("Unexpected value while making access");
        };

        if (field && ThreadLocalRandom.current().nextBoolean())
        {
            access += ACC_FINAL;
        }

        return access;
    }

    private AbstractDecryptionMethodArgument makeArgument()
    {
        return switch(ThreadLocalRandom.current().nextInt(4))
        {
            case 0 -> new ByteArgument();
            case 1 -> new IntegerArgument();
            case 2 -> new ShortArgument();
            case 3 -> new StringArgument();
            default -> throw new IllegalStateException("Illegal argument case");
        };
    }

    public Initializer makeInitializer()
    {
        return new Initializer(this);
    }

    private FieldNode makeCacheField()
    {
        return new FieldNode(
                makeAccess(true),
                makeFieldName(),
                "[[B",
                null,
                null
        );
    }

    private FieldNode makeInitField()
    {
        return new FieldNode(
                makeAccess(true),
                makeFieldName(),
                "[B",
                null,
                null
        );
    }

    private String makeFieldName()
    {
        for (FieldNode field : parent.fields)
        {
            Dictionary.addUsed(field.name, Purpose.FIELD);
        }

        return Dictionary.gen(1, Purpose.FIELD);
    }

    public byte[] encrypt(Map.Entry<String, Pair<AbstractDecryptionMethodArgument, Object>[]> entry)
    {
        byte[] bytes = entry.getKey().getBytes(StandardCharsets.UTF_8);

        for (AbstractDecryptionMethodCodePiece codePiece : code.reversed())
        {
            bytes = codePiece.encrypt(entry.getValue(), bytes, methodName, parent.name.replace("/", "."));
        }

        return bytes;
    }
}
