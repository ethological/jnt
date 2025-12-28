package war.metaphor.util.asm;

import lombok.SneakyThrows;
import org.objectweb.asm.Attribute;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.Type;
import org.objectweb.asm.commons.CodeSizeEvaluator;
import org.objectweb.asm.tree.*;
import org.objectweb.asm.tree.analysis.Analyzer;
import org.objectweb.asm.tree.analysis.BasicValue;
import org.objectweb.asm.tree.analysis.Frame;
import org.objectweb.asm.util.Printer;
import org.objectweb.asm.util.Textifier;
import org.objectweb.asm.util.TraceMethodVisitor;
import war.jnt.dash.Level;
import war.jnt.dash.Logger;
import war.jnt.dash.Origin;
import war.metaphor.analysis.SimpleInterpreter;
import war.metaphor.base.ObfuscatorContext;
import war.metaphor.gens.impl.NumberGenerator;
import war.metaphor.gens.impl.SaltGenerator;
import war.metaphor.tree.ClassMethod;
import war.metaphor.tree.JClassNode;
import war.metaphor.util.Descriptor;
import war.metaphor.util.interfaces.IRandom;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.lang.reflect.Modifier;
import java.util.*;
import java.util.stream.Collectors;

public class BytecodeUtil implements Opcodes, IRandom {

    static Printer printer = new Textifier();
    static TraceMethodVisitor methodPrinter = new TraceMethodVisitor(printer);

    public static String toString(AbstractInsnNode insnNode) {
        if (insnNode == null) return "";
        insnNode.accept(methodPrinter);
        StringWriter sw = new StringWriter();
        printer.print(new PrintWriter(sw));
        printer.getText().clear();
        return sw.toString().trim();
    }

    public static boolean rangeMatch(int op, int start, int end) {
        return (op >= start && op <= end);
    }

    public static AbstractInsnNode makeInteger(int number) {
        if (number >= -1 && number <= 5) {
            return new InsnNode(number + 3);
        } else if (number >= -128 && number <= 127) {
            return new IntInsnNode(BIPUSH, number);
        } else if (number >= -32768 && number <= 32767) {
            return new IntInsnNode(SIPUSH, number);
        } else {
            return new LdcInsnNode(number);
        }
    }

    public static boolean isReturning(AbstractInsnNode insn) {
        int op = insn.getOpcode();
        return (op >= IRETURN && op <= ARETURN);
    }

    public static boolean isFloatArithmetic(AbstractInsnNode insn) {
        return switch (insn.getOpcode()) {
            case FADD, FSUB, FDIV, FMUL,
                 FNEG -> true;
                default -> false;
        };
    }

    public static boolean isLongArithmetic(AbstractInsnNode insn) {
        return switch (insn.getOpcode()) {
            case LADD, LSUB, LDIV, LMUL,
                 LXOR, LSHL, LSHR, LAND,
                 LUSHR, LOR, LNEG -> true;
            default -> false;
        };
    }

    public static boolean isDoubleArithmetic(AbstractInsnNode insn) {
        return switch (insn.getOpcode()) {
            case DADD, DSUB, DDIV, DMUL,
                 DNEG -> true;
            default -> false;
        };
    }

    public static boolean isIntArithmetic(AbstractInsnNode insn) {
        return switch (insn.getOpcode()) {
            case IADD, ISUB, IDIV, IMUL,
                 IXOR, ISHL, ISHR, IAND,
                 IOR, IUSHR, INEG -> true;
            default -> false;
        };
    }

    public static boolean isArrayInstruction(AbstractInsnNode insn) {
        return switch (insn.getOpcode()) {
            case IASTORE, IALOAD,
                 // LASTORE, LALOAD,
                 // FASTORE, FALOAD,
                 // DASTORE, DALOAD,
                 BASTORE, BALOAD,
                 CASTORE, CALOAD,
                 SASTORE, SALOAD,
                 ARRAYLENGTH, NEWARRAY -> true;
            default -> false;
        };
    }

    public static boolean isArithmetic(AbstractInsnNode insn) {
        return switch (insn.getOpcode()) {
            case IADD, ISUB, IDIV, IMUL,
                 IXOR, ISHL, ISHR, IAND,
                 IOR, IUSHR, INEG -> true;
            default -> false;
        };
    }

    public static boolean isNumericLdc(LdcInsnNode ldc) {
       return switch (ldc.cst) {
           case String s -> false;
           case Integer n -> true;
           case Float n -> true;
           case Double n -> true;
           case Long n -> true;
           case null, default -> false;
        };
    }

