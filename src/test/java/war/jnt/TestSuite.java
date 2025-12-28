package war.jnt;


import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.Type;
import war.configuration.file.YamlConfiguration;
import war.metaphor.util.builder.ClassBuilder;
import war.metaphor.util.builder.InsnListBuilder;
import war.metaphor.util.builder.MethodBuilder;

public class TestSuite implements Opcodes {

    private YamlConfiguration baseConfig() {
        YamlConfiguration config = new YamlConfiguration();

        config.set("fingerprint", false);

        config.set("zig.os", "windows");
        config.set("zig.arch", "x86_64");
        config.set("zig.version", "0.14.0");
        config.set("zig.installation", "zig");

        config.set("targets", new String[] { "x86_64-windows" });

        config.set("mutators.metaphor.order", new String[0]);
        config.createSection("mutators.metaphor.transformers");

        config.set("mutators.jnt.order", new String[] { "cleanup", "integrate" });
        config.set("mutators.jnt.transformers.cleanup.enabled", true);
        config.set("mutators.jnt.transformers.integrate.enabled", true);

        config.set("mutators.jnt.transformers.integrate.enabled", true);
        config.set("mutators.jnt.virtualize.enabled", false);
        config.set("mutators.jnt.intrinsic", false);

        return config;
    }

    private final TestRunner runner = new TestRunner();

    @Test
    public void testDCMPLWithNaN() {
        Assertions.assertEquals(200,
                runner.runTest(ClassBuilder.create()
                        .withName("TestDCMPL")
                        .withVersion(V1_8)
                        .withSuperName("java/lang/Object")
                        .withMethod(MethodBuilder.create()
                                .withAccess(ACC_PUBLIC | ACC_STATIC)
                                .withName("main").withDesc("([Ljava/lang/String;)V")
                                .withInstructionBuilder(InsnListBuilder.builder()
                                        .constant(Double.NaN)    // push NaN
                                        .constant(5.0)           // push 5.0
                                        .dcmpl()                 // compare
                                        .istore(1)               // store result
                                        .iload(1)
                                        .constant(-1)            // expected for dcmpl NaN
                                        .if_icmpeq("ok")
                                        .constant(0)
                                        .invokestatic("java/lang/System", "exit", "(I)V")
                                        .label("ok")
                                        .constant(200)
                                        .invokestatic("java/lang/System", "exit", "(I)V")
                                        ._return())
                                .build())
                        .build(), baseConfig()),
                "DCMPL NaN test failed");
    }

    @Test
    public void testDCMPGWithNaN() {
        Assertions.assertEquals(200,
                runner.runTest(ClassBuilder.create()
                        .withName("TestDCMPG")
                        .withVersion(V1_8)
                        .withSuperName("java/lang/Object")
                        .withMethod(MethodBuilder.create()
                                .withAccess(ACC_PUBLIC | ACC_STATIC)
                                .withName("main").withDesc("([Ljava/lang/String;)V")
                                .withInstructionBuilder(InsnListBuilder.builder()
                                        .constant(Double.NaN)
                                        .constant(5.0)
                                        .dcmpg()
                                        .istore(1)
                                        .iload(1)
                                        .constant(1)             // expected for dcmpg NaN
                                        .if_icmpeq("ok")
                                        .constant(0)
                                        .invokestatic("java/lang/System", "exit", "(I)V")
                                        .label("ok")
                                        .constant(200)
                                        .invokestatic("java/lang/System", "exit", "(I)V")
                                        ._return())
                                .build())
                        .build(), baseConfig()),
                "DCMPG NaN test failed");
    }

