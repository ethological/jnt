package war.jnt.core.code.impl

import org.objectweb.asm.Opcodes.*
import org.objectweb.asm.tree.TypeInsnNode
import war.jnt.cache.Cache.Companion.request_klass
import war.jnt.core.code.UnitContext
import war.jnt.core.vm.TempJumpVM
import war.jnt.fusebox.impl.Internal

class TypeUnit {
    companion object {
        fun process(insn: TypeInsnNode, ctx: UnitContext, tjvm: TempJumpVM) {

            val idx = request_klass(insn.desc)
            when (insn.opcode) {
                NEW -> {
                    val computed = Internal.computePush(ctx.tracker)
//                    ctx.append("\t// NEW ${insn.desc}\n")
                    tjvm.makeValue(27)
                    ctx.append("\t$computed.l = ((jobject (*)(JNIEnv *, jclass)) (*((void **)*env + *(volatile int *)&output)))(env, request_klass(env, $idx));\n")
                }
                ANEWARRAY -> {
                    val size = Internal.computePop(ctx.tracker)
                    val computed = Internal.computePush(ctx.tracker)
                    tjvm.makeValue(172)
                    ctx.append("\t$computed.l = ((jobjectArray (*)(JNIEnv *, jsize, jclass, jobject)) (*((void **)*env + *(volatile int *)&output)))(env, $size.i, request_klass(env, $idx), NULL);\n")
                }
                CHECKCAST -> { // TODO: make dynamic call (i cba to handle 2 calls rn)
                    val computed = Internal.computePop(ctx.tracker)
                    val cceIdx = request_klass("java/lang/ClassCastException")
                    ctx.append("\tif (!(*env)->IsInstanceOf(env, $computed.l, request_klass(env, $idx))) {\n\t\t(*env)->ThrowNew(env, request_klass(env, $cceIdx), \"bad type cast\");\n\t}\n")
                    Internal.computePush(ctx.tracker)
                }
                INSTANCEOF -> {
                    val computed = Internal.computePop(ctx.tracker)
                    tjvm.makeValue(32)
                    ctx.append("$computed.i = $computed.l != NULL && (((jboolean (*)(JNIEnv *, jobject, jclass)) (*((void **)*env + *(volatile int *)&output)))(env, $computed.l, request_klass(env, $idx))) ? 1 : 0;")
                    Internal.computePush(ctx.tracker)
                }
            }
            //if (insn.opcode != Opcodes.NEW) TODO("Unsupported type instruction. ${insn.opcode}")
        }
    }
}