    public static int makeLoad(Type type) {
        return switch (type.getSort()) {
            case Type.INT, Type.SHORT, Type.BOOLEAN, Type.BYTE, Type.CHAR -> ILOAD;
            case Type.FLOAT -> FLOAD;
            case Type.LONG -> LLOAD;
            case Type.DOUBLE -> DLOAD;
            default -> ALOAD;
        };
    }

    public static boolean skip(int op, int size) {
        if (op >= INEG && op <= DNEG) {
            return size < 2;
        } else {
            return size == 0;
        }
    }

    public static boolean isConstant(AbstractInsnNode ain) {
        return isInteger(ain) || isFloat(ain) || isDouble(ain) || isLong(ain) || isString(ain);
    }

    private static String fromOperation(int op) {
        return switch (op) {
            case 0 -> "add";
            case 1 -> "sub";
            case 2 -> "div";
            case 3 -> "mul";
            case 4 -> "xor";
            case 5 -> "shl";
            case 6 -> "shr";
            case 7 -> "and";
            case 8 -> "or";
            case 9 -> "ushr";
            case 10 -> "neg";
            default -> "unknown";
        };
    }

    public static boolean isLoad(AbstractInsnNode insn) {
        int op = insn.getOpcode();
        return (op >= ILOAD && op <= ALOAD);
    }

    public static boolean isStore(AbstractInsnNode insn) {
        int op = insn.getOpcode();
        return (op >= ISTORE && op <= ASTORE);
    }

    public static int toEngineCode(AbstractInsnNode insn) {
        return switch (insn.getOpcode()) {
            case IADD -> 0;
            case ISUB -> 1;
            case IDIV -> 2;
            case IMUL -> 3;
            case IXOR -> 4;
            case ISHL -> 5;
            case ISHR -> 6;
            case IAND -> 7;
            case IOR -> 8;
            case IUSHR -> 9;
            case INEG -> 10;
            default -> -1;
        };
    }

    public static boolean isInteger(AbstractInsnNode ain) {
        if (ain instanceof LdcInsnNode ldc) {
            return ldc.cst instanceof Integer;
        }

        int opcode = ain.getOpcode();

        return (opcode >= ICONST_M1 && opcode <= ICONST_5) || opcode == BIPUSH || opcode == SIPUSH;
    }

    public static boolean isFloat(AbstractInsnNode ain) {
        if (ain instanceof LdcInsnNode ldc) {
            return ldc.cst instanceof Float;
        }

        int opcode = ain.getOpcode();
        return (opcode >= FCONST_0 && opcode <= FCONST_2);
    }

    public static boolean isDouble(AbstractInsnNode ain) {
        if (ain instanceof LdcInsnNode ldc) {
            return ldc.cst instanceof Double;
        }

        int opcode = ain.getOpcode();
        return opcode == DCONST_0 || opcode == DCONST_1;
    }

    public static boolean isLong(AbstractInsnNode ain) {
        if (ain instanceof LdcInsnNode ldc) {
            return ldc.cst instanceof Long;
        }

        int opcode = ain.getOpcode();
        return opcode == LCONST_0 || opcode == LCONST_1;
    }

    public static int getInteger(AbstractInsnNode ain) {
        if (ain instanceof LdcInsnNode ldc && ldc.cst instanceof Integer) {
            return (int) ldc.cst;
        }

        if (ain instanceof IntInsnNode iin) {
            return iin.operand;
        }

        return switch (ain.getOpcode()) {
            case ICONST_M1 -> -1;
            case ICONST_0 -> 0;
            case ICONST_1 -> 1;
            case ICONST_2 -> 2;
            case ICONST_3 -> 3;
            case ICONST_4 -> 4;
            case ICONST_5 -> 5;
            default -> throw new IllegalStateException("Unexpected value: " + ain.getOpcode());
        };
    }

    public static float getFloat(AbstractInsnNode ain) {
        int opcode = ain.getOpcode();

        if (ain instanceof LdcInsnNode ldc) {
            return (float) ldc.cst;
        } else if (opcode == FCONST_0) {
            return 0f;
        } else if (opcode == FCONST_1) {
            return 1f;
        } else if (opcode == FCONST_2) {
            return 2f;
        }
        throw new RuntimeException("Failed to extract float.");
    }

    public static double getDouble(AbstractInsnNode ain) {
        int opcode = ain.getOpcode();

        if (ain instanceof LdcInsnNode ldc) {
            return (double) ldc.cst;
        } else if (opcode == DCONST_0) {
            return 0D;
        } else if (opcode == DCONST_1) {
            return 1D;
        }
        throw new RuntimeException("Failed to extract double.");
    }

