package war.jnt.core.code;

import lombok.SneakyThrows;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.Type;
import org.objectweb.asm.tree.*;
import org.objectweb.asm.tree.analysis.BasicValue;
import org.objectweb.asm.tree.analysis.Frame;
import war.configuration.ConfigurationSection;
import war.jnt.cache.Cache;
import war.jnt.core.code.impl.*;
import war.jnt.core.code.impl.field.FieldUnit;
import war.jnt.core.code.impl.invoke.InvocationUnit;
import war.jnt.core.vm.TempJumpVM;
import war.jnt.dash.Level;
import war.jnt.dash.Logger;
import war.jnt.dash.Origin;
import war.jnt.fusebox.impl.Internal;
import war.jnt.fusebox.impl.VariableManager;
import war.jnt.innercache.InnerCache;
import war.jnt.stack.StackTracker;
import war.metaphor.analysis.graph.ControlFlowGraph;
import war.metaphor.tree.JClassNode;
import war.metaphor.util.asm.BytecodeUtil;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class CodeProcessor implements Opcodes {

    @SneakyThrows
    public static String forMethod(JClassNode node, MethodNode method, ConfigurationSection config) {
        List<TryCatchBlockNode> tryCatchBlocks = method.tryCatchBlocks;
        ControlFlowGraph graph = new ControlFlowGraph(node, method);
        graph.compute();

        Map<AbstractInsnNode, Frame<BasicValue>> frames = graph.getFrames();

        var builder = new StringBuilder();

        var tjvm = new TempJumpVM(builder, config.getConfigurationSection("mutators.jnt.virtualize").getBoolean("enabled", false), config.getConfigurationSection("mutators.jnt.virtualize").getInt("chance"));
        tjvm.insertSetupCode();

        builder.append("/* MAX_STACK */");
        builder.append(Internal.createLocals(method.maxLocals));

        var tracker = new StackTracker();
        UnitContext ctx = new UnitContext(builder, tracker, node, method, false);

        ctx.fmtAppend("\t/* %s */\n", String.format("%s -> %s%s", node.name, method.name, method.desc));

        // TODO: need to edit it a bit, sometimes it will crash the entire thing ;o
//        if (config.getBoolean("mutators.jnt.traceless", false))
//            ctx.fmtAppend("jnt_reset_traceback(env);\n");

        var varMan = new VariableManager();
        final InnerCache ic = new InnerCache(ctx, varMan);

        for (AbstractInsnNode insn : method.instructions) {

            String handlerLabel = "handler_" + insn.index;
            ctx.handlerLabel = handlerLabel;

            List<TryCatchBlockNode> range = new ArrayList<>(tryCatchBlocks
                    .stream()
                    .filter(tc -> tc.start.index <= insn.index && tc.end.index >= insn.index)
                    .toList());

            int prev = ctx.getBuilder().length();

            ctx.fmtAppend("\t/* %s */\n", BytecodeUtil.toString(insn).replace("\\", "\\\\").replace("*/", "*\\"));

            switch (insn) {
                case VarInsnNode varInsn -> VarUnit.process(varInsn, ctx, method);
                case IntInsnNode intInsn -> PushUnit.process(intInsn, ctx);
                case LdcInsnNode ldcInsn -> LdcUnit.process(ldcInsn, ctx, varMan, tjvm);
                case LabelNode label -> {
                    if (label != method.instructions.getLast()) { // label at end of compound statement only available with '-std=c++2b' or '-std=gnu++2b'
                        BlockUnit.process(label, frames, ctx);
                    }
                }
                case InsnNode iNode -> {
                    if (insn.getOpcode() >= ICONST_M1 && insn.getOpcode() <= ICONST_5 || insn.getOpcode() == FCONST_0 || insn.getOpcode() == FCONST_1 || insn.getOpcode() == FCONST_2 || insn.getOpcode() == DCONST_0 || insn.getOpcode() == DCONST_1 || insn.getOpcode() == LCONST_0 || insn.getOpcode() == LCONST_1) {
                        ImmediateUnit.process(iNode, ctx);
                    } else if (
                            insn.getOpcode() == IRETURN ||
                                    insn.getOpcode() == DRETURN ||
                                    insn.getOpcode() == FRETURN ||
                                    insn.getOpcode() == LRETURN ||
                                    insn.getOpcode() == ARETURN ||
                                    insn.getOpcode() == RETURN
                    ) {
                        ReturnUnit.process(iNode, ctx);
                    } else if (Internal.isArithmetic(insn.getOpcode())) {
                        ArithmeticUnit.process(ic, iNode, ctx, tjvm);
                    } else if (insn.getOpcode() == ACONST_NULL) {
                        ImmediateUnit.process(iNode, ctx);
                    } else if (Internal.isNumericConversion(insn.getOpcode())) {
                        CastUnit.process(iNode, ctx);
                    } else if (Internal.isArrayHandling(insn.getOpcode())) {
                        ArrayHandlerUnit.process(ic, iNode, ctx, tjvm);
                    } else if (insn.getOpcode() >= POP && insn.getOpcode() <= SWAP) {
                        StackUnit.process(iNode, frames, ctx);
                    } else if (insn.getOpcode() >= MONITORENTER && insn.getOpcode() <= MONITOREXIT) {
                        MonitorUnit.process(iNode, ctx);
                    } else if (insn.getOpcode() == ATHROW) {
                        ThrowUnit.process(ic, iNode, ctx, tjvm);
                    } else if (insn.getOpcode() == NOP) {
                        CommentUnit.process("NOP", ctx);
                    }
                }
                case JumpInsnNode jump -> {
                    if (Internal.isJump(jump.getOpcode())) {
                        JumpUnit.process(jump, ctx, tjvm);
                    }
                }
                case MethodInsnNode call -> {
                    if (
                        call.getOpcode() == INVOKESTATIC ||
                        call.getOpcode() == INVOKEVIRTUAL ||
                        call.getOpcode() == INVOKESPECIAL ||
                        call.getOpcode() == INVOKEINTERFACE
                    ) {
                        InvocationUnit.Companion.process(ic, call, ctx, varMan, tjvm, config.getBoolean("mutators.jnt.intrinsic", true));
                    }
                }
                case FieldInsnNode fieldInsn -> FieldUnit.Companion.process(ic, fieldInsn, ctx, tjvm, varMan);
                case TypeInsnNode typeInsn -> TypeUnit.Companion.process(typeInsn, ctx, tjvm);
                case LookupSwitchInsnNode lsin -> SwitchUnit.process(lsin, ctx);
                case TableSwitchInsnNode tsin -> SwitchUnit.process(tsin, ctx);
                case IincInsnNode iinc -> IincUnit.process(iinc, ctx, method);
                case LineNumberNode line -> CommentUnit.process(String.valueOf(line.line), ctx);
                case InvokeDynamicInsnNode indy -> IndyUnit.process(indy, varMan, ctx);
                case FrameNode ignored -> {}
                default -> Logger.INSTANCE.log(Level.WARNING, Origin.CORE, String.format("Unhandled instruction %s in %s -> %s%s\n", BytecodeUtil.toString(insn), node.name, method.name, method.desc));
            }

            if (insn.getOpcode() != -1 && ctx.getBuilder().length() == prev) {
                ctx.fmtAppend("\t/* SKIP -> %s */\n", BytecodeUtil.toString(insn));
                Logger.INSTANCE.log(Level.WARNING, Origin.CORE, String.format("Skipped instruction %s in %s -> %s%s\n", BytecodeUtil.toString(insn), node.name, method.name, method.desc));
            } else {
                Set<String> throwableTypes = TryUnit.canThrow(insn, frames);

                if (throwableTypes.isEmpty()) continue;

                ctx.fmtAppend("%s:\n", handlerLabel);

                ctx.fmtAppend("\tif((*env)->ExceptionCheck(env)) {\n");
                ctx.fmtAppend("\t\tjthrowable ex = (*env)->ExceptionOccurred(env);\n");
                ctx.fmtAppend("\t\tex = (*env)->NewLocalRef(env, ex);\n");
                ctx.fmtAppend("\t\t(*env)->ExceptionClear(env);\n");

                range.sort((a, b) -> {
                    int aDiff = Math.abs(a.start.index - insn.index);
                    int bDiff = Math.abs(b.start.index - insn.index);
                    return Integer.compare(aDiff, bDiff);
                });

                for (TryCatchBlockNode tcb : range) {
                    if (tcb.type == null || tcb.type.equals("*")) {
                        ctx.fmtAppend("\t\tif (ex) {\n");
                    } else if (tcb.type.equals("null")) {
                        int idx = Cache.Companion.request_klass("java/lang/NullPointerException");
                        ctx.fmtAppend("\t\tif ((*env)->IsInstanceOf(env, ex, request_klass(env, %s))) {\n", idx);
                    } else {
                        int idx = Cache.Companion.request_klass(tcb.type);
                        ctx.fmtAppend("\t\tif ((*env)->IsInstanceOf(env, ex, request_klass(env, %s))) {\n", idx);
                    }

                    ctx.fmtAppend("\t\t\tstack[0].l = ex;\n");
                    ctx.fmtAppend("\t\t\tgoto %s;\n", BlockUnit.resolveBlock(tcb.handler));
                    ctx.fmtAppend("\t\t}\n");
                }

                ctx.fmtAppend("\t\t(*env)->Throw(env, ex);\n");
                ctx.fmtAppend("\t\tgoto unwind;\n");
                ctx.fmtAppend("\t}\n");
            }
        }

        StringBuilder jniLookups = new StringBuilder();
        UnitContext tempCtx = new UnitContext(jniLookups, tracker, node, method, false);
        InnerCache tempIc = new InnerCache(tempCtx, varMan);

        tempIc.ccMap.putAll(ic.ccMap);
        tempIc.cmMap.putAll(ic.cmMap);
        tempIc.vmMap.putAll(ic.vmMap);
        tempIc.cfMap.putAll(ic.cfMap);
        tempIc.vfMap.putAll(ic.vfMap);

        tempIc.generateJNILookups();

        String commentPattern = "\t/* " + node.name + " -> " + method.name + method.desc + " */\n";
        String currentContent = builder.toString();
        int insertPos = currentContent.indexOf(commentPattern);
        if (insertPos != -1) {
            builder.insert(insertPos + commentPattern.length(), jniLookups);
        } else {
            String altPattern = "/* " + node.name + " -> " + method.name + method.desc + " */\n";
            insertPos = currentContent.indexOf(altPattern);
            if (insertPos != -1) {
                builder.insert(insertPos + altPattern.length(), jniLookups);
            }
        }

        ctx.fmtAppend("unwind:\n");
        switch (Type.getReturnType(method.desc).getSort()) {
            case Type.BOOLEAN, Type.BYTE, Type.CHAR, Type.SHORT, Type.INT -> ctx.fmtAppend("\treturn 0;\n");
            case Type.FLOAT -> ctx.fmtAppend("\treturn 0.0f;\n");
            case Type.LONG -> ctx.fmtAppend("\treturn 0L;\n");
            case Type.DOUBLE -> ctx.fmtAppend("\treturn 0.0;\n");
            case Type.ARRAY, Type.OBJECT -> ctx.fmtAppend("\treturn NULL;\n");
            default -> ctx.fmtAppend("\treturn;\n");
        }

        tjvm.buildVM();

        String out = builder.toString();
        out = out.replace("/* MAX_STACK */", Internal.createStack(tracker.max() + 1));

        return out;
    }
}
