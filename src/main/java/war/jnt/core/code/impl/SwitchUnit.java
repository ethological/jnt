package war.jnt.core.code.impl;

import org.objectweb.asm.Opcodes;
import org.objectweb.asm.tree.LookupSwitchInsnNode;
import org.objectweb.asm.tree.TableSwitchInsnNode;
import war.jnt.core.code.UnitContext;
import war.jnt.fusebox.impl.Internal;

import java.util.LinkedHashMap;
import java.util.Map;

public class SwitchUnit implements Opcodes {

    public static void process(LookupSwitchInsnNode lsin, UnitContext ctx) {
        ctx.getBuilder().append(String.format("\tswitch(%s.i) {\n", Internal.computePop(ctx.getTracker())));

        Map<Integer, String> caseMap = new LinkedHashMap<>();
        for (int i = 0; i < lsin.keys.size(); i++) {
            int key = lsin.keys.get(i);
            String label = BlockUnit.resolveBlock(lsin.labels.get(i));

            caseMap.putIfAbsent(key, label);
        }

        for (var entry : caseMap.entrySet()) {
            ctx.getBuilder().append(String.format("\tcase %d:\n\t\tgoto %s;\n", entry.getKey(), entry.getValue()));
        }
        ctx.getBuilder().append(String.format("\tdefault:\n\t\tgoto %s;\n\t}\n", BlockUnit.resolveBlock(lsin.dflt)));

//        for (int i = 0; i < lsin.keys.size(); i++) {
//            ctx.getBuilder().append(String.format("\tcase %d:\n\t\tgoto %s;\n", lsin.keys.get(i), BlockUnit.resolveBlock(lsin.labels.get(i))));
//        }
//
//        ctx.getBuilder().append(String.format("\tdefault:\n\t\tgoto %s;\n\t}\n", BlockUnit.resolveBlock(lsin.dflt)));
    }

    public static void process(TableSwitchInsnNode tsin, UnitContext ctx) {
        ctx.getBuilder().append(String.format("\tswitch(%s.i) {\n", Internal.computePop(ctx.getTracker())));

        for (int i = tsin.min; i <= tsin.max; i++) {
            ctx.getBuilder().append(String.format("\tcase %d:\n\t\tgoto %s;\n", i, BlockUnit.resolveBlock(tsin.labels.get(i - tsin.min))));
        }

        ctx.getBuilder().append(String.format("\tdefault:\n\t\tgoto %s;\n\t}\n", BlockUnit.resolveBlock(tsin.dflt)));
    }
}