    public static long getLong(AbstractInsnNode ain) {
        int opcode = ain.getOpcode();

        if (ain instanceof LdcInsnNode ldc) {
            return (long) ldc.cst;
        } else if (opcode == LCONST_0) {
            return 0L;
        } else if (opcode == LCONST_1) {
            return 1L;
        }
        throw new RuntimeException("Failed to extract long.");
    }

    public static String getString(AbstractInsnNode ain) {
        if (ain instanceof LdcInsnNode ldc) {
            if (ldc.cst instanceof String) {
                return (String) ldc.cst;
            }
        }
        return null;
    }

    // why did i even make this
    public static boolean isIntStore(int op) {
        return op == ISTORE;
    }

    public static boolean isIntLoad(int op) {
        return op == ILOAD;
    }

    public static LdcInsnNode makeObj(Object obj) {
        return new LdcInsnNode(obj);
    }

    public static AbstractInsnNode makeFloat(float value) {
        if (value == 0f) return new InsnNode(FCONST_0);
        else if (value == 1f) return new InsnNode(FCONST_1);
        else if (value == 2f) return new InsnNode(FCONST_2);
        else return makeObj(value);
    }

    public static AbstractInsnNode makeDouble(double value) {
        if (value == 0D) return new InsnNode(DCONST_0);
        else if (value == 1D) return new InsnNode(DCONST_1);
        else return makeObj(value);
    }

    public static AbstractInsnNode makeLong(long value) {
        if (value == 0L) return new InsnNode(LCONST_0);
        else if (value == 1L) return new InsnNode(LCONST_1);
        else return makeObj(value);
    }

    public static String getOperatorDesc(InsnNode operatorInsn) {
        var desc = "";
        var opcode = operatorInsn.getOpcode();

        if (opcode >= Opcodes.IADD && opcode <= Opcodes.DREM) {
            switch (opcode % 4) {
                case 0 -> desc = "I";
                case 1 -> desc = "J";
                case 2 -> desc = "F";
                case 3 -> desc = "D";
            }
        } else if (opcode >= Opcodes.ISHL && opcode <= Opcodes.LXOR) {
            switch (opcode % 2) {
                case 0 -> desc = "I";
                case 1 -> desc = "J";
            }
        }
        return desc;
    }

    public static boolean isString(AbstractInsnNode ain) {
        if (ain instanceof LdcInsnNode ldc) {
            return ldc.cst instanceof String;
        }
        return false;
    }

    /**
     * @param cst
     * @return Width of constant
     */
    public static int constantWidth(int cst) {
        return cst <= Short.MAX_VALUE && cst >= Short.MIN_VALUE ? 2 :
                cst <= Integer.MAX_VALUE && cst >= Integer.MIN_VALUE ? 4 :
                 cst <= Byte.MAX_VALUE && cst >= Byte.MIN_VALUE ? 1 : 4;
    }

    public static VarInsnNode getVarNode(String desc, int varIndex) {
        return switch (desc) {
            case "Z", "B", "C", "S", "I" -> new VarInsnNode(Opcodes.ILOAD, varIndex);
            case "J" -> new VarInsnNode(Opcodes.LLOAD, varIndex);
            case "F" -> new VarInsnNode(Opcodes.FLOAD, varIndex);
            case "D" -> new VarInsnNode(Opcodes.DLOAD, varIndex);
            default -> new VarInsnNode(Opcodes.ALOAD, varIndex);
        };
    }

    public static InsnNode getReturnNode(String desc) {
        return switch (desc) {
            case "Z", "B", "C", "S", "I" -> new InsnNode(Opcodes.IRETURN);
            case "J" -> new InsnNode(Opcodes.LRETURN);
            case "F" -> new InsnNode(Opcodes.FRETURN);
            case "D" -> new InsnNode(Opcodes.DRETURN);
            case "V" -> new InsnNode(Opcodes.RETURN);
            default -> new InsnNode(Opcodes.ARETURN);
        };
    }

    public static boolean isExitBlock(AbstractInsnNode insn) {
        if (insn instanceof JumpInsnNode || insn instanceof TableSwitchInsnNode
                || insn instanceof LookupSwitchInsnNode || (insn.getOpcode() >= IRETURN && insn.getOpcode() <= RETURN)
                || insn.getOpcode() == ATHROW)
            return !(insn instanceof JumpInsnNode) || insn.getOpcode() == GOTO;
        return false;
    }

    public static List<AbstractInsnNode> getBlock(MethodNode method, LabelNode label) {
        List<AbstractInsnNode> instructions = new ArrayList<>();
        boolean found = false;
        for (int i = label.index; i < method.instructions.size; i++) {
            AbstractInsnNode instruction = method.instructions.get(i);
            if (instruction instanceof LabelNode) {
                if (instruction == label) {
                    found = true;
                } else if (found) {
                    break;
                }
            } else if (found) {
                instructions.add(instruction);
            }
        }
        return instructions;
    }

