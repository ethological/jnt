package war.jnt.core.code.impl;

import lombok.AllArgsConstructor;
import org.apache.commons.lang3.Validate;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.tree.AbstractInsnNode;
import org.objectweb.asm.tree.InsnNode;
import org.objectweb.asm.tree.analysis.BasicValue;
import org.objectweb.asm.tree.analysis.Frame;
import war.jnt.core.code.UnitContext;
import war.jnt.fusebox.impl.Internal;
import war.jnt.stack.StackTracker;

import java.util.Map;
import java.util.Stack;

public class StackUnit implements Opcodes {

    @AllArgsConstructor
    static class StackItem {
        int index;
        int representation;
        BasicValue value;

        boolean isTop() {
            return false;
        }

        TopItem top() {
            return new TopItem(index, representation, value);
        }

        String get() {
            return String.format("stack[%d]", representation);
        }

        String fmt(String s) {
            return s.replace("{g}", get()).replace("{t}", "t_" + index);
        }

        @Override
        public String toString() {
            return String.format("StackItem{index=%d, representation=%d, value=%s}", index, representation, value);
        }
    }

    static class TopItem extends StackItem {

        public TopItem(int index, int representation, BasicValue value) {
            super(index, representation, value);
        }

        @Override
        boolean isTop() {
            return true;
        }

        @Override
        String fmt(String s) {
            return "";
        }

        @Override
        public String toString() {
            return String.format("TopItem{index=%d, representation=%d, value=%s}", index, representation, value);
        }
    }

