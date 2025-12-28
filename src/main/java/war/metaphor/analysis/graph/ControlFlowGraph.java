package war.metaphor.analysis.graph;

import lombok.Getter;
import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.SymbolTable;
import org.objectweb.asm.tree.*;
import org.objectweb.asm.tree.analysis.BasicValue;
import org.objectweb.asm.tree.analysis.Frame;
import war.jnt.dash.Level;
import war.jnt.dash.Logger;
import war.jnt.dash.Origin;
import war.metaphor.analysis.frames.FrameComputation;
import war.metaphor.analysis.values.NewInstanceValue;
import war.metaphor.asm.JClassWriter;
import war.metaphor.tree.JClassNode;
import war.metaphor.util.UnionFind;
import war.metaphor.util.asm.BytecodeUtil;

import java.lang.reflect.Modifier;
import java.util.*;

@Getter
public class ControlFlowGraph implements Opcodes {

    private static final Map<Frame<BasicValue>, String> frameHashCache = new HashMap<>();

    private final JClassNode classNode;
    private final MethodNode method;

    private Map<AbstractInsnNode, Frame<BasicValue>> frames;

    private final List<Block> blocks = new ArrayList<>();

    private final Map<LabelNode, LabelNode> labels = new HashMap<>();
    private final Map<LabelNode, Block> labelToBlockMap = new HashMap<>();

    private LabelNode start;
    private Block startBlock;

    public ControlFlowGraph(JClassNode classNode, MethodNode method) {
        this.classNode = classNode;
        this.method = method;
    }

    public boolean compute() {

        if (Modifier.isNative(method.access)) return false;
        if (Modifier.isAbstract(method.access)) return false;
        if (method.instructions == null) return false;
        if (method.instructions.size() == 0) return false;

        if (!blocks.isEmpty())
            throw new IllegalStateException("Control flow graph already computed");

        if (method.instructions.size == 0) return false;

        method.instructions.toArray();

        filterEmptyBlocks(method);

        try {
            frames = BytecodeUtil.analyzeAndComputeMaxes(classNode, method);
        } catch (Exception ex) {
//            System.err.println("Failed to analyze method " + classNode.name + "." + method.name + method.desc + ": " + ex.getMessage());
            return false;
//            throw new RuntimeException(ex);
        }

        start = new LabelNode();
        method.instructions.insert(start);

        for (AbstractInsnNode instruction : method.instructions) {
            if (instruction instanceof LabelNode) {
                labels.put((LabelNode) instruction, (LabelNode) instruction);
            } else if (instruction instanceof LineNumberNode) {
                labels.put(((LineNumberNode) instruction).start, ((LineNumberNode) instruction).start);
            }
        }

        classNode.cacheSymbolTable();
        SymbolTable sym = new SymbolTable(null);
        ClassWriter cw = new JClassWriter(ClassWriter.COMPUTE_FRAMES, sym);
        sym.classWriter = cw;
        cw.visit(classNode.version, classNode.access, classNode.name, classNode.signature, classNode.superName, classNode.interfaces.toArray(new String[0]));
        try {
            method.accept(cw);
        } catch (Exception ex) {
            Logger.INSTANCE.log(Level.ERROR, Origin.METAPHOR, String.format("Failed to accept method %s.%s%s: %s\n", classNode.name, method.name, method.desc, ex.getMessage()));
//            System.err.println("Failed to accept method " + classNode.name + "." + method.name + method.desc + ": " + ex.getMessage());
            return false;
        } finally {
            classNode.resetSymbolTable();
        }

        // Identify blocks
        for (AbstractInsnNode insn : method.instructions) {
            if (insn instanceof LabelNode label) {

                List<AbstractInsnNode> instructions = BytecodeUtil.getBlock(method, label);

                List<TryCatchBlockNode> withinTraps = new ArrayList<>();
                List<TryCatchBlockNode> traps = new ArrayList<>();
                List<TryCatchBlockNode> trapHandlers = new ArrayList<>();

                for (TryCatchBlockNode trap : method.tryCatchBlocks) {
                    int startOffset = trap.start.index;
                    int endOffset = trap.end.index;
                    if (label.index >= startOffset && label.index <= endOffset)
                        withinTraps.add(trap);
                    if (label.index == startOffset || label.index == endOffset)
                        traps.add(trap);
                    if (label == trap.handler)
                        trapHandlers.add(trap);
                }

                Frame<BasicValue> start = frames.get(label.nextInsn);

                boolean carrying = isCarrying(label.nextInsn);

                Block block = new Block();
                block.setStartFrame(start);
                try {
                    block.setStartComp(new FrameComputation(sym, label));
                } catch (Exception ex) {
                    //
                }

                withinTraps.sort(Comparator.comparingInt(o -> o.start.index));

                block.setId(blocks.size());
                block.setInstructions(instructions);
                block.setTraps(traps);
                block.setWithinTraps(withinTraps);
                block.setLabel(label);
                block.setTrapHandlers(trapHandlers);
                block.setTrapHandler(!trapHandlers.isEmpty());
                block.setCarrying(carrying);
                blocks.add(block);
                labelToBlockMap.put(label, block);
            }
        }

        startBlock = labelToBlockMap.get(start);

        Block currentBlock = null;
        // Connect blocks
        for (AbstractInsnNode insn : method.instructions) {
            if (insn instanceof LabelNode label) {
                currentBlock = labelToBlockMap.get(label);
            }
            if (currentBlock == null) continue;
            switch (insn.getType()) {
                case AbstractInsnNode.JUMP_INSN:
                case AbstractInsnNode.LOOKUPSWITCH_INSN:
                case AbstractInsnNode.TABLESWITCH_INSN:
                    List<LabelNode> targets = getJumpTargets(insn);
                    for (LabelNode target : targets) {
                        Block targetBlock = labelToBlockMap.get(target);
                        if (targetBlock != null) {
                            currentBlock.addVertex(targetBlock, insn);
                            targetBlock.addAccessor(currentBlock, insn);
                        }
                    }
                    break;
            }
        }

        // Trap vertices
        for (Block block : blocks) {
            for (TryCatchBlockNode trap : block.getWithinTraps()) {
                Block handler = labelToBlockMap.get(trap.handler);
                if (handler != null) {
                    block.addTrapVertex(handler);
                    handler.addTrapAccessor(block);
                }
            }
        }

        currentBlock = null;
        // Set fall through blocks
        for (AbstractInsnNode insn : method.instructions) {
            if (insn instanceof LabelNode label) {
                Block nextBlock = labelToBlockMap.get(label);
                if (currentBlock != null) {
                    currentBlock.setFallThrough(nextBlock, insn);
                }
                currentBlock = nextBlock;
            }
            if (BytecodeUtil.isExitBlock(insn))
                currentBlock = null;
        }

        // Remove fall through blocks
        for (Block block : blocks) {
            Block next = block.getFallThrough();
            if (next == null) continue;
            JumpInsnNode node = new JumpInsnNode(GOTO, next.getLabel());
            method.instructions.insertBefore(next.getLabel(), node);
            block.addVertex(next, node);
            next.addAccessor(block, node);
        }

        updateBlocks();

        return true;
    }

