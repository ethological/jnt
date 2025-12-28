package war.jnt.fusebox.impl;

import org.objectweb.asm.Opcodes;
import org.objectweb.asm.Type;
import org.objectweb.asm.tree.InsnNode;
import org.objectweb.asm.tree.MethodNode;
import war.jnt.core.code.impl.field.FieldOffsetInfo;
import war.jnt.core.code.impl.invoke.CallOffsetInfo;
import war.jnt.dash.Ansi;
import war.jnt.dash.Level;
import war.jnt.dash.Logger;
import war.jnt.dash.Origin;
import war.jnt.stack.StackTracker;
import war.metaphor.tree.JClassNode;

import java.lang.reflect.Modifier;

import static war.jnt.dash.Ansi.Color.*;

public class Internal implements Opcodes {
    private static final Logger logger = Logger.INSTANCE;

    public static boolean disallowedTranspile(JClassNode node, MethodNode method) {
        if (node.isInterface()) return true;
        if (Modifier.isAbstract(method.access)) return true;
        if (disallowedTranspile(method.name)) return true;
        return method.instructions.size == 0;
    }

    public static boolean disallowedTranspile(String name) {
        return name.equals("<init>") || name.equals("<clinit>");
    }

    public static void panic(int code) {
        System.exit(code);
    }

    public static boolean isSelf(MethodNode m, int index) {
        return index == 0 && !isAccess(m.access, Opcodes.ACC_STATIC);
    }

    public static boolean isParam(MethodNode method, int index) {
        int baseArgSizes = (Type.getArgumentsAndReturnSizes(method.desc) >> 2) - 1;
        return index < (baseArgSizes + (isAccess(method.access, ACC_STATIC) ? 0 : 1));
    }

    public static String createStack(int size) {
        return String.format("\tjvalue stack[%s];\n", size);
    }

    public static String createLocals(int size) {
        return String.format("\tjvalue locals[%s];\n", size);
    }

    public static boolean isAccess(int flags, int match) {
        return (flags & match) > 0;
    }

    public static String computePop(StackTracker tracker) {
        int index = tracker.dump();
        tracker.simPop();

        return String.format("stack[%s]", index);
    }

    public static int resolveConst(int op) {
        if (isIntConst(op)) return 0;
        else if (isFloatConst(op)) return 1;
        else if (isDoubleConst(op)) return 2;
        else if (isLongConst(op)) return 3;

        throw new RuntimeException("Unknown opcode passed to resolveConst.");
    }

    public static boolean isIntConst(int op) {
        return op >= Opcodes.ICONST_M1 && op <= Opcodes.ICONST_5;
    }

    public static boolean isFloatConst(int op) {
        return op >= Opcodes.FCONST_0 && op <= Opcodes.FCONST_2;
    }

    public static boolean isDoubleConst(int op) {
        return op == Opcodes.DCONST_0 || op == Opcodes.DCONST_1;
    }

    public static boolean isLongConst(int op) {
        return op == Opcodes.LCONST_0 || op == Opcodes.LCONST_1;
    }

    public static String computePush(StackTracker tracker) {
        tracker.simPush();
        int index = tracker.dump();

        return String.format("stack[%s]", index);
    }

    public static float fromFloatConst(InsnNode insn) {
        return switch (insn.getOpcode()) {
            case FCONST_0 -> 0f;
            case FCONST_1 -> 1f;
            case FCONST_2 -> 2f;
            default -> {
                logger.logln(Level.FATAL, Origin.FUSEBOX, "Unknown opcode passed to constant resolver.");
                panic(1);
                yield 0;
            }
        };
    }

    public static double fromDoubleConst(InsnNode insn) {
        return switch (insn.getOpcode()) {
            case DCONST_0 -> 0d;
            case DCONST_1 -> 1d;
            default -> {
                logger.logln(Level.FATAL, Origin.FUSEBOX, "Unknown opcode passed to constant resolver.");
                panic(1);
                yield 0;
            }
        };
    }

