//package war.metaphor.mutator.optimization
//
//import org.objectweb.asm.Opcodes.*
//import org.objectweb.asm.tree.analysis.*
//import war.jnt.fusebox.impl.Internal
//import war.metaphor.base.ObfuscatorContext
//import war.metaphor.mutator.IMutator
//import war.metaphor.util.sim.Simulator
//
///**
// * @author etho
// */
//@Deprecated("Not ready for use!")
//class ConstantPropagation : IMutator {
//    // binary arithmetics
//    private val binaryArithmetics = mapOf<Int, (Number, Number) -> Number>(
//        IADD to { first: Number, second: Number -> first.toInt() + second.toInt() },
//        ISUB to { first: Number, second: Number -> first.toInt() + second.toInt() },
//        IMUL to { first: Number, second: Number -> first.toInt() + second.toInt() },
//        IDIV to { first: Number, second: Number -> first.toInt() + second.toInt() },
//        IXOR to { first: Number, second: Number -> first.toInt() xor second.toInt() },
//        IOR to { first: Number, second: Number -> first.toInt() or second.toInt() },
//        IAND to { first: Number, second: Number -> first.toInt() and second.toInt() },
//        IREM to { first: Number, second: Number -> first.toInt() % second.toInt() },
//        ISHR to { first: Number, second: Number -> first.toInt() shr second.toInt() },
//        ISHL to { first: Number, second: Number -> first.toInt() shl second.toInt() },
//        IUSHR to { first: Number, second: Number -> first.toInt() ushr second.toInt() }
//    )
//
//    // unary arithmetics
//    private val unaryArithmetics = mapOf<Int, (Number) -> Number>(
//        INEG to { num: Number -> -num.toInt() },
//        I2B to { num: Number -> num.toByte() },
//        I2C to { num: Number -> num.toShort() },
//        I2D to { num: Number -> num.toDouble() },
//        I2L to { num: Number -> num.toLong() },
//        I2S to { num: Number -> num.toShort() }
//    )
//
//    override fun run(base: ObfuscatorContext?) {
//        for (klass in base?.whitelistedClasses!!) {
//            for (method in klass.base.methods) {
//                if (!Internal.isAccess(method.access, ACC_STATIC)) continue
//
//                val simulator = Simulator(method, 0).sim()
//
//                for (insn in method.instructions) {
//                    with (simulator) {
//                        when (insn.opcode) {
//                            IXOR -> {
//                                val x = unbox(method.instructions[downcall(2).offset], method)
//                                val y = unbox(method.instructions[downcall(1).offset], method)
//
//                                if (x != 420 && y != 420) {
//                                    println("simulated: $x ^ $y = ${x xor y}")
//                                }
//                            }
//                        }
//                    }
//                }
//            }
//        }
//    }
//
//    fun binaryArithmetic(first: Number, second: Number, opcode: Int): Number {
//        return binaryArithmetics[opcode]?.invoke(first, second)
//            ?: throw IllegalArgumentException("Unsupported opcode: $opcode")
//    }
//
//    fun unaryArithmetic(first: Number, opcode: Int): Number {
//        return unaryArithmetics[opcode]?.invoke(first)
//            ?: throw IllegalArgumentException("Unsupported opcode: $opcode")
//    }
//}