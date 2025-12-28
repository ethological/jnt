package war.metaphor.mutator.misc;

import org.objectweb.asm.Opcodes;
import org.objectweb.asm.Type;
import org.objectweb.asm.tree.*;
import org.objectweb.asm.tree.analysis.AnalyzerException;
import org.objectweb.asm.tree.analysis.BasicValue;
import org.objectweb.asm.tree.analysis.Frame;
import war.configuration.ConfigurationSection;
import war.jnt.annotate.Stability;
import war.jnt.dash.Level;
import war.jnt.dash.Logger;
import war.jnt.dash.Origin;
import war.jnt.fusebox.impl.Internal;
import war.metaphor.analysis.SimpleInterpreter;
import war.metaphor.analysis.graph.Block;
import war.metaphor.analysis.graph.ControlFlowGraph;
import war.metaphor.analysis.values.NewInstanceValue;
import war.metaphor.base.ObfuscatorContext;
import war.metaphor.mutator.Mutator;
import war.metaphor.tree.JClassNode;
import war.metaphor.util.Descriptor;
import war.metaphor.util.asm.BytecodeUtil;

import java.util.*;

@Stability(war.jnt.annotate.Level.MEDIUM)
public class LiftInitializersMutator extends Mutator {
    public static List<String> crashReasons = new ArrayList<>();

    public LiftInitializersMutator(ObfuscatorContext context, ConfigurationSection config) {
        super(context, config);
    }

    @Override
    public void run(ObfuscatorContext context) {
        if (!crashReasons.isEmpty()) {
            crashReasons.add("Profound Mental Retardation");

            String reason = Arrays.toString(crashReasons.toArray(String[]::new));
            reason = reason.substring(1, reason.length() - 1);

            Logger.INSTANCE.logln(Level.FATAL, Origin.METAPHOR, "Cannot lift initializers after executing the following: " + reason);

            Internal.panic(1);
            return;
        }

        for (JClassNode node : context.getClasses()) {
            if (node.isExempt() || node.isInterface()) {
                continue;
            }

            MethodNode si = node.getStaticInit();
            if (!node.isExempt(si)) {
                liftInitializer(node);
            }

            for (MethodNode method : new ArrayList<>(node.methods)) {
                if (method.name.equals("<init>")) {
                    if (node.isExempt(method)) {
                        continue;
                    }
                    liftConstructor(node, method);
                }
            }
        }
    }