    public static long fromLongConst(InsnNode insn) {
        return switch (insn.getOpcode()) {
            case LCONST_0 -> 0L;
            case LCONST_1 -> 1L;
            default -> {
                logger.logln(Level.FATAL, Origin.FUSEBOX, "Unknown opcode passed to constant resolver.");
                panic(1);
                yield 0;
            }
        };
    }

    public static String resolveType(Type type) {
        return switch (type.getSort()) {
            case Type.INT, Type.BOOLEAN, Type.SHORT, Type.CHAR, Type.BYTE -> ".i";
            case Type.FLOAT -> ".f";
            case Type.DOUBLE -> ".d";
            case Type.LONG -> ".j";
            case Type.OBJECT, Type.ARRAY -> ".l";
            default -> {
                logger.logln(Level.WARNING, Origin.FUSEBOX, String.format("Unknown type passed to type resolver : %s", new Ansi().c(YELLOW).s(type.getDescriptor())));
                yield ".l";
            }
        };
    }

    public static Type fromOpcode(int opcode) {
        switch (opcode) {
            case IALOAD, IASTORE -> {
                return Type.INT_TYPE;
            }
            case FALOAD, FASTORE -> {
                return Type.FLOAT_TYPE;
            }
            case DALOAD, DASTORE -> {
                return Type.DOUBLE_TYPE;
            }
            case LALOAD, LASTORE -> {
                return Type.LONG_TYPE;
            }
            case AALOAD, AASTORE -> {
                return Type.getObjectType("java/lang/Object");
            }
            case BALOAD, BASTORE -> {
                return Type.BYTE_TYPE;
            }
            case CALOAD, CASTORE -> {
                return Type.CHAR_TYPE;
            }
            case SALOAD, SASTORE -> {
                return Type.SHORT_TYPE;
            }
        }
        throw new RuntimeException("Unknown opcode passed to type resolver");
    }

    public static String resolveReturnType(String desc) {
        Type rType = Type.getReturnType(desc);

        return switch (rType.getSort()) {
            case Type.INT, Type.BOOLEAN, Type.SHORT, Type.CHAR, Type.BYTE -> ".i";
            case Type.FLOAT -> ".f";
            case Type.DOUBLE -> ".d";
            case Type.LONG -> ".j";
            case Type.OBJECT, Type.ARRAY -> ".l";
            default -> {
                throw new RuntimeException("Failed to resolve return type.");
            }
        };
    }

    public static String fromFieldType(String desc) {
        Type type = Type.getType(desc);

        return switch (type.getSort()) {
            case Type.INT, Type.BOOLEAN, Type.SHORT, Type.CHAR, Type.BYTE -> ".i";
            case Type.FLOAT -> ".f";
            case Type.DOUBLE -> ".d";
            case Type.LONG -> ".j";
            default -> ".l";
        };
    }

    public static String resolveFunction(String desc, int op) {
        StringBuilder builder = new StringBuilder();

        Type rType = Type.getReturnType(desc);

        builder.append(switch (op) {
            case INVOKESTATIC -> "CallStatic";
            case INVOKEVIRTUAL, INVOKEINTERFACE -> "Call";
            case INVOKESPECIAL -> "CallNonvirtual";
            default -> throw new IllegalStateException("Unexpected value: " + op);
        });

        switch (rType.getSort()) {
            case Type.VOID -> builder.append("VoidMethodA");
            case Type.INT -> builder.append("IntMethodA");
            case Type.FLOAT -> builder.append("FloatMethodA");
            case Type.DOUBLE -> builder.append("DoubleMethodA");
            case Type.LONG -> builder.append("LongMethodA");
            case Type.OBJECT, Type.ARRAY -> builder.append("ObjectMethodA");
            case Type.BOOLEAN -> builder.append("BooleanMethodA");
            case Type.SHORT -> builder.append("ShortMethodA");
            case Type.BYTE -> builder.append("ByteMethodA");
            case Type.CHAR -> builder.append("CharMethodA");
            default -> logger.logln(Level.WARNING, Origin.FUSEBOX, String.format("Unknown type passed to call function resolver : %s", new Ansi().c(YELLOW).s(rType.getDescriptor())));
        }
        return builder.toString();
    }

