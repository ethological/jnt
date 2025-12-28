//package war.metaphor.mutator.optimization
//
//import org.objectweb.asm.Opcodes
//import org.objectweb.asm.tree.InsnNode
//import org.objectweb.asm.tree.JumpInsnNode
//import war.jnt.dash.Level
//import war.jnt.dash.Logger
//import war.jnt.dash.Origin
//import war.metaphor.base.ObfuscatorContext
//import war.metaphor.mutator.IMutator
//import war.metaphor.util.asm.BytecodeUtil
//
//class JumpInlining : IMutator {
//    private val logger = Logger()
//
//    override fun run(ctx: ObfuscatorContext) {
//        var count = 0
//        ctx.whitelistedClasses.forEach { classWrapper ->
//            classWrapper.methods.forEach { methodWrapper ->
//                val methodNode = methodWrapper.base
//
//                methodNode.instructions.toArray()
//                    .filter { it.opcode == Opcodes.GOTO }
//                    .forEach { ain ->
//                        val goto = ain as JumpInsnNode
//                        val afterTarget = goto.label.next
//
//                        if (afterTarget != null && afterTarget.opcode == Opcodes.GOTO) {
//                            val secondGoto = afterTarget as JumpInsnNode
//                            goto.label = secondGoto.label
//                            count++
//                        }
//                    }
//
//                methodNode.instructions.toArray()
//                    .filter { it.opcode == Opcodes.GOTO }
//                    .forEach { ain ->
//                        val goto = ain as JumpInsnNode
//                        val afterTarget = goto.label.next
//
//                        if (afterTarget != null && BytecodeUtil.isReturning(afterTarget)) {
//                            methodNode.instructions.set(ain, InsnNode(afterTarget.opcode))
//                            count++
//                        }
//                    }
//            }
//        }
//
//        logger.log(Level.DEBUG, Origin.METAPHOR, "Inlined $count jump chains.\n")
//    }
//}