package war.metaphor.mutator.ref;

import org.apache.commons.lang3.RandomUtils;
import org.objectweb.asm.Handle;
import org.objectweb.asm.Type;
import org.objectweb.asm.tree.*;
import org.objectweb.asm.tree.analysis.BasicValue;
import org.objectweb.asm.tree.analysis.Frame;
import war.configuration.ConfigurationSection;
import war.jnt.annotate.Level;
import war.jnt.annotate.Stability;
import war.metaphor.analysis.frames.LazyFrameProvider;
import war.metaphor.analysis.values.NewInstanceValue;
import war.metaphor.base.ObfuscatorContext;
import war.metaphor.engine.types.IntegerEngine;
import war.metaphor.engine.types.PolymorphicEngine;
import war.metaphor.mutator.Mutator;
import war.metaphor.mutator.flow.BlockBreakMutator;
import war.metaphor.mutator.flow.InstructionShuffleMutator;
import war.metaphor.tree.JClassNode;
import war.metaphor.util.Dictionary;
import war.metaphor.util.Purpose;
import war.metaphor.util.asm.BytecodeUtil;

import java.lang.reflect.Modifier;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

@Stability(Level.HIGH)
public class ReferenceMutator extends Mutator {

    public ReferenceMutator(ObfuscatorContext base, ConfigurationSection config) {
        super(base, config);
    }

