package war.metaphor.mutator.loader;

import org.objectweb.asm.Handle;
import org.objectweb.asm.Type;
import org.objectweb.asm.tree.*;
import war.configuration.ConfigurationSection;
import war.jnt.annotate.Level;
import war.jnt.annotate.Stability;
import war.jnt.fusebox.impl.Internal;
import war.metaphor.base.ObfuscatorContext;
import war.metaphor.mutator.Mutator;
import war.metaphor.tree.JClassNode;
import war.metaphor.util.Dictionary;
import war.metaphor.util.Purpose;
import war.metaphor.util.asm.BytecodeUtil;

import java.lang.reflect.Modifier;
import java.util.HashMap;
import java.util.Map;

@Stability(Level.HIGH)
public class IndyMutator extends Mutator {

    public IndyMutator(ObfuscatorContext base, ConfigurationSection config) {
        super(base, config);
    }

    @Override
    public void run(ObfuscatorContext base) {
        for (JClassNode classNode : base.getClasses()) {
            if (classNode.isExempt()) continue;

            FieldNode methodHandlesCache = new FieldNode(
                    ACC_PUBLIC | ACC_STATIC | ACC_FINAL,
                    Dictionary.gen(3, Purpose.FIELD),
                    "[Ljava/lang/invoke/MethodHandle;",
                    null, null
            );

            Map<String, Integer> boostrapIds = new HashMap<>();

            for (MethodNode method : classNode.methods) {
                if (Modifier.isAbstract(method.access)) continue;
                if (classNode.isExempt(method)) continue;
                if (Internal.disallowedTranspile(classNode, method)) continue;

                BytecodeUtil.computeMaxLocals(method);

                int vCallSite = method.maxLocals++;
                int vStackArray = method.maxLocals++;
                int cacheFlag = method.maxLocals++;

                for (AbstractInsnNode instruction : method.instructions) {
                    if (instruction instanceof InvokeDynamicInsnNode node) {

                        Handle bsm = node.bsm;

                        InsnList callerObj = new InsnList();
                        callerObj.add(new LdcInsnNode(Type.getType("L" + classNode.name + ";")));

                        InsnList bootstrapMethodObj = new InsnList();
                        bootstrapMethodObj.add(createHandle(classNode, bsm));

                        InsnList nameObj = new InsnList();
                        nameObj.add(new LdcInsnNode(node.name));
//                        nameObj.add(new MethodInsnNode(INVOKEVIRTUAL, "java/lang/String", "intern", "()Ljava/lang/String;", false));

                        InsnList typeObj = createMethodType(classNode, node.desc);

                        InsnList staticArguments = new InsnList();
                        staticArguments.add(new LdcInsnNode(node.bsmArgs.length));
                        staticArguments.add(new TypeInsnNode(ANEWARRAY, "java/lang/Object"));
                        for (int i = 0; i < node.bsmArgs.length; i++) {
                            staticArguments.add(new InsnNode(DUP));
                            staticArguments.add(new LdcInsnNode(i));
                            switch (node.bsmArgs[i]) {
                                case Integer _ -> {
                                    staticArguments.add(new LdcInsnNode(node.bsmArgs[i]));
                                    staticArguments.add(new MethodInsnNode(INVOKESTATIC, "java/lang/Integer", "valueOf", "(I)Ljava/lang/Integer;", false));
                                }
                                case Float _ -> {
                                    staticArguments.add(new LdcInsnNode(node.bsmArgs[i]));
                                    staticArguments.add(new MethodInsnNode(INVOKESTATIC, "java/lang/Float", "valueOf", "(F)Ljava/lang/Float;", false));
                                }
                                case Double _ -> {
                                    staticArguments.add(new LdcInsnNode(node.bsmArgs[i]));
                                    staticArguments.add(new MethodInsnNode(INVOKESTATIC, "java/lang/Double", "valueOf", "(D)Ljava/lang/Double;", false));
                                }
                                case Long _ -> {
                                    staticArguments.add(new LdcInsnNode(node.bsmArgs[i]));
                                    staticArguments.add(new MethodInsnNode(INVOKESTATIC, "java/lang/Long", "valueOf", "(J)Ljava/lang/Long;", false));
                                }
                                case Handle _ -> {
                                    staticArguments.add(createHandle(classNode, (Handle) node.bsmArgs[i]));
                                }
                                case Type type -> {
                                    if (type.getSort() == 11) {
                                        staticArguments.add(createMethodType(classNode, type.getDescriptor()));
                                    } else {
                                        staticArguments.add(new LdcInsnNode(node.bsmArgs[i]));
                                    }
                                }
                                default -> staticArguments.add(new LdcInsnNode(node.bsmArgs[i]));
                            }
                            staticArguments.add(new InsnNode(AASTORE));
                        }

                        int vBootstrapId = boostrapIds.computeIfAbsent(BytecodeUtil.toString(node), _ -> boostrapIds.size());
//
//                        InsnList appendixResult = new InsnList();
//                        appendixResult.add(new LdcInsnNode(1));
//                        appendixResult.add(new TypeInsnNode(ANEWARRAY, "java/lang/Object"));
//                        appendixResult.add(new VarInsnNode(ASTORE, vCallSite));
//                        appendixResult.add(new VarInsnNode(ALOAD, vCallSite));

                        //static MemberName linkCallSite(Object callerObj,
                        //                                   Object bootstrapMethodObj,
                        //                                   Object nameObj, Object typeObj,
                        //                                   Object staticArguments,
                        //                                   Object[] appendixResult) {
                        InsnList list = new InsnList();

                        LabelNode preInvokeLabel = new LabelNode();
                        LabelNode invokeLabel = new LabelNode();

                        list.add(new FieldInsnNode(GETSTATIC, classNode.name, methodHandlesCache.name, methodHandlesCache.desc));
                        list.add(BytecodeUtil.generateInteger(vBootstrapId));
                        list.add(new InsnNode(AALOAD));
                        list.add(new VarInsnNode(ASTORE, vCallSite));
                        list.add(new VarInsnNode(ALOAD, vCallSite));
                        list.add(new JumpInsnNode(IFNONNULL, preInvokeLabel));

                        list.add(bootstrapMethodObj);
                        list.add(nameObj);
                        list.add(typeObj);
                        list.add(staticArguments);
                        list.add(callerObj);
//                        list.add(appendixResult);


                        //MethodHandle bootstrapMethod,
                        //                             // Callee information:
                        //                             String name, MethodType type,
                        //                             // Extra arguments for BSM, if any:
                        //                             Object info,
                        //                             // Caller information:
                        //                             Class<?> callerClass

                        list.add(new MethodInsnNode(INVOKESTATIC, "java/lang/invoke/CallSite", "makeSite",
                                "(Ljava/lang/invoke/MethodHandle;Ljava/lang/String;Ljava/lang/invoke/MethodType;Ljava/lang/Object;Ljava/lang/Class;)Ljava/lang/invoke/CallSite;", false));
                        list.add(new MethodInsnNode(INVOKEVIRTUAL, "java/lang/invoke/CallSite", "dynamicInvoker", "()Ljava/lang/invoke/MethodHandle;", false));
                        list.add(new VarInsnNode(ASTORE, vCallSite));
                        list.add(new InsnNode(ICONST_0));
                        list.add(new VarInsnNode(ISTORE, cacheFlag));
                        list.add(new JumpInsnNode(GOTO, invokeLabel));

//                        list.add(new MethodInsnNode(INVOKESTATIC, "java/lang/invoke/MethodHandleNatives", "linkCallSiteImpl", "(Ljava/lang/Class;Ljava/lang/invoke/MethodHandle;Ljava/lang/String;Ljava/lang/invoke/MethodType;Ljava/lang/Object;[Ljava/lang/Object;)Ljava/lang/invoke/MemberName;", false));
//                        list.add(new InsnNode(POP));

                        list.add(preInvokeLabel);
                        list.add(new InsnNode(ICONST_1));
                        list.add(new VarInsnNode(ISTORE, cacheFlag));

                        list.add(invokeLabel);

                        Type[] args = Type.getArgumentTypes(node.desc);

                        list.add(new LdcInsnNode(args.length));
                        list.add(new TypeInsnNode(ANEWARRAY, "java/lang/Object"));
                        list.add(new VarInsnNode(ASTORE, vStackArray));

                        for (int i = args.length - 1; i >= 0; i--) {
                            Type arg = args[i];
                            if (arg.getSort() < 9) {
                                list.add(boxType(arg));
                            }
                            list.add(new VarInsnNode(ALOAD, vStackArray));
                            list.add(new InsnNode(SWAP));
                            list.add(new LdcInsnNode(i));
                            list.add(new InsnNode(SWAP));
                            list.add(new InsnNode(AASTORE));
                        }

                        LabelNode saveLabel = new LabelNode();
                        LabelNode notSaveLabel = new LabelNode();

                        list.add(new VarInsnNode(ILOAD, cacheFlag));
                        list.add(new JumpInsnNode(IFEQ, saveLabel));
                        list.add(new JumpInsnNode(GOTO, notSaveLabel));
                        list.add(saveLabel);

                        list.add(new FieldInsnNode(GETSTATIC, classNode.name, methodHandlesCache.name, methodHandlesCache.desc));
                        list.add(BytecodeUtil.generateInteger(vBootstrapId));
                        list.add(new VarInsnNode(ALOAD, vCallSite));
                        list.add(new InsnNode(AASTORE));

                        list.add(notSaveLabel);
                        list.add(new VarInsnNode(ALOAD, vCallSite));

//                        list.add(new InsnNode(ICONST_0));
//                        list.add(new InsnNode(AALOAD));
//                        list.add(new TypeInsnNode(CHECKCAST, "java/lang/invoke/MethodHandle"));
//
//                        list.add(new InsnNode(DUP));
//                        list.add(new FieldInsnNode(GETSTATIC, "java/lang/System", "out", "Ljava/io/PrintStream;"));
//                        list.add(new InsnNode(SWAP));
//                        list.add(new MethodInsnNode(INVOKEVIRTUAL, "java/lang/invoke/MethodHandle", "toString", "()Ljava/lang/String;", false));
//                        list.add(new MethodInsnNode(INVOKEVIRTUAL, "java/io/PrintStream", "println", "(Ljava/lang/String;)V", false));
                        list.add(new VarInsnNode(ALOAD, vStackArray));
                        list.add(new MethodInsnNode(INVOKEVIRTUAL, "java/lang/invoke/MethodHandle", "invokeWithArguments", "([Ljava/lang/Object;)Ljava/lang/Object;", false));

                        Type returnType = Type.getReturnType(node.desc);

                        if (returnType != Type.VOID_TYPE) {
                            switch (returnType.getSort()) {
                                case Type.INT -> {
                                    list.add(new TypeInsnNode(CHECKCAST, "java/lang/Integer"));
                                    list.add(new MethodInsnNode(INVOKEVIRTUAL, "java/lang/Integer", "intValue", "()I", false));
                                }
                                case Type.LONG -> {
                                    list.add(new TypeInsnNode(CHECKCAST, "java/lang/Long"));
                                    list.add(new MethodInsnNode(INVOKEVIRTUAL, "java/lang/Long", "longValue", "()J", false));
                                }
                                case Type.FLOAT -> {
                                    list.add(new TypeInsnNode(CHECKCAST, "java/lang/Float"));
                                    list.add(new MethodInsnNode(INVOKEVIRTUAL, "java/lang/Float", "floatValue", "()F", false));
                                }
                                case Type.DOUBLE -> {
                                    list.add(new TypeInsnNode(CHECKCAST, "java/lang/Double"));
                                    list.add(new MethodInsnNode(INVOKEVIRTUAL, "java/lang/Double", "doubleValue", "()D", false));
                                }
                                case Type.BOOLEAN -> {
                                    list.add(new TypeInsnNode(CHECKCAST, "java/lang/Boolean"));
                                    list.add(new MethodInsnNode(INVOKEVIRTUAL, "java/lang/Boolean", "booleanValue", "()Z", false));
                                }
                                case Type.BYTE -> {
                                    list.add(new TypeInsnNode(CHECKCAST, "java/lang/Byte"));
                                    list.add(new MethodInsnNode(INVOKEVIRTUAL, "java/lang/Byte", "byteValue", "()B", false));
                                }
                                case Type.CHAR -> {
                                    list.add(new TypeInsnNode(CHECKCAST, "java/lang/Character"));
                                    list.add(new MethodInsnNode(INVOKEVIRTUAL, "java/lang/Character", "charValue", "()C", false));
                                }
                                case Type.SHORT -> {
                                    list.add(new TypeInsnNode(CHECKCAST, "java/lang/Short"));
                                    list.add(new MethodInsnNode(INVOKEVIRTUAL, "java/lang/Short", "shortValue", "()S", false));
                                }
                                case Type.ARRAY, Type.OBJECT -> {
                                    list.add(new TypeInsnNode(CHECKCAST, returnType.getInternalName()));
                                }
                            }
                        } else {
                            list.add(new InsnNode(POP));
                        }

                        method.instructions.insert(node, list);
                        method.instructions.remove(node);
                    }
                }
            }

            if (!boostrapIds.isEmpty()) {
                classNode.fields.add(methodHandlesCache);
                MethodNode clinit = classNode.getStaticInit();
                InsnList list = new InsnList();
                list.add(BytecodeUtil.generateInteger(boostrapIds.size()));
                list.add(new TypeInsnNode(ANEWARRAY, "java/lang/invoke/MethodHandle"));
                list.add(new FieldInsnNode(PUTSTATIC, classNode.name, methodHandlesCache.name, methodHandlesCache.desc));
                clinit.instructions.insert(list);
            }
        }
    }