    public void filterEmptyBlocks(MethodNode method) {
        Set<LabelNode> removal = new HashSet<>();
        do {
            removal.clear();
            for (AbstractInsnNode instruction : method.instructions) {
                if (instruction instanceof LabelNode) {
                    if (instruction.nextInsn instanceof LabelNode) {
                        removal.add((LabelNode) instruction);
                    }
                }
            }
            for (LabelNode label : removal) {
                for (TryCatchBlockNode trap : method.tryCatchBlocks) {
                    if (trap.start == label)
                        trap.start = (LabelNode) label.nextInsn;
                    if (trap.end == label)
                        trap.end = (LabelNode) label.nextInsn;
                    if (trap.handler == label)
                        trap.handler = (LabelNode) label.nextInsn;
                }
                for (AbstractInsnNode instruction : method.instructions) {
                    if (instruction instanceof JumpInsnNode jumpInsn) {
                        if (jumpInsn.label == label) {
                            jumpInsn.label = (LabelNode) label.nextInsn;
                        }
                    } else if (instruction instanceof LookupSwitchInsnNode lookupSwitchInsn) {
                        if (lookupSwitchInsn.dflt == label) {
                            lookupSwitchInsn.dflt = (LabelNode) label.nextInsn;
                        }
                        for (int i = 0; i < lookupSwitchInsn.labels.size(); i++) {
                            LabelNode node = lookupSwitchInsn.labels.get(i);
                            if (node == label) {
                                lookupSwitchInsn.labels.set(i, (LabelNode) label.nextInsn);
                            }
                        }
                    } else if (instruction instanceof TableSwitchInsnNode tableSwitchInsn) {
                        if (tableSwitchInsn.dflt == label) {
                            tableSwitchInsn.dflt = (LabelNode) label.nextInsn;
                        }
                        for (int i = 0; i < tableSwitchInsn.labels.size(); i++) {
                            LabelNode node = tableSwitchInsn.labels.get(i);
                            if (node == label) {
                                tableSwitchInsn.labels.set(i, (LabelNode) label.nextInsn);
                            }
                        }

                    }
                }
                if (method.localVariables != null) {
                    for (LocalVariableNode localVariable : method.localVariables) {
                        if (localVariable.start == label)
                            localVariable.start = (LabelNode) label.nextInsn;
                        if (localVariable.end == label)
                            localVariable.end = (LabelNode) label.nextInsn;
                    }
                }
            }
            removal.forEach(method.instructions::remove);
        } while (!removal.isEmpty());
    }

