package war.metaphor.mutator.parameter;

import org.objectweb.asm.Type;
import org.objectweb.asm.tree.*;
import org.reactfx.value.Var;
import war.configuration.ConfigurationSection;
import war.metaphor.base.ObfuscatorContext;
import war.metaphor.mutator.Mutator;
import war.metaphor.tree.JClassNode;
import war.metaphor.util.asm.BytecodeUtil;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class ExchangeMutator extends Mutator {
    public ExchangeMutator(ObfuscatorContext base, ConfigurationSection config) {
        super(base, config);
    }

    // 5 -> int
    // 6 -> float
    // 7 -> long
    // 8 -> double
    record Lane(int type) {}
    record LaneVertex(Lane lane, int index) {}

    @Override
    public void run(ObfuscatorContext base) {
        for (JClassNode jcn : base.getClasses()) {
            for (MethodNode mn : jcn.methods) {
                List<LaneVertex> vertices = new ArrayList<>();

                Type[] args = Type.getArgumentTypes(mn.desc);

                int slot = (mn.access & ACC_STATIC) == 0 ? 1 : 0;
                for (int i = 0; i < args.length; i++) {
                    Type t = args[i];
                    int sort = t.getSort();

                    if (sort == Type.INT || sort == Type.FLOAT || sort == Type.LONG || sort == Type.DOUBLE) {
                        vertices.add(new LaneVertex(new Lane(sort), slot));
                    }

                    slot += t.getSize();
                }

                for (LaneVertex vtx : vertices) {
                    String lane = fromVertex(vtx);
                    
                    for (AbstractInsnNode ain : mn.instructions) {
                        switch (ain.getOpcode()) {
                            case ILOAD, FLOAD, LLOAD, DLOAD -> {
                                VarInsnNode vin = (VarInsnNode) ain;
                                if (vin.index == vtx.index) {
                                    InsnList list = new InsnList();

                                    list.add(new FieldInsnNode(
                                            GETSTATIC,
                                            "war/jnt/xchg/Exchange",
                                            lane,
                                            fromVertex0(vtx)
                                    ));
                                    list.add(BytecodeUtil.makeInteger(vin.index));
                                    list.add(new InsnNode(fromVertex1(vtx)));

                                    mn.instructions.insertBefore(vin, list);
                                    mn.instructions.remove(vin);
                                }
                            }
                            case ISTORE, FSTORE, LSTORE, DSTORE -> {
                                VarInsnNode vin = (VarInsnNode) ain;
                                if (vin.index == vtx.index) {
                                    InsnList list = new InsnList();

                                    list.add(new FieldInsnNode(
                                            GETSTATIC,
                                            "war/jnt/xchg/Exchange",
                                            lane,
                                            fromVertex0(vtx)
                                    ));
                                    list.add(BytecodeUtil.makeInteger(vin.index));
                                    list.add(vin);
                                    list.add(new InsnNode(fromVertex2(vtx)));

                                    mn.instructions.insertBefore(vin, list);
                                    mn.instructions.remove(vin);
                                }
                            }
                        }
                    }
                }
            }
        }
    }
    
    private int fromSort(int sort) {
        return switch (sort) {
            case Type.INT -> ILOAD;
            case Type.FLOAT -> FLOAD;
            case Type.LONG -> LLOAD;
            case Type.DOUBLE -> DLOAD;
            default -> throw new IllegalStateException("Unexpected value: " + sort);
        };
    }

    private String fromVertex0(LaneVertex vtx) {
        return switch (vtx.lane.type()) {
            case 5 -> "[I";
            case 6 -> "[F";
            case 7 -> "[J";
            case 8 -> "[D";
            default -> throw new IllegalStateException("Unexpected value: " + vtx.lane.type());
        };
    }

    private int fromVertex1(LaneVertex vtx) {
        return switch (vtx.lane.type()) {
            case 5 -> IALOAD;
            case 6 -> FALOAD;
            case 7 -> LALOAD;
            case 8 -> DALOAD;
            default -> throw new IllegalStateException("Unexpected value: " + vtx.lane.type());
        };
    }

    private int fromVertex2(LaneVertex vtx) {
        return switch (vtx.lane.type()) {
            case 5 -> IASTORE;
            case 6 -> FASTORE;
            case 7 -> LASTORE;
            case 8 -> DASTORE;
            default -> throw new IllegalStateException("Unexpected value: " + vtx.lane.type());
        };
    }

    private String fromVertex(LaneVertex vtx) {
        return "xchg_ln" + vtx.lane.type();
    }
}