    public static String resolveField(String desc, int op) {
        StringBuilder builder = new StringBuilder();

        Type type = Type.getType(desc);

        builder.append(switch (op) {
            case GETSTATIC -> "GetStatic";
            case GETFIELD -> "Get";
            case PUTSTATIC -> "SetStatic";
            case PUTFIELD -> "Set";
            default -> throw new IllegalStateException("Unexpected value: " + op);
        });

        switch (type.getSort()) {
            case Type.INT -> builder.append("IntField");
            case Type.FLOAT -> builder.append("FloatField");
            case Type.DOUBLE -> builder.append("DoubleField");
            case Type.LONG -> builder.append("LongField");
            case Type.OBJECT, Type.ARRAY -> builder.append("ObjectField");
            case Type.BOOLEAN -> builder.append("BooleanField");
            case Type.SHORT -> builder.append("ShortField");
            case Type.BYTE -> builder.append("ByteField");
            case Type.CHAR -> builder.append("CharField");
        }
        return builder.toString();
    }

    /**
     * Shitcode premium
     */
    public static FieldOffsetInfo resolveFieldOffset(String desc, int op) {
        int out;

        Type type = Type.getType(desc);

        out = switch (op) {
            case GETSTATIC -> 145;
            case GETFIELD -> 95;
            case PUTSTATIC -> 154;
            case PUTFIELD -> 104;
            default -> throw new IllegalStateException("Unexpected value: " + op);
        };

        String objType;

        switch (type.getSort()) {
            case Type.INT:
                objType = "jint";
                out += 5;
                break;
            case Type.FLOAT:
                objType = "jfloat";
                out += 7;
                break;
            case Type.DOUBLE:
                objType = "jdouble";
                out += 8;
                break;
            case Type.LONG:
                objType = "jlong";
                out += 6;
                break;
            case Type.OBJECT:
            case Type.ARRAY:
                objType = "jobject";
                break;
            case Type.BOOLEAN:
                objType = "jboolean";
                out += 1;
                break;
            case Type.SHORT:
                objType = "jshort";
                out += 4;
                break;
            case Type.BYTE:
                objType = "jbyte";
                out += 2;
                break;
            case Type.CHAR:
                objType = "jchar";
                out += 3;
                break;
            default:
                throw new IllegalStateException("Unexpected value: " + type.getSort());
        }

        return new FieldOffsetInfo(out, objType);
    }

    /**
     * Shitcode deluxe
     */
    public static CallOffsetInfo resolveCallIdx(String func) {
        int offset =  switch (func) {
            case "CallObjectMethodA" -> 36;
            case "CallBooleanMethodA" -> 39;
            case "CallByteMethodA" -> 42;
            case "CallCharMethodA" -> 45;
            case "CallShortMethodA" -> 48;
            case "CallIntMethodA" -> 51;
            case "CallLongMethodA" -> 54;
            case "CallFloatMethodA" -> 57;
            case "CallDoubleMethodA" -> 60;
            case "CallVoidMethodA" -> 63;

            case "CallNonvirtualObjectMethodA" -> 66;
            case "CallNonvirtualBooleanMethodA" -> 69;
            case "CallNonvirtualByteMethodA" -> 72;
            case "CallNonvirtualCharMethodA" -> 75;
            case "CallNonvirtualShortMethodA" -> 77;
            case "CallNonvirtualIntMethodA" -> 81;
            case "CallNonvirtualLongMethodA" -> 84;
            case "CallNonvirtualFloatMethodA" -> 87;
            case "CallNonvirtualDoubleMethodA" -> 90;
            case "CallNonvirtualVoidMethodA" -> 93;

            case "CallStaticObjectMethodA" -> 116;
            case "CallStaticBooleanMethodA" -> 119;
            case "CallStaticByteMethodA" -> 122;
            case "CallStaticCharMethodA" -> 125;
            case "CallStaticShortMethodA" -> 128;
            case "CallStaticIntMethodA" -> 131;
            case "CallStaticLongMethodA" -> 134;
            case "CallStaticFloatMethodA" -> 137;
            case "CallStaticDoubleMethodA" -> 140;
            case "CallStaticVoidMethodA" -> 143;

            default -> throw new IllegalArgumentException("No match found for " + func);
        };

        String ret;

        if (func.contains("Object")) ret = "jobject";
        else if (func.contains("Boolean")) ret = "jboolean";
        else if (func.contains("Byte")) ret = "jbyte";
        else if (func.contains("Char")) ret = "jchar";
        else if (func.contains("Short")) ret = "jshort";
        else if (func.contains("Int")) ret = "jint";
        else if (func.contains("Long")) ret = "jlong";
        else if (func.contains("Float")) ret = "jfloat";
        else if (func.contains("Double")) ret = "jdouble";
        else ret = "void";

        return new CallOffsetInfo(offset, ret);
    }

