package war.metaphor.analysis.graph;

import lombok.Getter;
import lombok.Setter;
import org.objectweb.asm.tree.AbstractInsnNode;
import org.objectweb.asm.tree.LabelNode;
import org.objectweb.asm.tree.TryCatchBlockNode;
import org.objectweb.asm.tree.analysis.BasicValue;
import org.objectweb.asm.tree.analysis.Frame;
import war.metaphor.analysis.frames.FrameComputation;

import java.util.*;

@Getter
@Setter
public class Block {

    private final Set<Block> trapAccessors = new HashSet<>();
    private final Set<Block> trapVertices = new HashSet<>();
    private final Set<Block> accessors = new HashSet<>();
    private final Set<Block> vertices = new HashSet<>();

    private final Map<Block, Set<AbstractInsnNode>> accessorInsns = new HashMap<>();
    private final Map<Block, Set<AbstractInsnNode>> vertexInsns = new HashMap<>();

    private int id;
    private LabelNode label;
    private Frame<BasicValue> startFrame;
    private FrameComputation startComp;

    private Block fallThrough;
    private Block fallThroughAccessor;

    private List<TryCatchBlockNode> traps;
    private List<TryCatchBlockNode> withinTraps;
    private List<AbstractInsnNode> instructions;

    private List<TryCatchBlockNode> trapHandlers;
    private boolean trapHandler, carrying;

    private int seed = -1;

    public boolean isTrap() {
        return !traps.isEmpty();
    }

    public boolean inTrap() {
        return !withinTraps.isEmpty();
    }

    public AbstractInsnNode getStart() {
        return label;
    }

    public TryCatchBlockNode getLowestTrap() {
        TryCatchBlockNode lowest = null;
        for (TryCatchBlockNode trap : getWithinTraps()) {
            if (lowest == null || trap.start.index >= lowest.start.index && trap.end.index <= lowest.end.index) {
                lowest = trap;
            }
        }
        return lowest;
    }

    public AbstractInsnNode getEnd() {
        return instructions.getLast();
    }

    public int getStartIndex() {
        return label.index;
    }

    public int getEndIndex() {
        return getEnd().index;
    }

    public void setFallThrough(Block block, AbstractInsnNode insn) {
        fallThrough = block;
        block.fallThroughAccessor = this;
    }

    public void addAccessor(Block accessor, AbstractInsnNode insn) {
        accessors.add(accessor);
        accessorInsns.computeIfAbsent(accessor, k -> new HashSet<>()).add(insn);
    }

    public void addVertex(Block vertex, AbstractInsnNode insn) {
        vertices.add(vertex);
        vertexInsns.computeIfAbsent(vertex, k -> new HashSet<>()).add(insn);
    }

    public void addTrapAccessor(Block accessor) {
        trapAccessors.add(accessor);
    }

    public void addTrapVertex(Block vertex) {
        trapVertices.add(vertex);
    }

    public Set<AbstractInsnNode> getAccessorInsn(Block accessor) {
        return accessorInsns.get(accessor);
    }

    public Set<AbstractInsnNode> getVertexInsn(Block vertex) {
        return vertexInsns.get(vertex);
    }

    public boolean isSame(Block block) {
        if (startComp == null || block.startComp == null)
            return false;
        if (startFrame == null || block.startFrame == null)
            return false;
        if (!block.getStartComp().equals(startComp)) return false;
        return startFrame.toString().equals(block.startFrame.toString());
    }

    public boolean isFallThrough() {
        return fallThrough != null;
    }

    public boolean inRange(AbstractInsnNode insn) {
        if (instructions.isEmpty()) return false;
        int start = label.index;
        int end = instructions.getLast().index;
        return insn.index >= start && insn.index <= end;
    }

    public Set<Block> getParents() {
        return getParents(new HashSet<>());
    }

    public Set<Block> getParents(Set<Block> visited) {
        if (visited.contains(this)) return Collections.emptySet();
        visited.add(this);
        Set<Block> parents = new HashSet<>(getAllAccessors());
        Set<Block> temp = new HashSet<>();
        for (Block parent : parents) {
            temp.addAll(parent.getParents(visited));
        }
        parents.addAll(temp);
        return parents;
    }

    public Set<Block> getChildren() {
        return getChildren(new HashSet<>());
    }

    public Set<Block> getChildren(Set<Block> visited) {
        if (visited.contains(this)) return Collections.emptySet();
        visited.add(this);
        Set<Block> children = new HashSet<>(getAllVertices());
        Set<Block> temp = new HashSet<>();
        for (Block child : children) {
            temp.addAll(child.getChildren(visited));
        }
        children.addAll(temp);
        return children;
    }

    public Set<Block> getTree() {
        Set<Block> tree = new HashSet<>(getParents());
        tree.addAll(getChildren());
        tree.add(this);
        return tree;
    }

    public Set<Block> getAllVertices() {
        Set<Block> all = new HashSet<>(vertices);
        all.addAll(trapVertices);
        return all;
    }

    public Set<Block> getAllAccessors() {
        Set<Block> all = new HashSet<>(accessors);
        all.addAll(trapAccessors);
        return all;
    }

    public int getSize() {
        return instructions.size();
    }

    @Override
    public boolean equals(Object obj) {
        if (obj instanceof Block) {
            return id == ((Block) obj).id;
        }
        return false;
    }

    @Override
    public String toString() {
        return "Block " + id;
    }

}