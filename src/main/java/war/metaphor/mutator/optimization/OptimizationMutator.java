package war.metaphor.mutator.optimization;

import org.objectweb.asm.tree.*;
import war.configuration.ConfigurationSection;
import war.jnt.annotate.Level;
import war.jnt.annotate.Stability;
import war.metaphor.analysis.graph.Block;
import war.metaphor.analysis.graph.ControlFlowGraph;
import war.metaphor.base.ObfuscatorContext;
import war.metaphor.mutator.Mutator;
import war.metaphor.sim.Simulator;
import war.metaphor.sim.Value;
import war.metaphor.util.asm.BytecodeUtil;

import java.lang.reflect.Modifier;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

/**
 * Mostly ints for now
 * @author jan
 */
@SuppressWarnings("ALL")
@Stability(Level.VERY_LOW)
public class OptimizationMutator extends Mutator {

    private Map<String, Set<Block>> blockCache = new HashMap<>();

    public OptimizationMutator(ObfuscatorContext base, ConfigurationSection config) {
        super(base, config);
    }

    @Override
    public void run(ObfuscatorContext base) {
        base.getClasses().parallelStream().forEach(classNode -> {
            if (classNode.isExempt()) return;
            if (classNode.isInterface()) return;

            for (MethodNode method : classNode.methods) {
                if (Modifier.isAbstract(method.access)) continue;
                if (classNode.isExempt(method)) continue;
                ControlFlowGraph cfg = new ControlFlowGraph(classNode, method);
                final boolean computed = cfg.compute();

                boolean modified;

                do {
                    modified = false;

                    for (AbstractInsnNode instruction : method.instructions) {
                        if (isMath(instruction)) {
                            try {
                                Value value = new Simulator(method).simulateUntil(instruction.getNext()).getLast();

                                if (!value.getResolved()) continue;

                                method.instructions.insertBefore(instruction, BytecodeUtil.makeInteger((int) value.getValue()));
                                for (AbstractInsnNode creatorNode : value.getCreatorNodes()) {
                                    method.instructions.remove(creatorNode);
                                }

                                modified = true;
                                break;
                            } catch (Throwable _) {}
                        } else if (isRemovableStore(instruction, method.instructions)) {
                            try {
                                Simulator simulator = new Simulator(method);
                                simulator.simulateUntil(instruction.getNext()); // we dont need the output

                                Value value = simulator.getLocals()[((VarInsnNode)instruction).var];

                                if (!value.getResolved()) continue;

                                for (AbstractInsnNode creatorNode : value.getCreatorNodes()) {
                                    method.instructions.remove(creatorNode);
                                }
                                method.instructions.remove(instruction);

                                modified = true;
                                break;
                            } catch (Throwable _) {}
                        } else if (computed && shouldOptimizeLoad(instruction, cfg)) {
                            try {
                                Value value = new Simulator(method).simulateUntil(instruction.getNext()).getLast();

                                if (!value.getResolved()) continue;

                                method.instructions.insertBefore(instruction, BytecodeUtil.makeInteger((int) value.getValue()));
                                for (AbstractInsnNode creatorNode : value.getCreatorNodes()) {
                                    method.instructions.remove(creatorNode);
                                }

                                modified = true;
                                break;
                            } catch (Throwable _) {}
                        }
                        if (BytecodeUtil.isInteger(instruction)
                                && BytecodeUtil.getInteger(instruction) == 0
                                && isUselessWithZero(instruction.getNext())) {
                            method.instructions.remove(instruction.getNext());
                            method.instructions.remove(instruction);

                            modified = true;
                            break;
                        } else if (BytecodeUtil.isInteger(instruction)
                                && BytecodeUtil.getInteger(instruction) == 1
                                && instruction.getNext().getOpcode() == IMUL) {
                            method.instructions.remove(instruction.getNext());
                            method.instructions.remove(instruction);

                            modified = true;
                            break;
                        } else if (instruction instanceof JumpInsnNode jin) {
                            switch (jin.opcode) {
                                case GOTO -> {
                                    if (instruction.getNext() == jin.label) {
                                        method.instructions.remove(instruction);
                                        modified = true;
                                        break;
                                    } else if (jin.label.getNext() instanceof JumpInsnNode ljin && ljin.opcode == GOTO) {
                                        jin.label = ljin.label;
                                        modified = true;
                                        break;
                                    }
                                }
                                case IF_ICMPNE -> { // unoptimized piece of shit
                                    try {
                                        Simulator sim = new Simulator(method);
                                        sim.simulateUntil(instruction);

                                        int size = sim.getStack().size();
                                        Value a = sim.getStack().get(size - 1);
                                        Value b = sim.getStack().get(size - 2);

                                        if (!a.getResolved()) continue;
                                        if (!b.getResolved()) continue;

                                        int aVal = (int) a.getValue();
                                        int bVal = (int) b.getValue();

                                        if (aVal != bVal) {
                                            method.instructions.set(jin, new JumpInsnNode(GOTO, jin.label));
                                        } else {
                                            method.instructions.remove(jin);
                                        }
                                        for (AbstractInsnNode creatorNode : a.getCreatorNodes()) {
                                            method.instructions.remove(creatorNode);
                                        }
                                        for (AbstractInsnNode creatorNode : b.getCreatorNodes()) {
                                            method.instructions.remove(creatorNode);
                                        }

                                        modified = true;
                                        break;
                                    } catch (Throwable _) {}
                                }
                            }
                        } else if (instruction instanceof TableSwitchInsnNode tableSwitchNode) {
                            try {
                                Value a = new Simulator(method).simulateUntil(instruction).getLast();

                                if (!a.getResolved()) continue;

                                int aVal = (int) a.getValue();

                                LabelNode lbl = tableSwitchNode.dflt;

                                int idx = aVal - tableSwitchNode.min;

                                if (idx <= tableSwitchNode.labels.size()) {
                                    lbl = tableSwitchNode.labels.get(idx);
                                }

                                method.instructions.set(tableSwitchNode, new JumpInsnNode(GOTO, lbl));
                                for (AbstractInsnNode creatorNode : a.getCreatorNodes()) {
                                    method.instructions.remove(creatorNode);
                                }

                                modified = true;
                                break;
                            } catch (Throwable _) {}
                        }
                    }
                } while(modified);
            }
        });
    }