    private boolean isCarrying(AbstractInsnNode insn) {
        Frame<BasicValue> frame = frames.get(insn);
        if (frame == null) return false;
        int stackSize = frame.getStackSize();
        for (int i = 0; i < stackSize; i++) {
            BasicValue value = frame.getStack(i);
            if (value instanceof NewInstanceValue)
                return true;
        }
        return false;
    }

    public <T extends AbstractInsnNode> T clone(T insn) {
        return (T) insn.clone(labels);
    }

    public Block getBlock(LabelNode node) {
        for (Block block : blocks) {
            if (block.getLabel() == node) return block;
        }
        return null;
    }

    public List<LabelNode> getJumpTargets(AbstractInsnNode insn) {
        List<LabelNode> targets = new ArrayList<>();
        switch (insn.getType()) {
            case AbstractInsnNode.JUMP_INSN:
                JumpInsnNode jumpInsn = (JumpInsnNode) insn;
                targets.add(jumpInsn.label);
                break;
            case AbstractInsnNode.LOOKUPSWITCH_INSN:
                LookupSwitchInsnNode lookupSwitchInsn = (LookupSwitchInsnNode) insn;
                targets.add(lookupSwitchInsn.dflt);
                targets.addAll(lookupSwitchInsn.labels);
                break;
            case AbstractInsnNode.TABLESWITCH_INSN:
                TableSwitchInsnNode tableSwitchInsn = (TableSwitchInsnNode) insn;
                targets.add(tableSwitchInsn.dflt);
                targets.addAll(tableSwitchInsn.labels);
                break;
            default:
                break;
        }
        return targets;
    }

    public Block getContainingBlock(AbstractInsnNode insn) {
        for (Block block : blocks) {
            if (block.inRange(insn))
                return block;
        }
        return null;
    }

    public void updateBlocks() {
        method.instructions.toArray();
        for (Block block : blocks)
            block.setInstructions(BytecodeUtil.getBlock(method, block.getLabel()));
    }

    public List<List<AbstractInsnNode>> groupSameFrames(List<AbstractInsnNode> nodes) {

        UnionFind<AbstractInsnNode> uf = new UnionFind<>();

        for (AbstractInsnNode node : nodes)
            uf.add(node);

        for (int i = 0; i < nodes.size(); i++)
            for (int j = i + 1; j < nodes.size(); j++) {
                Frame<BasicValue> frame1 = frames.get(nodes.get(i));
                Frame<BasicValue> frame2 = frames.get(nodes.get(j));
                if (frame1 == null || frame2 == null) continue;
                if (frameHash(frame1).equals(frameHash(frame2)))
                    uf.union(nodes.get(i), nodes.get(j));
            }

        Map<AbstractInsnNode, List<AbstractInsnNode>> groupsMap = new HashMap<>();
        for (AbstractInsnNode node : nodes) {
            AbstractInsnNode root = uf.find(node);
            groupsMap.computeIfAbsent(root, k -> new ArrayList<>()).add(node);
        }

        return new ArrayList<>(groupsMap.values());
    }

    public List<List<Block>> groupSameBlocks() {

        UnionFind<Block> uf = new UnionFind<>();

        List<Block> nodes = new ArrayList<>(blocks);

        for (Block node : nodes)
            uf.add(node);

        for (int i = 0; i < nodes.size(); i++)
            for (int j = i + 1; j < nodes.size(); j++)
                if (nodes.get(i).isSame(nodes.get(j)))
                    uf.union(nodes.get(i), nodes.get(j));

        Map<Block, List<Block>> groupsMap = new HashMap<>();
        for (Block node : nodes) {
            Block root = uf.find(node);
            groupsMap.computeIfAbsent(root, _ -> new ArrayList<>()).add(node);
        }

        return new ArrayList<>(groupsMap.values());

    }

    private static String frameHash(Frame<BasicValue> f) {
        return frameHashCache.computeIfAbsent(f, Frame::toString);
    }

}