    @Test
    public void testFCMPLAndFCMPGWithNaN() {
        Assertions.assertEquals(200,
                runner.runTest(ClassBuilder.create()
                        .withName("TestFCMP")
                        .withVersion(V1_8)
                        .withSuperName("java/lang/Object")
                        .withMethod(MethodBuilder.create()
                                .withAccess(ACC_PUBLIC | ACC_STATIC)
                                .withName("main").withDesc("([Ljava/lang/String;)V")
                                .withInstructionBuilder(InsnListBuilder.builder()
                                        .constant(Float.NaN)
                                        .constant(2.0f)
                                        .fcmpl()
                                        .istore(1)

                                        .constant(Float.NaN)
                                        .constant(2.0f)
                                        .fcmpg()
                                        .istore(2)

                                        .iload(1)
                                        .constant(-1)
                                        .if_icmpne("fail")

                                        .iload(2)
                                        .constant(1)
                                        .if_icmpne("fail")

                                        .constant(200)
                                        .invokestatic("java/lang/System", "exit", "(I)V")
                                        .label("fail")
                                        .constant(0)
                                        .invokestatic("java/lang/System", "exit", "(I)V")
                                        ._return())
                                .build())
                        .build(), baseConfig()),
                "FCMPL/FCMPG NaN test failed");
    }

    @Test
    public void testPrimitiveClasses() {
        Assertions.assertEquals(200,
                runner.runTest(ClassBuilder.create()
                        .withName("TestPrimitiveClasses")
                        .withVersion(V1_8)
                        .withSuperName("java/lang/Object")
                        .withMethod(MethodBuilder.create()
                                .withAccess(ACC_PUBLIC | ACC_STATIC)
                                .withName("main")
                                .withDesc("([Ljava/lang/String;)V")
                                .withInstructionBuilder(InsnListBuilder.builder()
                                        .iconst_1()
                                        .newarray(T_INT)
                                        .invokevirtual("java/lang/Object", "getClass", "()Ljava/lang/Class;")
                                        .invokevirtual("java/lang/Class", "getComponentType", "()Ljava/lang/Class;")
                                        .getstatic("java/lang/Integer", "TYPE", "Ljava/lang/Class;")
                                        .if_acmpne("fail")

                                        .iconst_1()
                                        .newarray(T_BOOLEAN)
                                        .invokevirtual("java/lang/Object", "getClass", "()Ljava/lang/Class;")
                                        .invokevirtual("java/lang/Class", "getComponentType", "()Ljava/lang/Class;")
                                        .getstatic("java/lang/Boolean", "TYPE", "Ljava/lang/Class;")
                                        .if_acmpne("fail")

                                        .iconst_1()
                                        .newarray(T_DOUBLE)
                                        .invokevirtual("java/lang/Object", "getClass", "()Ljava/lang/Class;")
                                        .invokevirtual("java/lang/Class", "getComponentType", "()Ljava/lang/Class;")
                                        .getstatic("java/lang/Double", "TYPE", "Ljava/lang/Class;")
                                        .if_acmpne("fail")

                                        .iconst_1()
                                        .newarray(T_CHAR)
                                        .invokevirtual("java/lang/Object", "getClass", "()Ljava/lang/Class;")
                                        .invokevirtual("java/lang/Class", "getComponentType", "()Ljava/lang/Class;")
                                        .getstatic("java/lang/Character", "TYPE", "Ljava/lang/Class;")
                                        .if_acmpne("fail")

                                        .ldc(200)
                                        .invokestatic("java/lang/System", "exit", "(I)V")
                                        ._goto("end")

                                        .label("fail")
                                        .iconst_0()
                                        .invokestatic("java/lang/System", "exit", "(I)V")

                                        .label("end")
                                        ._return())
                                .build())
                        .build(), baseConfig()),
                "Primitive class mapping failed");
    }

