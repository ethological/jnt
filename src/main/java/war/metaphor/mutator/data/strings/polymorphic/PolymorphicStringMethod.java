package war.metaphor.mutator.data.strings.polymorphic;

import org.objectweb.asm.Opcodes;
import org.objectweb.asm.Type;
import org.objectweb.asm.tree.*;
import war.metaphor.mutator.data.strings.polymorphic.math.IPolymorphicMath;
import war.metaphor.mutator.data.strings.polymorphic.math.impl.ReminderShift;
import war.metaphor.mutator.data.strings.polymorphic.math.impl.SeededRandom;
import war.metaphor.mutator.data.strings.polymorphic.math.impl.SimpleXor;
import war.metaphor.tree.JClassNode;
import war.metaphor.util.Dictionary;
import war.metaphor.util.Purpose;
import war.metaphor.util.asm.BytecodeUtil;
import war.metaphor.util.interfaces.IRandom;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedList;

/**
 * Polymorphic string decryption method generator
 * (works, but i will have to make it actually random soon)
 * @author jan
 */
public class PolymorphicStringMethod implements Opcodes, IRandom {
    private final LinkedHashMap<String, Integer> cachedStrings = new LinkedHashMap<>();
    private final LinkedList<IPolymorphicMath> idxModifiers = new LinkedList<>();
    public final boolean shouldStringCast = RANDOM.nextBoolean();
    public final ArrayList<Type> params = new ArrayList<>();
    public final MethodNode method;
    public final JClassNode parent;
    public final FieldNode cache;
    public final FieldNode init;
    public final String cacheName;
    public final String initName;
    private int secondInt;
    private int firstInt;

    private PolymorphicStringMethod(JClassNode clazz, String name, String cache, String init) {
        this.parent = clazz;
        this.cacheName = cache;
        this.initName = init;
        this.method = new MethodNode(ACC_PUBLIC | ACC_STATIC, name, makeParams(params), null, null);
        this.cache = new FieldNode(ACC_PRIVATE | ACC_STATIC, cache, "[Ljava/lang/Object;", null, null);
        this.init = new FieldNode(ACC_PRIVATE | ACC_STATIC, init, "[Ljava/lang/String;", null, null);

        for (int i = 0; i < RANDOM.nextInt(5) + 2; i++) {
            this.idxModifiers.add(makePolymorphic());
        }

        this.method.instructions = getInstructions();
    }

    /**
     * Generates a random piece of math to make the method more random
     * @return Polymorphic math to be dumped into the method
     */
    private IPolymorphicMath makePolymorphic() {
        return switch (RANDOM.nextInt(3)) {
            case 0 -> new SeededRandom();
            case 1 -> new SimpleXor();
            case 2 -> new ReminderShift();
            default -> throw new IllegalStateException("Unexpected polymorphic value");
        };
    }

    /**
     * Makes up parameters and returns a descriptor
     * @param params Arraylist to add the parameters to
     * @return Method descriptor
     */
    private String makeParams(ArrayList<Type> params) {
        StringBuilder sb = new StringBuilder("(");

        for (int i = 0; i < RANDOM.nextInt(5); i++) {
            Type type = BytecodeUtil.getRandomType();
            params.add(type);
            sb.append(type.getDescriptor());
        }

        firstInt = params.size();
        params.add(Type.INT_TYPE);
        sb.append("I");

        for (int i = 0; i < RANDOM.nextInt(5); i++) {
            Type type = BytecodeUtil.getRandomType();
            params.add(type);
            sb.append(type.getDescriptor());
        }

        secondInt = params.size();
        params.add(Type.INT_TYPE);
        sb.append("I");

        for (int i = 0; i < RANDOM.nextInt(5); i++) {
            Type type = BytecodeUtil.getRandomType();
            params.add(type);
            sb.append(type.getDescriptor());
        }

        sb.append(")").append(shouldStringCast ? "Ljava/lang/Object;" : "Ljava/lang/String;");

        return sb.toString();
    }