    @Override
    public void run(ObfuscatorContext base) {
        for (JClassNode classNode : base.getClasses()) {
            if (classNode.isExempt()) continue;
            if (classNode.version < 52) continue;

            FieldNode methodHandleCache = new FieldNode(ACC_PUBLIC | ACC_STATIC | ACC_FINAL,
                    Dictionary.gen(3, Purpose.FIELD), "[Ljava/lang/invoke/MethodHandle;", null, null);

            int strength = 10;

            PolymorphicEngine mEngine0 = new PolymorphicEngine(strength);
            IntegerEngine mEngine1 = new IntegerEngine(strength);
            IntegerEngine mEngine4 = new IntegerEngine(strength);
            PolymorphicEngine mEngine2 = new PolymorphicEngine(strength);
            PolymorphicEngine mEngine3 = new PolymorphicEngine(strength);

            PolymorphicEngine fEngine0 = new PolymorphicEngine(strength);
            IntegerEngine fEngine1 = new IntegerEngine(strength);
            IntegerEngine fEngine4 = new IntegerEngine(strength);
            PolymorphicEngine fEngine2 = new PolymorphicEngine(strength);
            PolymorphicEngine fEngine3 = new PolymorphicEngine(strength);

            Map<Integer, Integer> opcodeMapping = new HashMap<>();

            while (true) {
                opcodeMapping.put(H_INVOKESTATIC, RandomUtils.nextInt());
                opcodeMapping.put(H_INVOKESPECIAL, RandomUtils.nextInt());
                opcodeMapping.put(H_INVOKEINTERFACE, RandomUtils.nextInt());
                opcodeMapping.put(H_INVOKEVIRTUAL, RandomUtils.nextInt());
                opcodeMapping.put(H_NEWINVOKESPECIAL, RandomUtils.nextInt());
                opcodeMapping.put(H_GETFIELD, RandomUtils.nextInt());
                opcodeMapping.put(H_GETSTATIC, RandomUtils.nextInt());
                opcodeMapping.put(H_PUTFIELD, RandomUtils.nextInt());
                opcodeMapping.put(H_PUTSTATIC, RandomUtils.nextInt());

                Set<Integer> uniqueValues = new HashSet<>(opcodeMapping.values());
                if (uniqueValues.size() == opcodeMapping.size()) {
                    break;
                }
            }

            String dispatcherParams = "(Ljava/lang/invoke/MethodHandles$Lookup;Ljava/lang/String;Ljava/lang/invoke/MethodType;[Ljava/lang/Object;)Ljava/lang/Object;";

            String mDispatcherName = "0";
            String fDispatcherName = "1";

            AtomicBoolean hasMDispatcher = new AtomicBoolean(false);
            AtomicBoolean hasFDispatcher = new AtomicBoolean(false);

            AtomicInteger cacheCounter = new AtomicInteger(0);

            for (MethodNode method : classNode.methods) {

                int leeway = BytecodeUtil.leeway(method);
                if (leeway < 30000)
                    break;

                if (classNode.isExempt(method)) continue;

                LazyFrameProvider frames = new LazyFrameProvider(classNode, method);

                for (AbstractInsnNode instruction : method.instructions) {

                    if (instruction instanceof MethodInsnNode node) {

                        if (node.owner.startsWith("[L")) continue;

                        int opcode = node.getOpcode();
                        boolean constructor = opcode == INVOKESPECIAL && node.name.equals("<init>");
                        String newSig = constructor ? node.desc.replace(")V", ")Ljava/lang/Object;") : opcode == INVOKESTATIC ? node.desc : node.desc.replace("(", "(Ljava/lang/Object;");

                        Type origReturnType = Type.getReturnType(newSig);
                        Type[] args = Type.getArgumentTypes(newSig);

                        for (int i = 0; i < args.length; i++) {
                            Type type = args[i];
                            args[i] = type.getSort() == Type.OBJECT ? Type.getType(Object.class) : type;
                        }

                        newSig = Type.getMethodDescriptor(origReturnType, args);

                        Object[] bsmArgs = new Object[5];
                        bsmArgs[0] = switch (opcode) {
                            case INVOKESTATIC -> H_INVOKESTATIC;
                            case INVOKESPECIAL -> H_INVOKESPECIAL;
                            case INVOKEINTERFACE -> H_INVOKEINTERFACE;
                            case INVOKEVIRTUAL -> H_INVOKEVIRTUAL;
                            default -> throw new IllegalStateException("Unexpected opcode: " + opcode);
                        };

                        if (constructor) {
                            bsmArgs[0] = H_NEWINVOKESPECIAL;
                            Frame<BasicValue> frame = frames.get(instruction);
                            if (frame == null || frame.getStackSize() < 1) continue;

                            BasicValue classInstanceValue = frame.getStack(frame.getStackSize() - args.length - 1);
                            if (!(classInstanceValue instanceof NewInstanceValue iv)) {
                                continue;
                            }

                            boolean cleared = false;

                            for (AbstractInsnNode __node : method.instructions) {
                                if (__node.getOpcode() != NEW || __node.getNext() == null || __node.getNext().getOpcode() != DUP)
                                    continue;

                                AbstractInsnNode next = __node.getNext();

                                Frame<BasicValue> frame2 = frames.get(next);
                                if (frame2 == null || frame2.getStackSize() < 1) continue;

                                BasicValue value = frame2.getStack(frame2.getStackSize() - 1);
                                if (value instanceof NewInstanceValue nv) {
                                    if (nv.isCopy(iv)) {
                                        method.instructions.remove(__node.getNext());
                                        method.instructions.remove(__node);
                                        cleared = true;
                                        break;
                                    }
                                }
                            }

                            if (!cleared) continue;

                        }

                        bsmArgs[0] = opcodeMapping.get(bsmArgs[0]);
                        bsmArgs[0] = mEngine1.run((Integer) bsmArgs[0]);

                        char[] encryptedName = node.name.toCharArray();
                        for (int i = 0; i < encryptedName.length; i++)
                            encryptedName[i] = (char) mEngine0.run(encryptedName[i]);

                        char[] encryptedOwner = node.owner.replace("/", ".").toCharArray();
                        for (int i = 0; i < encryptedOwner.length; i++)
                            encryptedOwner[i] = (char) mEngine2.run(encryptedOwner[i]);

                        char[] encryptedDesc = node.desc.toCharArray();
                        for (int i = 0; i < encryptedDesc.length; i++)
                            encryptedDesc[i] = (char) mEngine3.run(encryptedDesc[i]);

                        bsmArgs[1] = new String(encryptedName);
                        bsmArgs[2] = new String(encryptedDesc);
                        bsmArgs[3] = new String(encryptedOwner);
                        bsmArgs[4] = mEngine4.run(cacheCounter.getAndIncrement());

                        InvokeDynamicInsnNode indy = new InvokeDynamicInsnNode("JNT", newSig, new Handle(H_INVOKESTATIC, classNode.name, mDispatcherName, dispatcherParams, classNode.isInterface()), bsmArgs);

                        InsnList list = new InsnList();

//                        list.add(new FieldInsnNode(GETSTATIC, "java/lang/System", "out", "Ljava/io/PrintStream;"));
//                        list.add(new LdcInsnNode("Preparing to invoke method: " + node.owner + "." + node.name + " with desc: " + node.desc));
//                        list.add(new MethodInsnNode(INVOKEVIRTUAL, "java/io/PrintStream", "println", "(Ljava/lang/String;)V", false));

                        list.add(indy);

//                        list.add(new FieldInsnNode(GETSTATIC, "java/lang/System", "out", "Ljava/io/PrintStream;"));
//                        list.add(new LdcInsnNode("Method invoked: " + node.owner + "." + node.name + " with desc: " + node.desc));
//                        list.add(new MethodInsnNode(INVOKEVIRTUAL, "java/io/PrintStream", "println", "(Ljava/lang/String;)V", false));

                        if (origReturnType.getSort() == Type.ARRAY)
                            list.add(new TypeInsnNode(CHECKCAST, origReturnType.getInternalName()));

                        if (constructor)
                            list.add(new TypeInsnNode(CHECKCAST, node.owner));

                        method.instructions.insert(instruction, list);
                        method.instructions.remove(instruction);

                        hasMDispatcher.set(true);

                    } else if (instruction instanceof FieldInsnNode node) {

                        boolean isStatic = node.getOpcode() == GETSTATIC || node.getOpcode() == PUTSTATIC;
                        int opcode = node.getOpcode();

                        if (opcode == PUTSTATIC || opcode == PUTFIELD) {
                            if (method.name.startsWith("<")) {
                                JClassNode owner = base.loadClass(node.owner);
                                if (owner == null) continue;
                                FieldNode field = owner.getField(node.name, node.desc);
                                if (Modifier.isFinal(field.access)) continue;

                                if (opcode == PUTFIELD) {
                                    Frame<BasicValue> frame = frames.get(instruction);
                                    if (frame == null || frame.getStackSize() < 1) continue;
                                    BasicValue classInstanceValue = frame.getStack(frame.getStackSize() - 2);
                                    if (classInstanceValue instanceof NewInstanceValue) continue;
                                }
                            }
                        }

                        String sig = switch (opcode) {
                            case GETFIELD:
                            case GETSTATIC:
                                yield isStatic ? "()" + node.desc : "(Ljava/lang/Object;)" + node.desc;
                            case PUTFIELD:
                            case PUTSTATIC:
                                yield isStatic ? "(" + node.desc + ")V" : "(Ljava/lang/Object;" + node.desc + ")V";
                            default:
                                throw new IllegalStateException("Unexpected value: " + opcode);
                        };

                        Object[] bsmArgs = new Object[5];
                        bsmArgs[0] = switch (opcode) {
                            case GETFIELD -> H_GETFIELD;
                            case GETSTATIC -> H_GETSTATIC;
                            case PUTFIELD -> H_PUTFIELD;
                            case PUTSTATIC -> H_PUTSTATIC;
                            default -> throw new IllegalStateException("Unexpected opcode: " + opcode);
                        };

                        bsmArgs[0] = opcodeMapping.get(bsmArgs[0]);
                        bsmArgs[0] = fEngine1.run((Integer) bsmArgs[0]);

                        char[] encryptedName = node.name.toCharArray();
                        for (int i = 0; i < encryptedName.length; i++)
                            encryptedName[i] = (char) fEngine0.run(encryptedName[i]);

                        char[] encryptedOwner = node.owner.replace("/", ".").toCharArray();
                        for (int i = 0; i < encryptedOwner.length; i++)
                            encryptedOwner[i] = (char) fEngine2.run(encryptedOwner[i]);

                        char[] encryptedDesc = "()".concat(node.desc).toCharArray();
                        for (int i = 0; i < encryptedDesc.length; i++)
                            encryptedDesc[i] = (char) fEngine3.run(encryptedDesc[i]);

                        bsmArgs[1] = new String(encryptedName);
                        bsmArgs[2] = new String(encryptedOwner);
                        bsmArgs[3] = new String(encryptedDesc);
                        bsmArgs[4] = fEngine4.run(cacheCounter.getAndIncrement());

                        InvokeDynamicInsnNode indy = new InvokeDynamicInsnNode("JNT", sig, new Handle(H_INVOKESTATIC, classNode.name, fDispatcherName, dispatcherParams, classNode.isInterface()), bsmArgs);

                        method.instructions.set(instruction, indy);

                        hasFDispatcher.set(true);
                    }

                    leeway = BytecodeUtil.leeway(method);
                }
            }

            BlockBreakMutator blockBreakMutator = new BlockBreakMutator(base, null);
            InstructionShuffleMutator shuffleMutator = new InstructionShuffleMutator(base, null);

            if (hasMDispatcher.get() || hasFDispatcher.get()) {
                classNode.fields.add(methodHandleCache);
                MethodNode clinit = classNode.getStaticInit();
                InsnList list = new InsnList();
                list.add(BytecodeUtil.generateInteger(cacheCounter.get()));
                list.add(new TypeInsnNode(ANEWARRAY, "java/lang/invoke/MethodHandle"));
                list.add(new FieldInsnNode(PUTSTATIC, classNode.name, methodHandleCache.name, methodHandleCache.desc));
                clinit.instructions.insert(list);
            }

            if (hasMDispatcher.get()) {

                // MethodLookup, String, MethodType, ...Object
                MethodNode dispatcher = new MethodNode(ACC_PUBLIC | ACC_STATIC | ACC_VARARGS, mDispatcherName, dispatcherParams, null, null);

                InsnList code = new InsnList();

                LabelNode handleToCallSite = new LabelNode();

                code.add(new VarInsnNode(ALOAD, 3)); // our data
                code.add(new InsnNode(ICONST_4));
                code.add(new InsnNode(AALOAD));
                code.add(new TypeInsnNode(CHECKCAST, "java/lang/Integer"));
                code.add(new MethodInsnNode(INVOKEVIRTUAL, "java/lang/Integer", "intValue", "()I", false));
                code.add(new VarInsnNode(ISTORE, 10));

                code.add(new FieldInsnNode(GETSTATIC, classNode.name, methodHandleCache.name, methodHandleCache.desc));
                code.add(new VarInsnNode(ILOAD, 10));
                code.add(mEngine4.getInstructions());
                code.add(new InsnNode(AALOAD));
                code.add(new InsnNode(DUP));
                code.add(new JumpInsnNode(IFNONNULL, handleToCallSite));
                code.add(new InsnNode(POP));

                code.add(new VarInsnNode(ALOAD, 3)); // our data
                code.add(new InsnNode(ICONST_0));
                code.add(new InsnNode(AALOAD));
                code.add(new TypeInsnNode(CHECKCAST, "java/lang/Integer"));
                code.add(new MethodInsnNode(INVOKEVIRTUAL, "java/lang/Integer", "intValue", "()I", false));
                code.add(new VarInsnNode(ISTORE, 4)); // Tag

                code.add(new VarInsnNode(ALOAD, 3)); // our data
                code.add(new InsnNode(ICONST_1));
                code.add(new InsnNode(AALOAD));
                code.add(new TypeInsnNode(CHECKCAST, "java/lang/String"));
                generateEngineCode(mEngine0, code);
                code.add(new VarInsnNode(ASTORE, 5)); // Name

                code.add(new VarInsnNode(ALOAD, 3)); // our data
                code.add(new InsnNode(ICONST_2));
                code.add(new InsnNode(AALOAD));
                code.add(new TypeInsnNode(CHECKCAST, "java/lang/String"));

                generateEngineCode(mEngine3, code);
                code.add(new LdcInsnNode(Type.getType("L" + classNode.name + ";")));
                code.add(new MethodInsnNode(INVOKEVIRTUAL, "java/lang/Class", "getClassLoader", "()Ljava/lang/ClassLoader;", false));
                code.add(new MethodInsnNode(INVOKESTATIC, "java/lang/invoke/MethodType", "fromMethodDescriptorString", "(Ljava/lang/String;Ljava/lang/ClassLoader;)Ljava/lang/invoke/MethodType;", false));
                code.add(new VarInsnNode(ASTORE, 6)); // MethodType

                code.add(new VarInsnNode(ALOAD, 3)); // our data
                code.add(new InsnNode(ICONST_3));
                code.add(new InsnNode(AALOAD));
                code.add(new TypeInsnNode(CHECKCAST, "java/lang/String"));

                generateEngineCode(mEngine2, code);
                code.add(new InsnNode(ICONST_0));
                code.add(new LdcInsnNode(Type.getType("L" + classNode.name + ";")));
                code.add(new MethodInsnNode(INVOKEVIRTUAL, "java/lang/Class", "getClassLoader", "()Ljava/lang/ClassLoader;", false));
                code.add(new MethodInsnNode(INVOKESTATIC, "java/lang/Class", "forName", "(Ljava/lang/String;ZLjava/lang/ClassLoader;)Ljava/lang/Class;", false));
                code.add(new VarInsnNode(ASTORE, 7)); // Class

                LabelNode[] labels = new LabelNode[5];
                for (int i = 0; i < labels.length; i++) {
                    labels[i] = new LabelNode();
                }

                LabelNode defaultLabel = new LabelNode();

                LookupSwitchInsnNode switchNode = new LookupSwitchInsnNode(defaultLabel,
                        new int[] {
                                opcodeMapping.get(H_INVOKESTATIC),
                                opcodeMapping.get(H_INVOKESPECIAL),
                                opcodeMapping.get(H_INVOKEINTERFACE),
                                opcodeMapping.get(H_INVOKEVIRTUAL),
                                opcodeMapping.get(H_NEWINVOKESPECIAL),
                        }, labels);

                code.add(new VarInsnNode(ILOAD, 4));
                code.add(mEngine1.getInstructions());
                code.add(switchNode);

                BytecodeUtil.fixLookupSwitch(switchNode);

                code.add(labels[0]); // H_INVOKESTATIC
                code.add(new VarInsnNode(ALOAD, 0));
                code.add(new VarInsnNode(ALOAD, 7));
                code.add(new VarInsnNode(ALOAD, 5));
                code.add(new VarInsnNode(ALOAD, 6));
                code.add(new MethodInsnNode(INVOKEVIRTUAL, "java/lang/invoke/MethodHandles$Lookup", "findStatic", "(Ljava/lang/Class;Ljava/lang/String;Ljava/lang/invoke/MethodType;)Ljava/lang/invoke/MethodHandle;", false));
                code.add(new JumpInsnNode(GOTO, handleToCallSite));

                code.add(labels[1]); // H_INVOKESPECIAL
                code.add(new VarInsnNode(ALOAD, 0));
                code.add(new VarInsnNode(ALOAD, 7));
                code.add(new VarInsnNode(ALOAD, 5));
                code.add(new VarInsnNode(ALOAD, 6));
                code.add(new LdcInsnNode(Type.getType("L" + classNode.name + ";")));
                code.add(new MethodInsnNode(INVOKEVIRTUAL, "java/lang/invoke/MethodHandles$Lookup", "findSpecial", "(Ljava/lang/Class;Ljava/lang/String;Ljava/lang/invoke/MethodType;Ljava/lang/Class;)Ljava/lang/invoke/MethodHandle;", false));
                code.add(new JumpInsnNode(GOTO, handleToCallSite));

                code.add(labels[2]); // H_INVOKEINTERFACE
                code.add(labels[3]); // H_INVOKEVIRTUAL
                code.add(new VarInsnNode(ALOAD, 0));
                code.add(new VarInsnNode(ALOAD, 7));
                code.add(new VarInsnNode(ALOAD, 5));
                code.add(new VarInsnNode(ALOAD, 6));
                code.add(new MethodInsnNode(INVOKEVIRTUAL, "java/lang/invoke/MethodHandles$Lookup", "findVirtual", "(Ljava/lang/Class;Ljava/lang/String;Ljava/lang/invoke/MethodType;)Ljava/lang/invoke/MethodHandle;", false));
                code.add(new JumpInsnNode(GOTO, handleToCallSite));

                code.add(labels[4]);
                code.add(new VarInsnNode(ALOAD, 0));
                code.add(new VarInsnNode(ALOAD, 7));
                code.add(new VarInsnNode(ALOAD, 6));
                code.add(new MethodInsnNode(INVOKEVIRTUAL, "java/lang/invoke/MethodHandles$Lookup", "findConstructor", "(Ljava/lang/Class;Ljava/lang/invoke/MethodType;)Ljava/lang/invoke/MethodHandle;", false));
                code.add(new JumpInsnNode(GOTO, handleToCallSite));

                code.add(defaultLabel);
                code.add(new TypeInsnNode(NEW, "java/lang/UnsupportedOperationException"));
                code.add(new InsnNode(DUP));
                code.add(new LdcInsnNode("Unsupported handle type!"));
                code.add(new MethodInsnNode(INVOKESPECIAL, "java/lang/UnsupportedOperationException", "<init>", "(Ljava/lang/String;)V", false));
                code.add(new InsnNode(ATHROW));

                code.add(handleToCallSite);

                code.add(new InsnNode(DUP));
                code.add(new FieldInsnNode(GETSTATIC, classNode.name, methodHandleCache.name, methodHandleCache.desc));
                code.add(new InsnNode(SWAP));
                code.add(new VarInsnNode(ILOAD, 10));
                code.add(mEngine4.getInstructions());
                code.add(new InsnNode(SWAP));
                code.add(new InsnNode(AASTORE));

                code.add(new VarInsnNode(ALOAD, 2));
                code.add(new MethodInsnNode(INVOKEVIRTUAL, "java/lang/invoke/MethodHandle", "asType", "(Ljava/lang/invoke/MethodType;)Ljava/lang/invoke/MethodHandle;", false));
                code.add(new VarInsnNode(ASTORE, 0));
                code.add(new TypeInsnNode(NEW, "java/lang/invoke/MutableCallSite"));
                code.add(new InsnNode(DUP));
                code.add(new VarInsnNode(ALOAD, 0)); // MethodHandle
                code.add(new MethodInsnNode(INVOKESPECIAL, "java/lang/invoke/MutableCallSite", "<init>", "(Ljava/lang/invoke/MethodHandle;)V", false));
                code.add(new InsnNode(ARETURN));

                dispatcher.instructions = code;

                JClassNode fake = new JClassNode();
                fake.visit(V1_8, ACC_PUBLIC | ACC_FINAL, "war/metaphor/mutator/ref/ReferenceMutator$FakeDispatcher", null, "java/lang/Object", null);
                fake.methods.add(dispatcher);

                blockBreakMutator.run(ObfuscatorContext.builder().classes(Set.of(fake)).build());
                shuffleMutator.run(ObfuscatorContext.builder().classes(Set.of(fake)).build());

                MethodNode addedMethod = fake.methods.getFirst();
                classNode.methods.add(addedMethod);
                // Mark as bootstrap method to exclude from obfuscation
                addedMethod.signature = "bsm::jnt:excluded";
                fake.methods.clear();
            }

            if (hasFDispatcher.get()) {
                // MethodLookup, String, MethodType, ...Object
                MethodNode dispatcher = new MethodNode(ACC_PUBLIC | ACC_STATIC | ACC_VARARGS, fDispatcherName, dispatcherParams, null, null);

                InsnList code = new InsnList();

                LabelNode trampHandleToCallSite = new LabelNode();
                LabelNode divertHandleToCallSite = new LabelNode();
                LabelNode handleToCallSite = new LabelNode();

                code.add(new InsnNode(ICONST_0));
                code.add(new VarInsnNode(ISTORE, 11));

                code.add(new VarInsnNode(ALOAD, 3)); // our data
                code.add(new InsnNode(ICONST_4));
                code.add(new InsnNode(AALOAD));
                code.add(new TypeInsnNode(CHECKCAST, "java/lang/Integer"));
                code.add(new MethodInsnNode(INVOKEVIRTUAL, "java/lang/Integer", "intValue", "()I", false));
                code.add(new VarInsnNode(ISTORE, 10));

                code.add(new FieldInsnNode(GETSTATIC, classNode.name, methodHandleCache.name, methodHandleCache.desc));
                code.add(new VarInsnNode(ILOAD, 10));
                code.add(fEngine4.getInstructions());
                code.add(new InsnNode(AALOAD));
                code.add(new InsnNode(DUP));
                code.add(new JumpInsnNode(IFNONNULL, handleToCallSite));
                code.add(new InsnNode(POP));

                code.add(new JumpInsnNode(GOTO, divertHandleToCallSite));
                code.add(trampHandleToCallSite);
                code.add(new InsnNode(ICONST_1));
                code.add(new VarInsnNode(ISTORE, 11));

                code.add(divertHandleToCallSite);

                code.add(new VarInsnNode(ALOAD, 3)); // our data
                code.add(new InsnNode(ICONST_0));
                code.add(new InsnNode(AALOAD));
                code.add(new TypeInsnNode(CHECKCAST, "java/lang/Integer"));
                code.add(new MethodInsnNode(INVOKEVIRTUAL, "java/lang/Integer", "intValue", "()I", false));
                code.add(new VarInsnNode(ISTORE, 4)); // Tag

                code.add(new VarInsnNode(ALOAD, 3)); // our data
                code.add(new InsnNode(ICONST_1));
                code.add(new InsnNode(AALOAD));
                code.add(new TypeInsnNode(CHECKCAST, "java/lang/String"));
                generateEngineCode(fEngine0, code);
                code.add(new VarInsnNode(ASTORE, 5)); // Name

                code.add(new VarInsnNode(ALOAD, 3)); // our data
                code.add(new InsnNode(ICONST_2));
                code.add(new InsnNode(AALOAD));
                code.add(new TypeInsnNode(CHECKCAST, "java/lang/String"));
                generateEngineCode(fEngine2, code);
                code.add(new InsnNode(ICONST_0));
                code.add(new LdcInsnNode(Type.getType("L" + classNode.name + ";")));
                code.add(new MethodInsnNode(INVOKEVIRTUAL, "java/lang/Class", "getClassLoader", "()Ljava/lang/ClassLoader;", false));
                code.add(new MethodInsnNode(INVOKESTATIC, "java/lang/Class", "forName", "(Ljava/lang/String;ZLjava/lang/ClassLoader;)Ljava/lang/Class;", false));
                code.add(new VarInsnNode(ASTORE, 6)); // Class

                code.add(new VarInsnNode(ALOAD, 3)); // our data
                code.add(new InsnNode(ICONST_3));
                code.add(new InsnNode(AALOAD));
                code.add(new TypeInsnNode(CHECKCAST, "java/lang/String"));
                generateEngineCode(fEngine3, code);
                code.add(new LdcInsnNode(Type.getType("L" + classNode.name + ";")));
                code.add(new MethodInsnNode(INVOKEVIRTUAL, "java/lang/Class", "getClassLoader", "()Ljava/lang/ClassLoader;", false));
                code.add(new MethodInsnNode(INVOKESTATIC, "java/lang/invoke/MethodType", "fromMethodDescriptorString", "(Ljava/lang/String;Ljava/lang/ClassLoader;)Ljava/lang/invoke/MethodType;", false));
                code.add(new MethodInsnNode(INVOKEVIRTUAL, "java/lang/invoke/MethodType", "returnType", "()Ljava/lang/Class;"));
                code.add(new VarInsnNode(ASTORE, 7)); // Type

                LabelNode[] labels = new LabelNode[4];
                for (int i = 0; i < labels.length; i++) {
                    labels[i] = new LabelNode();
                }

                LabelNode defaultLabel = new LabelNode();

                LookupSwitchInsnNode switchNode = new LookupSwitchInsnNode(defaultLabel,
                        new int[] {
                                opcodeMapping.get(H_GETFIELD),
                                opcodeMapping.get(H_GETSTATIC),
                                opcodeMapping.get(H_PUTFIELD),
                                opcodeMapping.get(H_PUTSTATIC)},
                        labels);

                code.add(new VarInsnNode(ALOAD, 0));
                code.add(new VarInsnNode(ALOAD, 6));
                code.add(new VarInsnNode(ALOAD, 5));
                code.add(new VarInsnNode(ALOAD, 7));

                code.add(new VarInsnNode(ILOAD, 4));
                code.add(fEngine1.getInstructions());
                code.add(switchNode);

                BytecodeUtil.fixLookupSwitch(switchNode);

                code.add(labels[0]);
                code.add(new MethodInsnNode(INVOKEVIRTUAL, "java/lang/invoke/MethodHandles$Lookup", "findGetter", "(Ljava/lang/Class;Ljava/lang/String;Ljava/lang/Class;)Ljava/lang/invoke/MethodHandle;", false));
                code.add(new JumpInsnNode(GOTO, handleToCallSite));

                code.add(labels[1]);
                code.add(new MethodInsnNode(INVOKEVIRTUAL, "java/lang/invoke/MethodHandles$Lookup", "findStaticGetter", "(Ljava/lang/Class;Ljava/lang/String;Ljava/lang/Class;)Ljava/lang/invoke/MethodHandle;", false));
                code.add(new JumpInsnNode(GOTO, handleToCallSite));

                code.add(labels[2]);
                code.add(new MethodInsnNode(INVOKEVIRTUAL, "java/lang/invoke/MethodHandles$Lookup", "findSetter", "(Ljava/lang/Class;Ljava/lang/String;Ljava/lang/Class;)Ljava/lang/invoke/MethodHandle;", false));
                code.add(new JumpInsnNode(GOTO, handleToCallSite));

                code.add(labels[3]);
                code.add(new MethodInsnNode(INVOKEVIRTUAL, "java/lang/invoke/MethodHandles$Lookup", "findStaticSetter", "(Ljava/lang/Class;Ljava/lang/String;Ljava/lang/Class;)Ljava/lang/invoke/MethodHandle;", false));
                code.add(new JumpInsnNode(GOTO, handleToCallSite));

                code.add(defaultLabel);
                code.add(new TypeInsnNode(NEW, "java/lang/UnsupportedOperationException"));
                code.add(new InsnNode(DUP));
                code.add(new LdcInsnNode("Unsupported handle type!"));
                code.add(new MethodInsnNode(INVOKESPECIAL, "java/lang/UnsupportedOperationException", "<init>", "(Ljava/lang/String;)V", false));
                code.add(new InsnNode(ATHROW));

                code.add(handleToCallSite);

                LabelNode saveHandle = new LabelNode();
                LabelNode notSaveHandle = new LabelNode();

                code.add(new VarInsnNode(ILOAD, 11));
                code.add(new JumpInsnNode(IFEQ, saveHandle));
                code.add(new JumpInsnNode(GOTO, notSaveHandle));

                code.add(saveHandle);
                code.add(new InsnNode(DUP));
                code.add(new FieldInsnNode(GETSTATIC, classNode.name, methodHandleCache.name, methodHandleCache.desc));
                code.add(new InsnNode(SWAP));
                code.add(new VarInsnNode(ILOAD, 10));
                code.add(fEngine4.getInstructions());
                code.add(new InsnNode(SWAP));
                code.add(new InsnNode(AASTORE));

                code.add(notSaveHandle);
                code.add(new VarInsnNode(ALOAD, 2));
                code.add(new MethodInsnNode(INVOKEVIRTUAL, "java/lang/invoke/MethodHandle", "asType", "(Ljava/lang/invoke/MethodType;)Ljava/lang/invoke/MethodHandle;", false));
                code.add(new VarInsnNode(ASTORE, 0));
                code.add(new TypeInsnNode(NEW, "java/lang/invoke/MutableCallSite"));
                code.add(new InsnNode(DUP));
                code.add(new VarInsnNode(ALOAD, 0)); // MethodHandle
                code.add(new MethodInsnNode(INVOKESPECIAL, "java/lang/invoke/MutableCallSite", "<init>", "(Ljava/lang/invoke/MethodHandle;)V", false));
                code.add(new InsnNode(ARETURN));

                dispatcher.instructions = code;

                JClassNode fake = new JClassNode();
                fake.visit(V1_8, ACC_PUBLIC | ACC_FINAL, "war/metaphor/mutator/ref/ReferenceMutator$FakeDispatcher", null, "java/lang/Object", null);
                fake.methods.add(dispatcher);

                blockBreakMutator.run(ObfuscatorContext.builder().classes(Set.of(fake)).build());
                shuffleMutator.run(ObfuscatorContext.builder().classes(Set.of(fake)).build());

                MethodNode addedMethod = fake.methods.getFirst();
                classNode.methods.add(addedMethod);
                // Mark as bootstrap method to exclude from obfuscation
                addedMethod.signature = "bsm::jnt:excluded";
                fake.methods.clear();
            }
        }
    }

