package war.metaphor.mutator.data.integer;

import lombok.val;
import org.objectweb.asm.Type;
import org.objectweb.asm.tree.*;
import war.configuration.ConfigurationSection;
import war.jnt.annotate.Level;
import war.jnt.annotate.Stability;
import war.jnt.annotate.Warning;
import war.jnt.utility.mapping.Mapping;
import war.jnt.utility.mapping.impl.MemberIdentity;
import war.metaphor.analysis.callgraph.CallGraph;
import war.metaphor.base.ObfuscatorContext;
import war.metaphor.mutator.Mutator;
import war.metaphor.tree.ClassMethod;
import war.metaphor.tree.Hierarchy;
import war.metaphor.tree.JClassNode;
import war.metaphor.util.Descriptor;
import war.metaphor.util.Dictionary;
import war.metaphor.util.Purpose;
import war.metaphor.util.asm.BytecodeUtil;

import java.lang.reflect.Modifier;
import java.util.Base64;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

/*
 TODO:  Need to redo the entire renaming/mapping system, this is a mess
 TODO:  Basically, instead use ASM and create custom remapper that ALSO handles descriptors since that's why I did it like this
 */
@Stability(Level.HIGH)
@Warning("Number salting can break reflection.")
public class SaltingIntegerMutator extends Mutator {

    public Level level;
    public boolean skipBsm;

    public enum Level {
        LOW, MEDIUM, HIGH
    }

    public SaltingIntegerMutator(ObfuscatorContext base, ConfigurationSection config) {
        super(base, config);
        try {
            this.level = Level.valueOf(config.getString("level", "LOW").toUpperCase());
        } catch (IllegalArgumentException e) {
            this.level = Level.LOW;
        }
        this.skipBsm = config.getBoolean("skipBsm", true);
    }

