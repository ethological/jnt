package war.jnt.core.code.impl.invoke;

import org.objectweb.asm.Opcodes;
import org.objectweb.asm.tree.MethodInsnNode;
import war.jnt.cache.Cache;
import war.jnt.cache.struct.CachedMethod;
import war.jnt.core.code.UnitContext;
import war.jnt.dash.Level;
import war.jnt.dash.Logger;
import war.jnt.dash.Origin;
import war.jnt.fusebox.impl.VariableManager;
import war.jnt.innercache.CacheMemberInfo;
import war.jnt.innercache.InnerCache;

// may allah smite this shit into hell
// TODO: rewrite.
public class LookupUnit implements Opcodes {

    private static final Logger logger = Logger.INSTANCE;

    public static Lookup process(InnerCache ic, MethodInsnNode insn, UnitContext ctx, VariableManager varMan, Boolean shouldIntrinsic) {
        // null out for intrinsics
        if (shouldIntrinsic) {
            if (insn.name.equals("length") && insn.owner.equals("java/lang/String")) {
                return null;
            } else if (insn.name.equals("getClass") && insn.owner.equals("java/lang/Object")) {
                return null;
            } else if (insn.name.equals("isEmpty") && insn.owner.equals("java/lang/String")) {
                return null;
            }
        }

        final String klass = insn.owner;
        final String method = insn.name;
        final String desc = insn.desc;

        return switch (insn.getOpcode()) {
            case INVOKESTATIC -> {
                final String kv = ic.FindClass(insn.owner);
                final String lv = ic.GetStaticMethod(new CacheMemberInfo(
                        klass, method, desc
                ));

                yield new Lookup(lv, kv);
            }
            case INVOKEVIRTUAL, INVOKESPECIAL, INVOKEINTERFACE -> {
                final String kv = ic.FindClass(insn.owner);
                final String lv = ic.GetVirtualMethod(new CacheMemberInfo(
                        klass, method, desc
                ));
                yield new Lookup(lv, kv);
            }
            default -> {
                logger.logln(Level.FATAL, Origin.CORE, "Failed to transpile method lookup.");
                yield null;
            }
        };
    }
}
