package war.metaphor.mutator.flow;

import org.apache.commons.lang3.RandomUtils;
import org.objectweb.asm.tree.*;
import war.configuration.ConfigurationSection;
import war.jnt.annotate.Level;
import war.jnt.annotate.Stability;
import war.metaphor.analysis.graph.Block;
import war.metaphor.analysis.graph.ControlFlowGraph;
import war.metaphor.base.ObfuscatorContext;
import war.metaphor.mutator.Mutator;
import war.metaphor.mutator.flow.opaque.IntegerOpaque;
import war.metaphor.mutator.flow.opaque.Opaque;
import war.metaphor.mutator.flow.opaque.SwitchOpaque;
import war.metaphor.tree.JClassNode;
import war.metaphor.util.asm.BytecodeUtil;
import war.metaphor.util.builder.InsnListBuilder;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

@Stability(Level.UNKNOWN)
public class OpaquePredicatesMutator extends Mutator {

    private final List<Opaque> opaques = List.of(
            new SwitchOpaque(),
            new IntegerOpaque()
    );

    public OpaquePredicatesMutator(ObfuscatorContext base, ConfigurationSection config) {
        super(base, config);
    }

    @Override
    public void run(ObfuscatorContext base) {
        for (JClassNode classNode : base.getClasses()) {
            if (classNode.isExempt()) continue;
            for (MethodNode method : classNode.methods) {
                if (classNode.isExempt(method)) continue;

                int leeway = BytecodeUtil.leeway(method);
                if (leeway < 30000)
                    continue;

                ControlFlowGraph graph = new ControlFlowGraph(classNode, method);

                if (!graph.compute()) continue;

                if (graph.getBlocks().isEmpty()) continue;

                int predicateLocal = method.maxLocals++;

                int initialSeed = RandomUtils.nextInt();

                graph.getStartBlock().setSeed(initialSeed);

                for (Block block : graph.getBlocks())
                    block.setSeed(initialSeed);

                // Add the initial seed to the method
                InsnList list = new InsnList();

                list.add(BytecodeUtil.generateInteger(initialSeed));
                list.add(new VarInsnNode(ISTORE, predicateLocal));
                method.instructions.insert(list);

                // Add seeds at each vertex
                for (Block block : graph.getBlocks()) {
                    int blockSeed = block.getSeed();
                    for (Block vertex : block.getVertices()) {
                        int targetSeed = vertex.getSeed();
                        Set<AbstractInsnNode> insns = block.getVertexInsn(vertex);
                        for (AbstractInsnNode insn : insns) {
                            if (insn instanceof JumpInsnNode) {
                                method.instructions.insertBefore(insn, InsnListBuilder.builder()
                                        .list(BytecodeUtil.generateSeeded(predicateLocal, targetSeed, blockSeed))
                                        .istore(predicateLocal).build());
                                if (insn.getOpcode() != GOTO) {
                                    method.instructions.insert(insn, InsnListBuilder.builder()
                                            .list(BytecodeUtil.generateSeeded(predicateLocal, blockSeed, targetSeed))
                                            .istore(predicateLocal).build());
                                }
                            }
                        }
                    }
                }

                // Add seeds to switches
                for (AbstractInsnNode instruction : method.instructions) {
                    Block block = graph.getContainingBlock(instruction);
                    if (block == null) continue;
                    int blockSeed = block.getSeed();
                    if (instruction instanceof LookupSwitchInsnNode || instruction instanceof TableSwitchInsnNode) {

                        // Clone the original switch,
                        // based off the key, set the new opaque on the supposed target block

                        Map<LabelNode, LabelNode> newLabels = new HashMap<>();
                        List<LabelNode> targets = graph.getJumpTargets(instruction);

                        for (LabelNode target : targets) {
                            newLabels.put(target, new LabelNode());
                        }

                        AbstractInsnNode clone = graph.clone(instruction);
                        LabelNode end = new LabelNode();

                        InsnList blockList = new InsnList();

                        blockList.add(new InsnNode(DUP));
                        blockList.add(clone);

                        for (LabelNode original : newLabels.keySet()) {
                            LabelNode newLabel = newLabels.get(original);
                            BytecodeUtil.replaceLabelNode(clone, original, newLabel);
                            blockList.add(newLabel);
                            Block targetBlock = graph.getBlock(original);
                            int targetSeed = targetBlock.getSeed();
                            blockList.add(InsnListBuilder.builder()
                                    .list(BytecodeUtil.generateSeeded(predicateLocal, targetSeed, blockSeed))
                                    .istore(predicateLocal).build());
                            blockList.add(new JumpInsnNode(GOTO, end));
                        }

                        blockList.add(end);

                        method.instructions.insertBefore(instruction, blockList);

                    }
                }

                // Add seeds to trap handlers
                for (Block block : graph.getBlocks()) {
                    int targetSeed = block.getSeed();
                    if (block.isTrapHandler()) {
                        if (block.getTrapAccessors().size() == 1) {
                            int blockSeed = block.getTrapAccessors().iterator().next().getSeed();
                            method.instructions.insert(block.getStart(), InsnListBuilder.builder()
                                    .list(BytecodeUtil.generateSeeded(predicateLocal, targetSeed, blockSeed))
                                    .istore(predicateLocal).build());
                        } else {

                            Map<LabelNode, Integer> labelSeeds = new HashMap<>();
                            LabelNode dflt = new LabelNode();

                            for (Block accessor : block.getTrapAccessors())
                                labelSeeds.put(new LabelNode(), accessor.getSeed());
                            labelSeeds.put(new LabelNode(), targetSeed);

                            LookupSwitchInsnNode lookupSwitch = new LookupSwitchInsnNode(dflt, labelSeeds.values().stream().mapToInt(Integer::intValue).toArray(), labelSeeds.keySet().toArray(new LabelNode[0]));

                            BytecodeUtil.fixLookupSwitch(lookupSwitch);
                            LabelNode end = new LabelNode();

                            InsnList lookupList = new InsnList();

                            lookupList.add(new VarInsnNode(ILOAD, predicateLocal));
                            lookupList.add(lookupSwitch);

                            lookupList.add(dflt);
                            lookupList.add(new TypeInsnNode(NEW, "java/lang/RuntimeException"));
                            lookupList.add(new InsnNode(DUP));
                            lookupList.add(new LdcInsnNode("Error in Hash"));
                            lookupList.add(new MethodInsnNode(INVOKESPECIAL, "java/lang/RuntimeException", "<init>", "(Ljava/lang/String;)V", false));
                            lookupList.add(new InsnNode(ATHROW));

                            for (LabelNode label : labelSeeds.keySet()) {
                                lookupList.add(label);
                                lookupList.add(InsnListBuilder.builder()
                                        .list(BytecodeUtil.generateSeeded(predicateLocal, targetSeed, labelSeeds.get(label)))
                                        .istore(predicateLocal).build());
                                lookupList.add(new JumpInsnNode(GOTO, end));
                            }

                            lookupList.add(end);

                            method.instructions.insert(block.getStart(), lookupList);

                        }
                    }
                }

                for (Block block : graph.getBlocks()) {
                    for (Opaque opaque : opaques) {
                        opaque.handle(method, block, predicateLocal);
                    }
                }

            }
        }
    }
}
