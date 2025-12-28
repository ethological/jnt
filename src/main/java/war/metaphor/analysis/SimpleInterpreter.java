package war.metaphor.analysis;

import lombok.SneakyThrows;
import org.objectweb.asm.*;
import org.objectweb.asm.tree.*;
import org.objectweb.asm.tree.analysis.AnalyzerException;
import org.objectweb.asm.tree.analysis.BasicInterpreter;
import org.objectweb.asm.tree.analysis.BasicValue;
import org.objectweb.asm.tree.analysis.Frame;
import war.metaphor.analysis.values.*;
import war.metaphor.base.ObfuscatorContext;
import war.metaphor.tree.Hierarchy;
import war.metaphor.tree.JClassNode;
import war.metaphor.util.asm.TypeChecker;
import war.metaphor.util.asm.TypeResolver;
import war.metaphor.util.asm.TypeUtil;

import java.util.List;

/**
 * @author ssheera
 * Some stuff was taken from SimAnalyser by Col-E
 */
public class SimpleInterpreter extends BasicInterpreter implements Opcodes {

    public static final Type NULL_TYPE = TypeUtil.OBJECT_TYPE;

    private final TypeChecker typeChecker;
    private final TypeResolver typeResolver;

    private final MethodNode method;

    public SimpleInterpreter(MethodNode method) {
        super(ASM9);
        this.method = method;
        this.typeChecker = createTypeChecker();
        this.typeResolver = createTypeResolver();
    }

    @Override
    public BasicValue newEmptyValue(int local) {
        return UninitializedValue.INSTANCE;
    }

    @Override
    public BasicValue newValue(Type type) {
        if (type == null)
            return UninitializedValue.INSTANCE;
        else if (type == Type.VOID_TYPE)
            return null;
        else if (type.getSort() <= Type.DOUBLE)
            return PrimitiveValue.of(type);
        return VirtualValue.of(type);
    }

    @Override
    public BasicValue newExceptionValue(TryCatchBlockNode tryCatchBlockNode, Frame<BasicValue> handlerFrame, Type exceptionType) {
        return ExceptionValue.of(exceptionType);
    }

    @Override
    public BasicValue copyOperation(AbstractInsnNode insn, BasicValue value) {
        SimpleValue v = (SimpleValue) value;
        return v.copy();
    }

    @Override
    public BasicValue newOperation(AbstractInsnNode insn) throws AnalyzerException {
        switch (insn.getOpcode()) {
            case ACONST_NULL:
                return NullValue.of(NULL_TYPE);
            case ICONST_M1:
            case ICONST_0:
            case ICONST_1:
            case ICONST_2:
            case ICONST_3:
            case ICONST_4:
            case ICONST_5:
            case BIPUSH:
            case SIPUSH:
                return newValue(Type.INT_TYPE);
            case LCONST_0:
            case LCONST_1:
                return newValue(Type.LONG_TYPE);
            case FCONST_0:
            case FCONST_1:
            case FCONST_2:
                return newValue(Type.FLOAT_TYPE);
            case DCONST_0:
            case DCONST_1:
                return newValue(Type.DOUBLE_TYPE);
            case LDC:
                Object value = ((LdcInsnNode) insn).cst;
                switch (value) {
                    case Integer _ -> {
                        return newValue(Type.INT_TYPE);
                    }
                    case Float _ -> {
                        return newValue(Type.FLOAT_TYPE);
                    }
                    case Long _ -> {
                        return newValue(Type.LONG_TYPE);
                    }
                    case Double _ -> {
                        return newValue(Type.DOUBLE_TYPE);
                    }
                    case String _ -> {
                        return MemoryValue.of(Type.getObjectType("java/lang/String"), value);
                    }
                    case Type type -> {
                        int sort = type.getSort();
                        if (sort != Type.OBJECT && sort != Type.ARRAY) {
                            if (sort == Type.METHOD) {
                                return newValue(Type.getObjectType("java/lang/invoke/MethodType"));
                            }
                            throw new AnalyzerException(insn, "Illegal LDC value " + value);
                        }
                        return MemoryValue.of(Type.getObjectType("java/lang/Class"), value);
                    }
                    case Handle _ -> {
                        return newValue(Type.getObjectType("java/lang/invoke/MethodHandle"));
                    }
                    case ConstantDynamic constantDynamic -> {
                        return newValue(Type.getType(constantDynamic.getDescriptor()));
                    }
                    case null, default -> throw new AnalyzerException(insn, "Illegal LDC value " + value);
                }
            case JSR:
                return new ReturnAddressValue();
            case GETSTATIC:
                return newValue(Type.getType(((FieldInsnNode) insn).desc));
            case NEW:
                return NewInstanceValue.of(Type.getObjectType(((TypeInsnNode) insn).desc));
            default:
                throw new AssertionError();
        }
    }

    @Override
    @SneakyThrows
    public BasicValue binaryOperation(AbstractInsnNode insn, BasicValue value1, BasicValue value2) {
        switch (insn.getOpcode()) {
            case 97:
            case 101:
            case 105:
            case 109:
            case 113:
            case 121:
            case 123:
            case 125:
            case 127:
            case 129:
            case 131:
            case LALOAD:
                return newValue(Type.LONG_TYPE);
            case 98:
            case 102:
            case 106:
            case 110:
            case 114:
            case FALOAD:
                return newValue(Type.FLOAT_TYPE);
            case 99:
            case 103:
            case 107:
            case 111:
            case 115:
            case DALOAD:
                return newValue(Type.DOUBLE_TYPE);
            case AALOAD:
                if (value1.getType() == null || value1.getType().getSort() == Type.OBJECT)
                    return newValue(TypeUtil.OBJECT_TYPE);
                else
                    return newValue(Type.getType(value1.getType().getDescriptor().substring(1)));
            case IALOAD:
            case BALOAD:
            case CALOAD:
            case SALOAD:
            case 96:
            case 100:
            case 104:
            case 108:
            case 112:
            case 120:
            case 122:
            case 124:
            case 126:
            case 128:
            case 130:
            case 148:
            case 149:
            case 150:
            case 151:
            case 152:
                return newValue(Type.INT_TYPE);
            case IF_ICMPEQ:
            case IF_ICMPNE:
            case IF_ICMPLT:
            case IF_ICMPGE:
            case IF_ICMPGT:
            case IF_ICMPLE:
            case IF_ACMPEQ:
            case IF_ACMPNE:
            case PUTFIELD:
                return null;
            default:
                break;
        }
        return super.binaryOperation(insn, value1, value2);
    }