    public static void process(InsnNode insn, Map<AbstractInsnNode, Frame<BasicValue>> frames, UnitContext ctx) {

        StackTracker tracker = ctx.getTracker();

        Frame<BasicValue> frame = frames.get(insn);

        Validate.notNull(frame, "Frame is null");

        Stack<StackItem> stack = new Stack<>();
        int head = tracker.dump();
        int j = 0;
        for (int i = 0; i < frame.getStackSize(); i++) {
            BasicValue value = frame.getStack(i);
            StackItem item = new StackItem(j, head - (frame.getStackSize() - i) + 1, value);
            stack.push(item);
            if (value.getSize() == 2) {
                stack.push(item.top());
                j++;
            }
            j++;
        }

        switch (insn.getOpcode()) {
            case SWAP -> {
                int curr = tracker.dump();
                int prev = curr - 1;
                ctx.fmtAppend("\t{\n");
                ctx.fmtAppend("\t\t// SWAP\n");
                ctx.fmtAppend("\t\tjvalue t = stack[%s];\n", curr);
                ctx.fmtAppend("\t\tstack[%s] = stack[%s];\n", curr, prev);
                ctx.fmtAppend("\t\tstack[%s] = t;\n", prev);
                ctx.fmtAppend("\t}\n");
            }
            case POP -> {
                ctx.fmtAppend("\t// POP\n");
                tracker.simPop();
            }
            case POP2 -> {
                StackItem val = stack.pop();
                if (!val.isTop()) {
                    tracker.simPop();
                    tracker.simPop();
                } else {
                    tracker.simPop();
                }
                ctx.fmtAppend(String.format("\t// POP2 (%s) :: (%s)\n", val + (val.isTop() ? "" : " <- " + stack.pop()), tracker.dump()));
            }
            case DUP -> {
                StackItem v1 = stack.pop();
                ctx.fmtAppend("\t{\n");
                ctx.fmtAppend("\t\t// DUP\n");

                tracker.simPop();

                ctx.fmtAppend(v1.fmt("\t\tjvalue {t} = {g};\n"));

                ctx.fmtAppend(v1.fmt("\t\t%s = {t};\n"), Internal.computePush(tracker));
                ctx.fmtAppend(v1.fmt("\t\t%s = {t};\n"), Internal.computePush(tracker));

                ctx.fmtAppend("\t}\n");
            }
            case DUP2 -> {
                StackItem v1 = stack.pop();
                StackItem v2 = stack.pop();
                ctx.fmtAppend("\t{\n");
                ctx.fmtAppend("\t\t// DUP2\n");

                if (!v1.isTop()) tracker.simPop();
                tracker.simPop();

                if (!v1.isTop()) ctx.fmtAppend(v1.fmt("\t\tjvalue {t} = {g};\n"));
                ctx.fmtAppend(v2.fmt("\t\tjvalue {t} = {g};\n"));

                ctx.fmtAppend(v2.fmt("\t\t%s = {t};\n"), Internal.computePush(tracker));
                if (!v1.isTop()) ctx.fmtAppend(v1.fmt("\t\t%s = {t};\n"), Internal.computePush(tracker));
                ctx.fmtAppend(v2.fmt("\t\t%s = {t};\n"), Internal.computePush(tracker));
                if (!v1.isTop()) ctx.fmtAppend(v1.fmt("\t\t%s = {t};\n"), Internal.computePush(tracker));

                ctx.fmtAppend("\t}\n");
            }
            case DUP_X1 -> {
                StackItem v1 = stack.pop();
                StackItem v2 = stack.pop();
                ctx.fmtAppend("\t{\n");
                ctx.fmtAppend("\t\t// DUP_X1\n");

                tracker.simPop();
                tracker.simPop();

                ctx.fmtAppend(v1.fmt("\t\tjvalue {t} = {g};\n"));
                ctx.fmtAppend(v2.fmt("\t\tjvalue {t} = {g};\n"));

                ctx.fmtAppend(v1.fmt("\t\t%s = {t};\n"), Internal.computePush(tracker));
                ctx.fmtAppend(v2.fmt("\t\t%s = {t};\n"), Internal.computePush(tracker));
                ctx.fmtAppend(v1.fmt("\t\t%s = {t};\n"), Internal.computePush(tracker));
                ctx.fmtAppend("\t}\n");
            }
            case DUP_X2 -> {
                StackItem v1 = stack.pop();
                StackItem v2 = stack.pop();
                StackItem v3 = stack.pop();
                ctx.fmtAppend("\t{\n");
                ctx.fmtAppend("\t\t// DUP_X2\n");

                tracker.simPop();
                if (!v2.isTop()) tracker.simPop();
                tracker.simPop();

                ctx.fmtAppend(v1.fmt("\t\tjvalue {t} = {g};\n"));
                if (!v2.isTop()) ctx.fmtAppend(v2.fmt("\t\tjvalue {t} = {g};\n"));
                ctx.fmtAppend(v3.fmt("\t\tjvalue {t} = {g};\n"));

                ctx.fmtAppend(v1.fmt("\t\t%s = {t};\n"), Internal.computePush(tracker));
                ctx.fmtAppend(v3.fmt("\t\t%s = {t};\n"), Internal.computePush(tracker));
                if (!v2.isTop()) ctx.fmtAppend(v2.fmt("\t\t%s = {t};\n"), Internal.computePush(tracker));
                ctx.fmtAppend(v1.fmt("\t\t%s = {t};\n"), Internal.computePush(tracker));
                ctx.fmtAppend("\t}\n");
            }
            case DUP2_X1 -> {
                StackItem v1 = stack.pop();
                StackItem v2 = stack.pop();
                StackItem v3 = stack.pop();
                ctx.fmtAppend("\t{\n");
                ctx.fmtAppend("\t\t// DUP2_X1\n");

                if (!v1.isTop()) tracker.simPop();
                tracker.simPop();
                tracker.simPop();

                if (!v1.isTop()) ctx.fmtAppend(v1.fmt("\t\tjvalue {t} = {g};\n"));
                ctx.fmtAppend(v2.fmt("\t\tjvalue {t} = {g};\n"));
                ctx.fmtAppend(v3.fmt("\t\tjvalue {t} = {g};\n"));

                ctx.fmtAppend(v2.fmt("\t\t%s = {t};\n"), Internal.computePush(tracker));
                if (!v1.isTop()) ctx.fmtAppend(v1.fmt("\t\t%s = {t};\n"), Internal.computePush(tracker));
                ctx.fmtAppend(v3.fmt("\t\t%s = {t};\n"), Internal.computePush(tracker));
                ctx.fmtAppend(v2.fmt("\t\t%s = {t};\n"), Internal.computePush(tracker));
                if (!v1.isTop()) ctx.fmtAppend(v1.fmt("\t\t%s = {t};\n"), Internal.computePush(tracker));
                ctx.fmtAppend("\t}\n");

            }
            case DUP2_X2 -> {
                StackItem v1 = stack.pop();
                StackItem v2 = stack.pop();
                StackItem v3 = stack.pop();
                StackItem v4 = stack.pop();
                ctx.fmtAppend("\t{\n");
                ctx.fmtAppend("\t\t// DUP2_X2\n");

                if (!v1.isTop()) ctx.fmtAppend(v1.fmt("\t\tjvalue {t} = {g};\n"));
                ctx.fmtAppend(v2.fmt("\t\tjvalue {t} = {g};\n"));
                if (!v3.isTop())  ctx.fmtAppend(v3.fmt("\t\tjvalue {t} = {g};\n"));
                ctx.fmtAppend(v4.fmt("\t\tjvalue {t} = {g};\n"));

                if (!v1.isTop()) tracker.simPop();
                tracker.simPop();
                if (!v3.isTop())  tracker.simPop();
                tracker.simPop();

                ctx.fmtAppend(v2.fmt("\t\t%s = {t};\n"), Internal.computePush(tracker));
                if (!v1.isTop()) ctx.fmtAppend(v1.fmt("\t\t%s = {t};\n"), Internal.computePush(tracker));
                ctx.fmtAppend(v4.fmt("\t\t%s = {t};\n"), Internal.computePush(tracker));
                if (!v3.isTop()) ctx.fmtAppend(v3.fmt("\t\t%s = {t};\n"), Internal.computePush(tracker));
                ctx.fmtAppend(v2.fmt("\t\t%s = {t};\n"), Internal.computePush(tracker));
                if (!v1.isTop()) ctx.fmtAppend(v1.fmt("\t\t%s = {t};\n"), Internal.computePush(tracker));
                ctx.fmtAppend("\t}\n");
            }
            default -> throw new IllegalArgumentException("Unknown opcode: " + insn.getOpcode());
        }
    }
}
