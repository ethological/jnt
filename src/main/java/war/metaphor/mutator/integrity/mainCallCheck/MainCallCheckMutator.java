package war.metaphor.mutator.integrity.mainCallCheck;

import org.jetbrains.annotations.NotNull;
import org.objectweb.asm.tree.*;
import war.configuration.ConfigurationSection;
import war.jnt.annotate.Level;
import war.jnt.annotate.Stability;
import war.metaphor.base.ObfuscatorContext;
import war.metaphor.mutator.Mutator;
import war.metaphor.mutator.integrity.mainCallCheck.math.IMath;
import war.metaphor.mutator.integrity.mainCallCheck.math.impl.ConstantXORMath;
import war.metaphor.mutator.integrity.mainCallCheck.math.impl.SwitchXORMath;
import war.metaphor.tree.JClassNode;
import war.metaphor.util.Dictionary;
import war.metaphor.util.Purpose;
import war.metaphor.util.asm.BytecodeUtil;

import java.lang.reflect.Modifier;
import java.util.ArrayList;

/**
 * Checks if the jars "main" method has been called by setting a field
 * TODO: Polymorphic
 * @author jan
 */
@Stability(Level.UNKNOWN)
public class MainCallCheckMutator extends Mutator {
    public MainCallCheckMutator(ObfuscatorContext base, ConfigurationSection config) {
        super(base, config);
    }

    @Override
    public void run(ObfuscatorContext base) {
        if (base.getClasses().size() < 2) return;

        final int targetValue = RANDOM.nextInt();

        JClassNode mainClass = base.classes.stream().filter(c -> c.getRealName().equals(config.getString("entry"))).findFirst().orElse(null);

        if (mainClass == null) return;
        MethodNode mainClinit = mainClass.methods.stream().filter(m -> m.name.equals("<clinit>") && m.desc.equals("()V"))
                .findFirst().orElse(null);

        JClassNode fieldHolder = new JClassNode();
        fieldHolder.visit(V1_6, ACC_PUBLIC, String.format("war/%s", Dictionary.gen(2, Purpose.CLASS)), null, "java/lang/Object", null);

        int fieldValue;
        do {
            fieldValue = RANDOM.nextInt();
        } while(fieldValue == targetValue); // just gotta be sure (trust)
        FieldNode fn = new FieldNode(ACC_PUBLIC + ACC_VOLATILE + ACC_STATIC, Dictionary.gen(4, Purpose.FIELD), "I", null, fieldValue);

        fieldHolder.fields.add(fn);

        base.classes.forEach(jcn -> jcn.methods.forEach(mn -> {
            int leeway = BytecodeUtil.leeway(mn);
            if (leeway < 30000)
                return;

            if (Modifier.isAbstract(mn.access)) return;
            if (jcn.isExempt(mn)) return;
            if (mn == mainClinit) return;
            mn.instructions.insertBefore(mn.instructions.getFirst(), getCode(fieldHolder, fn, targetValue));
        }));

        MethodNode niggerNode = mainClinit; // made this so java doesn't cry about the variable not being "effectively final" inside the lambda for some fucking reason

        if (niggerNode == null) {
            niggerNode = new MethodNode(ACC_STATIC, "<clinit>", "()V", null, null);
            mainClass.methods.add(niggerNode);
            niggerNode.instructions = new InsnList();
            niggerNode.instructions.add(new InsnNode(RETURN));
        }

        InsnList list = new InsnList();

        list.add(BytecodeUtil.makeInteger(targetValue));
        list.add(new FieldInsnNode(PUTSTATIC, fieldHolder.name, fn.name, "I"));

        niggerNode.instructions.insertBefore(niggerNode.instructions.getFirst(), list);

        base.classes.add(fieldHolder);
    }

    @NotNull
    private static InsnList getCode(JClassNode fieldHolder, FieldNode fn, int targetValue) {
        InsnList list = new InsnList();
        LabelNode l = new LabelNode();
        ArrayList<IMath> math = getMath();

        for (IMath iMath : math) {
            targetValue = iMath.apply(targetValue);
        }

        final boolean pushBefore = RANDOM.nextBoolean();

        if (pushBefore) {
            list.add(new InsnNode(ACONST_NULL));
        }
        list.add(new FieldInsnNode(GETSTATIC, fieldHolder.name, fn.name, "I"));
        for (IMath iMath : math) {
            list.add(iMath.dump());
        }
        list.add(BytecodeUtil.makeInteger(targetValue));
        list.add(new JumpInsnNode(IF_ICMPEQ, l));
        if (!pushBefore) {
            list.add(new InsnNode(ACONST_NULL));
        }
        list.add(new InsnNode(ATHROW));
        list.add(l);
        if (pushBefore) {
            list.add(new InsnNode(POP));
        }
        return list;
    }

    private static ArrayList<IMath> getMath() {
        ArrayList<IMath> math = new ArrayList<>();

        for (int i = 0; i < RANDOM.nextInt(5) + 2; i++) {
            switch (RANDOM.nextInt(2)) {
                case 0 -> math.add(new ConstantXORMath());
                case 1 -> math.add(new SwitchXORMath());
            }
        }

        return math;
    }
}
