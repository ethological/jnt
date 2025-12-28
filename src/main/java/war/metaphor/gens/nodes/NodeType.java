package war.metaphor.gens.nodes;

import org.objectweb.asm.Opcodes;

public interface NodeType extends Opcodes {

    int T_PUSH = 1;
    int T_POP = 1 << 2;
    int T_NONE = 1 << 3;
    int T_MASK_CONSTANT = 1 << 4;
    int T_MASK_CONSTANT_INT = 1 << 5;
    int T_MASK_CONSTANT_LONG = 1 << 6;
    int T_MASK_CONSTANT_DOUBLE = 1 << 7;
    int T_MASK_CONSTANT_FLOAT = 1 << 8;
    int T_MASK_CONSTANT_OBJECT = 1 << 10;
    int T_MASK_LOAD = 1 << 9;
    int T_MASK_MATH = 1 << 11;
    int T_MASK_STORE = 1 << 12;
    int T_CONV = 1 << 13;

    int T_CONSTANT = T_PUSH | T_MASK_CONSTANT;
    int T_CONSTANT_INT = T_CONSTANT | T_MASK_CONSTANT_INT;
    int T_CONSTANT_LONG = T_CONSTANT | T_MASK_CONSTANT_LONG;
    int T_CONSTANT_DOUBLE = T_CONSTANT | T_MASK_CONSTANT_DOUBLE;
    int T_CONSTANT_FLOAT = T_CONSTANT | T_MASK_CONSTANT_FLOAT;
    int T_CONSTANT_OBJECT = T_CONSTANT | T_MASK_CONSTANT_OBJECT;
    int T_LOAD = T_PUSH | T_MASK_LOAD;
    int T_MATH = T_POP | T_MASK_MATH;
    int T_STORE = T_POP | T_MASK_STORE;

    static boolean isPop(Node node) {
        return (node.type() & T_POP) != 0;
    }

    static boolean isConv(Node node) {
        return (node.type() & T_CONV) != 0;
    }

}