    @Override
    public void run(ObfuscatorContext base) {

        CallGraph callGraph = new CallGraph();
        callGraph.buildGraph();

        val xrefs = callGraph.getXrefs();

        var classes = base.getClasses();

        Map<ClassMethod, String> mapping = new HashMap<>();
        Map<ClassMethod, String> nameMapping = new HashMap<>();
        Map<ClassMethod, Number> seeds = new HashMap<>();
        Map<ClassMethod, Type> types = new HashMap<>();
        Map<ClassMethod, Type> additional = new HashMap<>();

        for (JClassNode classNode : classes) {
            if (classNode.isExempt()) continue;
            for (MethodNode method : classNode.methods) {
                if (Modifier.isAbstract(method.access)) continue;
                if (classNode.isExempt(method))
                    continue;

                if (method.name.equals("main") && method.desc.equals("([Ljava/lang/String;)V"))
                    continue;
                if (method.name.equals("<clinit>"))
                    continue;
                if (method.name.equals("<init>") && level != Level.LOW) continue;

                // Skip bootstrap methods - they have fixed signatures that can't be changed
                // Bootstrap method signature: (Ljava/lang/invoke/MethodHandles$Lookup;Ljava/lang/String;Ljava/lang/invoke/MethodType;[Ljava/lang/Object;)Ljava/lang/Object;
                if (skipBsm) {
                    // Check if method is marked as a bootstrap method by ref mutator
                    if (method.signature != null && method.signature.equals("bsm::jnt:excluded")) {
                        continue;
                    }
                    
                    String bootstrapSignature = "(Ljava/lang/invoke/MethodHandles$Lookup;Ljava/lang/String;Ljava/lang/invoke/MethodType;[Ljava/lang/Object;)Ljava/lang/Object;";
                    if (method.desc.equals(bootstrapSignature)) {
                        continue;
                    }

                    // Skip methods that are referenced as bootstrap methods in invokedynamic instructions
                    // This prevents breaking the ref mutator which uses invokedynamic
                    boolean isBootstrapMethod = false;
                    for (JClassNode checkClass : classes) {
                        if (checkClass.isExempt()) continue;
                        for (MethodNode checkMethod : checkClass.methods) {
                            for (AbstractInsnNode insn : checkMethod.instructions) {
                                if (insn instanceof InvokeDynamicInsnNode indy) {
                                    org.objectweb.asm.Handle bsm = indy.bsm;
                                    if (bsm.getOwner().equals(classNode.name) && 
                                        bsm.getName().equals(method.name) && 
                                        bsm.getDesc().equals(method.desc)) {
                                        isBootstrapMethod = true;
                                        break;
                                    }
                                    // Also check bsmArgs for Handle objects
                                    for (Object bsmArg : indy.bsmArgs) {
                                        if (bsmArg instanceof org.objectweb.asm.Handle handle) {
                                            if (handle.getOwner().equals(classNode.name) && 
                                                handle.getName().equals(method.name) && 
                                                handle.getDesc().equals(method.desc)) {
                                                isBootstrapMethod = true;
                                                break;
                                            }
                                        }
                                    }
                                    if (isBootstrapMethod) break;
                                }
                            }
                            if (isBootstrapMethod) break;
                        }
                        if (isBootstrapMethod) break;
                    }
                    if (isBootstrapMethod) continue;
                }

                ClassMethod self = ClassMethod.of(classNode, method);

//                if (mapping.containsKey(self))
//                    continue;

                Set<ClassMethod> hierarchy = Hierarchy.INSTANCE.getMethodHierarchy(self);

                if (!canRename(hierarchy)) continue;

                boolean canContinue = true;

                for (ClassMethod member : hierarchy) {
                    if (xrefs.containsKey(member)) {
                        Set<CallGraph.CallGraphNode> nodes = xrefs.get(member);
                        for (CallGraph.CallGraphNode node : nodes) {
                            if (!node.isEdit()) canContinue = false;
                            else {
                                ClassMethod caller = node.getMember();
                                JClassNode callerNode = caller.getClassNode();
                                MethodNode cMethod = caller.getMember();
                                if (callerNode.isExempt() || callerNode.isExempt(cMethod)) canContinue = false;
                            }
                        }
                    }
                }

                if (!canContinue) continue;

                Descriptor __desc;

                Type saltType, additionalType = null;
                Type[] saltTypes = {Type.LONG_TYPE, Type.INT_TYPE};

                if (level == Level.LOW) {
                    saltType = saltTypes[rand.nextInt(saltTypes.length)];
                    __desc = Descriptor.of(method.desc).add(saltType);
                } else if (level == Level.MEDIUM) {
                    saltType = Type.getType("[Ljava/lang/Object;");
                    __desc = Descriptor.of(method.desc).removeAll().add(saltType);
                } else {
                    additionalType = saltTypes[rand.nextInt(saltTypes.length)];
                    saltType = Type.getType("[Ljava/lang/Object;");
                    __desc = Descriptor.of(method.desc).removeAll().add(saltType);
                }

                String desc = __desc.toString();

                Number seed = rand.nextLong();

                String name = method.name.equals("<init>") ? method.name : Dictionary.gen(3, Purpose.METHOD);

                for (ClassMethod cm : hierarchy) {
                    if (mapping.containsKey(cm)) desc = mapping.get(cm);
                    if (nameMapping.containsKey(cm)) name = nameMapping.get(cm);
                    if (seeds.containsKey(cm)) seed = seeds.get(cm);
                    if (types.containsKey(cm)) saltType = types.get(cm);
                    if (additional.containsKey(cm)) additionalType = additional.get(cm);
                }

                if (canRename(hierarchy)) {
                    for (ClassMethod cm : hierarchy) {
                        mapping.put(cm, desc);
                        seeds.put(cm, seed);
                        types.put(cm, saltType);
                        nameMapping.put(cm, name);
                        if (additionalType != null)
                            additional.put(cm, additionalType);
                    }
                }

                if (method.signature == null || !method.signature.startsWith("pass::jnt")) {
                    method.signature = "pass::jnt:" + Base64.getEncoder().encodeToString(method.name.getBytes());
                }

                base.getRepository().add(new Mapping(
                        new MemberIdentity(".d_same " + classNode.name, method.name, method.desc),
                        new MemberIdentity(classNode.name, name, desc)
                ));
            }
        }

        mapping.forEach((cm, desc) -> {
            Type saltType = types.get(cm);
            Type additionalType = additional.get(cm);
            MethodNode method = cm.getMember();
            int sum = saltType.getSize();
            if (level == Level.LOW || level == Level.HIGH) {
                if (level == Level.HIGH) {
                    desc = Descriptor.of(method.desc).add(additionalType).toString();
                    saltType = additionalType;
                    sum = additionalType.getSize();
                }
                int var = Descriptor.of(desc).getLast(saltType, Modifier.isStatic(method.access));
                for (AbstractInsnNode instruction : method.instructions) {
                    if (instruction instanceof VarInsnNode) {
                        int varIndex = ((VarInsnNode) instruction).var;
                        if (varIndex >= var) {
                            ((VarInsnNode) instruction).var += sum;
                        }
                    } else if (instruction instanceof IincInsnNode) {
                        int varIndex = ((IincInsnNode) instruction).var;
                        if (varIndex >= var) {
                            ((IincInsnNode) instruction).var += sum;
                        }
                    }
                }
            }
        });

        callGraph.getXrefs().forEach((called, nodes) -> {
            if (mapping.containsKey(called)) {
                Type saltType = types.get(called);
                Type additionalType = additional.get(called);
                String name = nameMapping.get(called);
                String desc = mapping.get(called);
                nodes.forEach(node -> {
                    ClassMethod caller = node.getMember();
                    MethodNode cMethod = caller.getMember();
                    AbstractInsnNode insn = node.getInstruction();
                    MethodInsnNode methodInsn = (MethodInsnNode) insn;
                    String originalDesc = methodInsn.desc;
                    methodInsn.name = name;
                    methodInsn.desc = desc;
                    if (node.isEdit()) {
                        if (level == Level.LOW || level == Level.HIGH) {
                            Number seed = seeds.get(called);
                            InsnList list = BytecodeUtil.generateNumber(seed, level == Level.LOW ? saltType : additionalType);
                            cMethod.instructions.insertBefore(insn, list);
                        }
                        if (level == Level.MEDIUM || level == Level.HIGH) {
                            if (level == Level.HIGH)
                                originalDesc = Descriptor.of(originalDesc).add(additionalType).toString();
                            cMethod.instructions.insertBefore(insn, storeArray(originalDesc));
                        }
                    }
                });
            }
        });

        mapping.forEach((cm, desc) -> {
            MethodNode method = cm.getMember();

            if (cm.getClassNode().isExempt() || cm.getClassNode().isExempt(method)) return;

            Type saltType = types.get(cm);
            Type additionalType = additional.get(cm);
            Number seed = seeds.get(cm);
            String originalDesc = method.desc;

            method.name = nameMapping.get(cm);
            method.desc = desc;
            method.access &= ~ACC_VARARGS;

            method.parameters = null;
            method.invisibleParameterAnnotations = null;
            method.visibleParameterAnnotations = null;

            if (method.instructions.size() == 0) return;

            BytecodeUtil.computeMaxLocals(method);
            int var = -1;

            if (level == Level.LOW) {
                var = Descriptor.of(desc).getLast(saltType, Modifier.isStatic(method.access));
            } else if (level == Level.HIGH) {
                var = Descriptor.of(originalDesc).add(additionalType).getLast(additionalType, Modifier.isStatic(method.access));
            }

            if (level == Level.LOW || level == Level.HIGH) {
                for (AbstractInsnNode instruction : method.instructions) {
                    if (BytecodeUtil.isLong(instruction)) {
                        long value = BytecodeUtil.getLong(instruction);
                        InsnList list = BytecodeUtil.generateSeeded(var, value, seed, Type.LONG_TYPE, level == Level.LOW ? saltType : additionalType);
                        method.instructions.insert(instruction, list);
                        method.instructions.remove(instruction);
                    } else if (BytecodeUtil.isInteger(instruction)) {
                        long lvalue = BytecodeUtil.getInteger(instruction);
                        InsnList list = BytecodeUtil.generateSeeded(var, lvalue, seed, Type.INT_TYPE, level == Level.LOW ? saltType : additionalType);
                        method.instructions.insert(instruction, list);
                        method.instructions.remove(instruction);
                    }
                }
            }

            if (level == Level.MEDIUM || level == Level.HIGH) {
                if (level == Level.HIGH) originalDesc = Descriptor.of(originalDesc).add(additionalType).toString();
                Descriptor descriptor = Descriptor.of(originalDesc);
                if (Type.getArgumentTypes(originalDesc).length == 0) return;
                int buffer = method.maxLocals + 100;
                int point = descriptor.getLast(Modifier.isStatic(method.access));
                for (AbstractInsnNode instruction : method.instructions) {
                    if (instruction instanceof VarInsnNode) {
                        int varIndex = ((VarInsnNode) instruction).var;
                        if (varIndex == 0 && !Modifier.isStatic(method.access)) continue;
                        if (varIndex < point) {
                            ((VarInsnNode) instruction).var += buffer;
                        }
                    } else if (instruction instanceof IincInsnNode) {
                        int varIndex = ((IincInsnNode) instruction).var;
                        if (varIndex < point) {
                            ((IincInsnNode) instruction).var += buffer;
                        }
                    }
                }
                method.instructions.insert(loadArray(originalDesc, Modifier.isStatic(method.access), buffer));
            }
        });

        Hierarchy.INSTANCE.reset();
        Hierarchy.INSTANCE.ensureGraphBuilt();
    }

