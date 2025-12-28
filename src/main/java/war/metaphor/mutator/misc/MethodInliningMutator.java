package war.metaphor.mutator.misc;

import org.objectweb.asm.ConstantDynamic;
import org.objectweb.asm.Handle;
import org.objectweb.asm.Type;
import org.objectweb.asm.tree.*;
import org.objectweb.asm.tree.analysis.BasicValue;
import org.objectweb.asm.tree.analysis.Frame;
import war.configuration.ConfigurationSection;
import war.jnt.annotate.Stability;
import war.jnt.dash.Level;
import war.jnt.dash.Logger;
import war.jnt.dash.Origin;
import war.metaphor.analysis.frames.LazyFrameProvider;
import war.metaphor.base.ObfuscatorContext;
import war.metaphor.mutator.Mutator;
import war.metaphor.tree.ClassMethod;
import war.metaphor.tree.Hierarchy;
import war.metaphor.tree.JClassNode;
import war.metaphor.util.asm.BytecodeUtil;

import java.lang.reflect.Modifier;
import java.util.*;

@Stability(war.jnt.annotate.Level.LOW)
public class MethodInliningMutator extends Mutator {

    private final boolean debug;
    private int iterate;
    private final List<String> membersExempt;

    public MethodInliningMutator(ObfuscatorContext base, ConfigurationSection config) {
        super(base, config);
        debug = config.getBoolean("debug", false);
        iterate = config.getInt("iterate", 0);
        membersExempt = config.getStringList("members-exempt");
    }

    @Override
    public void run(ObfuscatorContext base) {

        int counter = 0;

        for (JClassNode classNode : base.getClasses()) {
            if (classNode.isExempt()) continue;

            for (MethodNode method : classNode.methods) {
                if (classNode.isExempt(method))
                    continue;

                LazyFrameProvider frames = new LazyFrameProvider(classNode, method);

                for (AbstractInsnNode instruction : method.instructions) {
                    if (instruction instanceof MethodInsnNode node) {
                        ClassMethod target = BytecodeUtil.getMethodNode(node.owner, node.name, node.desc);
                        if (target == null)
                            continue;

                        JClassNode fromOwner = target.getClassNode();
                        MethodNode fromMethod = fromOwner.getMethod(node.name, node.desc);

                        ClassMethod member = ClassMethod.of(fromOwner, fromMethod);
                        if (membersExempt.contains(member.toString())) {
                            if (debug)
                                Logger.INSTANCE.log(Level.DEBUG, Origin.METAPHOR, String.format("Skipping inlining of %s.%s (%s) into %s.%s (%s): Member is exempt\n",
                                        fromOwner.name, fromMethod.name, fromOwner.getRealName(), classNode.name, method.name, classNode.getRealName()));
                            continue;
                        }

                        if (canInline(fromOwner, fromMethod, classNode, method, debug)) {
                            inline(fromMethod, method, instruction, frames);
                            Logger.INSTANCE.log(Level.DEBUG, Origin.METAPHOR, String.format("Inlining method %s.%s (%s) into %s.%s (%s)\n",
                                    fromOwner.name, fromMethod.name, fromOwner.getRealName(), classNode.name, method.name, classNode.getRealName()));
                            counter++;
                        }
                    }
                }
            }
        }

        if (counter > 0 && iterate-- > 0) {
            Logger.INSTANCE.log(Level.INFO, Origin.METAPHOR, String.format("Inlined %d methods, re-running inlining mutator\n", counter));
            run(base);
        } else if (counter > 0) {
            Logger.INSTANCE.log(Level.INFO, Origin.METAPHOR, String.format("Inlined %d methods\n", counter));
        } else {
            Logger.INSTANCE.log(Level.INFO, Origin.METAPHOR, "No methods inlined\n");
        }

    }

