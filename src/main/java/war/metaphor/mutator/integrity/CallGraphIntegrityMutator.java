package war.metaphor.mutator.integrity;

import org.apache.commons.lang3.RandomUtils;
import org.objectweb.asm.Type;
import org.objectweb.asm.tree.*;
import war.configuration.ConfigurationSection;
import war.jnt.annotate.Level;
import war.jnt.annotate.Stability;
import war.metaphor.analysis.callgraph.CallGraph;
import war.metaphor.base.ObfuscatorContext;
import war.metaphor.mutator.Mutator;
import war.metaphor.tree.ClassMethod;
import war.metaphor.tree.JClassNode;
import war.metaphor.util.asm.BytecodeUtil;

import java.lang.reflect.Modifier;
import java.util.*;

@Stability(Level.HIGH)
public class CallGraphIntegrityMutator extends Mutator {

    public CallGraphIntegrityMutator(ObfuscatorContext base, ConfigurationSection config) {
        super(base, config);
    }

    @Override
    public void run(ObfuscatorContext base) {

        CallGraph callGraph = new CallGraph();
        callGraph.buildGraph();

        Map<ClassMethod, Integer> checkKey = new HashMap<>();
        Map<ClassMethod, Integer> checkVar = new HashMap<>();

        for (Map.Entry<ClassMethod, Set<CallGraph.CallGraphNode>> entry : callGraph.getXrefs().entrySet()) {

            ClassMethod called = entry.getKey();
            JClassNode callee = called.getClassNode();
            MethodNode member = called.getMember();

            int leeway = BytecodeUtil.leeway(member);
            if (leeway < 30000)
                continue;

            if (callee.isExempt()) continue;
            if (callee.isInterface()) continue;
            if (callee.isExempt(member)) continue;
            if (Modifier.isAbstract(called.getMember().access)) continue;

            Set<CallGraph.CallGraphNode> callingNodes = entry.getValue();

            Set<MethodNode> callers = new HashSet<>();

            callingNodes.forEach(cgn -> callers.add(cgn.getMethod()));

//            FieldNode callStackField = new FieldNode(ACC_PRIVATE | ACC_STATIC, Dictionary.gen(1, Purpose.FIELD), "Ljava/lang/Object;", null,null);
//            callee.fields.add(callStackField);

//            var list = new InsnList();
//
//            list.add(BytecodeUtil.makeInteger(hashes.length));
//            list.add(new IntInsnNode(NEWARRAY, Type.INT));
//
//            for (int i = 0; i < hashes.length; i++) {
//                int tableEntry = engine.run(hashes[i]);
//                list.add(new InsnNode(DUP));
//                list.add(BytecodeUtil.makeInteger(i));
//                list.add(BytecodeUtil.makeInteger(tableEntry));
//                list.add(new InsnNode(IASTORE));
//            }
//
//            list.add(new FieldInsnNode(
//                    PUTSTATIC,
//                    callee.name,
//                    callStackField.name,
//                    callStackField.desc
//            ));
//
//            MethodNode clinit = callee.getStaticInit();
//            clinit.instructions.insert(list);

            InsnList checkInsns = new InsnList();

            if (!callers.isEmpty()) {

                int[] hashes = callers.stream()
                        .mapToInt(m -> m.name.hashCode())
                        .toArray();

                int key = checkKey.computeIfAbsent(called, _ -> {
                    int seed = RandomUtils.nextInt();
                    int var = member.maxLocals++;
                    for (AbstractInsnNode instruction : member.instructions) {
                        if (BytecodeUtil.isLong(instruction)) {
                            long value = BytecodeUtil.getLong(instruction);
                            member.instructions.insert(instruction, BytecodeUtil.generateSeeded(var, value, seed, Type.LONG_TYPE, Type.INT_TYPE));
                            member.instructions.remove(instruction);
                        } else if (BytecodeUtil.isInteger(instruction)) {
                            int value = BytecodeUtil.getInteger(instruction);
                            member.instructions.insert(instruction, BytecodeUtil.generateSeeded(var, value, seed, Type.INT_TYPE, Type.INT_TYPE));
                            member.instructions.remove(instruction);
                        }
                    }
                    checkVar.put(called, var);
                    return seed;
                });

                int var = checkVar.get(called);
                int stackArrayVar = member.maxLocals++;
                int stackLengthVar = member.maxLocals++;
                int hashVar = member.maxLocals++;

                checkInsns.add(new InsnNode(ICONST_0));
                checkInsns.add(new VarInsnNode(ISTORE, hashVar));
                checkInsns.add(new InsnNode(ICONST_0));
                checkInsns.add(new VarInsnNode(ISTORE, var));
                checkInsns.add(new InsnNode(ICONST_0));
                checkInsns.add(new VarInsnNode(ISTORE, stackLengthVar));

                checkInsns.add(new MethodInsnNode(INVOKESTATIC, "java/lang/Thread", "currentThread", "()Ljava/lang/Thread;"));
                checkInsns.add(new MethodInsnNode(INVOKEVIRTUAL, "java/lang/Thread", "getStackTrace", "()[Ljava/lang/StackTraceElement;"));

                checkInsns.add(new VarInsnNode(ASTORE, stackArrayVar));

                checkInsns.add(new VarInsnNode(ALOAD, stackArrayVar));
                checkInsns.add(new InsnNode(ARRAYLENGTH));
                checkInsns.add(new VarInsnNode(ISTORE, stackLengthVar));

                checkInsns.add(new InsnNode(ICONST_0));
                checkInsns.add(new VarInsnNode(ISTORE, var));

                LabelNode loopStart = new LabelNode();
                LabelNode loopEnd = new LabelNode();
                LabelNode loopTramp = new LabelNode();
                LabelNode loopNext = new LabelNode();
                checkInsns.add(loopStart);

                checkInsns.add(new VarInsnNode(ILOAD, var));
                checkInsns.add(new VarInsnNode(ILOAD, stackLengthVar));

                checkInsns.add(new JumpInsnNode(IF_ICMPGE, loopTramp));

                checkInsns.add(new VarInsnNode(ALOAD, stackArrayVar));
                checkInsns.add(new VarInsnNode(ILOAD, var));
                checkInsns.add(new InsnNode(AALOAD));
                checkInsns.add(new MethodInsnNode(INVOKEVIRTUAL, "java/lang/StackTraceElement", "getMethodName", "()Ljava/lang/String;"));
                checkInsns.add(new MethodInsnNode(INVOKEVIRTUAL, "java/lang/String", "hashCode", "()I"));
                checkInsns.add(new VarInsnNode(ISTORE, hashVar));

                LabelNode[] hashCheckLabels = new LabelNode[hashes.length];
                for (int i = 0; i < hashes.length; i++) {
                    hashCheckLabels[i] = new LabelNode();
                }

                LookupSwitchInsnNode switchInsnNode = new LookupSwitchInsnNode(
                        loopNext,
                        hashes,
                        hashCheckLabels
                );
                BytecodeUtil.fixLookupSwitch(switchInsnNode);
                checkInsns.add(new VarInsnNode(ILOAD, hashVar));
                checkInsns.add(switchInsnNode);

                for (int i = 0; i < hashes.length; i++) {
                    checkInsns.add(hashCheckLabels[i]);
                    checkInsns.add(BytecodeUtil.generateSeeded(hashVar, key, hashes[i]));
                    checkInsns.add(new VarInsnNode(ISTORE, var));
                    checkInsns.add(new JumpInsnNode(GOTO, loopEnd));
                }

                checkInsns.add(loopNext);
                checkInsns.add(new IincInsnNode(var, 1));
                checkInsns.add(new JumpInsnNode(GOTO, loopStart));

                checkInsns.add(loopTramp);

//                checkInsns.add(new FieldInsnNode(GETSTATIC, "java/lang/System", "out", "Ljava/io/PrintStream;"));
//                checkInsns.add(new LdcInsnNode("FAILED: " + called + " (" + called.getClassNode().getRealName() + "), expected one of " +
//                        callers.stream().map(m -> m.name).toList()));
//                checkInsns.add(new MethodInsnNode(INVOKEVIRTUAL, "java/io/PrintStream", "println", "(Ljava/lang/String;)V"));

                checkInsns.add(BytecodeUtil.generateSeeded(hashVar, key, RandomUtils.nextInt()));
                checkInsns.add(new VarInsnNode(ISTORE, var));

                checkInsns.add(loopEnd);

            }

            member.instructions.insert(checkInsns);
        }
    }

    private TableIntegrity makeTable(Set<MethodNode> callers) {
        List<String> table = new ArrayList<>();

        for (var caller : callers) {
            table.add(caller.name);
        }

        String[] array = new String[table.size()];

        table.toArray(array);

        return new TableIntegrity(array);
    }
}