    /**
     * Please don't question this
     * @return InsnList that contains the content of the decryption method
     */
    private InsnList getInstructions() {
        InsnList list = new InsnList();

        // methodVisitor = classWriter.visitMethod(Opcodes.ACC_PRIVATE | Opcodes.ACC_STATIC, "xor", "(II)Ljava/lang/String;", null, null);
        LabelNode label0 = new LabelNode();
        list.add(label0);
        list.add(new MethodInsnNode(Opcodes.INVOKESTATIC, "java/lang/Thread", "currentThread", "()Ljava/lang/Thread;", false));
        list.add(new MethodInsnNode(Opcodes.INVOKEVIRTUAL, "java/lang/Thread", "getStackTrace", "()[Ljava/lang/StackTraceElement;", false));
        list.add(new InsnNode(Opcodes.ICONST_2));
        list.add(new InsnNode(Opcodes.AALOAD));
        list.add(new MethodInsnNode(Opcodes.INVOKEVIRTUAL, "java/lang/StackTraceElement", "getMethodName", "()Ljava/lang/String;", false));
        list.add(new MethodInsnNode(Opcodes.INVOKEVIRTUAL, "java/lang/String", "intern", "()Ljava/lang/String;", false));
        list.add(new MethodInsnNode(Opcodes.INVOKEVIRTUAL, "java/lang/String", "hashCode", "()I", false));
        list.add(new VarInsnNode(Opcodes.ISTORE, 2 + params.size()));
        list.add(new VarInsnNode(Opcodes.ILOAD, firstInt));
        list.add(new VarInsnNode(Opcodes.ILOAD, 2 + params.size()));
        for (IPolymorphicMath idxModifier : idxModifiers) {
            list.add(idxModifier.dump());
        }
        list.add(new InsnNode(Opcodes.IXOR));
        list.add(new VarInsnNode(Opcodes.ISTORE, 3 + params.size()));
        list.add(new FieldInsnNode(Opcodes.GETSTATIC, parent.name, cacheName, "[Ljava/lang/Object;"));
        list.add(new VarInsnNode(Opcodes.ILOAD, 3 + params.size()));
        list.add(new InsnNode(Opcodes.AALOAD));
        LabelNode label3 = new LabelNode();
        list.add(new JumpInsnNode(Opcodes.IFNONNULL, label3));
        list.add(new FieldInsnNode(Opcodes.GETSTATIC, parent.name, initName, "[Ljava/lang/String;"));
        list.add(new VarInsnNode(Opcodes.ILOAD, 3 + params.size()));
        list.add(new InsnNode(Opcodes.AALOAD));
        list.add(new MethodInsnNode(Opcodes.INVOKEVIRTUAL, "java/lang/String", "toCharArray", "()[C", false));
        list.add(new VarInsnNode(Opcodes.ASTORE, 4 + params.size()));
        list.add(new InsnNode(Opcodes.ICONST_0));
        list.add(new VarInsnNode(Opcodes.ISTORE, 5 + params.size()));
        LabelNode label6 = new LabelNode();
        list.add(label6);
        list.add(new VarInsnNode(Opcodes.ILOAD, 5 + params.size()));
        list.add(new VarInsnNode(Opcodes.ALOAD, 4 + params.size()));
        list.add(new InsnNode(Opcodes.ARRAYLENGTH));
        LabelNode label7 = new LabelNode();
        list.add(new JumpInsnNode(Opcodes.IF_ICMPGE, label7));
        list.add(new VarInsnNode(Opcodes.ALOAD, 4 + params.size()));
        list.add(new VarInsnNode(Opcodes.ILOAD, 5 + params.size()));
        list.add(new InsnNode(Opcodes.DUP2));
        list.add(new InsnNode(Opcodes.CALOAD));
        list.add(new VarInsnNode(Opcodes.ILOAD, 5 + params.size()));
        list.add(new VarInsnNode(Opcodes.ILOAD, secondInt));
        list.add(new InsnNode(Opcodes.IXOR));
        list.add(new InsnNode(Opcodes.I2C));
        list.add(new InsnNode(Opcodes.IXOR));
        list.add(new InsnNode(Opcodes.I2C));
        list.add(new InsnNode(Opcodes.CASTORE));
        list.add(new IincInsnNode(5 + params.size(), 1));
        list.add(new JumpInsnNode(Opcodes.GOTO, label6));
        list.add(label7);
        list.add(new FieldInsnNode(Opcodes.GETSTATIC, parent.name, cacheName, "[Ljava/lang/Object;"));
        list.add(new VarInsnNode(Opcodes.ILOAD, 3 + params.size()));
        list.add(new TypeInsnNode(Opcodes.NEW, "java/lang/String"));
        list.add(new InsnNode(Opcodes.DUP));
        list.add(new VarInsnNode(Opcodes.ALOAD, 4 + params.size()));
        list.add(new MethodInsnNode(Opcodes.INVOKESPECIAL, "java/lang/String", "<init>", "([C)V", false));
        list.add(new MethodInsnNode(Opcodes.INVOKEVIRTUAL, "java/lang/String", "intern", "()Ljava/lang/String;", false));
        list.add(new InsnNode(Opcodes.AASTORE));
        list.add(label3);
        list.add(new FieldInsnNode(Opcodes.GETSTATIC, parent.name, cacheName, "[Ljava/lang/Object;"));
        list.add(new VarInsnNode(Opcodes.ILOAD, 3 + params.size()));
        list.add(new InsnNode(Opcodes.AALOAD));
        list.add(new TypeInsnNode(Opcodes.CHECKCAST, "java/lang/String"));
        list.add(new InsnNode(Opcodes.ARETURN));

        return list;
    }