    private void inline(MethodNode from, MethodNode into, AbstractInsnNode point, LazyFrameProvider frames) {
        // Get frame at start
        Frame<BasicValue> frame = frames.get(point);

        // Clone everything
        InsnList instructions = new InsnList();
        List<TryCatchBlockNode> tryCatchBlocks = new ArrayList<>();
        Map<LabelNode, LabelNode> labels = new HashMap<>();
        for (AbstractInsnNode instruction : from.instructions)
            if (instruction instanceof LabelNode) labels.put((LabelNode) instruction, new LabelNode());

        if (from.tryCatchBlocks != null) // TODO: Find out why this happens.
            for (TryCatchBlockNode tryCatchBlock : from.tryCatchBlocks)
                tryCatchBlocks.add(new TryCatchBlockNode(labels.get(tryCatchBlock.start), labels.get(tryCatchBlock.end), labels.get(tryCatchBlock.handler), tryCatchBlock.type));

        for (AbstractInsnNode instruction : from.instructions)
            instructions.add(instruction.clone(labels));

        // Allocate space for return
        int returnLocal = -1;
        LabelNode inlineEnd = new LabelNode();
        Type returnType = Type.getReturnType(from.desc);
        if (returnType != Type.VOID_TYPE) {
            returnLocal = Math.max(from.maxLocals, into.maxLocals);
            into.maxLocals = returnLocal + returnType.getSize();
        }
        for (AbstractInsnNode instruction : instructions) {
            if (instruction.getOpcode() >= IRETURN && instruction.getOpcode() <= RETURN) {
                InsnList list = new InsnList();
                if (returnLocal != -1) list.add(new VarInsnNode(returnType.getOpcode(ISTORE), returnLocal));
                list.add(new JumpInsnNode(GOTO, inlineEnd));
                instructions.insertBefore(instruction, list);
                instructions.remove(instruction);
            }
        }

        // Fix Stack
        InsnList restoreStack = new InsnList();
        InsnList saveStack = new InsnList();

        Type[] args = Type.getArgumentTypes(from.desc);
        int parameters = args.length;
        if (!Modifier.isStatic(from.access)) parameters++;

        if (frame != null && frame.getStackSize() > 0) {
            for (int i = 0; i < frame.getStackSize() - parameters; i++) {
                Type type = frame.getStack(i).getType();
                saveStack.insert(new VarInsnNode(type.getOpcode(ISTORE), into.maxLocals));
                restoreStack.add(new VarInsnNode(type.getOpcode(ILOAD), into.maxLocals));
                into.maxLocals += type.getSize();
            }
        }

        // Fix variables
        for (AbstractInsnNode instruction : instructions) {
            if (instruction instanceof VarInsnNode node) {
                if (node.var != returnLocal) {
                    node.var += into.maxLocals;
                }
            } else if (instruction instanceof IincInsnNode node) {
                if (node.var != returnLocal) {
                    node.var += into.maxLocals;
                }
            }
        }

        // Fix Parameters
        InsnList parametersList = new InsnList();
        if (!Modifier.isStatic(from.access)) parametersList.add(new VarInsnNode(ASTORE, into.maxLocals++));
        for (Type arg : args) {
            int opcode = arg.getOpcode(ISTORE);
            parametersList.insert(new VarInsnNode(opcode, into.maxLocals));
            into.maxLocals += arg.getSize();
        }

        instructions.insert(saveStack);
        instructions.insert(parametersList);
        instructions.add(inlineEnd);
        instructions.add(restoreStack);
        if (returnLocal != -1) instructions.add(new VarInsnNode(returnType.getOpcode(ILOAD), returnLocal));

        if (BytecodeUtil.hasSpace(into, instructions)) {
            into.tryCatchBlocks.addAll(tryCatchBlocks);
            into.instructions.insert(point, instructions);
            into.instructions.remove(point);
        }

        BytecodeUtil.computeMaxLocals(into);
    }

