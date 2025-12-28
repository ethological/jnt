package war.jnt.core.code.impl;

import war.jnt.core.code.UnitContext;

public class CommentUnit {
    public static void process(String comment, UnitContext ctx) {
        ctx.fmtAppend("\t/* %s */\n", comment);
    }
}