    private AbstractInsnNode boxType(Type arg) {
        switch (arg.getSort()) {
            case Type.BOOLEAN -> {
                return new MethodInsnNode(INVOKESTATIC, "java/lang/Boolean", "valueOf", "(Z)Ljava/lang/Boolean;", false);
            }
            case Type.BYTE -> {
                return new MethodInsnNode(INVOKESTATIC, "java/lang/Byte", "valueOf", "(B)Ljava/lang/Byte;", false);
            }
            case Type.CHAR -> {
                return new MethodInsnNode(INVOKESTATIC, "java/lang/Character", "valueOf", "(C)Ljava/lang/Character;", false);
            }
            case Type.SHORT -> {
                return new MethodInsnNode(INVOKESTATIC, "java/lang/Short", "valueOf", "(S)Ljava/lang/Short;", false);
            }
            case Type.INT -> {
                return new MethodInsnNode(INVOKESTATIC, "java/lang/Integer", "valueOf", "(I)Ljava/lang/Integer;", false);
            }
            case Type.LONG -> {
                return new MethodInsnNode(INVOKESTATIC, "java/lang/Long", "valueOf", "(J)Ljava/lang/Long;", false);
            }
            case Type.FLOAT -> {
                return new MethodInsnNode(INVOKESTATIC, "java/lang/Float", "valueOf", "(F)Ljava/lang/Float;", false);
            }
            case Type.DOUBLE -> {
                return new MethodInsnNode(INVOKESTATIC, "java/lang/Double", "valueOf", "(D)Ljava/lang/Double;", false);
            }
            default -> {
                throw new IllegalStateException("Unexpected value: " + arg.getSort());
            }
        }
    }