    public boolean canInline(JClassNode from, MethodNode inlining, JClassNode into, MethodNode receiver, boolean debug) {
        int size = BytecodeUtil.leeway(receiver);
        if (BytecodeUtil.leeway(inlining) - 30000 > size) {
            if (debug) Logger.INSTANCE.log(Level.WARNING, Origin.METAPHOR, String.format("Cannot inline %s.%s into %s.%s: No room\n",
                    from.name, inlining.name, into.name, receiver.name));
            return false;
        }

        ClassMethod member = ClassMethod.of(from, inlining);

        if ("java/lang/Object".equals(from.name)) return false;
        if ("<init>".equals(inlining.name)) return false;
        if (inlining.instructions == null || inlining.instructions.size() == 0) return false;
        if (Modifier.isAbstract(inlining.access) || Modifier.isNative(inlining.access)) return false;

        if (hasHierarchy(member)) {
            if (debug) Logger.INSTANCE.log(Level.WARNING, Origin.METAPHOR, String.format("Cannot inline %s.%s into %s.%s: Method has hierarchy\n",
                    from.name, inlining.name, into.name, receiver.name));
            return false;
        }

        if (from.name.equals(into.name) && inlining.name.equals(receiver.name) && inlining.desc.equals(receiver.desc)) {
            if (debug) Logger.INSTANCE.log(Level.WARNING, Origin.METAPHOR, String.format("Cannot inline %s.%s into %s.%s: Method is the same\n",
                    from.name, inlining.name, into.name, receiver.name));
            return false;
        }

        if (config.getBoolean("skip-transpiled-checks") && base.isBeingTranspiled(into, receiver)) return true;

        if (!canAccess(from, into)) {
            if (debug) Logger.INSTANCE.log(Level.WARNING, Origin.METAPHOR, String.format("Cannot inline %s.%s into %s.%s: Access denied\n",
                    from.name, inlining.name, into.name, receiver.name));
            return false;
        }

        for (AbstractInsnNode insn : inlining.instructions) {
            switch (insn.getType()) {
                case AbstractInsnNode.METHOD_INSN: {
                    MethodInsnNode m = (MethodInsnNode) insn;

                    if (m.getOpcode() == INVOKESPECIAL && !"<init>".equals(m.name)) {
                        if (debug) Logger.INSTANCE.log(Level.WARNING, Origin.METAPHOR, String.format("Cannot inline %s.%s into %s.%s: INVOKESPECIAL on non-constructor\n",
                                from.name, inlining.name, into.name, receiver.name));
                        return false;
                    }
                    JClassNode owner = base.loadClass(m.owner);
                    if (owner == null || !canAccess(owner, into)) {
                        if (debug) Logger.INSTANCE.log(Level.WARNING, Origin.METAPHOR, String.format("Cannot inline %s.%s into %s.%s: Access denied to owner %s\n",
                                from.name, inlining.name, into.name, receiver.name, m.owner));
                        return false;
                    }

                    MethodNode target = owner.getMethod(m.name, m.desc);
                    if (target == null || !checkAccess(target.access, owner, into)) {
                        if (debug) Logger.INSTANCE.log(Level.WARNING, Origin.METAPHOR, String.format("Cannot inline %s.%s into %s.%s: Method %s.%s not found or access denied\n",
                                from.name, inlining.name, into.name, receiver.name, m.owner, m.name));
                        return false;
                    }

                    if (from.name.equals(owner.name)
                            && inlining.name.equals(m.name)
                            && inlining.desc.equals(m.desc)) {
                        if (debug) Logger.INSTANCE.log(Level.WARNING, Origin.METAPHOR, String.format("Cannot inline %s.%s into %s.%s: Method is the same\n",
                                from.name, inlining.name, into.name, receiver.name));
                    }
                    break;
                }
                case AbstractInsnNode.INVOKE_DYNAMIC_INSN: {

                    if (into.version < V1_7) {
                        if (debug) Logger.INSTANCE.log(Level.WARNING, Origin.METAPHOR, String.format("Cannot inline %s.%s into %s.%s: INVOKEDYNAMIC not supported in pre-Java 7\n",
                                from.name, inlining.name, into.name, receiver.name));
                        return false;
                    }

                    InvokeDynamicInsnNode indy = (InvokeDynamicInsnNode) insn;
                    Handle bsm = indy.bsm;
                    JClassNode owner = base.loadClass(bsm.getOwner());
                    if (owner == null || !canAccess(owner, into)) {
                        if (debug) Logger.INSTANCE.log(Level.WARNING, Origin.METAPHOR, String.format("Cannot inline %s.%s into %s.%s: Access denied to bootstrap owner %s\n",
                                from.name, inlining.name, into.name, receiver.name, bsm.getOwner()));
                        return false;
                    }
                    MethodNode bootstrap = owner.getMethod(bsm.getName(), bsm.getDesc());
                    if (bootstrap == null || !checkAccess(bootstrap.access, owner, into)) {
                        if (debug) Logger.INSTANCE.log(Level.WARNING, Origin.METAPHOR, String.format("Cannot inline %s.%s into %s.%s: Bootstrap method %s.%s not found or access denied\n",
                                from.name, inlining.name, into.name, receiver.name, bsm.getOwner(), bsm.getName()));
                        return false;
                    }

                    for (Object arg : indy.bsmArgs) {
                        if (arg instanceof Handle h) {
                            JClassNode argOwner = base.loadClass(h.getOwner());
                            if (argOwner == null || !canAccess(argOwner, into)) {
                                if (debug) Logger.INSTANCE.log(Level.WARNING, Origin.METAPHOR, String.format("Cannot inline %s.%s into %s.%s: Access denied to argument owner %s\n",
                                        from.name, inlining.name, into.name, receiver.name, h.getOwner()));
                                return false;
                            }
                            MethodNode argTarget = argOwner.getMethod(h.getName(), h.getDesc());
                            if (argTarget == null || !checkAccess(argTarget.access, argOwner, into)) {
                                if (debug) Logger.INSTANCE.log(Level.WARNING, Origin.METAPHOR, String.format("Cannot inline %s.%s into %s.%s: Argument method %s.%s not found or access denied\n",
                                        from.name, inlining.name, into.name, receiver.name, h.getOwner(), h.getName()));
                                return false;
                            }
                        }
                    }
                    break;
                }
                case AbstractInsnNode.FIELD_INSN: {
                    FieldInsnNode f = (FieldInsnNode) insn;
                    JClassNode owner = base.loadClass(f.owner);
                    if (owner == null || !canAccess(owner, into)) {
                        if (debug) Logger.INSTANCE.log(Level.WARNING, Origin.METAPHOR, String.format("Cannot inline %s.%s into %s.%s: Access denied to field owner %s\n",
                                from.name, inlining.name, into.name, receiver.name, f.owner));
                        return false;
                    }
                    FieldNode field = owner.getField(f.name, f.desc);
                    if (field == null || !checkAccess(field.access, owner, into)) {
                        if (debug) Logger.INSTANCE.log(Level.WARNING, Origin.METAPHOR, String.format("Cannot inline %s.%s into %s.%s: Field %s.%s not found or access denied\n",
                                from.name, inlining.name, into.name, receiver.name, f.owner, f.name));
                        return false;
                    }
                    if ((f.getOpcode() & 1) == 1 && Modifier.isFinal(field.access)) {
                        if (debug) Logger.INSTANCE.log(Level.WARNING, Origin.METAPHOR, String.format("Cannot inline %s.%s into %s.%s: Cannot access final field %s.%s\n",
                                from.name, inlining.name, into.name, receiver.name, f.owner, f.name));
                        return false;
                    }
                    break;
                }
                case AbstractInsnNode.TYPE_INSN: {
                    TypeInsnNode t = (TypeInsnNode) insn;
                    JClassNode owner = base.loadClass(t.desc);
                    if (owner == null || !canAccess(owner, into)) {
                        if (debug) Logger.INSTANCE.log(Level.WARNING, Origin.METAPHOR, String.format("Cannot inline %s.%s into %s.%s: Access denied to type %s\n",
                                from.name, inlining.name, into.name, receiver.name, t.desc));
                        return false;
                    }
                    break;
                }
                case AbstractInsnNode.LDC_INSN: {
                    LdcInsnNode ldc = (LdcInsnNode) insn;
                    if (ldc.cst instanceof Type ty) {
                        JClassNode owner = base.loadClass(ty.getInternalName());
                        if (owner == null || !canAccess(owner, into)) {
                            if (debug) Logger.INSTANCE.log(Level.WARNING, Origin.METAPHOR, String.format("Cannot inline %s.%s into %s.%s: Access denied to type %s\n",
                                    from.name, inlining.name, into.name, receiver.name, ty.getInternalName()));
                            return false;
                        }
                    } else if (ldc.cst instanceof Handle h) {
                        JClassNode owner = base.loadClass(h.getOwner());
                        if (owner == null || !canAccess(owner, into)) {
                            if (debug) Logger.INSTANCE.log(Level.WARNING, Origin.METAPHOR, String.format("Cannot inline %s.%s into %s.%s: Access denied to handle owner %s\n",
                                    from.name, inlining.name, into.name, receiver.name, h.getOwner()));
                            return false;
                        }
                        MethodNode target = owner.getMethod(h.getName(), h.getDesc());
                        if (target == null || !checkAccess(target.access, owner, into)) {
                            if (debug) Logger.INSTANCE.log(Level.WARNING, Origin.METAPHOR, String.format("Cannot inline %s.%s into %s.%s: Handle method %s.%s not found or access denied\n",
                                    from.name, inlining.name, into.name, receiver.name, h.getOwner(), h.getName()));
                            return false;
                        }
                    } else if (ldc.cst instanceof ConstantDynamic cd) {
                        if (into.version < V11) {
                            if (debug) Logger.INSTANCE.log(Level.WARNING, Origin.METAPHOR, String.format("Cannot inline %s.%s into %s.%s: ConstantDynamic not supported in pre-Java 11\n",
                                    from.name, inlining.name, into.name, receiver.name));
                            return false;
                        }
                        Handle bsm = cd.bootstrapMethod;
                        JClassNode owner = base.loadClass(bsm.getOwner());
                        if (owner == null || !canAccess(owner, into)) {
                            if (debug) Logger.INSTANCE.log(Level.WARNING, Origin.METAPHOR, String.format("Cannot inline %s.%s into %s.%s: Access denied to bootstrap owner %s\n",
                                    from.name, inlining.name, into.name, receiver.name, bsm.getOwner()));
                            return false;
                        }
                        MethodNode bootstrap = owner.getMethod(bsm.getName(), bsm.getDesc());
                        if (bootstrap == null || !checkAccess(bootstrap.access, owner, into)) {
                            if (debug) Logger.INSTANCE.log(Level.WARNING, Origin.METAPHOR, String.format("Cannot inline %s.%s into %s.%s: Bootstrap method %s.%s not found or access denied\n",
                                    from.name, inlining.name, into.name, receiver.name, bsm.getOwner(), bsm.getName()));
                            return false;
                        }
                    }
                    break;
                }
                case AbstractInsnNode.MULTIANEWARRAY_INSN: {
                    MultiANewArrayInsnNode m = (MultiANewArrayInsnNode) insn;
                    JClassNode owner = base.loadClass(m.desc);
                    if (owner == null || !canAccess(owner, into)) {
                        if (debug) Logger.INSTANCE.log(Level.WARNING, Origin.METAPHOR, String.format("Cannot inline %s.%s into %s.%s: Access denied to multi-array type %s\n",
                                from.name, inlining.name, into.name, receiver.name, m.desc));
                        return false;
                    }
                    break;
                }
            }
        }
        return true;
    }

    public boolean canAccess(JClassNode c1, JClassNode c2) {

        if (c1.name.equals(c2.name))
            return true;

        if (Modifier.isPrivate(c1.access))
            return false;

        String pkg1 = c1.getPackage();
        String pkg2 = c2.getPackage();

        if (!Modifier.isPublic(c1.access))
            return pkg1.equals(pkg2);

        return true;
    }

    public boolean checkAccess(int access, JClassNode classNode, JClassNode into) {
        if (classNode.name.startsWith("jdk/internal/")) return false;

        if (Modifier.isPublic(access)) return true;

        if (Modifier.isPrivate(access)) {
            return classNode.name.equals(into.name);
        }

        if (Modifier.isProtected(access)) {
            if (classNode.name.equals(into.name)) return true;
            return classNode.getPackage().equals(into.getPackage());
        }

        return false;
    }

    public boolean hasHierarchy(ClassMethod member) {
        Set<ClassMethod> hierarchy = Hierarchy.INSTANCE.getMethodHierarchy(member);
        if (hierarchy == null) return false;
        if (hierarchy.contains(member)) return hierarchy.size() > 1;
        else return !hierarchy.isEmpty();
    }
}