    public static String callStaticArg(String desc, int sp) {
        int nArgs = Type.getArgumentCount(desc);
        return String.format("stack + %s", (sp + 1) - nArgs); // account for -1 stack initial
    }
    public static boolean isArithmetic(int op) {
        return (op >= IADD && op <= LXOR) ||
                op == ARRAYLENGTH || (op >= LCMP && op <= DCMPG);
    }

    public static boolean isJump(int op) {
        return op == GOTO ||
                op == IFEQ ||
                op == IFNE ||
                op == IFLT ||
                op == IFGE ||
                op == IFGT ||
                op == IFLE ||
                op == IF_ICMPEQ ||
                op == IF_ICMPNE ||
                op == IF_ICMPLT ||
                op == IF_ICMPGE ||
                op == IF_ICMPGT ||
                op == IF_ICMPLE ||
                op == IF_ACMPEQ ||
                op == IF_ACMPNE ||
                op == IFNULL ||
                op == IFNONNULL;
    }

    public static String callArg(String desc, int sp) {
        int nArgs = Type.getArgumentCount(desc);
        return String.format("stack + %s", (sp + 1) - nArgs - 1);
    }

    public static int fromIntConst(InsnNode insn) {
        return switch (insn.getOpcode()) {
            case ICONST_0 -> 0;
            case ICONST_1 -> 1;
            case ICONST_2 -> 2;
            case ICONST_3 -> 3;
            case ICONST_4 -> 4;
            case ICONST_5 -> 5;
            case ICONST_M1 -> -1;
            default -> {
                logger.logln(Level.FATAL, Origin.FUSEBOX, "Unknown opcode passed to constant resolver.");
                panic(1);
                yield 0;
            }
        };
    }

    public static String resolveVar(int var, MethodNode method) {
        boolean isParameter = Internal.isParam(method, var);
        boolean isSelf = Internal.isSelf(method, var);

        if (isParameter) {
            return String.format("p%d", var);
        } else if (isSelf) {
            return "p0";
        } else {
            return String.format("locals[%d]", var);
        }
    }

    public static boolean isNumericConversion(int op) {
        return op == I2L ||
                op == I2F ||
                op == I2D ||
                op == I2B ||
                op == I2C ||
                op == I2S ||
                op == L2I ||
                op == L2F ||
                op == L2D ||
                op == F2I ||
                op == F2L ||
                op == F2D ||
                op == D2I ||
                op == D2L ||
                op == D2F;
    }

    public static boolean isArrayHandling(int op) {
        return (op >= 79 && op <= 86) || (op >= 46 && op <= 53);
    }

    public static String mapTypeFromChar(char c) {
        switch (c) {
            case 'c' -> {
                return "jchar";
            }
            case 'i' -> {
                return "jint";
            }
            case 'j' -> {
                return "jlong";
            }
            case 'f' -> {
                return "jfloat";
            }
            case 'd' -> {
                return "jdouble";
            }
            case 'l' -> {
                return "jobject";
            }
            case 'b' -> {
                return "jbyte";
            }
            case 's' -> {
                return "jshort";
            }
            case 'z' -> {
                return "jboolean";
            }
            default -> {
                logger.logln(Level.WARNING, Origin.FUSEBOX, String.format("Unknown type passed to mapTypeFromChar : %s", new Ansi().c(YELLOW).s(c)));
                return "jobject";
            }
        }
    }
}