    /**
     * Adds the generated method to the class wrapper given to the constructor
     */
    public void addMethod() {
        parent.methods.add(method);
        parent.fields.add(init);
        parent.fields.add(cache);

        makeClinit();
    }

    /**
     * Does the clinit setup
     */
    private void makeClinit() {
        InsnList list = new InsnList();

        // encrypted setup that i lowkey stole from zkm
        final int idxKey = RANDOM.nextInt(Character.MAX_VALUE);

        final ArrayList<Integer> keys = new ArrayList<>();

        for (int i = 0; i < 6; i++) {
            keys.add(RANDOM.nextInt(255));
        }

        final StringBuilder sb = new StringBuilder();

        for (String s : cachedStrings.keySet()) {
            char[] chars = s.toCharArray();

            for (int i = 0; i < chars.length; i++) {

                chars[i] ^= (char) (i ^ cachedStrings.get(s));

                chars[i] ^= keys.get(i % keys.size());

            }

            sb.append((char) (s.length() ^ idxKey)).append(new String(chars));
        }

        MethodNode si = parent.getStaticInit();
        
        final int arrVarIdx = ++si.maxLocals;    // 0
        final int strIdx = ++si.maxLocals;       // 1
        final int idx = ++si.maxLocals;          // 2
        final int arrIdx = ++si.maxLocals;       // 3
        final int lenIdx = ++si.maxLocals;       // 4
        final int subIdx = ++si.maxLocals;       // 5
        final int loopIndexIdx = ++si.maxLocals; // 6

        final LabelNode veryBegin = new LabelNode();         // L4
        final LabelNode switchBegin = new LabelNode();       // L7
        final LabelNode transformedString = new LabelNode(); // L8
        final LabelNode switchCase0 = new LabelNode();       // L10
        final LabelNode switchCase1 = new LabelNode();       // L11
        final LabelNode switchCase2 = new LabelNode();       // L12
        final LabelNode switchCase3 = new LabelNode();       // L13
        final LabelNode switchCase4 = new LabelNode();       // L14
        final LabelNode switchCaseDflt = new LabelNode();    // L15
        final LabelNode switchSkip = new LabelNode();        // L16

        list.add(BytecodeUtil.makeInteger(cachedStrings.size()));
        list.add(new TypeInsnNode(Opcodes.ANEWARRAY, "java/lang/String"));
        list.add(new VarInsnNode(Opcodes.ASTORE, arrVarIdx));

        list.add(new LdcInsnNode(sb.toString()));
        list.add(new VarInsnNode(Opcodes.ASTORE, strIdx));

        list.add(BytecodeUtil.makeInteger(0));
        list.add(new VarInsnNode(Opcodes.ISTORE, idx));

        list.add(BytecodeUtil.makeInteger(0));
        list.add(new VarInsnNode(Opcodes.ISTORE, arrIdx));

        list.add(veryBegin);
        list.add(new VarInsnNode(Opcodes.ALOAD, strIdx));
        list.add(new VarInsnNode(Opcodes.ILOAD, idx));
        list.add(new IincInsnNode(idx, 1));
        list.add(new MethodInsnNode(Opcodes.INVOKEVIRTUAL, "java/lang/String", "charAt", "(I)C"));
        list.add(BytecodeUtil.makeInteger(idxKey));
        list.add(new InsnNode(Opcodes.IXOR));
        list.add(new VarInsnNode(Opcodes.ISTORE, lenIdx));

        list.add(new VarInsnNode(Opcodes.ALOAD, strIdx));
        list.add(new VarInsnNode(Opcodes.ILOAD, idx));
        list.add(new VarInsnNode(Opcodes.ILOAD, lenIdx));
        list.add(new VarInsnNode(Opcodes.ILOAD, idx));
        list.add(new InsnNode(Opcodes.IADD));
        list.add(new MethodInsnNode(Opcodes.INVOKEVIRTUAL, "java/lang/String", "substring", "(II)Ljava/lang/String;"));
        list.add(new MethodInsnNode(Opcodes.INVOKEVIRTUAL, "java/lang/String", "toCharArray", "()[C"));
        list.add(new VarInsnNode(Opcodes.ASTORE, subIdx));

        list.add(BytecodeUtil.makeInteger(0));
        list.add(new VarInsnNode(Opcodes.ISTORE, loopIndexIdx));

        list.add(switchBegin);
        list.add(new VarInsnNode(Opcodes.ILOAD, loopIndexIdx));
        list.add(new VarInsnNode(Opcodes.ALOAD, subIdx));
        list.add(new InsnNode(Opcodes.ARRAYLENGTH));
        list.add(new JumpInsnNode(Opcodes.IF_ICMPGE, transformedString));

        list.add(new VarInsnNode(Opcodes.ALOAD, subIdx));
        list.add(new VarInsnNode(Opcodes.ILOAD, loopIndexIdx));
        list.add(new InsnNode(Opcodes.DUP2));
        list.add(new InsnNode(Opcodes.CALOAD));
        list.add(new VarInsnNode(Opcodes.ILOAD, loopIndexIdx));
        list.add(BytecodeUtil.makeInteger(6));
        list.add(new InsnNode(Opcodes.IREM));
        list.add(new TableSwitchInsnNode(0, 4, switchCaseDflt, switchCase0, switchCase1, switchCase2, switchCase3, switchCase4));

        list.add(switchCase0);
        list.add(BytecodeUtil.makeInteger(keys.getFirst()));
        list.add(new JumpInsnNode(Opcodes.GOTO, switchSkip));

        list.add(switchCase1);
        list.add(BytecodeUtil.makeInteger(keys.get(1)));
        list.add(new JumpInsnNode(Opcodes.GOTO, switchSkip));

        list.add(switchCase2);
        list.add(BytecodeUtil.makeInteger(keys.get(2)));
        list.add(new JumpInsnNode(Opcodes.GOTO, switchSkip));

        list.add(switchCase3);
        list.add(BytecodeUtil.makeInteger(keys.get(3)));
        list.add(new JumpInsnNode(Opcodes.GOTO, switchSkip));

        list.add(switchCase4);
        list.add(BytecodeUtil.makeInteger(keys.get(4)));
        list.add(new JumpInsnNode(Opcodes.GOTO, switchSkip));

        list.add(switchCaseDflt);
        list.add(BytecodeUtil.makeInteger(keys.get(5)));

        list.add(switchSkip);
        list.add(new InsnNode(Opcodes.IXOR));
        list.add(new InsnNode(Opcodes.I2C));
        list.add(new InsnNode(Opcodes.CASTORE));
        list.add(new IincInsnNode(loopIndexIdx, 1));
        list.add(new JumpInsnNode(Opcodes.GOTO, switchBegin));

        list.add(transformedString);
        list.add(new VarInsnNode(Opcodes.ALOAD, arrVarIdx));
        list.add(new VarInsnNode(Opcodes.ILOAD, arrIdx));
        list.add(new IincInsnNode(arrIdx, 1));
        list.add(new TypeInsnNode(Opcodes.NEW, "java/lang/String"));
        list.add(new InsnNode(Opcodes.DUP));
        list.add(new VarInsnNode(Opcodes.ALOAD, subIdx));
        list.add(new MethodInsnNode(Opcodes.INVOKESPECIAL, "java/lang/String", "<init>", "([C)V"));
        list.add(new MethodInsnNode(Opcodes.INVOKEVIRTUAL, "java/lang/String", "intern", "()Ljava/lang/String;"));
        list.add(new InsnNode(Opcodes.AASTORE));

        list.add(new VarInsnNode(Opcodes.ILOAD, idx));
        list.add(new VarInsnNode(Opcodes.ILOAD, lenIdx));
        list.add(new InsnNode(Opcodes.IADD));
        list.add(new VarInsnNode(Opcodes.ISTORE, idx));

        list.add(new VarInsnNode(Opcodes.ILOAD, idx));
        list.add(new VarInsnNode(Opcodes.ALOAD, strIdx));
        list.add(new MethodInsnNode(Opcodes.INVOKEVIRTUAL, "java/lang/String", "length", "()I"));
        list.add(new JumpInsnNode(Opcodes.IF_ICMPLT, veryBegin));

        list.add(new VarInsnNode(Opcodes.ALOAD, arrVarIdx));
        list.add(new InsnNode(Opcodes.DUP)); // for the array length getter
        list.add(new FieldInsnNode(Opcodes.PUTSTATIC, parent.name, initName, "[Ljava/lang/String;"));

        list.add(new InsnNode(Opcodes.ARRAYLENGTH));
        list.add(new TypeInsnNode(Opcodes.ANEWARRAY, "java/lang/String"));
        list.add(new FieldInsnNode(Opcodes.PUTSTATIC, parent.name, cacheName, "[Ljava/lang/Object;"));

        si.instructions.insertBefore(si.instructions.getFirst(), list);
    }