    @Test
    public void testUtf8ConversionInMetadata() {
        Assertions.assertEquals(200,
                runner.runTest(ClassBuilder.create()
                        .withName("Weird\u0000Class")
                        .withVersion(V1_8)
                        .withSuperName("java/lang/Object")
                        .withMethod(MethodBuilder.create()
                                .withAccess(ACC_PUBLIC | ACC_STATIC)
                                .withName("méthodΩ")
                                .withDesc("()V")
                                .withInstructionBuilder(InsnListBuilder.builder()
                                        .constant(200)
                                        .invokestatic("java/lang/System", "exit", "(I)V")
                                        ._return())
                                .build())
                        .withMethod(MethodBuilder.create()
                                .withAccess(ACC_PUBLIC | ACC_STATIC)
                                .withName("main")
                                .withDesc("([Ljava/lang/String;)V")
                                .withInstructionBuilder(InsnListBuilder.builder()
                                        .constant("Weird\u0000Class")
                                        .invokestatic("java/lang/Class", "forName",
                                                "(Ljava/lang/String;)Ljava/lang/Class;")
                                        .constant("méthodΩ")
                                        .constant(0)
                                        .anewarray("java/lang/Class")
                                        .invokevirtual("java/lang/Class", "getDeclaredMethod",
                                                "(Ljava/lang/String;[Ljava/lang/Class;)Ljava/lang/reflect/Method;")
                                        .aconst_null()
                                        .constant(0)
                                        .anewarray("java/lang/Object")
                                        .invokevirtual("java/lang/reflect/Method", "invoke",
                                                "(Ljava/lang/Object;[Ljava/lang/Object;)Ljava/lang/Object;")
                                        .pop()
                                        .constant(0)
                                        .invokestatic("java/lang/System", "exit", "(I)V")
                                        ._return())
                                .build())
                        .build(), baseConfig()),
                "UTF-16 -> UTF-8 metadata conversion failed");
    }

    @Test
    public void testArithmeticException() {
        Assertions.assertEquals(200,
                runner.runTest(ClassBuilder.create()
                        .withName("TestArithmetic")
                        .withVersion(V1_8)
                        .withSuperName("java/lang/Object")
                        .withMethod(MethodBuilder.create()
                                .withAccess(ACC_PUBLIC | ACC_STATIC)
                                .withName("main").withDesc("([Ljava/lang/String;)V")
                                .withInstructionBuilder(InsnListBuilder.builder()
                                        .label("start")
                                        .constant(10)
                                        .constant(0)
                                        .idiv()
                                        .istore(1)
                                        ._goto("end")
                                        .label("handler")
                                        .astore(2)
                                        .aload(2)
                                        .invokevirtual("java/lang/Throwable", "printStackTrace", "()V")
                                        .constant(200)
                                        .invokestatic("java/lang/System", "exit", "(I)V")
                                        .label("end")
                                        ._return())
                                .withTrap("java/lang/ArithmeticException", "start", "end", "handler")
                                .build())
                        .build(), baseConfig()),
                "ArithmeticException test failed");
    }

    @Test
    public void testNoArithmeticException() {
        Assertions.assertEquals(200,
                runner.runTest(ClassBuilder.create()
                        .withName("TestArithmetic")
                        .withVersion(V1_8)
                        .withSuperName("java/lang/Object")
                        .withMethod(MethodBuilder.create()
                                .withAccess(ACC_PUBLIC | ACC_STATIC)
                                .withName("main").withDesc("([Ljava/lang/String;)V")
                                .withInstructionBuilder(InsnListBuilder.builder()
                                        .label("start")
                                        .constant(10.0)
                                        .constant(1.0)
                                        .ddiv()
                                        .dstore(1)
                                        ._goto("end")
                                        .label("handler")
                                        .astore(3)
                                        .aload(3)
                                        .invokevirtual("java/lang/Throwable", "printStackTrace", "()V")
                                        .constant(0)
                                        .invokestatic("java/lang/System", "exit", "(I)V")
                                        .label("end")
                                        .constant(200)
                                        .invokestatic("java/lang/System", "exit", "(I)V")
                                        ._return())
                                .withTrap("java/lang/ArithmeticException", "start", "end", "handler")
                                .build())
                        .build(), baseConfig()),
                "ArithmeticException test failed");
    }