    private InsnList createMethodType(ClassNode classNode, String desc) {
        InsnList list = new InsnList();
        list.add(new LdcInsnNode(desc));
        list.add(new LdcInsnNode(Type.getType("L" + classNode.name + ";")));
        list.add(new MethodInsnNode(INVOKEVIRTUAL, "java/lang/Class", "getClassLoader", "()Ljava/lang/ClassLoader;", false));
        list.add(new MethodInsnNode(INVOKESTATIC, "java/lang/invoke/MethodType", "fromMethodDescriptorString", "(Ljava/lang/String;Ljava/lang/ClassLoader;)Ljava/lang/invoke/MethodType;", false));
        return list;
    }

    private InsnList createHandle(ClassNode classNode, Handle handle) {
        InsnList list = new InsnList();
        list.add(new MethodInsnNode(INVOKESTATIC, "java/lang/invoke/MethodHandles", "lookup", "()Ljava/lang/invoke/MethodHandles$Lookup;", false));
        list.add(new LdcInsnNode(Type.getType("L" + handle.getOwner() + ";")));
        switch (handle.getTag()) {
            case H_INVOKESTATIC -> {
                list.add(new LdcInsnNode(handle.getName()));
                list.add(createMethodType(classNode, handle.getDesc()));
                list.add(new MethodInsnNode(INVOKEVIRTUAL, "java/lang/invoke/MethodHandles$Lookup", "findStatic", "(Ljava/lang/Class;Ljava/lang/String;Ljava/lang/invoke/MethodType;)Ljava/lang/invoke/MethodHandle;", false));
            }
            case H_INVOKESPECIAL -> {
                list.add(new LdcInsnNode(handle.getName()));
                list.add(createMethodType(classNode, handle.getDesc()));
                list.add(new LdcInsnNode(Type.getType("L" + classNode.name + ";")));
                list.add(new MethodInsnNode(INVOKEVIRTUAL, "java/lang/invoke/MethodHandles$Lookup", "findSpecial", "(Ljava/lang/Class;Ljava/lang/String;Ljava/lang/invoke/MethodType;Ljava/lang/Class;)Ljava/lang/invoke/MethodHandle;", false));
            }
            case H_NEWINVOKESPECIAL -> {
                list.add(createMethodType(classNode, handle.getDesc()));
                list.add(new MethodInsnNode(INVOKEVIRTUAL, "java/lang/invoke/MethodHandles$Lookup", "findConstructor", "(Ljava/lang/Class;Ljava/lang/invoke/MethodType;)Ljava/lang/invoke/MethodHandle;", false));
            }
            case H_INVOKEINTERFACE, H_INVOKEVIRTUAL -> {
                list.add(new LdcInsnNode(handle.getName()));
                list.add(createMethodType(classNode, handle.getDesc()));
                list.add(new MethodInsnNode(INVOKEVIRTUAL, "java/lang/invoke/MethodHandles$Lookup", "findVirtual", "(Ljava/lang/Class;Ljava/lang/String;Ljava/lang/invoke/MethodType;)Ljava/lang/invoke/MethodHandle;", false));
            }
            case H_GETFIELD -> {
                list.add(new LdcInsnNode(handle.getName()));
                list.add(new LdcInsnNode(Type.getType(handle.getDesc())));
                list.add(new MethodInsnNode(INVOKEVIRTUAL, "java/lang/invoke/MethodHandles$Lookup", "findGetter", "(Ljava/lang/Class;Ljava/lang/String;Ljava/lang/Class;)Ljava/lang/invoke/MethodHandle;", false));
            }
            case H_PUTFIELD -> {
                list.add(new LdcInsnNode(handle.getName()));
                list.add(new LdcInsnNode(Type.getType(handle.getDesc())));
                list.add(new MethodInsnNode(INVOKEVIRTUAL, "java/lang/invoke/MethodHandles$Lookup", "findSetter", "(Ljava/lang/Class;Ljava/lang/String;Ljava/lang/Class;)Ljava/lang/invoke/MethodHandle;", false));
            }
            case H_GETSTATIC -> {
                list.add(new LdcInsnNode(handle.getName()));
                list.add(new LdcInsnNode(Type.getType(handle.getDesc())));
                list.add(new MethodInsnNode(INVOKEVIRTUAL, "java/lang/invoke/MethodHandles$Lookup", "findStaticGetter", "(Ljava/lang/Class;Ljava/lang/String;Ljava/lang/Class;)Ljava/lang/invoke/MethodHandle;", false));
            }
            case H_PUTSTATIC -> {
                list.add(new LdcInsnNode(handle.getName()));
                list.add(new LdcInsnNode(Type.getType(handle.getDesc())));
                list.add(new MethodInsnNode(INVOKEVIRTUAL, "java/lang/invoke/MethodHandles$Lookup", "findStaticSetter", "(Ljava/lang/Class;Ljava/lang/String;Ljava/lang/Class;)Ljava/lang/invoke/MethodHandle;", false));
            }
            default -> {
                throw new IllegalStateException("Unexpected value: " + handle.getTag());
            }
        }
        return list;
    }
}