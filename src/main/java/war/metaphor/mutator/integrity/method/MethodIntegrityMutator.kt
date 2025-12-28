package war.metaphor.mutator.integrity.method

import org.objectweb.asm.ClassReader
import org.objectweb.asm.Type
import org.objectweb.asm.tree.*
import war.configuration.ConfigurationSection
import war.jnt.annotate.Level
import war.jnt.annotate.Stability
import war.metaphor.base.ObfuscatorContext
import war.metaphor.mutator.Mutator
import war.metaphor.tree.JClassNode
import war.metaphor.util.asm.BytecodeUtil
import java.io.File
import java.lang.reflect.Modifier
import java.util.jar.JarFile


/**
 * @author etho
 */

/*
 *  TODO: fix
 *
 *  Exception in thread "main" java.lang.SecurityException: Modified method
 *       at dev.sim0n.iridium.math.statistic.Stats.stdDev(Unknown Source)
 *       at dev.sim0n.app.test.impl.numbers.NumberComparisonTest.run(Unknown Source)
 *       at java.base/java.util.Arrays$ArrayList.forEach(Unknown Source)
 *       at dev.sim0n.app.test.TestRepository.run(Unknown Source)
 *       at dev.sim0n.app.Application.run(Unknown Source)
 *       at dev.sim0n.app.Main.main(Unknown Source)
 */

// p.s. i fucking hate visitors
@Stability(Level.LOW)
class MethodIntegrityMutator(base: ObfuscatorContext, config: ConfigurationSection) : Mutator(base, config) {

    private fun buildByteGetter(): MethodNode {
        val node = MethodNode(ACC_PUBLIC or ACC_STATIC, "getClassBytes", "(Ljava/lang/Class;)[B", "(Ljava/lang/Class<*>;)[B", null)
        val list = InsnList()

        list.add(VarInsnNode(ALOAD, 0))
        list.add(MethodInsnNode(INVOKEVIRTUAL, "java/lang/Class", "getName", "()Ljava/lang/String;", false))
        list.add(IntInsnNode(BIPUSH, 46))
        list.add(IntInsnNode(BIPUSH, 47))
        list.add(MethodInsnNode(INVOKEVIRTUAL, "java/lang/String", "replace", "(CC)Ljava/lang/String;", false))
        list.add(VarInsnNode(ASTORE, 1))

        list.add(TypeInsnNode(NEW, "java/lang/StringBuilder"))
        list.add(InsnNode(DUP))
        list.add(MethodInsnNode(INVOKESPECIAL, "java/lang/StringBuilder", "<init>", "()V", false))
        list.add(VarInsnNode(ALOAD, 1))
        list.add(MethodInsnNode(INVOKEVIRTUAL, "java/lang/StringBuilder", "append", "(Ljava/lang/String;)Ljava/lang/StringBuilder;", false))
        list.add(LdcInsnNode(".class"))
        list.add(MethodInsnNode(INVOKEVIRTUAL, "java/lang/StringBuilder", "append", "(Ljava/lang/String;)Ljava/lang/StringBuilder;", false))
        list.add(MethodInsnNode(INVOKEVIRTUAL, "java/lang/StringBuilder", "toString", "()Ljava/lang/String;", false))
        list.add(VarInsnNode(ASTORE, 1))

        list.add(VarInsnNode(ALOAD, 0))
        list.add(MethodInsnNode(INVOKEVIRTUAL, "java/lang/Class", "getClassLoader", "()Ljava/lang/ClassLoader;", false))
        list.add(VarInsnNode(ALOAD, 1))
        list.add(MethodInsnNode(INVOKEVIRTUAL, "java/lang/ClassLoader", "getResourceAsStream", "(Ljava/lang/String;)Ljava/io/InputStream;", false))
        list.add(VarInsnNode(ASTORE, 2))

        val labelNonNull = LabelNode()
        list.add(VarInsnNode(ALOAD, 2))
        list.add(JumpInsnNode(IFNONNULL, labelNonNull))
        list.add(TypeInsnNode(NEW, "java/io/IOException"))
        list.add(InsnNode(DUP))
        list.add(TypeInsnNode(NEW, "java/lang/StringBuilder"))
        list.add(InsnNode(DUP))
        list.add(MethodInsnNode(INVOKESPECIAL, "java/lang/StringBuilder", "<init>", "()V", false))
        list.add(LdcInsnNode("Class not found: "))
        list.add(MethodInsnNode(INVOKEVIRTUAL, "java/lang/StringBuilder", "append", "(Ljava/lang/String;)Ljava/lang/StringBuilder;", false))
        list.add(VarInsnNode(ALOAD, 1))
        list.add(MethodInsnNode(INVOKEVIRTUAL, "java/lang/StringBuilder", "append", "(Ljava/lang/String;)Ljava/lang/StringBuilder;", false))
        list.add(MethodInsnNode(INVOKEVIRTUAL, "java/lang/StringBuilder", "toString", "()Ljava/lang/String;", false))
        list.add(MethodInsnNode(INVOKESPECIAL, "java/io/IOException", "<init>", "(Ljava/lang/String;)V", false))
        list.add(InsnNode(ATHROW))

        list.add(labelNonNull)
        list.add(VarInsnNode(ALOAD, 2))
        list.add(MethodInsnNode(INVOKEVIRTUAL, "java/io/InputStream", "readAllBytes", "()[B", false))
        list.add(VarInsnNode(ASTORE, 3))
        list.add(VarInsnNode(ALOAD, 2))
        list.add(MethodInsnNode(INVOKEVIRTUAL, "java/io/InputStream", "close", "()V", false))
        list.add(VarInsnNode(ALOAD, 3))
        list.add(InsnNode(ARETURN))

        node.instructions = list
        node.maxStack = 3
        node.maxLocals = 4
        return node
    }