    private boolean shouldOptimizeLoad(AbstractInsnNode instruction, ControlFlowGraph cfg) {
        if (!(instruction instanceof VarInsnNode vin)) return false;

        final int opcode = instruction.getOpcode();
        if (opcode < ILOAD || opcode > ALOAD) return false;

        final int storeOpcode = opcode - ILOAD + ISTORE;
        String key = String.format("%s.%s%s-%s", cfg.getClassNode().name, cfg.getMethod().name, cfg.getMethod().desc, instruction.getOpcode());
        Set<Block> blocks;
        if (blockCache.containsKey(key)) {
            blocks = blockCache.get(key);
        } else {
            blocks = getContainingBlock(instruction, cfg).getChildren();
            blockCache.put(key, blocks);
        }
        // Randomly happens no idea why
        if (blocks == null) {
            blocks = getContainingBlock(instruction, cfg).getChildren();
            blockCache.put(key, blocks);
        }
        for (Block child : blocks) {
            for (AbstractInsnNode insn : child.getInstructions()) {
                if (insn == instruction) continue;

                if (insn.getOpcode() == storeOpcode && insn instanceof VarInsnNode loadInsn) {
                    if (loadInsn.var == vin.var) {
                        return false;
                    }
                } else if (storeOpcode == ISTORE && insn instanceof IincInsnNode iincInsn) {
                    if (iincInsn.var == vin.var) {
                        return false;
                    }
                }
            }
        }

        return true;
    }

    private static Block getContainingBlock(AbstractInsnNode instruction, ControlFlowGraph cfg) {
        return cfg.getBlocks().stream().filter(b -> b.getInstructions().contains(instruction)).findFirst().orElseThrow();
    }

    private boolean isRemovableStore(AbstractInsnNode instruction, InsnList instructions) {
        if (!(instruction instanceof VarInsnNode vin)) return false;

        final int opcode = instruction.getOpcode();
        if (opcode < ISTORE || opcode > ASTORE) return false;

        final int loadOpcode = opcode - ISTORE + ILOAD;

        for (AbstractInsnNode insn = instructions.getFirst(); insn != null; insn = insn.getNext()) {
            if (insn == instruction) continue; // skip the store itself

            if (insn.getOpcode() == loadOpcode && insn instanceof VarInsnNode loadInsn) {
                if (loadInsn.var == vin.var) {
                    return false; // found a load of the same var
                }
            }
        }

        return true; // no load found, store is removable
    }


    private boolean isUselessWithZero(AbstractInsnNode ain) {
        return switch (ain.getOpcode()) {
            case IADD, ISUB, IXOR, ISHL, ISHR, IUSHR, IOR -> true;
            default -> false;
        };
    }

    private boolean isMath(AbstractInsnNode ain) {
        return switch (ain.getOpcode()) {
            case IADD, ISUB, IDIV, IMUL,
                 IXOR, ISHL, ISHR, IAND,
                 IOR, IREM, IUSHR -> true;
            default -> false;
        };
    }
}