    @Test
    public void testNullPointerException() {
        Assertions.assertEquals(200,
                runner.runTest(ClassBuilder.create()
                        .withName("TestNPE")
                        .withVersion(V1_8)
                        .withSuperName("java/lang/Object")
                        .withMethod(MethodBuilder.create()
                                .withAccess(ACC_PUBLIC | ACC_STATIC)
                                .withName("main").withDesc("([Ljava/lang/String;)V")
                                .withInstructionBuilder(InsnListBuilder.builder()
                                        .label("start")
                                        .aconst_null()
                                        .invokevirtual("java/lang/Object", "toString", "()Ljava/lang/String;")
                                        .pop()
                                        ._goto("end")
                                        .label("handler")
                                        .astore(1)
                                        .aload(1)
                                        .invokevirtual("java/lang/Throwable", "printStackTrace", "()V")
                                        .constant(200)
                                        .invokestatic("java/lang/System", "exit", "(I)V")
                                        .label("end")
                                        ._return())
                                .withTrap("java/lang/NullPointerException", "start", "end", "handler")
                                .build())
                        .build(), baseConfig()),
                "NullPointerException test failed");
    }

    @Test
    public void testArrayIndexOutOfBounds() {
        Assertions.assertEquals(200,
                runner.runTest(ClassBuilder.create()
                        .withName("TestAIOOBE")
                        .withVersion(V1_8)
                        .withSuperName("java/lang/Object")
                        .withMethod(MethodBuilder.create()
                                .withAccess(ACC_PUBLIC | ACC_STATIC)
                                .withName("main").withDesc("([Ljava/lang/String;)V")
                                .withInstructionBuilder(InsnListBuilder.builder()
                                        .label("start")
                                        .constant(1)
                                        .newarray(Type.INT)
                                        .constant(5)
                                        .iaload()
                                        .pop()
                                        ._goto("end")
                                        .label("handler")
                                        .astore(1)
                                        .aload(1)
                                        .invokevirtual("java/lang/Throwable", "printStackTrace", "()V")
                                        .constant(200)
                                        .invokestatic("java/lang/System", "exit", "(I)V")
                                        .label("end")
                                        ._return())
                                .withTrap("java/lang/ArrayIndexOutOfBoundsException", "start", "end", "handler")
                                .build())
                        .build(), baseConfig()),
                "ArrayIndexOutOfBoundsException test failed");
    }

    @Test
    public void testNegativeArraySize() {
        Assertions.assertEquals(200,
                runner.runTest(ClassBuilder.create()
                        .withName("TestNegArray")
                        .withVersion(V1_8)
                        .withSuperName("java/lang/Object")
                        .withMethod(MethodBuilder.create()
                                .withAccess(ACC_PUBLIC | ACC_STATIC)
                                .withName("main").withDesc("([Ljava/lang/String;)V")
                                .withInstructionBuilder(InsnListBuilder.builder()
                                        .label("start")
                                        .constant(-1)
                                        .newarray(Type.INT)
                                        .pop()
                                        ._goto("end")
                                        .label("handler")
                                        .astore(1)
                                        .aload(1)
                                        .invokevirtual("java/lang/Throwable", "printStackTrace", "()V")
                                        .constant(200)
                                        .invokestatic("java/lang/System", "exit", "(I)V")
                                        .label("end")
                                        ._return())
                                .withTrap("java/lang/NegativeArraySizeException", "start", "end", "handler")
                                .build())
                        .build(), baseConfig()),
                "NegativeArraySizeException test failed");
    }

    @Test
    public void testArrayStoreException() {
        Assertions.assertEquals(200,
                runner.runTest(ClassBuilder.create()
                        .withName("TestArrayStore")
                        .withVersion(V1_8)
                        .withSuperName("java/lang/Object")
                        .withMethod(MethodBuilder.create()
                                .withAccess(ACC_PUBLIC | ACC_STATIC)
                                .withName("main").withDesc("([Ljava/lang/String;)V")
                                .withInstructionBuilder(InsnListBuilder.builder()
                                        .label("start")
                                        .constant(1)
                                        .anewarray("java/lang/String")
                                        .constant(0)
                                        .constant(5)
                                        .invokestatic("java/lang/Integer", "valueOf", "(I)Ljava/lang/Integer;")
                                        .aastore()
                                        ._goto("end")
                                        .label("handler")
                                        .astore(1)
                                        .aload(1)
                                        .invokevirtual("java/lang/Throwable", "printStackTrace", "()V")
                                        .constant(200)
                                        .invokestatic("java/lang/System", "exit", "(I)V")
                                        .label("end")
                                        ._return())
                                .withTrap("java/lang/ArrayStoreException", "start", "end", "handler")
                                .build())
                        .build(), baseConfig()),
                "ArrayStoreException test failed");
    }

