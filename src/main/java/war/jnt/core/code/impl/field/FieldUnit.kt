package war.jnt.core.code.impl.field

import org.apache.commons.lang3.RandomUtils
import org.objectweb.asm.Opcodes.*
import org.objectweb.asm.tree.FieldInsnNode
import war.jnt.cache.Cache.Companion.request_field
import war.jnt.cache.Cache.Companion.request_klass
import war.jnt.cache.struct.CachedField
import war.jnt.core.code.UnitContext
import war.jnt.core.vm.TempJumpVM
import war.jnt.fusebox.impl.Internal
import war.jnt.fusebox.impl.VariableManager
import war.jnt.innercache.CacheMemberInfo
import war.jnt.innercache.InnerCache

// absolute fucking maggot code
class FieldUnit {
    companion object {
        fun process(ic: InnerCache, insn: FieldInsnNode, ctx: UnitContext, tjvm: TempJumpVM, varMan: VariableManager) {
            val func = Internal.resolveFieldOffset(insn.desc, insn.opcode)

            val klassName = ic.FindClass(insn.owner)
            val type = Internal.fromFieldType(insn.desc)
            val npe = ic.FindClass("java/lang/NullPointerException")

            val xorKey = RandomUtils.nextInt()

            when (insn.opcode) {
                GETSTATIC -> {
                    val name = ic.GetStaticField(CacheMemberInfo(
                        insn.owner, insn.name, insn.desc
                    ))
                    val psh = Internal.computePush(ctx.tracker)

                    tjvm.makeValue(func.offset.xor(xorKey))
                    ctx.append("\t$psh$type = ((${func.value} (*)(JNIEnv *, jclass, jfieldID)) (*((void **)*env + (*(volatile int *)&output ^ $xorKey))))(env, $klassName, $name);\n")

                    ctx.fieldManager.fields++
                    ctx.fieldManager.classes++
                }
                GETFIELD -> {
                    val pop = Internal.computePop(ctx.tracker)
                    val psh = Internal.computePush(ctx.tracker)
                    val type = Internal.fromFieldType(insn.desc)

                    ctx.fmtAppend(
                        "\tif (%s.l == NULL) { (*env)->ThrowNew(env, $npe, \"instance is null\"); goto %s; }\n",
                        pop, ctx.handlerLabel
                    )

                    tjvm.makeValue(func.offset.xor(xorKey))
                    val name = ic.GetVirtualField(CacheMemberInfo(
                        insn.owner, insn.name, insn.desc
                    ))
                    ctx.fmtAppend("\t$psh$type = ((${func.value} (*)(JNIEnv *, jobject, jfieldID)) (*((void **)*env + (*(volatile int *)&output ^ $xorKey))))(env, $pop.l, $name);\n")

                    ctx.fieldManager.fields++
                    ctx.fieldManager.classes++
                }
                PUTSTATIC -> {
                    val name = ic.GetStaticField(CacheMemberInfo(
                        insn.owner, insn.name, insn.desc
                    ))

                    val pop = Internal.computePop(ctx.tracker)

                    tjvm.makeValue(func.offset.xor(xorKey))
                    ctx.append("\t((void (*)(JNIEnv *, jclass, jfieldID, ${func.value})) (*((void **)*env + (*(volatile int *)&output ^ $xorKey))))(env, $klassName, $name, $pop$type);\n")

                    ctx.fieldManager.fields++
                    ctx.fieldManager.classes++
                }
                PUTFIELD -> {
                    val name = ic.GetVirtualField(CacheMemberInfo(
                        insn.owner, insn.name, insn.desc
                    ))

                    val value = Internal.computePop(ctx.tracker)
                    val obj = Internal.computePop(ctx.tracker)

                    ctx.fmtAppend(
                        "\tif (%s.l == NULL) { (*env)->ThrowNew(env, $npe, \"instance is null\"); goto %s; }\n",
                        obj, ctx.handlerLabel
                    )

                    tjvm.makeValue(func.offset.xor(xorKey))
                    ctx.append("\t((void (*)(JNIEnv *, jobject, jfieldID, ${func.value})) (*((void **)*env + (*(volatile int *)&output ^ $xorKey))))(env, $obj.l, $name, $value$type);\n")

                    ctx.fieldManager.fields++
                    ctx.fieldManager.classes++
                }
            }
        }
    }
}