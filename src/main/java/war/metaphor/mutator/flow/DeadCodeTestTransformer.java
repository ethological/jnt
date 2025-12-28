//package war.metaphor.mutator.flow;
//
//import org.objectweb.asm.Opcodes;
//import org.objectweb.asm.tree.VarInsnNode;
//import war.metaphor.base.ObfuscatorContext;
//import war.metaphor.deadCodeEngine.DeadCodeGenerator;
//import war.metaphor.deadCodeEngine.LocalVariableObjectType;
//import war.metaphor.mutator.Mutator;
//import war.metaphor.util.wrapper.MethodWrapper;
//
//import java.lang.reflect.Modifier;
//import java.util.HashMap;
//import java.util.HashSet;
//import java.util.Map;
//import java.util.Set;
//import java.util.concurrent.atomic.AtomicBoolean;
//
///**
// * Breaks shit somehow, dont use
// */
//@Deprecated
//public class DeadCodeTestTransformer implements Mutator {
//    @Override
//    public void run(ObfuscatorContext base) {
//        base.getWhitelistedClasses().forEach(classWrapper -> {
//
//            classWrapper.getMethods().forEach(methodWrapper -> {
//
//                if (checkForDoubleTypeLocals(methodWrapper)) return;
//
//                if (RANDOM.nextBoolean()) return;
//
//                final DeadCodeGenerator dcg = new DeadCodeGenerator(methodWrapper);
//
//                AtomicBoolean initDone = new AtomicBoolean();
//
//                initDone.set(!methodWrapper.getBase().name.equals("<init>"));
//
//                methodWrapper.getInstructions().forEach(ain -> {
//                    if (ain.getOpcode() == Opcodes.INVOKESPECIAL) { // this works perfectly fine with no issues whatsoever >:)
//                        initDone.set(true);
//                    }
//                    if (initDone.get()) {
//                        dcg.insertDeadCode(ain);
//                    }
//                });
//
//            });
//
//        });
//    }
//
//    private boolean checkForDoubleTypeLocals(MethodWrapper methodWrapper) {
//        Map<Integer, Set<LocalVariableObjectType>> localVarTypes = new HashMap<>();
//
//        methodWrapper.getInstructions().forEach(ain -> {
//            if (ain instanceof VarInsnNode vin) {
//                if (!Modifier.isStatic(methodWrapper.getBase().access) && vin.var == 0) return;
//
//                LocalVariableObjectType type;
//                switch (vin.getOpcode()) {
//                    case Opcodes.ISTORE, Opcodes.ILOAD -> type = LocalVariableObjectType.INT;
//                    case Opcodes.LSTORE, Opcodes.LLOAD -> type = LocalVariableObjectType.LONG;
//                    case Opcodes.DSTORE, Opcodes.DLOAD -> type = LocalVariableObjectType.DOUBLE;
//                    case Opcodes.FSTORE, Opcodes.FLOAD -> type = LocalVariableObjectType.FLOAT;
//                    case Opcodes.ASTORE, Opcodes.ALOAD -> type = LocalVariableObjectType.OBJECT;
//                    default -> throw new IllegalStateException("Wtf");
//                }
//
//                localVarTypes.putIfAbsent(vin.var, new HashSet<>());
//                localVarTypes.get(vin.var).add(type);
//            }
//        });
//
//        return localVarTypes.values().stream().anyMatch(types -> types.size() > 1);
//    }
//}
