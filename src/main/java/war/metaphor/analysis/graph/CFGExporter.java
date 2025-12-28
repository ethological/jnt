package war.metaphor.analysis.graph;

import org.objectweb.asm.Frame;
import org.objectweb.asm.tree.*;

import java.io.IOException;
import java.io.Writer;
import java.util.HashSet;
import java.util.Set;

public class CFGExporter {
    public static void exportToDot(ControlFlowGraph cfg, Writer writer) throws IOException {
        exportToDot(cfg, writer, false);
    }

    public static void exportToDot(ControlFlowGraph cfg, Writer writer, boolean showInstructions) throws IOException {
        writer.write("digraph G {\n");
        writer.write("  rankdir=TB;\n");
        writer.write("  node [shape=rectangle, fontname=\"Martian Mono\", fontsize=10];\n");
        writer.write("  edge [fontname=\"Martian Mono\", fontsize=8];\n\n");


        for (Block block : cfg.getBlocks()) {
            String shape = "rectangle";

            writer.write(String.format("  block%d [label=\"", block.getId()));

            if (block.isTrapHandler()) {
                writer.write("Handler: ");
                for (TryCatchBlockNode trap : block.getTrapHandlers()) {
                    writer.write(escape(trap.type == null ? "finally" : trap.type) + "\\n");
                }
            }

            writer.write(String.valueOf(block.getId()));

            if (showInstructions) {
                writer.write("\\n----------------\\n");
                for (AbstractInsnNode insn : block.getInstructions()) {
                    if (insn instanceof FrameNode || insn instanceof LineNumberNode) continue;
                    if (insn instanceof LabelNode) continue;

                    writer.write(escape(getInstructionName(insn.getOpcode())) + "\\n");
                }
            } else {
                writer.write("\\n" + block.getInstructions().size() + " instructions");
            }

            if (block.isCarrying()) {
                writer.write("\\n(Carrying)");
            }

            writer.write("\"");

            writer.write(String.format(", shape=%s", shape));

            if (block.isTrapHandler()) {
                writer.write(", style=filled, fillcolor=\"#FFF3F3\"");
            } else if (!block.getWithinTraps().isEmpty()) {
                writer.write(", style=filled, fillcolor=\"#F3F3FF\"");
            }

            writer.write("];\n");
        }


        Set<String> edgesWritten = new HashSet<>();
        for (Block block : cfg.getBlocks()) {

            for (Block successor : block.getVertices()) {
                String edgeKey = "block" + block.getId() + "->block" + successor.getId();
                if (!edgesWritten.contains(edgeKey)) {
                    writer.write(String.format("  block%d -> block%d [label=\"", block.getId(), successor.getId()));

                    Set<AbstractInsnNode> insns = block.getVertexInsn(successor);
                    if (insns != null && !insns.isEmpty()) {
                        boolean first = true;
                        for (AbstractInsnNode insn : insns) {
                            if (!first) writer.write("\\n");
                            first = false;
                            //writer.write(escape(insn.getOpcode() + ": " + insn.getClass().getSimpleName()));
                        }
                    }

                    writer.write("\"];\n");
                    edgesWritten.add(edgeKey);
                }
            }

            for (Block successor : block.getTrapVertices()) {
                String edgeKey = "block" + block.getId() + "->block" + successor.getId();
                if (!edgesWritten.contains(edgeKey)) {
                    writer.write(String.format("  block%d -> block%d [color=red, style=dashed, label=\"exception\"];\n",
                            block.getId(), successor.getId()));
                    edgesWritten.add(edgeKey);
                }
            }

            if (block.getFallThrough() != null) {
                String edgeKey = "block" + block.getId() + "->block" + block.getFallThrough().getId();
                if (!edgesWritten.contains(edgeKey)) {
                    writer.write(String.format("  block%d -> block%d [style=dotted, label=\"fall\"];\n",
                            block.getId(), block.getFallThrough().getId()));
                    edgesWritten.add(edgeKey);
                }
            }
        }

        writer.write("}\n");
        writer.flush();
    }

    private static String escape(String input) {
        if (input == null) return "";
        return input.replace("\"", "\\\"")
                .replace("\n", "\\n")
                .replace("\r", "\\r")
                .replace("\t", "\\t");
    }