    private fun getIntegrityCode(
        method: String,
        klass: String,
        bytesVar: Int,
        readerVar: Int,
        nodeVar: Int,
        countVar: Int,
        methodNodeVar: Int,
        ainVar: Int,
        iteratorVar: Int,
        otherIteratorVar: Int
    ): InsnList {
        val list = InsnList()

        list.add(LdcInsnNode(Type.getType("L$klass;")))
        list.add(MethodInsnNode(INVOKESTATIC, klass, "getClassBytes", "(Ljava/lang/Class;)[B", false))
        list.add(VarInsnNode(ASTORE, bytesVar))

        list.add(TypeInsnNode(NEW, "org/objectweb/asm/ClassReader"))
        list.add(InsnNode(DUP))
        list.add(VarInsnNode(ALOAD, bytesVar))
        list.add(MethodInsnNode(INVOKESPECIAL, "org/objectweb/asm/ClassReader", "<init>", "([B)V", false))
        list.add(VarInsnNode(ASTORE, readerVar))

        list.add(TypeInsnNode(NEW, "org/objectweb/asm/tree/ClassNode"))
        list.add(InsnNode(DUP))
        list.add(MethodInsnNode(INVOKESPECIAL, "org/objectweb/asm/tree/ClassNode", "<init>", "()V", false))
        list.add(VarInsnNode(ASTORE, nodeVar))

        list.add(VarInsnNode(ALOAD, readerVar))
        list.add(VarInsnNode(ALOAD, nodeVar))
        list.add(InsnNode(ICONST_0))
        list.add(MethodInsnNode(INVOKEVIRTUAL, "org/objectweb/asm/ClassReader", "accept", "(Lorg/objectweb/asm/ClassVisitor;I)V", false))

        list.add(InsnNode(ICONST_0))
        list.add(VarInsnNode(ISTORE, countVar))

        list.add(VarInsnNode(ALOAD, nodeVar))
        list.add(FieldInsnNode(GETFIELD, "org/objectweb/asm/tree/ClassNode", "methods", "Ljava/util/List;"))
        list.add(MethodInsnNode(INVOKEINTERFACE, "java/util/List", "iterator", "()Ljava/util/Iterator;", true))
        list.add(VarInsnNode(ASTORE, iteratorVar))

        val loopStart = LabelNode()
        val loopEnd = LabelNode()
        list.add(loopStart)
        list.add(VarInsnNode(ALOAD, iteratorVar))
        list.add(MethodInsnNode(INVOKEINTERFACE, "java/util/Iterator", "hasNext", "()Z", true))
        list.add(JumpInsnNode(IFEQ, loopEnd))
        list.add(VarInsnNode(ALOAD, iteratorVar))
        list.add(MethodInsnNode(INVOKEINTERFACE, "java/util/Iterator", "next", "()Ljava/lang/Object;", true))
        list.add(TypeInsnNode(CHECKCAST, "org/objectweb/asm/tree/MethodNode"))
        list.add(VarInsnNode(ASTORE, methodNodeVar))

        val nextMethod = LabelNode()
        list.add(VarInsnNode(ALOAD, methodNodeVar))
        list.add(FieldInsnNode(GETFIELD, "org/objectweb/asm/tree/MethodNode", "name", "Ljava/lang/String;"))
        list.add(LdcInsnNode(method))
        list.add(MethodInsnNode(INVOKEVIRTUAL, "java/lang/String", "equals", "(Ljava/lang/Object;)Z", false))
        list.add(JumpInsnNode(IFEQ, nextMethod))

        list.add(VarInsnNode(ALOAD, methodNodeVar))
        list.add(FieldInsnNode(GETFIELD, "org/objectweb/asm/tree/MethodNode", "instructions", "Lorg/objectweb/asm/tree/InsnList;"))
        list.add(MethodInsnNode(INVOKEVIRTUAL, "org/objectweb/asm/tree/InsnList", "iterator", "()Ljava/util/ListIterator;", false))
        list.add(VarInsnNode(ASTORE, otherIteratorVar))

        val otherLoopStart = LabelNode()
        list.add(otherLoopStart)
        list.add(VarInsnNode(ALOAD, otherIteratorVar))
        list.add(MethodInsnNode(INVOKEINTERFACE, "java/util/Iterator", "hasNext", "()Z", true))
        list.add(JumpInsnNode(IFEQ, nextMethod))
        list.add(VarInsnNode(ALOAD, otherIteratorVar))
        list.add(MethodInsnNode(INVOKEINTERFACE, "java/util/Iterator", "next", "()Ljava/lang/Object;", true))
        list.add(TypeInsnNode(CHECKCAST, "org/objectweb/asm/tree/AbstractInsnNode"))
        list.add(VarInsnNode(ASTORE, ainVar))

        list.add(VarInsnNode(ALOAD, ainVar))
        list.add(TypeInsnNode(INSTANCEOF, "org/objectweb/asm/tree/LineNumberNode"))
        list.add(JumpInsnNode(IFNE, otherLoopStart))
        list.add(VarInsnNode(ALOAD, ainVar))
        list.add(TypeInsnNode(INSTANCEOF, "org/objectweb/asm/tree/FrameNode"))
        list.add(JumpInsnNode(IFNE, otherLoopStart))
        list.add(VarInsnNode(ALOAD, ainVar))
        list.add(TypeInsnNode(INSTANCEOF, "org/objectweb/asm/tree/LabelNode"))
        list.add(JumpInsnNode(IFNE, otherLoopStart))

        list.add(IincInsnNode(countVar, 1))
        list.add(JumpInsnNode(GOTO, otherLoopStart))

        list.add(nextMethod)
        list.add(JumpInsnNode(GOTO, loopStart))
        list.add(loopEnd)

        return list
    }