    /**
     * Makes a string decryption call and reuses indices if they are cached already
     * @param str String to make the call for
     * @param callerMethod Method that its being called from
     * @return InsnList that returns the string
     */
    public InsnList storeString(String str, String callerMethod) {
        int idx = -1;
        if (cachedStrings.containsKey(str)) {
            int index = 0;
            for (String s : cachedStrings.keySet()) {
                if (s.equals(str)) {
                    idx = index;
                    break;
                }
                index++;
            }
        } else {
            idx = cachedStrings.size();
            cachedStrings.put(str, RANDOM.nextInt());
        }

        if (idx == -1) {
            throw new IllegalStateException("How does this even happen???");
        }

        InsnList list = new InsnList();

        for (int i = 0; i < params.size(); i++) {
            if (i == firstInt) {
                int currentValue = callerMethod.hashCode();
                for (IPolymorphicMath idxModifier : idxModifiers) {
                    currentValue = idxModifier.apply(currentValue);
                }
                list.add(BytecodeUtil.makeInteger(currentValue ^ idx));
            } else if (i == secondInt) {
                list.add(BytecodeUtil.makeInteger(cachedStrings.get(str)));
            } else {
                switch (params.get(i).getInternalName()) {
                    case "Z" -> {
                        list.add(BytecodeUtil.makeInteger(RANDOM.nextInt(1)));
                    }
                    case "C" -> {
                        list.add(BytecodeUtil.makeInteger(RANDOM.nextInt(Character.MIN_VALUE, Character.MAX_VALUE)));
                    }
                    case "B" -> {
                        list.add(BytecodeUtil.makeInteger(RANDOM.nextInt(Byte.MIN_VALUE, Byte.MAX_VALUE)));
                    }
                    case "S" -> {
                        list.add(BytecodeUtil.makeInteger(RANDOM.nextInt(Short.MIN_VALUE, Short.MAX_VALUE)));
                    }
                    case "I" -> {
                        list.add(BytecodeUtil.makeInteger(RANDOM.nextInt()));
                    }
                    case "F" -> {
                        list.add(new LdcInsnNode(RANDOM.nextFloat()));
                    }
                    default -> throw new IllegalStateException("Parameter " + params.get(i).getInternalName() + " not handled");
                }
            }
        }

        list.add(new MethodInsnNode(Opcodes.INVOKESTATIC, parent.name, method.name, method.desc));
        if (shouldStringCast) {
            list.add(new TypeInsnNode(Opcodes.CHECKCAST, "java/lang/String"));
        }

        return list;
    }

    /**
     * Use this to generate an instance of this class
     * @param clazz Input class wrapper (will add the decryption method to this class)
     * @return An instance of this
     */
    public static PolymorphicStringMethod generate(JClassNode clazz) {
        for (MethodNode method : clazz.methods) {
            Dictionary.addUsed(method.name, Purpose.METHOD);
        }
        for (FieldNode method : clazz.fields) {
            Dictionary.addUsed(method.name, Purpose.FIELD);
        }

        return new PolymorphicStringMethod(clazz, Dictionary.gen(1, Purpose.CLASS), Dictionary.gen(1, Purpose.FIELD), Dictionary.gen(1, Purpose.FIELD));
    }
}