    @SneakyThrows
    public static Map<AbstractInsnNode, Frame<BasicValue>> analyzeAndComputeMaxes(ClassNode klass, MethodNode method) {
        Map<AbstractInsnNode, Frame<BasicValue>> frames = new HashMap<>();
        Analyzer<BasicValue> analyzer = new Analyzer<>(new SimpleInterpreter(method));
        try {
            analyzer.analyzeAndComputeMaxs(klass.name, method);
        } catch (Exception exception) {
            exception.printStackTrace(System.err);
            Logger.INSTANCE.log(Level.WARNING, Origin.METAPHOR, String.format("Failed to analyze %s#%s.%s\n", klass.name, method.name, method.desc));
            throw new RuntimeException("Failure!");
        }
        Frame<BasicValue>[] framesArray = analyzer.getFrames();
        if (framesArray == null) return frames;
        if (framesArray.length == 0) return frames;
        for (AbstractInsnNode instruction : method.instructions) {
            Frame<BasicValue> frame = framesArray[instruction.index];
            frames.put(instruction, frame);
        }
        return frames;
    }

    public static void fixLookupSwitch(LookupSwitchInsnNode lookupSwitch) {

        int previousKey = Integer.MIN_VALUE;
        boolean sorted = true;
        for (int key : lookupSwitch.keys) {
            if (key < previousKey) {
                sorted = false;
                break;
            }
            previousKey = key;
        }
        if (sorted) return;

        Map<Integer, LabelNode> labels = new HashMap<>();
        for (int i = 0; i < lookupSwitch.keys.size(); i++) {
            labels.put(lookupSwitch.keys.get(i), lookupSwitch.labels.get(i));
        }
        labels = labels.entrySet().stream().sorted(Map.Entry.comparingByKey()).collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue, (e1, e2) -> e1, LinkedHashMap::new));
        lookupSwitch.keys.clear();
        lookupSwitch.labels.clear();
        for (Map.Entry<Integer, LabelNode> entry : labels.entrySet()) {
            lookupSwitch.keys.add(entry.getKey());
            lookupSwitch.labels.add(entry.getValue());
        }
    }

    public static Type getRandomType() {
        int val = RANDOM.nextInt(6);
        return switch (val) {
            case 0 -> Type.BOOLEAN_TYPE;
            case 1 -> Type.CHAR_TYPE;
            case 2 -> Type.BYTE_TYPE;
            case 3 -> Type.SHORT_TYPE;
            case 4 -> Type.INT_TYPE;
            case 5 -> Type.FLOAT_TYPE;
            default -> throw new IllegalStateException("Unexpected value: " + val);
        };
    }

    public static VarInsnNode makeStore(char c, int idx) {
        int opcode = switch (c) {
            case 'I', 'C', 'Z', 'S', 'B' -> Opcodes.ISTORE;
            case 'J' -> Opcodes.LSTORE;
            case 'F' -> Opcodes.FSTORE;
            case 'D' -> Opcodes.DSTORE;
            case 'L', '[' -> Opcodes.ASTORE;
            default -> throw new IllegalStateException("Unexpected value: " + c);
        };

        return new VarInsnNode(opcode, idx);
    }

    public static void addInstructions(InsnList list, List<AbstractInsnNode> nodes) {
        if (list.lastInsn != null) {
            addInstructions(list, list.lastInsn, nodes);
        } else {
            AbstractInsnNode start = new InsnNode(NOP);
            list.add(start);
            addInstructions(list, start, nodes);
            list.remove(start);
        }
    }

    public static void addInstructions(InsnList list, AbstractInsnNode prev, List<AbstractInsnNode> nodes) {
        if (nodes.isEmpty()) return;
        list.size += nodes.size();

        AbstractInsnNode firstNode = nodes.get(0);
        AbstractInsnNode lastNode = nodes.get(nodes.size() - 1);

        for (AbstractInsnNode node : nodes) {
            if (node.previousInsn != null) {
                AbstractInsnNode _prev = node.previousInsn;
                _prev.nextInsn = node.nextInsn;
            }
            if (node.nextInsn != null) {
                AbstractInsnNode _next = node.nextInsn;
                _next.previousInsn = node.previousInsn;
            }
        }

        for (int i = 0; i < nodes.size(); i++) {
            AbstractInsnNode next = i + 1 < nodes.size() ? nodes.get(i + 1) : prev.nextInsn;
            AbstractInsnNode node = nodes.get(i);
            node.nextInsn = next;
            node.previousInsn = i == 0 ? prev : nodes.get(i - 1);
        }

        if (prev.nextInsn != null) {
            prev.nextInsn.previousInsn = lastNode;
        } else {
            list.lastInsn = lastNode;
        }
        prev.nextInsn = firstNode;

        list.cache = null;
    }

    public static ClassMethod getMethodNode(String owner, String name, String desc) {
        JClassNode classNode = ObfuscatorContext.INSTANCE.loadClass(owner);
        if (classNode == null) return null;
        return getMethodNode(classNode, name, desc);
    }

    public static ClassMethod getMethodNode(JClassNode owner, String name, String desc) {
        MethodNode found = null;
        for (MethodNode method : owner.methods) {
            if (method.name.equals(name) && method.desc.equals(desc)) {
                found = method;
                break;
            }
        }
        if (found != null)
            return ClassMethod.of(owner, found);
        for (JClassNode parent : owner.getParents()) {
            ClassMethod method = getMethodNode(parent, name, desc);
            if (method != null)
                return method;
        }
        return null;
    }

    private static int getVarSize(int opcode) {
        return switch (opcode) {
            case LLOAD, LSTORE, DLOAD, DSTORE -> 2;
            default -> 1;
        };
    }

    public static void computeMaxLocals(MethodNode method) {
        int maxLocals;

        Descriptor desc = Descriptor.of(method.desc);
        maxLocals = desc.getLast(Modifier.isStatic(method.access));

        for (AbstractInsnNode instruction : method.instructions) {
            if (instruction instanceof VarInsnNode varInsn) {
                maxLocals = Math.max(maxLocals, varInsn.var + getVarSize(varInsn.getOpcode()));
            } else if (instruction instanceof IincInsnNode iincInsn) {
                maxLocals = Math.max(maxLocals, iincInsn.var + 1);
            }
        }

        method.maxLocals = maxLocals;
    }

    public static int getMethodSize(MethodNode method) {
        CodeSizeEvaluator cse = new CodeSizeEvaluator(null);
        method.accept(cse);
        return cse.getMaxSize();
    }

    public static boolean hasSpace(MethodNode method, InsnList list) {
        MethodNode dummy = new MethodNode();
        dummy.instructions = list;
        int size0 = getMethodSize(method);
        int size1 = getMethodSize(dummy);
        int difference = 65535 - size0 - size1;
        return difference >= 10000;
    }

    public static boolean hasSpace(MethodNode method, int space) {
        int size = getMethodSize(method);
        int difference = 65535 - size;
        return difference >= space;
    }

    public static boolean hasSpace(MethodNode method) {
        return hasSpace(method, 10000);
    }

    public static MethodNode clone(MethodNode method) {
        MethodNode cloned = new MethodNode(method.access, method.name, method.desc, method.signature, method.exceptions.toArray(new String[0]));
        Map<LabelNode, LabelNode> labels = new HashMap<>();
        for (AbstractInsnNode instruction : method.instructions) {
            if (instruction instanceof LabelNode) {
                labels.put((LabelNode) instruction, new LabelNode());
            }
        }
        for (AbstractInsnNode instruction : method.instructions)
            cloned.instructions.add(instruction.clone(labels));
        for (TryCatchBlockNode tryCatchBlock : method.tryCatchBlocks)
            cloned.tryCatchBlocks.add(new TryCatchBlockNode(labels.get(tryCatchBlock.start), labels.get(tryCatchBlock.end), labels.get(tryCatchBlock.handler), tryCatchBlock.type));
        if (method.attrs != null)
            for (Attribute attr : method.attrs)
                cloned.visitAttribute(attr);
        if (method.annotationDefault != null)
            cloned.annotationDefault = method.annotationDefault;
        if (method.visibleAnnotations != null)
            for (AnnotationNode annotation : method.visibleAnnotations) {
                AnnotationNode clone = new AnnotationNode(annotation.desc);
                clone.values = annotation.values == null ? null : new ArrayList<>(annotation.values);
                if (cloned.visibleAnnotations == null)
                    cloned.visibleAnnotations = new ArrayList<>();
                cloned.visibleAnnotations.add(clone);
            }
        if (method.invisibleAnnotations != null)
            for (AnnotationNode annotation : method.invisibleAnnotations) {
                AnnotationNode clone = new AnnotationNode(annotation.desc);
                clone.values = annotation.values == null ? null : new ArrayList<>(annotation.values);
                if (cloned.invisibleAnnotations == null)
                    cloned.invisibleAnnotations = new ArrayList<>();
                cloned.invisibleAnnotations.add(clone);
            }
        if (method.visibleParameterAnnotations != null) {
            cloned.visibleParameterAnnotations = new ArrayList[method.visibleParameterAnnotations.length];
            for (int i = 0; i < method.visibleParameterAnnotations.length; i++) {
                List<AnnotationNode> annotations = method.visibleParameterAnnotations[i];
                if (annotations == null) continue;
                List<AnnotationNode> clone = new ArrayList<>();
                for (AnnotationNode annotation : annotations) {
                    AnnotationNode cloneAnnotation = new AnnotationNode(annotation.desc);
                    cloneAnnotation.values = annotation.values == null ? null : new ArrayList<>(annotation.values);
                    clone.add(cloneAnnotation);
                }
                if (cloned.visibleParameterAnnotations == null)
                    cloned.visibleParameterAnnotations = new List[method.visibleParameterAnnotations.length];
                cloned.visibleParameterAnnotations[i] = clone;
            }
        }
        if (method.invisibleParameterAnnotations != null) {
            cloned.invisibleParameterAnnotations = new ArrayList[method.invisibleParameterAnnotations.length];
            for (int i = 0; i < method.invisibleParameterAnnotations.length; i++) {
                List<AnnotationNode> annotations = method.invisibleParameterAnnotations[i];
                if (annotations == null) continue;
                List<AnnotationNode> clone = new ArrayList<>();
                for (AnnotationNode annotation : annotations) {
                    AnnotationNode cloneAnnotation = new AnnotationNode(annotation.desc);
                    cloneAnnotation.values = annotation.values == null ? null : new ArrayList<>(annotation.values);
                    clone.add(cloneAnnotation);
                }
                cloned.invisibleParameterAnnotations[i] = clone;
            }
        }
        if (method.visibleTypeAnnotations != null)
            for (TypeAnnotationNode typeAnnotation : method.visibleTypeAnnotations) {
                TypeAnnotationNode clone = new TypeAnnotationNode(typeAnnotation.typeRef, typeAnnotation.typePath, typeAnnotation.desc);
                clone.values = typeAnnotation.values == null ? null : new ArrayList<>(typeAnnotation.values);
                if (cloned.visibleTypeAnnotations == null)
                    cloned.visibleTypeAnnotations = new ArrayList<>();
                cloned.visibleTypeAnnotations.add(clone);
            }
        if (method.invisibleTypeAnnotations != null)
            for (TypeAnnotationNode typeAnnotation : method.invisibleTypeAnnotations) {
                TypeAnnotationNode clone = new TypeAnnotationNode(typeAnnotation.typeRef, typeAnnotation.typePath, typeAnnotation.desc);
                clone.values = typeAnnotation.values == null ? null : new ArrayList<>(typeAnnotation.values);
                if (cloned.invisibleTypeAnnotations == null)
                    cloned.invisibleTypeAnnotations = new ArrayList<>();
                cloned.invisibleTypeAnnotations.add(clone);
            }
        if (method.localVariables != null)
            for (LocalVariableNode localVariable : method.localVariables)
                cloned.localVariables.add(new LocalVariableNode(localVariable.name, localVariable.desc, localVariable.signature, labels.get(localVariable.start), labels.get(localVariable.end), localVariable.index));

        return cloned;
    }

    public static InsnList generateInteger(int i) {
        return generateNumber(i, Type.INT_TYPE);
    }

    public static InsnList generateLong(long i) {
        return generateNumber(i, Type.LONG_TYPE);
    }

    public static InsnList generateNumber(Number i, Type type) {
        NumberGenerator generator = new NumberGenerator(i, type);
        generator.setup();
        return generator.generate(3, 3);
    }

    public static InsnList generateSeeded(int var, Number value, Number seed) {
        return generateSeeded(var, value, seed, Type.INT_TYPE, Type.INT_TYPE);
    }

    public static InsnList generateSeeded(int var, Number value, Number seed, Type type, Type varType) {
        switch (varType.getSort()) {
            case Type.CHAR:
            case Type.BYTE:
            case Type.SHORT:
            case Type.INT:
                varType = Type.INT_TYPE;
                break;
        }
        MethodNode method = new MethodNode();
        method.maxLocals = var + 1;
        SaltGenerator gen = new SaltGenerator(var, seed, value, type, varType);
        gen.setup(new JClassNode(), method);
        return gen.generate(3, 3);
    }

    public static int nextIntUnique(Collection<Integer> values) {
        int next;
        do {
            next = RANDOM.nextInt();
        } while (values.contains(next));
        return next;
    }

    public static void replaceLabelNode(AbstractInsnNode jump, LabelNode label, LabelNode replaceLabel) {
        if (jump instanceof JumpInsnNode jumpInsn) {
            if (jumpInsn.label == label) {
                jumpInsn.label = replaceLabel;
            }
        } else if (jump instanceof LookupSwitchInsnNode lookupSwitch) {
            if (lookupSwitch.dflt == label) {
                lookupSwitch.dflt = replaceLabel;
            }
            for (int i = 0; i < lookupSwitch.labels.size(); i++) {
                LabelNode node = lookupSwitch.labels.get(i);
                if (node == label) {
                    lookupSwitch.labels.set(i, replaceLabel);
                }
            }
        } else if (jump instanceof TableSwitchInsnNode tableSwitch) {
            if (tableSwitch.dflt == label) {
                tableSwitch.dflt = replaceLabel;
            }
            for (int i = 0; i < tableSwitch.labels.size(); i++) {
                LabelNode node = tableSwitch.labels.get(i);
                if (node == label) {
                    tableSwitch.labels.set(i, replaceLabel);
                }
            }
        }
    }

    public static int leeway(MethodNode methodNode) {
        CodeSizeEvaluator sizeEvaluator = new CodeSizeEvaluator(null);
        methodNode.accept(sizeEvaluator);
        return 65536 - sizeEvaluator.getMaxSize();
    }

    public static void translateConcatenation(MethodNode methodNode) {
        final char STACK_ARG_CONSTANT = '\u0001';
        final char BSM_ARG_CONSTANT = '\u0002';

        for (AbstractInsnNode ain : methodNode.instructions.toArray()) {
            if (ain.getOpcode() == INVOKEDYNAMIC) {
                final InvokeDynamicInsnNode indy = (InvokeDynamicInsnNode) ain;

                if (indy.bsm.getOwner().equals("java/lang/invoke/StringConcatFactory")
                        && indy.bsm.getName().equals("makeConcatWithConstants")) {
                    final String pattern = (String) indy.bsmArgs[0];

                    final Type[] stackArgs = Type.getArgumentTypes(indy.desc);
                    final Object[] bsmArgs = Arrays.copyOfRange(indy.bsmArgs, 1, indy.bsmArgs.length);

                    int stackArgsCount = 0;
                    for (char c : pattern.toCharArray()) {
                        if (c == STACK_ARG_CONSTANT)
                            stackArgsCount++;
                    }

                    int bsmArgsCount = 0;
                    for (char c : pattern.toCharArray()) {
                        if (c == BSM_ARG_CONSTANT)
                            bsmArgsCount++;
                    }

                    if (stackArgs.length != stackArgsCount)
                        continue;

                    if (bsmArgs.length != bsmArgsCount)
                        continue;

                    int freeVarIndex = methodNode.maxLocals++;
                    final int[] stackIndices = new int[stackArgsCount];

                    for (int i = 0; i < stackArgs.length; i++) {
                        stackIndices[i] = freeVarIndex;
                        freeVarIndex += stackArgs[i].getSize();
                    }

                    for (int i = stackIndices.length - 1; i >= 0; i--) {
                        methodNode.instructions.insertBefore(indy, new VarInsnNode(stackArgs[i].getOpcode(ISTORE), stackIndices[i]));
                    }

                    final InsnList list = new InsnList();
                    final char[] arr = pattern.toCharArray();

                    int stackArgsIndex = 0;
                    int bsmArgsIndex = 0;

                    StringBuilder builder = new StringBuilder();
                    list.add(new TypeInsnNode(NEW, "java/lang/StringBuilder"));
                    list.add(new InsnNode(DUP));
                    list.add(new MethodInsnNode(INVOKESPECIAL, "java/lang/StringBuilder", "<init>", "()V"));

                    for (char c : arr) {
                        if (c == STACK_ARG_CONSTANT) {
                            if (!builder.isEmpty()) {
                                list.add(new LdcInsnNode(builder.toString()));
                                list.add(new MethodInsnNode(INVOKEVIRTUAL, "java/lang/StringBuilder", "append", "(Ljava/lang/String;)Ljava/lang/StringBuilder;"));
                                builder = new StringBuilder();
                            }

                            final Type stackArg = stackArgs[stackArgsIndex++];
                            final int stackIndex = stackIndices[stackArgsIndex - 1];

                            if (stackArg.getSort() == Type.OBJECT) {
                                list.add(new VarInsnNode(ALOAD, stackIndex));
                                list.add(new MethodInsnNode(INVOKEVIRTUAL, "java/lang/StringBuilder", "append", "(Ljava/lang/Object;)Ljava/lang/StringBuilder;"));
                            } else if (stackArg.getSort() == Type.ARRAY) {
                                list.add(new VarInsnNode(ALOAD, stackIndex));
                                list.add(new MethodInsnNode(INVOKESTATIC, "java/util/Arrays", "toString", "([Ljava/lang/Object;)Ljava/lang/String;"));
                                list.add(new MethodInsnNode(INVOKEVIRTUAL, "java/lang/StringBuilder", "append", "(Ljava/lang/String;)Ljava/lang/StringBuilder;"));
                            } else {
                                list.add(new VarInsnNode(stackArg.getOpcode(ILOAD), stackIndex));
                                String adaptedDescriptor = stackArg.getDescriptor();
                                if (adaptedDescriptor.equals("B")
                                        || adaptedDescriptor.equals("S")) {
                                    adaptedDescriptor = "I";
                                }
                                list.add(new MethodInsnNode(INVOKEVIRTUAL, "java/lang/StringBuilder", "append", "(" + adaptedDescriptor + ")Ljava/lang/StringBuilder;"));
                            }
                        } else if (c == BSM_ARG_CONSTANT) {
                            list.add(new LdcInsnNode(bsmArgs[bsmArgsIndex++]));
                            list.add(new MethodInsnNode(INVOKEVIRTUAL, "java/lang/StringBuilder", "append", "(Ljava/lang/Object;)Ljava/lang/StringBuilder;"));
                        } else {
                            builder.append(c);
                        }
                    }

                    if (!builder.isEmpty()) {
                        list.add(new LdcInsnNode(builder.toString()));
                        list.add(new MethodInsnNode(INVOKEVIRTUAL, "java/lang/StringBuilder", "append", "(Ljava/lang/String;)Ljava/lang/StringBuilder;"));
                    }

                    list.add(new MethodInsnNode(INVOKEVIRTUAL, "java/lang/StringBuilder", "toString", "()Ljava/lang/String;"));

                    methodNode.instructions.insertBefore(indy, list);
                    methodNode.instructions.remove(indy);
                }
            }
        }
    }

    public static InsnList boxType(Type type) {
        InsnList list = new InsnList();
        if (type == null)
            return list;
        switch (type.getSort()) {
            case Type.BOOLEAN:
                list.add(new MethodInsnNode(INVOKESTATIC, "java/lang/Boolean", "valueOf", "(Z)Ljava/lang/Boolean;", false));
                break;
            case Type.BYTE:
            case Type.CHAR:
            case Type.SHORT:
            case Type.INT:
                list.add(new MethodInsnNode(INVOKESTATIC, "java/lang/Integer", "valueOf", "(I)Ljava/lang/Integer;", false));
                break;
            case Type.LONG:
                list.add(new MethodInsnNode(INVOKESTATIC, "java/lang/Long", "valueOf", "(J)Ljava/lang/Long;", false));
                break;
            case Type.FLOAT:
                list.add(new MethodInsnNode(INVOKESTATIC, "java/lang/Float", "valueOf", "(F)Ljava/lang/Float;", false));
                break;
            case Type.DOUBLE:
                list.add(new MethodInsnNode(INVOKESTATIC, "java/lang/Double", "valueOf", "(D)Ljava/lang/Double;", false));
                break;
        }
        return list;
    }

    public static InsnList unboxType(Type type) {
        InsnList list = new InsnList();
        if (type == null)
            return list;
        switch (type.getSort()) {
            case Type.BOOLEAN:
                list.add(new TypeInsnNode(CHECKCAST, "java/lang/Boolean"));
                list.add(new MethodInsnNode(INVOKEVIRTUAL, "java/lang/Boolean", "booleanValue", "()Z", false));
                break;
            case Type.BYTE:
            case Type.CHAR:
            case Type.SHORT:
            case Type.INT:
                list.add(new TypeInsnNode(CHECKCAST, "java/lang/Integer"));
                list.add(new MethodInsnNode(INVOKEVIRTUAL, "java/lang/Integer", "intValue", "()I", false));
                break;
            case Type.LONG:
                list.add(new TypeInsnNode(CHECKCAST, "java/lang/Long"));
                list.add(new MethodInsnNode(INVOKEVIRTUAL, "java/lang/Long", "longValue", "()J", false));
                break;
            case Type.FLOAT:
                list.add(new TypeInsnNode(CHECKCAST, "java/lang/Float"));
                list.add(new MethodInsnNode(INVOKEVIRTUAL, "java/lang/Float", "floatValue", "()F", false));
                break;
            case Type.DOUBLE:
                list.add(new TypeInsnNode(CHECKCAST, "java/lang/Double"));
                list.add(new MethodInsnNode(INVOKEVIRTUAL, "java/lang/Double", "doubleValue", "()D", false));
                break;
            case Type.OBJECT:
                if (type == SimpleInterpreter.NULL_TYPE) break;
                list.add(new TypeInsnNode(CHECKCAST, type.getInternalName()));
                break;
            case Type.ARRAY:
                list.add(new TypeInsnNode(CHECKCAST, type.getDescriptor()));
                break;
        }
        return list;
    }
}
