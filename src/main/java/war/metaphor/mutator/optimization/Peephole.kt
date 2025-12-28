//package war.metaphor.mutator.optimization
//
//import org.objectweb.asm.Opcodes.*
//import org.objectweb.asm.tree.AbstractInsnNode
//import org.objectweb.asm.tree.InsnNode
//import org.objectweb.asm.tree.IntInsnNode
//import war.jnt.dash.Level
//import war.jnt.dash.Logger
//import war.jnt.dash.Origin
//import war.metaphor.base.ObfuscatorContext
//import war.metaphor.mutator.IMutator
//import war.metaphor.util.asm.BytecodeUtil
//
///**
// * @author etho
// * Peephole optimizations.
// */
//class Peephole : IMutator {
//    private val logger = Logger()
//
//    override fun run(base: ObfuscatorContext?) {
//        var optimized = 0
//
//        for (klass in base?.whitelistedClasses!!) {
//            for (method in klass.base.methods) {
//                for (insn in method.instructions) {
//                    when {
//                        isStoreLoad(insn) -> {
//                            method.instructions.remove(insn.next)
//                            method.instructions.remove(insn)
//
//                            optimized++
//                        }
//                        isDoubleLoad(insn) -> {
//                            method.instructions.insertBefore(insn.next, InsnNode(DUP))
//                            method.instructions.remove(insn.next)
//
//                            optimized++
//                        }
//                        isMultiply(insn) -> {
//                            if (insn.previous != null && BytecodeUtil.isInteger(insn.previous)) {
//                                val value = BytecodeUtil.getInteger(insn)
//                                val shift = canOptimize(value)
//
//                                if (shift != -1) {
//                                    method.instructions.insert(insn.previous, BytecodeUtil.makeInteger(shift))
//                                    method.instructions.insert(insn.next, InsnNode(ISHL))
//
//                                    method.instructions.remove(insn.next)
//                                    method.instructions.remove(insn.previous)
//
//                                    optimized++
//                                }
//                            }
//                        }
//                    }
//                }
//            }
//        }
//
//        logger.log(Level.DEBUG, Origin.METAPHOR, "Optimized $optimized instructions.\n")
//    }
//
//    private fun isMultiply(insn: AbstractInsnNode): Boolean {
//        if (insn.next != null && insn.previous != null) {
//            if (!BytecodeUtil.isInteger(insn.previous)) return false
//            if (!BytecodeUtil.isInteger(insn)) return false
//            if (insn.opcode != IMUL) return false
//
//            return true
//        }
//        return false
//    }
//
//    private fun isDoubleLoad(insn: AbstractInsnNode): Boolean {
//        if (insn.next != null) {
//            if (!BytecodeUtil.isLoad(insn)) return false
//            if (!BytecodeUtil.isLoad(insn.next)) return false
//
//            return true
//        }
//        return false
//    }
//
//    private fun isStoreLoad(insn: AbstractInsnNode): Boolean {
//        if (insn.next != null && insn.previous != null) {
//            if (!BytecodeUtil.isInteger(insn.previous)) return false
//            if (!BytecodeUtil.isStore(insn)) return false
//            if (!BytecodeUtil.isLoad(insn.next)) return false
//
//            return true
//        }
//        return false
//    }
//
//    private fun canOptimize(multiplier: Int): Int {
//        if (multiplier > 0 && (multiplier and (multiplier - 1)) == 0) {
//            val shiftCount = Integer.numberOfTrailingZeros(multiplier)
//            return shiftCount
//        } else {
//            return -1
//        }
//    }
//}