    private InsnList storeArray(String desc) {

        Type[] types = Type.getArgumentTypes(desc);
        InsnList list = new InsnList();

        list.add(BytecodeUtil.makeInteger(types.length));
        list.add(new TypeInsnNode(ANEWARRAY, "java/lang/Object"));

        for (int i = types.length - 1; i >= 0; i--) {
            Type type = types[i];
            list.add(new InsnNode(DUP));
            if (type.getSize() == 2) {
                list.add(new InsnNode(DUP2_X2));
                list.add(new InsnNode(POP2));
                list.add(BytecodeUtil.boxType(type));
                list.add(BytecodeUtil.makeInteger(i));
                list.add(new InsnNode(SWAP));
                list.add(new InsnNode(AASTORE));
            } else {
                list.add(new InsnNode(DUP2_X1));
                list.add(new InsnNode(POP2));
                list.add(BytecodeUtil.boxType(type));
                list.add(BytecodeUtil.makeInteger(i));
                list.add(new InsnNode(SWAP));
                list.add(new InsnNode(AASTORE));
            }
        }

        return list;
    }

    private InsnList loadArray(String desc, boolean isStatic, int maxLocals) {

        Descriptor descriptor = Descriptor.of(desc);

        Type[] types = Type.getArgumentTypes(desc);

        int startingVar = descriptor.getLast(isStatic);

        int arrayVar = isStatic ? 0 : 1;

        InsnList list = new InsnList();

        for (int i = types.length - 1; i >= 0; i--) {
            Type type = types[i];
            startingVar -= type.getSize();
            list.add(new VarInsnNode(ALOAD, arrayVar));
            list.add(BytecodeUtil.makeInteger(i));
            list.add(new InsnNode(AALOAD));
            list.add(BytecodeUtil.unboxType(types[i]));
            list.add(new VarInsnNode(type.getOpcode(ISTORE), startingVar + maxLocals));
        }

        return list;

    }

    private boolean canRename(Set<ClassMethod> methods) {
        for (ClassMethod cm : methods) {
            JClassNode node = cm.getClassNode();
            if (node.isAnnotation()) return false;
            MethodNode method = cm.getMember();
            if (node.isExempt(method)) return false;
            if (node.hasAnnotation("Ljava/lang/FunctionalInterface;")) return false;
            if (method.name.equals("<clinit>")) return false;
            if (node.isEnum() && method.name.equals("values") && method.desc.equals("()[L" + node.name + ";"))
                return false;
            if (node.isEnum() && method.name.equals("valueOf") && method.desc.equals("(Ljava/lang/String;)L" + node.name + ";"))
                return false;
            if (Modifier.isNative(method.access)) return false;
            if (node.isLibrary()) return false;
            if (node.isExempt()) return false;
        }
        return true;
    }
}
