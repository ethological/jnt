package war.metaphor.analysis.callgraph;

import lombok.Getter;
import org.objectweb.asm.Handle;
import org.objectweb.asm.tree.AbstractInsnNode;
import org.objectweb.asm.tree.InvokeDynamicInsnNode;
import org.objectweb.asm.tree.MethodInsnNode;
import org.objectweb.asm.tree.MethodNode;
import war.jnt.dash.Logger;
import war.metaphor.base.ObfuscatorContext;
import war.metaphor.tree.ClassMethod;
import war.metaphor.tree.JClassNode;
import war.metaphor.util.asm.BytecodeUtil;

import java.util.Collections;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

public class CallGraph {

    private final Logger logger = Logger.INSTANCE;

    @Getter
    private final Map<ClassMethod, Set<CallGraphNode>> xrefs;

    public CallGraph() {
        this.xrefs = new ConcurrentHashMap<>();
    }

    public void buildGraph() {
        ObfuscatorContext core = ObfuscatorContext.INSTANCE;
        xrefs.clear();
        core.getClasses().parallelStream().forEach(classNode ->
                classNode.methods.parallelStream().forEach(method -> {

                    ClassMethod caller = ClassMethod.of(classNode, method);

                    for (AbstractInsnNode instruction : method.instructions) {
                        if (instruction instanceof MethodInsnNode insn) {
                            ClassMethod called = BytecodeUtil.getMethodNode(insn.owner, insn.name, insn.desc);
                            if (called != null) {
                                addNode(caller, called, insn);
                            }
                        } else if (instruction instanceof InvokeDynamicInsnNode insn) {
                            processHandle(core, caller, insn.bsm, insn);
                            for (Object bsmArg : insn.bsmArgs) {
                                if (bsmArg instanceof Handle handle) {
                                    processHandle(core, caller, handle, insn);
                                }
                            }
                        }
                    }
                })
        );
    }

    private void processHandle(ObfuscatorContext core, ClassMethod caller, Handle handle, InvokeDynamicInsnNode insn) {
        JClassNode owner = core.loadClass(handle.getOwner());
        if (owner == null) return;

        ClassMethod called = BytecodeUtil.getMethodNode(owner, handle.getName(), handle.getDesc());
        if (called == null) {
//            logger.logln(Level.WARNING, Origin.CORE, String.format("Failed to find method: %s",
//                    new Ansi().c(YELLOW).s(String.format("%s.%s", handle.getName(), handle.getDesc())).r(false).c(BRIGHT_YELLOW)));
            return;
        }
        addNode(caller, called, insn);
    }

    private void addNode(ClassMethod caller, ClassMethod called, AbstractInsnNode instruction) {
        CallGraphNode node = new CallGraphNode(caller, instruction);
        xrefs.computeIfAbsent(called, k -> ConcurrentHashMap.newKeySet()).add(node);
    }

    public Set<CallGraphNode> getNodes(ClassMethod method) {
        return xrefs.getOrDefault(method, Collections.emptySet());
    }

    public record CallGraphNode(ClassMethod member, AbstractInsnNode instruction) {
        public ClassMethod getMember() {
            return member;
        }
        public MethodNode getMethod() {
            return member.getMember();
        }
        public AbstractInsnNode getInstruction() {
            return instruction;
        }
        public boolean isEdit() {
            return !member.getClassNode().isLibrary() && !isInvokeDynamic();
        }
        public boolean isInvokeDynamic() {
            return instruction instanceof InvokeDynamicInsnNode;
        }
    }
}