    @Override
    public BasicValue ternaryOperation(AbstractInsnNode insn, BasicValue value1, BasicValue value2, BasicValue value3) throws AnalyzerException {
        return super.ternaryOperation(insn, value1, value2, value3);
    }

    @Override
    public void naryExtendedOperation(AbstractInsnNode insn, List<? extends BasicValue> values, Frame<BasicValue> frame) {
        if (insn.getOpcode() == INVOKESPECIAL) {
            MethodInsnNode node = (MethodInsnNode) insn;
            if (node.name.equals("<init>")) {
                if (values.getFirst() instanceof NewInstanceValue acted) {
                    for (int i = 0; i < frame.getStackSize(); i++) {
                        BasicValue value = frame.getStack(i);
                        if (value instanceof NewInstanceValue v) {
                            if (v.isCopy(acted)) {
                                frame.setStack(i, newValue(v.getVirtualType()));
                            }
                        }
                    }
                    for (int i = 0; i < frame.getLocals(); i++) {
                        BasicValue value = frame.getLocal(i);
                        if (value instanceof NewInstanceValue v) {
                            if (v.isCopy(acted)) {
                                frame.setLocal(i, newValue(v.getVirtualType()));
                            }
                        }
                    }
                }
            }
        }
    }

    @Override
    public BasicValue unaryOperation(AbstractInsnNode insn, BasicValue value) throws AnalyzerException {
        BasicValue v = super.unaryOperation(insn, value);
        if (insn.getOpcode() == CHECKCAST) {
            if (value instanceof NullValue) {
                return NullValue.of(v.getType());
            }
        }
        return switch (insn.getOpcode()) {
            case I2B -> newValue(Type.BYTE_TYPE);
            case I2C -> newValue(Type.CHAR_TYPE);
            case I2S -> newValue(Type.SHORT_TYPE);
            case 116, 132, 136, 139, 142, 190, 193 -> newValue(Type.INT_TYPE);
            case 117, 133, 140, 143 -> newValue(Type.LONG_TYPE);
            case 118, 134, 137, 144 -> newValue(Type.FLOAT_TYPE);
            case 119, 135, 138, 141 -> newValue(Type.DOUBLE_TYPE);
            default -> v;
        };
    }

    @Override
    public void returnOperation(AbstractInsnNode insn, BasicValue value, BasicValue expected) throws AnalyzerException {
        super.returnOperation(insn, value, expected);
    }

    @Override
    public BasicValue merge(BasicValue value1, BasicValue value2) {
        if (value2.equals(UninitializedValue.INSTANCE))
            return value1;
        if (value1.equals(value2))
            return value1;
        if (value2 instanceof NullValue)
            return value1;
        if (value1 instanceof NullValue)
            return value2;
        if (value1 instanceof SimpleValue s1) {
            if (s1.canMerge(value2, typeChecker))
                return newValue(value1.getType());
        }
        if (value2 instanceof SimpleValue s2) {
            if (s2.canMerge(value1, typeChecker))
                return newValue(value2.getType());
        }
        if (value1 instanceof ExceptionValue && value2 instanceof ExceptionValue)
            return ExceptionValue.of(typeResolver.commonException(value1.getType(), value2.getType()));
        if (value1 instanceof VirtualValue && value2 instanceof VirtualValue)
            return newValue(typeResolver.common(value1.getType(), value2.getType()));
        return UninitializedValue.INSTANCE;
    }

    @Override
    public BasicValue newParameterValue(boolean isInstanceMethod, int local, Type type) {
        BasicValue v = super.newParameterValue(isInstanceMethod, local, type);
        if (local == 0 && isInstanceMethod && method.name.equals("<init>")) {
            return NewInstanceValue.of(type, false, true);
        }
        return v;
    }

    @Override
    public BasicValue newReturnTypeValue(Type type) {
        return super.newReturnTypeValue(type);
    }

    protected TypeChecker createTypeChecker() {
        return (parent, child) -> {
            JClassNode parentClass = ObfuscatorContext.INSTANCE.loadClass(parent.getInternalName());
            JClassNode childClass = ObfuscatorContext.INSTANCE.loadClass(child.getInternalName());
            if (parentClass == null || childClass == null)
                return false;
            return parentClass.isAssignableFrom(childClass);
        };
    }

    protected TypeResolver createTypeResolver() {
        return new TypeResolver() {
            public Type common(Type type1, Type type2) {
                String common;
                try {
                    common = Hierarchy.INSTANCE.getCommonSuperClass(type1.getInternalName(), type2.getInternalName());
                } catch (Exception ex) {
                    common = type1.getInternalName();
                }
                return Type.getObjectType(common);
            }

            public Type commonException(Type type1, Type type2) {
                String common;
                try {
                    common = Hierarchy.INSTANCE.getCommonSuperClass(type1.getInternalName(), type2.getInternalName());
                } catch (Exception ex) {
                    common = type1.getInternalName();
                }
                return Type.getObjectType(common);
            }
        };
    }
}
