package war.metaphor.analysis.frames;

import org.objectweb.asm.tree.AbstractInsnNode;
import org.objectweb.asm.tree.MethodNode;
import org.objectweb.asm.tree.analysis.BasicValue;
import org.objectweb.asm.tree.analysis.Frame;
import war.metaphor.tree.JClassNode;
import war.metaphor.util.asm.BytecodeUtil;

import java.util.Collections;
import java.util.Map;

public class LazyFrameProvider {
    private final JClassNode classNode;
    private final MethodNode methodNode;
    private Map<AbstractInsnNode, Frame<BasicValue>> frames;

    public LazyFrameProvider(JClassNode cn, MethodNode mn) {
        this.classNode = cn;
        this.methodNode = mn;
    }

    public Frame<BasicValue> get(AbstractInsnNode insn) {
        if (frames == null) {
            try {
                frames = BytecodeUtil.analyzeAndComputeMaxes(classNode, methodNode);
            } catch (Exception e) {
                frames = Collections.emptyMap();
            }
        }
        return frames.get(insn);
    }
}