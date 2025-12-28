package war.metaphor.mutator.flow;

import org.apache.commons.lang3.ArrayUtils;
import org.objectweb.asm.tree.*;
import war.configuration.ConfigurationSection;
import war.jnt.annotate.Level;
import war.jnt.annotate.Stability;
import war.metaphor.analysis.graph.Block;
import war.metaphor.analysis.graph.ControlFlowGraph;
import war.metaphor.base.ObfuscatorContext;
import war.metaphor.mutator.Mutator;
import war.metaphor.tree.JClassNode;
import war.metaphor.util.asm.BytecodeUtil;

import java.lang.reflect.Modifier;
import java.util.*;

/**
 * This works now
 *
 * @author jan
 */
@Stability(Level.VERY_HIGH)
public class ControlFlowFlatteningMutator extends Mutator {

    public ControlFlowFlatteningMutator(ObfuscatorContext base, ConfigurationSection config) {
        super(base, config);
    }

    @Override
    public void run(ObfuscatorContext ctx) {

        for (JClassNode classNode : ctx.getClasses()) {
            if (classNode.isExempt()) continue;
            for (MethodNode method : classNode.methods) {
                if (Modifier.isAbstract(method.access))continue;
                if (classNode.isExempt(method)) continue;

                int size = BytecodeUtil.leeway(method);

                if (size < 30000)
                    continue;

                ControlFlowGraph graph = new ControlFlowGraph(classNode, method);

                if (!graph.compute())
                    continue;

                flatten(method, graph);
            }
        }
    }

    private void flatten(MethodNode method, ControlFlowGraph graph) {

        List<List<Block>> grouped = graph.groupSameBlocks();

        grouped.forEach(blocks -> blocks.removeIf(Block::isCarrying));
        grouped.forEach(blocks -> blocks.removeIf(block -> block == graph.getStartBlock()));

        grouped.removeIf(blocks -> blocks.size() <= 1);

        if (grouped.isEmpty()) return;

        int var = method.maxLocals++;
        InsnList init = new InsnList();
        init.add(BytecodeUtil.makeInteger(RANDOM.nextInt()));
        init.add(new VarInsnNode(ISTORE, var));
        method.instructions.insert(init);

        Collections.shuffle(grouped);

        for (List<Block> group : grouped) {

            LookupSwitchInsnNode lookupSwitch;

            Map<Block, LabelNode> labels = new HashMap<>();
            Map<Block, Integer> keys = new HashMap<>();

            for (Block block : group) {
                keys.put(block, nextIntUnique(keys.values()));
                labels.put(block, new LabelNode());
            }

            LabelNode start = new LabelNode();

            lookupSwitch = new LookupSwitchInsnNode(labels.values().toArray(new LabelNode[0])[0], keys.values().stream().mapToInt(i -> i).toArray(), labels.values().toArray(new LabelNode[0]));

            BytecodeUtil.fixLookupSwitch(lookupSwitch);

            InsnList list = new InsnList();
            list.add(start);
            list.add(new VarInsnNode(ILOAD, var));
            list.add(lookupSwitch);

            for (Block block : group) {

                InsnList blockList = new InsnList();

                InsnList sub = new InsnList();

//                if (block.getAllAccessors().size() == 1 && group.contains(block.getAllAccessors().iterator().next())) {
//                    sub.add(BytecodeUtil.generateSeeded(var, keys.get(block), keys.get(block.getAllAccessors().iterator().next())));
//                } else {

                sub.add(BytecodeUtil.generateInteger(keys.get(block)));
//                }

                sub.add(new VarInsnNode(ISTORE, var));
                sub.add(new JumpInsnNode(GOTO, start));

                LabelNode blockLabel = labels.get(block);
                blockList.add(blockLabel);

                for (AbstractInsnNode instruction : block.getInstructions()) {
                    blockList.add(graph.clone(instruction));
                    method.instructions.remove(instruction);
                }

                method.instructions.insert(block.getStart(), sub);

                if (block.inTrap()) {
                    method.instructions.insertBefore(block.getLowestTrap().end, blockList);
                } else {
                    list.add(blockList);
                }
            }

            method.instructions.add(list);
        }
    }

    public int nextIntUnique(Collection<Integer> keys, int... others) {
        Random rand = new Random();
        int key = rand.nextInt();
        while (keys.contains(key) || ArrayUtils.contains(others, key))
            key = rand.nextInt();
        return key;
    }

}