    /*
    Fernflower
     */
    public static String getInstructionName(int opcode) {
        return opcodeNames[opcode];
    }

    private static final String[] opcodeNames = {
            "nop",
            "aconst_null",
            "iconst_m1",
            "iconst_0",
            "iconst_1",
            "iconst_2",
            "iconst_3",
            "iconst_4",
            "iconst_5",
            "lconst_0",
            "lconst_1",
            "fconst_0",
            "fconst_1",
            "fconst_2",
            "dconst_0",
            "dconst_1",
            "bipush",
            "sipush",
            "ldc",
            "ldc_w",
            "ldc2_w",
            "iload",
            "lload",
            "fload",
            "dload",
            "aload",
            "iload_0",
            "iload_1",
            "iload_2",
            "iload_3",
            "lload_0",
            "lload_1",
            "lload_2",
            "lload_3",
            "fload_0",
            "fload_1",
            "fload_2",
            "fload_3",
            "dload_0",
            "dload_1",
            "dload_2",
            "dload_3",
            "aload_0",
            "aload_1",
            "aload_2",
            "aload_3",
            "iaload",
            "laload",
            "faload",
            "daload",
            "aaload",
            "baload",
            "caload",
            "saload",
            "istore",
            "lstore",
            "fstore",
            "dstore",
            "astore",
            "istore_0",
            "istore_1",
            "istore_2",
            "istore_3",
            "lstore_0",
            "lstore_1",
            "lstore_2",
            "lstore_3",
            "fstore_0",
            "fstore_1",
            "fstore_2",
            "fstore_3",
            "dstore_0",
            "dstore_1",
            "dstore_2",
            "dstore_3",
            "astore_0",
            "astore_1",
            "astore_2",
            "astore_3",
            "iastore",
            "lastore",
            "fastore",
            "dastore",
            "aastore",
            "bastore",
            "castore",
            "sastore",
            "pop",
            "pop2",
            "dup",
            "dup_x1",
            "dup_x2",
            "dup2",
            "dup2_x1",
            "dup2_x2",
            "swap",
            "iadd",
            "ladd",
            "fadd",
            "dadd",
            "isub",
            "lsub",
            "fsub",
            "dsub",
            "imul",
            "lmul",
            "fmul",
            "dmul",
            "idiv",
            "ldiv",
            "fdiv",
            "ddiv",
            "irem",
            "lrem",
            "frem",
            "drem",
            "ineg",
            "lneg",
            "fneg",
            "dneg",
            "ishl",
            "lshl",
            "ishr",
            "lshr",
            "iushr",
            "lushr",
            "iand",
            "land",
            "ior",
            "lor",
            "ixor",
            "lxor",
            "iinc",
            "i2l",
            "i2f",
            "i2d",
            "l2i",
            "l2f",
            "l2d",
            "f2i",
            "f2l",
            "f2d",
            "d2i",
            "d2l",
            "d2f",
            "i2b",
            "i2c",
            "i2s",
            "lcmp",
            "fcmpl",
            "fcmpg",
            "dcmpl",
            "dcmpg",
            "ifeq",
            "ifne",
            "iflt",
            "ifge",
            "ifgt",
            "ifle",
            "if_icmpeq",
            "if_icmpne",
            "if_icmplt",
            "if_icmpge",
            "if_icmpgt",
            "if_icmple",
            "if_acmpeq",
            "if_acmpne",
            "goto",
            "jsr",
            "ret",
            "tableswitch",
            "lookupswitch",
            "ireturn",
            "lreturn",
            "freturn",
            "dreturn",
            "areturn",
            "return",
            "getstatic",
            "putstatic",
            "getfield",
            "putfield",
            "invokevirtual",
            "invokespecial",
            "invokestatic",
            "invokeinterface",
            "invokedynamic",
            "new",
            "newarray",
            "anewarray",
            "arraylength",
            "athrow",
            "checkcast",
            "instanceof",
            "monitorenter",
            "monitorexit",
            "wide",
            "multianewarray",
            "ifnull",
            "ifnonnull",
            "goto_w",
            "jsr_w"
    };
}
