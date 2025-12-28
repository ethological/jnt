package war.metaphor.analysis.frames;

import lombok.Getter;
import org.objectweb.asm.Frame;
import org.objectweb.asm.SymbolTable;
import org.objectweb.asm.tree.LabelNode;

import static org.objectweb.asm.Frame.*;

/**
 * @author ssheera
 * This basically just converts the frame into a string
 * because I found that asm analyser and asm stackframes can be
 * different. Basically helps to lock in whether frame A and frame B are the same
 */
@Getter
public class FrameComputation {

    private final String info;

    public FrameComputation(SymbolTable sym, LabelNode label) {

        try {

            Frame frame = label.getLabel().frame;

            StringBuilder sb = new StringBuilder();

            sb.append("stack=[");
            if (frame.inputStack != null) {
                for (int i : frame.inputStack) {
                    sb.append(getType(sym, i)).append(", ");
                }
                if (frame.inputStack.length > 0) {
                    sb.delete(sb.length() - 2, sb.length());
                }
            }
            sb.append("], locals=[");
            if (frame.inputLocals != null) {
                for (int i : frame.inputLocals) {
                    sb.append(getType(sym, i)).append(", ");
                }
                if (frame.inputLocals.length > 0) {
                    sb.delete(sb.length() - 2, sb.length());
                }
            }
            sb.append("]");
            info = sb.toString();

        } catch (Exception ex) {
            throw new RuntimeException("Error while computing frame", ex);
        }
    }

    private String getType(SymbolTable symbolTable, int val) {
        String type;
        if (val == TOP) {
            type = "top";
        } else if (val == BYTE) {
            type = "byte";
        } else if (val == CHAR) {
            type = "char";
        } else if (val == SHORT) {
            type = "short";
        } else if (val == BOOLEAN) {
            type = "boolean";
        } else if (val == Frame.INTEGER) {
            type = "int";
        } else if (val == Frame.FLOAT) {
            type = "float";
        } else if (val == Frame.DOUBLE) {
            type = "double";
        } else if (val == Frame.LONG) {
            type = "long";
        } else if (val == Frame.NULL) {
            type = "null";
        } else if (val == Frame.UNINITIALIZED_THIS) {
            type = "uninitialized_this";
        } else if ((val & REFERENCE_KIND) != 0) {
            val = val & ~REFERENCE_KIND;
            if ((val & ARRAY_OF) != 0) {
                val = val & ~ARRAY_OF;
                return getType(symbolTable, val) + "[]";
            } else {
                try {
                    type = symbolTable.getType(val & 0xFF).value;
                } catch (Exception e) {
                    type = "reference";
                }
            }
        } else {
            if ((val & ARRAY_OF) != 0) {
                val = val & ~ARRAY_OF;
                return getType(symbolTable, val) + "[]";
            }
            type = "top";
        }
        return type;
    }

    @Override
    public boolean equals(Object obj) {
        if (obj instanceof FrameComputation) {
            return info.equals(((FrameComputation) obj).info);
        }
        return false;
    }

    @Override
    public String toString() {
        return info;
    }
}
