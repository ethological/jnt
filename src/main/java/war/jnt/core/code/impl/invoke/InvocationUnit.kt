package war.jnt.core.code.impl.invoke

import org.apache.commons.lang3.RandomUtils
import org.objectweb.asm.Opcodes.*
import org.objectweb.asm.Type
import org.objectweb.asm.tree.MethodInsnNode
import war.jnt.cache.Cache.Companion.request_klass
import war.jnt.core.code.UnitContext
import war.jnt.core.vm.TempJumpVM
import war.jnt.fusebox.impl.Internal
import war.jnt.fusebox.impl.VariableManager
import war.jnt.innercache.InnerCache

/**
 * @author etho
 */
class InvocationUnit {
    companion object {
        private fun signature(min: MethodInsnNode): String = "${min.owner}#${min.name}.${min.desc}"

        private val singleArgLookup: Map<String, String> = mapOf(
            // exit

            "java/lang/Runtime#halt.(I)V" to "\tjnt_Runtime_halt(env, %s.i);\n",
            "java/lang/System#exit.(I)V" to "\tjnt_System_exit(env, %s.i);\n",

            // Abs
            "java/lang/Math#abs.(I)I" to "\t%s.i = jnt_Math_abs_int(env, %s.i);\n",
            "java/lang/Math#abs.(F)F" to "\t%s.f = jnt_Math_abs_float(env, %s.f);\n",
            "java/lang/Math#abs.(J)J" to "\t%s.j = jnt_Math_abs_long(env, %s.j);\n",
            "java/lang/Math#abs.(D)D" to "\t%s.d = jnt_Math_abs_double(env, %s.d);\n",

            // Sqrt
            "java/lang/Math#sqrt.(D)D" to "\t%s.d = jnt_Math_sqrt_double(env, %s.d);\n",

            // Round
            "java/lang/Math#round.(F)F" to "\t%s.f = jnt_Math_round_float(env, %s.f);\n",
            "java/lang/Math#round.(D)D" to "\t%s.d = jnt_Math_round_double(env, %s.d);\n",
        )

        private val dualArgLookup: Map<String, String> = mapOf(
            // Min
            "java/lang/Math#min.(II)I" to "\t%s.i = jnt_Math_min_int(env, %s.i, %s.i);\n",
            "java/lang/Math#min.(FF)F" to "\t%s.f = jnt_Math_min_float(env, %s.f, %s.f);\n",
            "java/lang/Math#min.(JJ)J" to "\t%s.j = jnt_Math_min_long(env, %s.j, %s.j);\n",
            "java/lang/Math#min.(DD)D" to "\t%s.d = jnt_Math_min_double(env, %s.d, %s.d);\n",

            // Max
            "java/lang/Math#max.(II)I" to "\t%s.i = jnt_Math_max_int(env, %s.i, %s.i);\n",
            "java/lang/Math#max.(FF)F" to "\t%s.f = jnt_Math_max_float(env, %s.f, %s.f);\n",
            "java/lang/Math#max.(JJ)J" to "\t%s.j = jnt_Math_max_long(env, %s.j, %s.j);\n",
            "java/lang/Math#max.(DD)D" to "\t%s.d = jnt_Math_max_double(env, %s.d, %s.d);\n",

            // Pow
            "java/lang/Math#pow.(DD)D" to "\t%s.d = jnt_Math_pow_double(env, %s.d, %s.d);\n"
        )

        private fun handle(ic: InnerCache, lookup: Lookup?, ain: MethodInsnNode, ctx: UnitContext, tjvm: TempJumpVM, shouldIntrinsic: Boolean) {
            val signature = signature(ain)
            val comment = "// Replaced func access with an intrinsic func: \"$signature\""
            val npe = ic.FindClass("java/lang/NullPointerException")

            var yeah = false

            when (ain.opcode) {
                INVOKESPECIAL, INVOKEVIRTUAL, INVOKEINTERFACE -> {
                    var sp = ctx.tracker.dump()
                    val argc = Type.getArgumentCount(ain.desc)
                    for (i in argc - 1 downTo 0) {
                        sp--
                    }
                    val pop = "stack[${sp}]"
                    ctx.fmtAppend(
                        "\tif (%s.l == NULL) { (*env)->ThrowNew(env, $npe, \"instance is null\"); goto %s; }\n",
                        pop, ctx.handlerLabel
                    )
                }
            }

            if (shouldIntrinsic) {

                when {
                    ain.name.equals("getClass") && ain.owner.equals("java/lang/Object") -> {
                        val computedPop = Internal.computePop(ctx.tracker)
                        val computedPush = Internal.computePush(ctx.tracker)

                        ctx.fmtAppend("\t%s.l = jnt_Object_getClass(env, %s.l);\n", computedPush,
                            computedPop)

                        yeah = true
                    }
                    ain.name.equals("length") && ain.owner.equals("java/lang/String") -> {
                        val computedPop = Internal.computePop(ctx.tracker)
                        val computedPush = Internal.computePush(ctx.tracker)

                        ctx.fmtAppend("\t%s.i = jnt_String_length(env, %s.l);\n", computedPush,
                            computedPop)

                        yeah = true
                    }
                    ain.name.equals("isEmpty") && ain.owner.equals("java/lang/String") -> {
                        val computedPop = Internal.computePop(ctx.tracker)
                        val computedPush = Internal.computePush(ctx.tracker)

                        ctx.fmtAppend("\t%s.i = jnt_String_isEmpty(env, %s.l);\n", computedPush,
                            computedPop)

                        yeah = true
                    }
    //                ain.name.equals("charAt") && ain.owner.equals("java/lang/String") -> {
    //                    val popIndex = Internal.computePop(ctx.tracker)
    //                    val popInstance = Internal.computePop(ctx.tracker)
    //
    //                    val computedPush = Internal.computePush(ctx.tracker)
    //                    ctx.fmtAppend("\t%s.c = jnt_String_charAt(env, %s.l, %s.i);\n", computedPush,
    //                        popInstance, popIndex)
    //
    //                    yeah = true
    //                }
    //                ain.name.equals("equals") && ain.owner.equals("java/lang/String") -> {
    //                    val popInstanceB = Internal.computePop(ctx.tracker)
    //                    val popInstanceA = Internal.computePop(ctx.tracker)
    //
    //                    val computedPush = Internal.computePush(ctx.tracker)
    //                    ctx.fmtAppend("\t%s.z = jnt_String_equals(env, %s.l, %s.l);\n", computedPush,
    //                        popInstanceA, popInstanceB)
    //
    //                    yeah = true
    //                }
                }

                when (signature) {
                    in singleArgLookup -> {
                        val computedPop = Internal.computePop(ctx.tracker)

                        val computedPush = Internal.computePush(ctx.tracker)
                        ctx.fmtAppend("\t${comment}\n")
                        ctx.fmtAppend("${singleArgLookup[signature]}", computedPush,
                            computedPop)

                        yeah = true
                    }
                    in dualArgLookup -> {
                        val computedPopB = Internal.computePop(ctx.tracker)
                        val computedPopA = Internal.computePop(ctx.tracker)

                        val computedPush = Internal.computePush(ctx.tracker)
                        ctx.fmtAppend("\t${comment}\n")
                        ctx.fmtAppend("${dualArgLookup[signature]}", computedPush,
                            computedPopA, computedPopB)

                        yeah = true
                    }
                }
            }

            if (yeah) {
//                Logger.INSTANCE.logln(Level.INFO, Origin.CORE, "Swapped intrinsic in ${ctx.classNode.name}")
                return
            }

            val returnType = Type.getReturnType(ain.desc)
            var func = Internal.resolveFunction(ain.desc, ain.opcode)

            val argc = Type.getArgumentCount(ain.desc)

            // TODO: Revisit direct calling

            var name = "args" + ctx.argManager.args

            if (argc != 0) ctx.fmtAppend("\tjvalue %s[%d];\n", name, argc)
            for (i in argc - 1 downTo 0) {
                val pop = Internal.computePop(ctx.tracker)
                ctx.append("\t$name[$i] = $pop;\n")
            }
            if (argc != 0)
                ctx.argManager.args++
            else
                name = "NULL"

            /*
                tjvm.makeValue(13);
                ctx.fmtAppend("\t\t\t((jint (*)(JNIEnv *, jthrowable)) (*((void **)*env + (*(volatile int *)&output ^ $xorKey))))(env, thrown);\n");
             */

            if (ain.name.equals("<init>")) {
                func = "CallVoidMethodA"
            }

            val dynamicCall = Internal.resolveCallIdx(func) // :trol:
            ctx.append("/* $signature */\n")

            val xorKey = RandomUtils.nextInt()

            when (ain.opcode) {
                INVOKESTATIC -> {
                    val classVar = lookup?.classVar
                    val lookupVar = lookup?.lookupVar

                    tjvm.makeValue(dynamicCall.offset.xor(xorKey))

                    when (returnType.sort) {
                        Type.VOID -> {
                            ctx.append("\t((${dynamicCall.value} (*)(JNIEnv *, jclass, jmethodID, jvalue *)) (*((void **)*env + (*(volatile int *)&output ^ $xorKey))))(env, $classVar, $lookupVar, $name);\n")
                        }
                        else -> {
                            val computed = Internal.computePush(ctx.tracker)
                            val computedReturn = Internal.resolveReturnType(ain.desc)
//                            ctx.fmtAppend("\t%s%s = (*env)->%s(env, %s, %s, $name);\n", computed, computedReturn, func, classVar, lookupVar)
                            ctx.append("\t$computed$computedReturn = ((${dynamicCall.value} (*)(JNIEnv *, jclass, jmethodID, jvalue *)) (*((void **)*env + (*(volatile int *)&output ^ $xorKey))))(env, $classVar, $lookupVar, $name);\n")
                        }
                    }
                }
                INVOKESPECIAL, INVOKEVIRTUAL, INVOKEINTERFACE -> {
                    val lookupVar = lookup?.lookupVar
                    val classVar = lookup?.classVar
                    val pop = Internal.computePop(ctx.tracker)

                    tjvm.makeValue(dynamicCall.offset.xor(xorKey))


                    ctx.append("\t/* " + func + " */");

                    if (func.startsWith("CallNonvirtual")) {
                        when (returnType.sort) {
                            Type.VOID -> {
//                                ctx.append("\t(*env)->$func(env, $pop.l, $classVar, $lookupVar, $name);\n")
                                ctx.append("\t((${dynamicCall.value} (*)(JNIEnv *, jobject, jclass, jmethodID, jvalue *)) (*((void **)*env + (*(volatile int *)&output ^ $xorKey))))(env, $pop.l, $classVar, $lookupVar, $name);\n")
                            }
                            else -> {
                                val computed = Internal.computePush(ctx.tracker)
                                val computedReturn = Internal.resolveReturnType(ain.desc)
//                                ctx.append("\t$computed$computedReturn = (*env)->$func(env, $pop.l, $classVar, $lookupVar, $name);\n")
                                ctx.append("\t$computed$computedReturn = ((${dynamicCall.value} (*)(JNIEnv *, jobject, jclass, jmethodID, jvalue *)) (*((void **)*env + (*(volatile int *)&output ^ $xorKey))))(env, $pop.l, $classVar, $lookupVar, $name);\n")
                            }
                        }
                    } else {
                        when (returnType.sort) {
                            Type.VOID -> {
//                                ctx.append("\t(*env)->$func(env, $pop.l, $lookupVar, $name);\n")
                                ctx.append("\t((${dynamicCall.value} (*)(JNIEnv *, jobject, jmethodID, jvalue *)) (*((void **)*env + (*(volatile int *)&output ^ $xorKey))))(env, $pop.l, $lookupVar, $name);\n")
                            }
                            else -> {
                                val computed = Internal.computePush(ctx.tracker)
                                val computedReturn = Internal.resolveReturnType(ain.desc)
                                ctx.append("\t$computed$computedReturn = ((${dynamicCall.value} (*)(JNIEnv *, jobject, jmethodID, jvalue *)) (*((void **)*env + (*(volatile int *)&output ^ $xorKey))))(env, $pop.l, $lookupVar, $name);\n")
//                                ctx.append("\t$computed$computedReturn = (*env)->$func(env, $pop.l, $lookupVar, $name);\n")
                            }
                        }
                    }
                }
            }
        }

        fun process(
            ic: InnerCache,
            insn: MethodInsnNode,
            ctx: UnitContext,
            varMan: VariableManager,
            tjvm: TempJumpVM,
            shouldIntrinsic: Boolean
        ) {
            val lookup: Lookup? = LookupUnit.process(ic, insn, ctx, varMan, shouldIntrinsic)

            handle(ic, lookup, insn, ctx, tjvm, shouldIntrinsic)
        }
    }
}