    @Test
    public void testClassCastException() {
        Assertions.assertEquals(200,
                runner.runTest(ClassBuilder.create()
                        .withName("TestCCE")
                        .withVersion(V1_8)
                        .withSuperName("java/lang/Object")
                        .withMethod(MethodBuilder.create()
                                .withAccess(ACC_PUBLIC | ACC_STATIC)
                                .withName("main").withDesc("([Ljava/lang/String;)V")
                                .withInstructionBuilder(InsnListBuilder.builder()
                                        .label("start")
                                        .constant("hello")
                                        .checkcast("java/lang/Integer")
                                        .pop()
                                        ._goto("end")
                                        .label("handler")
                                        .astore(1)
                                        .aload(1)
                                        .invokevirtual("java/lang/Throwable", "printStackTrace", "()V")
                                        .constant(200)
                                        .invokestatic("java/lang/System", "exit", "(I)V")
                                        .label("end")
                                        ._return())
                                .withTrap("java/lang/ClassCastException", "start", "end", "handler")
                                .build())
                        .build(), baseConfig()),
                "ClassCastException test failed");
    }

    @Test
    public void testIllegalMonitorState() {
        Assertions.assertEquals(200,
                runner.runTest(ClassBuilder.create()
                        .withName("TestIMSE")
                        .withVersion(V1_8)
                        .withSuperName("java/lang/Object")
                        .withMethod(MethodBuilder.create()
                                .withAccess(ACC_PUBLIC | ACC_STATIC)
                                .withName("main").withDesc("([Ljava/lang/String;)V")
                                .withInstructionBuilder(InsnListBuilder.builder()
                                        .label("start")
                                        ._new("java/lang/Object")
                                        .dup()
                                        .invokespecial("java/lang/Object", "<init>", "()V")
                                        .dup()
                                        .dup()
                                        .monitorenter()
                                        .monitorexit()
                                        .monitorexit()
                                        ._goto("end")
                                        .label("handler")
                                        .astore(1)
                                        .aload(1)
                                        .invokevirtual("java/lang/Throwable", "printStackTrace", "()V")
                                        .constant(200)
                                        .invokestatic("java/lang/System", "exit", "(I)V")
                                        .label("end")
                                        ._return())
                                .withTrap("java/lang/IllegalMonitorStateException", "start", "end", "handler")
                                .build())
                        .build(), baseConfig()),
                "IllegalMonitorStateException test failed");
    }

    @Test
    public void testThrowNull() {
        Assertions.assertEquals(200,
                runner.runTest(ClassBuilder.create()
                        .withName("TestThrowNull")
                        .withVersion(V1_8)
                        .withSuperName("java/lang/Object")
                        .withMethod(MethodBuilder.create()
                                .withAccess(ACC_PUBLIC | ACC_STATIC)
                                .withName("main").withDesc("([Ljava/lang/String;)V")
                                .withInstructionBuilder(InsnListBuilder.builder()
                                        .label("start")
                                        .aconst_null()
                                        .athrow()
                                        ._goto("end")
                                        .label("handler")
                                        .astore(1)
                                        .aload(1)
                                        .invokevirtual("java/lang/Throwable", "printStackTrace", "()V")
                                        .constant(200)
                                        .invokestatic("java/lang/System", "exit", "(I)V")
                                        .label("end")
                                        ._return())
                                .withTrap("java/lang/NullPointerException", "start", "end", "handler")
                                .build())
                        .build(), baseConfig()),
                "Throw-null test failed");
    }

}
