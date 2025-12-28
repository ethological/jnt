//package war.metaphor.mutator.optimization
//
//import org.objectweb.asm.tree.ClassNode
//import org.objectweb.asm.tree.MethodInsnNode
//import war.metaphor.base.ObfuscatorContext
//import war.metaphor.mutator.IMutator
//import war.metaphor.util.asm.BytecodeUtil
//
///**
// * @author etho
// * Eliminating calls to pure methods. (no arguments)
// */
//class PureCallInliner : IMutator {
//    override fun run(base: ObfuscatorContext?) {
//        for (klass in base?.whitelistedClasses!!) {
//            val pures = collectPure(klass.base)
//
//            for (method in klass.base.methods) {
//                for (insn in method.instructions) {
//                    if (insn !is MethodInsnNode) continue
//                    val pure = isPureCall(pures, insn.name, insn.desc)
//
//                    if (pure != null) {
//                        method.instructions.insertBefore(insn, pure.cst)
//                        method.instructions.remove(insn)
//                    }
//                }
//            }
//        }
//    }
//
//    private fun isPureCall(pures: List<PureMethod>, name: String, desc: String): PureMethod? {
//        for (pure in pures) {
//            val m = pure.method
//
//            if (m.name == name && m.desc == desc) {
//                return pure
//            }
//        }
//        return null
//    }
//
//    private fun collectPure(owner: ClassNode): List<PureMethod> {
//        val methods = mutableListOf<PureMethod>()
//
//        for (method in owner.methods) {
//            if (!method.desc.equals("()I")) continue
//            if (method.instructions.size() > 2) continue
//
//            for (insn in method.instructions) {
//                if (BytecodeUtil.isInteger(insn)) {
//                    if (insn.next == null) continue
//
//                    if (BytecodeUtil.isReturning(insn.next)) {
//                        methods.add(PureMethod(method, insn))
//                    }
//                }
//            }
//        }
//
//        return methods
//    }
//}