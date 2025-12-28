@file:Suppress("UNCHECKED_CAST")

package war.metaphor.sim

import org.objectweb.asm.tree.AbstractInsnNode
import org.objectweb.asm.tree.MethodNode
import java.util.*

import org.objectweb.asm.Opcodes.*
import org.objectweb.asm.Type
import org.objectweb.asm.tree.IincInsnNode
import org.objectweb.asm.tree.IntInsnNode
import org.objectweb.asm.tree.InvokeDynamicInsnNode
import org.objectweb.asm.tree.JumpInsnNode
import org.objectweb.asm.tree.LabelNode
import org.objectweb.asm.tree.LdcInsnNode
import org.objectweb.asm.tree.MethodInsnNode
import org.objectweb.asm.tree.TableSwitchInsnNode
import org.objectweb.asm.tree.VarInsnNode

/**
 * State-of-the-art JVM simulator that does not care about
 * types (and only properly tried to handle ints anyway)
 * @author jan
 */
class Simulator(private val mn: MethodNode) {
    val stack = ArrayList<Value>()
    val locals = Array(mn.maxLocals) {Value(Placeholder(), false, null)}
    var idx = 0

    fun pop(): Value = stack.removeLast()

    fun simulateUntil(target: AbstractInsnNode): ArrayList<Value> {
        while(idx != mn.instructions.size) {
            val insn = mn.instructions.get(idx++)

            if (insn == target) return stack

            when (insn.opcode) {
                /* push */
                LDC -> stack.add(Value((insn as LdcInsnNode).cst, true, arrayOf(insn)))
                ACONST_NULL -> stack.add(Value(null, true, arrayOf(insn)))
                ICONST_M1 -> stack.add(Value(-1, true, arrayOf(insn)))
                ICONST_0 -> stack.add(Value(0, true, arrayOf(insn)))
                ICONST_1 -> stack.add(Value(1, true, arrayOf(insn)))
                ICONST_2 -> stack.add(Value(2, true, arrayOf(insn)))
                ICONST_3 -> stack.add(Value(3, true, arrayOf(insn)))
                ICONST_4 -> stack.add(Value(4, true, arrayOf(insn)))
                ICONST_5 -> stack.add(Value(5, true, arrayOf(insn)))
                FCONST_0 -> insertUnresolved()
                FCONST_1 -> insertUnresolved()
                FCONST_2 -> insertUnresolved()
                DCONST_0 -> insertUnresolved()
                DCONST_1 -> insertUnresolved()
                BIPUSH -> stack.add(Value((insn as IntInsnNode).operand, true, arrayOf(insn)))
                SIPUSH -> stack.add(Value((insn as IntInsnNode).operand, true, arrayOf(insn)))

                /* stack */
                DUP -> { // TODO: Add to creator insn list
                    val a = pop()
                    stack.add(a)
                    stack.add(a)
                }
                POP -> pop()
                POP2 -> {
                    pop()
                    pop()
                }
                SWAP -> { // TODO: Add to creator insn list
                    val a = pop()
                    val b = pop()
                    stack.add(a)
                    stack.add(b)
                }

                /* jumps */
                GOTO -> jump(insn)
                IF_ICMPNE -> {
                    val b = pop()
                    val a = pop()

                    if (!a.resolved) continue
                    if (!b.resolved) continue

                    val aVal = a.value as Int
                    val bVal = b.value as Int

                    if (aVal != bVal) jump(insn)
                }
                IF_ICMPEQ -> {
                    val b = pop()
                    val a = pop()

                    if (!a.resolved) continue
                    if (!b.resolved) continue

                    val aVal = a.value as Int
                    val bVal = b.value as Int

                    if (aVal == bVal) jump(insn)
                }
                IF_ICMPGE -> {
                    val b = pop()
                    val a = pop()

                    if (!a.resolved) continue
                    if (!b.resolved) continue

                    val aVal = a.value as Int
                    val bVal = b.value as Int

                    if (aVal > bVal) jump(insn)
                }
                IFNE -> {
                    val a = pop()

                    if (!a.resolved) continue

                    val aVal = a.value as Int

                    if (aVal != 0) jump(insn)
                }
                TABLESWITCH -> {
                    val a = pop()
                    val tableSwitchNode = insn as TableSwitchInsnNode

                    if (!a.resolved) continue

                    val aVal = a.value as Int

                    var lbl = tableSwitchNode.dflt

                    val idx = aVal - tableSwitchNode.min

                    if (idx <= tableSwitchNode.labels.size) {
                        lbl = tableSwitchNode.labels[idx]
                    }

                    jumpToLabel(lbl)
                }

                /* arrays */
                ANEWARRAY -> {
                    pop() // size
                    insertUnresolved()
                }
                NEWARRAY -> {
                    val arrayType = (insn as IntInsnNode).operand
                    val size = pop().value as Int

                    stack.add(Value(when (arrayType) {
                        T_BOOLEAN -> BooleanArray(size)
                        T_CHAR -> CharArray(size)
                        T_FLOAT -> FloatArray(size)
                        T_DOUBLE -> DoubleArray(size)
                        T_BYTE -> ByteArray(size)
                        T_SHORT -> ShortArray(size)
                        T_INT -> IntArray(size)
                        T_LONG -> LongArray(size)
                        else -> throw IllegalStateException("Illegal array type $arrayType")
                    }, true, arrayOf(insn)))
                }
                IASTORE, LASTORE, FASTORE, DASTORE, AASTORE, BASTORE, CASTORE, SASTORE -> {
                    val value = pop()
                    val index = pop()
                    val array = pop()

                    if (array.resolved) {
                        if (index.resolved) {
                            val indexedArray = (array.value as Array<Value>)[index.value as Int]

                            if (value.resolved) {
                                indexedArray.value = value
                            } else {
                                indexedArray.resolved = false
                                indexedArray.value = Placeholder.me
                            }
                        }
                    }
                }
                IALOAD, LALOAD, FALOAD, DALOAD, AALOAD, BALOAD, CALOAD, SALOAD -> {
                    val index = pop()
                    val array = pop()

                    if (array.resolved) {
                        if (index.resolved) {
                            stack.add((array.value as Array<Value>)[index.value as Int])
                        } else {
                            insertUnresolved()
                        }
                    } else {
                        insertUnresolved()
                    }
                }
                ARRAYLENGTH -> {
                    val array = pop()

                    if (array.resolved) {
                        stack.add(Value((array.value as Array<*>).size, true, arrayOf(insn)))
                    } else {
                        insertUnresolved()
                    }
                }

                /* locals */
                ISTORE, LSTORE, FSTORE, DSTORE, ASTORE -> {
                    locals[(insn as VarInsnNode).`var`] = pop()
                }
                ILOAD, LLOAD, FLOAD, DLOAD, ALOAD -> {
                    val value = locals[(insn as VarInsnNode).`var`]
                    value.creatorNodes = arrayOf(insn)
                    stack.add(value)
                }
                IINC -> {
                    val iinc = (insn as IincInsnNode)
                    val value = locals[iinc.`var`]
                    value.value = (value.value as Int) + iinc.incr
                }

                /* integer math */
                IADD -> doMath(insn, MathType.ADD)
                ISUB -> doMath(insn, MathType.SUB)
                IDIV -> doMath(insn, MathType.DIV)
                IMUL -> doMath(insn, MathType.MUL)
                IXOR -> doMath(insn, MathType.XOR)
                ISHL -> doMath(insn, MathType.SHL)
                ISHR -> doMath(insn, MathType.SHR)
                IAND -> doMath(insn, MathType.AND)
                IOR -> doMath(insn, MathType.OR)
                IREM -> doMath(insn, MathType.REM)
                IUSHR -> doMath(insn, MathType.USHR)
                INEG -> stack.add(Value(-(pop().value as Int), true, arrayOf(insn)))

                /* fields */
                GETSTATIC -> insertUnresolved()
                GETFIELD -> {
                    pop()
                    insertUnresolved()
                }
                PUTSTATIC -> pop()
                PUTFIELD -> {
                    pop()
                    pop()
                }

                /* invoke */
                INVOKESTATIC -> handleMethodArgs(insn as MethodInsnNode)
                INVOKEVIRTUAL, INVOKEINTERFACE, INVOKESPECIAL -> {
                    pop() // instance
                    handleMethodArgs(insn as MethodInsnNode)
                }
                INVOKEDYNAMIC -> {
                    val indy = (insn as InvokeDynamicInsnNode)
                    for (ignored in Type.getArgumentTypes(indy.desc)) {
                        pop()
                    }

                    if (Type.getReturnType(indy.desc) != Type.VOID_TYPE) {
                        insertUnresolved()
                    }
                }

                /* misc */
                NEW -> insertUnresolved()
                CHECKCAST -> {}
                INSTANCEOF -> insertUnresolved()
                MONITORENTER, MONITOREXIT -> pop()

                /* non-opcode */
                -1, 0 -> {}

                else -> throw IllegalStateException("Illegal opcode ${insn.opcode}")
            }
        }

        return stack
    }

    private fun jump(insn: AbstractInsnNode) = jumpToLabel((insn as JumpInsnNode).label)

    private fun jumpToLabel(label: LabelNode) {
        idx = mn.instructions.indexOf(label)
    }

    private fun handleMethodArgs(node: MethodInsnNode) {
        for (ignored in Type.getArgumentTypes(node.desc)) {
            pop()
        }

        if (Type.getReturnType(node.desc) != Type.VOID_TYPE) {
            insertUnresolved()
        }
    }

    private fun doMath(insn: AbstractInsnNode, type: MathType) {
        val b = pop()
        val a = pop()

        if (a.resolved && b.resolved) {
            val output = type.func.apply(a, b)
            stack.add(Value(output, true, arrayOf(a.creatorNodes!![0], b.creatorNodes!![0], insn)))
        } else {
            insertUnresolved()
        }
    }

    private fun insertUnresolved() {
        stack.add(Value(Placeholder.me, false, null))
    }
}