    private fun sizeOfCode(list: InsnList): Int {
        return list.toArray().count { ain ->
            ain !is LineNumberNode && ain !is FrameNode && ain !is LabelNode
        }
    }

    fun loadFromJar(jarFile: File, base: ObfuscatorContext) {
        JarFile(jarFile).use { jar ->
            jar.entries().asSequence()
                .filter { it.name.endsWith(".class") }
                .forEach { entry ->
                    jar.getInputStream(entry).use { inputStream ->
                        val bytes = inputStream.readAllBytes()

                        val node = JClassNode()
                        val reader = ClassReader(bytes)
                        reader.accept(node, ClassReader.SKIP_FRAMES)

                        node.version = V1_8

                        base.addClass(node)
                    }
                }
        }
    }

    override fun run(base: ObfuscatorContext) {
        loadFromJar(File("libraries/asm-tree.jar"), base)
        loadFromJar(File("libraries/asm-pure.jar"), base)

        base.classes.forEach { jcn ->
            if (jcn.name.startsWith("org/objectweb") || Modifier.isInterface(jcn.access)) {
                return@forEach
            }

            jcn.methods.add(buildByteGetter())

            jcn.methods.forEach { mn ->
                val leeway = BytecodeUtil.leeway(mn)

                if (leeway < 30000)
                    return@forEach

                if (mn.name == "<clinit>" || "jnt" in mn.name
                    || Modifier.isNative(mn.access) || Modifier.isAbstract(mn.access) || mn.name == "getClassBytes"
                ) {
                    return@forEach
                }

                val v0 = mn.maxLocals++
                val v1 = mn.maxLocals++
                val v2 = mn.maxLocals++
                val v3 = mn.maxLocals++
                val v4 = mn.maxLocals++
                val v5 = mn.maxLocals++
                val v6 = mn.maxLocals++
                val v7 = mn.maxLocals++

                val code = getIntegrityCode(mn.name, jcn.name, v0, v1, v2, v3, v4, v5, v6, v7)
                val codeSize = sizeOfCode(code)

                val check = InsnList().apply {
                    val begin = LabelNode()

                    add(VarInsnNode(ILOAD, v3))
                    add(LdcInsnNode(0))
                    add(JumpInsnNode(IF_ICMPEQ, begin))

                    add(TypeInsnNode(NEW, "java/lang/SecurityException"))
                    add(InsnNode(DUP))
                    add(LdcInsnNode("Modified method"))
                    add(MethodInsnNode(INVOKESPECIAL, "java/lang/SecurityException", "<init>", "(Ljava/lang/String;)V"))
                    add(InsnNode(ATHROW))

                    add(begin)
                }

                val checkSize = sizeOfCode(check)

                val size = sizeOfCode(mn.instructions)
                val expectedSize = size + codeSize + checkSize

                check.set(check.get(1), LdcInsnNode(expectedSize))

                mn.instructions = InsnList().apply {
                    add(code)
                    add(check)
                    add(mn.instructions)
                }

                println("${mn.name} ${sizeOfCode(mn.instructions)}")
            }
        }
    }
}