    private void liftConstructor(JClassNode node, MethodNode method) {

        JumpInsnNode jumpingJack = null;

        Map<AbstractInsnNode, Frame<BasicValue>> frames = BytecodeUtil.analyzeAndComputeMaxes(node, method);
        for (AbstractInsnNode instruction : method.instructions) {
            if (instruction instanceof MethodInsnNode insn) {
                if (insn.name.equals("<init>")) {
                    if (insn.owner.equals(node.superName)) {

                        Frame<BasicValue> frame = frames.get(instruction);
                        if (frame == null) continue;

                        BasicValue v = frame.getLocal(0);
                        if (v instanceof NewInstanceValue) {
                            Frame<BasicValue> next = new Frame<>(frame);
                            try {
                                next.execute(insn, new SimpleInterpreter(method));
                                if (!(next.getLocal(0) instanceof NewInstanceValue)) {
                                    LabelNode label = new LabelNode();
                                    InsnList insnList = new InsnList();
                                    insnList.add(new InsnNode(NOP));
                                    jumpingJack = new JumpInsnNode(GOTO, label);
                                    insnList.add(jumpingJack);
                                    insnList.add(label);
                                    method.instructions.insert(instruction, insnList);
                                }
                            } catch (AnalyzerException e) {
                                throw new RuntimeException(e);
                            }
                        }
                    }
                }
            }
        }

        ControlFlowGraph graph = new ControlFlowGraph(node, method);
        graph.compute();

        Block initBlock = null;

        for (Block block : graph.getBlocks()) {
            for (AbstractInsnNode instruction : block.getInstructions()) {
                if (instruction.getOpcode() == NOP) {
                    initBlock = block;
                    break;
                }
            }
        }

        if (initBlock == null) {
            Logger.INSTANCE.log(Level.WARNING, Origin.METAPHOR, "Could not find init block for " + node.name + "." + method.name + "\n");
            return;
        }

        if (initBlock.getAllVertices().size() > 1) return;

        Block nextBlock = initBlock.getVertices().iterator().next();

        if (nextBlock.getAllAccessors().size() > 1) return;

        if (nextBlock.getStartFrame().getStackSize() > 0) return;

        List<AbstractInsnNode> outlined = new ArrayList<>();

        Map<LabelNode, LabelNode> labelMap = new HashMap<>();
        for (AbstractInsnNode instruction : method.instructions) {
            if (instruction instanceof LabelNode label) {
                labelMap.put(label, new LabelNode());
            }
        }

        for (Block block : graph.getBlocks()) {
            if (isOnPath(block, initBlock)) continue;
            if (block.getLabel().index < nextBlock.getLabel().index) return;
            outlined.add(block.getLabel());
            for (AbstractInsnNode instruction : block.getInstructions()) {
                if (instruction.index < nextBlock.getLabel().index) return;
                outlined.add(instruction);
            }
        }

        List<TryCatchBlockNode> copy = new ArrayList<>();

        for (TryCatchBlockNode tryCatchBlock : method.tryCatchBlocks) {
            if (outlined.contains(tryCatchBlock.start) ||
                outlined.contains(tryCatchBlock.end) ||
                outlined.contains(tryCatchBlock.handler)) {
                // must contain all 3 to be valid
                if (!outlined.contains(tryCatchBlock.start) ||
                    !outlined.contains(tryCatchBlock.end) ||
                    !outlined.contains(tryCatchBlock.handler)) {
                    return;
                }
                copy.add(tryCatchBlock);
            }
        }

        Descriptor desc = new Descriptor("()V");
        Map<Integer, Type> locals = new HashMap<>();
        locals.put(0, Type.getObjectType(node.name));

        Frame<BasicValue> frame = nextBlock.getStartFrame();
        for (int i = 0; i < frame.getLocals(); i++) {
            BasicValue value = frame.getLocal(i);
            if (value == null || value.getType() == null) continue;
            Type type = value.getType();
            if (!locals.containsKey(i)) {
                locals.put(i, type);
            }
        }

        locals.keySet().stream().sorted().forEach(
                key -> desc.add(locals.get(key))
        );

        desc.remove(0);

        MethodNode outlinedMethod = new MethodNode(
                Opcodes.ACC_PUBLIC,
                "__jnt__init__" + Math.abs(RANDOM.nextLong()) + "__",
                desc.toString(),
                null,
                null
        );

        for (TryCatchBlockNode tryCatchBlockNode : copy) {
            outlinedMethod.tryCatchBlocks.add(new TryCatchBlockNode(
                    labelMap.get(tryCatchBlockNode.start),
                    labelMap.get(tryCatchBlockNode.end),
                    labelMap.get(tryCatchBlockNode.handler),
                    tryCatchBlockNode.type
            ));
            method.tryCatchBlocks.remove(tryCatchBlockNode);
        }

        AbstractInsnNode point = nextBlock.getLabel().previousInsn;

        for (AbstractInsnNode instruction : outlined) {
            outlinedMethod.instructions.add(instruction.clone(labelMap));
            method.instructions.remove(instruction);
        }

        BytecodeUtil.computeMaxLocals(outlinedMethod);
        node.methods.add(outlinedMethod);

        InsnList list = new InsnList();
        locals.forEach((index, type) ->
                list.add(new VarInsnNode(type.getOpcode(Opcodes.ILOAD), index))
        );
        list.add(new MethodInsnNode(INVOKEVIRTUAL, node.name, outlinedMethod.name, outlinedMethod.desc, false));
        list.add(new InsnNode(RETURN));

        method.instructions.insert(point, list);
        method.instructions.remove(jumpingJack);

        for (AbstractInsnNode instruction : outlinedMethod.instructions) {
            if (instruction instanceof FieldInsnNode insn) {
                JClassNode owner = base.loadClass(insn.owner);
                if (owner == null || owner.isExempt()) continue;
                FieldNode field = owner.getField(insn.name, insn.desc);
                if (field == null || owner.isExempt(field)) continue;
                if ((field.access & Opcodes.ACC_FINAL) != 0) {
                    field.access &= ~Opcodes.ACC_FINAL;
                }
            }
        }
    }

    public static boolean isOnPath(Block from, Block to) {
        if (from == to) {
            return true; // A block is trivially on its own path
        }

        Set<Block> visited = new HashSet<>();
        Deque<Block> stack = new ArrayDeque<>();
        stack.push(from);

        while (!stack.isEmpty()) {
            Block current = stack.pop();
            if (!visited.add(current)) {
                continue; // already seen
            }

            for (Block succ : current.getVertices()) {
                if (succ == to) {
                    return true; // Found the target
                }
                stack.push(succ);
            }
        }

        return false;
    }

    private void liftInitializer(JClassNode classNode) {
        MethodNode outlinedMethod = classNode.getStaticInit();
        outlinedMethod.name = "__jnt__" + Math.abs(RANDOM.nextLong()) + "__";

        for (AbstractInsnNode instruction : outlinedMethod.instructions) {
            if (instruction instanceof FieldInsnNode insn) {
                JClassNode owner = base.loadClass(insn.owner);
                if (owner == null || owner.isExempt()) continue;
                FieldNode field = owner.getField(insn.name, insn.desc);
                if (field == null || owner.isExempt(field)) continue;
                if ((field.access & Opcodes.ACC_FINAL) != 0) {
                    field.access &= ~Opcodes.ACC_FINAL;
                }
            }
        }

        MethodNode method = new MethodNode(
                Opcodes.ACC_PUBLIC | Opcodes.ACC_STATIC,
                "<clinit>",
                "()V",
                null,
                null
        );

        method.instructions.add(
                new MethodInsnNode(
                        Opcodes.INVOKESTATIC,
                        classNode.name,
                        outlinedMethod.name,
                        outlinedMethod.desc,
                        false
                )
        );

        method.instructions.add(new InsnNode(Opcodes.RETURN));

        classNode.methods.add(method);

        classNode.setLiftedInitializer(outlinedMethod.name);
    }
}