    private void generateEngineCode(PolymorphicEngine engine, InsnList code) {

        code.add(new TypeInsnNode(NEW, "java/lang/StringBuilder"));
        code.add(new InsnNode(DUP));
        code.add(new InsnNode(DUP2_X1));
        code.add(new InsnNode(POP2));
        code.add(new MethodInsnNode(INVOKESPECIAL, "java/lang/StringBuilder", "<init>", "(Ljava/lang/String;)V", false)); // S: [COUNTER, INIT]

        LabelNode start = new LabelNode(); // S: [INIT, COUNTER]
        LabelNode end = new LabelNode(); // : [INIT, COUNTER, STRLEN]

        code.add(BytecodeUtil.generateInteger(0));

        code.add(start);
        code.add(new InsnNode(SWAP)); // S: [COUNTER, INIT]

        code.add(new InsnNode(DUP));
        code.add(new MethodInsnNode(INVOKEVIRTUAL, "java/lang/StringBuilder", "length", "()I", false)); // S: [COUNTER, INIT, STRLEN]

        code.add(new InsnNode(SWAP));
        code.add(new InsnNode(DUP_X2));
        code.add(new InsnNode(POP)); // S: [INIT, COUNTER, STRLEN]

        code.add(new InsnNode(DUP2));  // S: [INIT, COUNTER, STRLEN, COUNTER, STRLEN]
        code.add(new JumpInsnNode(IF_ICMPGE, end)); // S: [INIT, COUNTER, STRLEN]

        code.add(new InsnNode(POP)); // S: [INIT, COUNTER]
        code.add(new InsnNode(DUP2)); // S: [INIT, COUNTER, INIT, COUNTER]

        code.add(new MethodInsnNode(INVOKEVIRTUAL, "java/lang/StringBuilder", "charAt", "(I)C", false)); // S: [INIT, COUNTER, CHAR]

        code.add(engine.getInstructions());
        code.add(new InsnNode(I2C)); // S: [INIT, COUNTER, CHAR]

        code.add(new InsnNode(DUP));
        code.add(new InsnNode(DUP2_X2));
        code.add(new InsnNode(POP2));
        code.add(new InsnNode(DUP2_X2));
        code.add(new InsnNode(DUP2_X1));
        code.add(new InsnNode(POP2)); // S: [INIT, COUNTER, CHAR, INIT, COUNTER, CHAR]

        code.add(new MethodInsnNode(INVOKEVIRTUAL, "java/lang/StringBuilder", "setCharAt", "(IC)V", false));

        code.add(new InsnNode(POP)); // S: [INIT, COUNTER]
        code.add(BytecodeUtil.generateInteger(1));
        code.add(new InsnNode(IADD)); // S: [INIT, COUNTER]

        code.add(new JumpInsnNode(GOTO, start));

        code.add(end);
        code.add(new InsnNode(POP2));
        code.add(new MethodInsnNode(INVOKEVIRTUAL, "java/lang/StringBuilder", "toString", "()Ljava/lang/String;", false));

    }

}
