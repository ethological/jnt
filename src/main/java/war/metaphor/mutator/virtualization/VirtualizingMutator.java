package war.metaphor.mutator.virtualization;

import org.jetbrains.annotations.NotNull;
import org.objectweb.asm.tree.*;
import war.configuration.ConfigurationSection;
import war.metaphor.base.ObfuscatorContext;
import war.metaphor.mutator.Mutator;
import war.metaphor.tree.JClassNode;
import war.metaphor.util.asm.BytecodeUtil;

@Deprecated
public class VirtualizingMutator extends Mutator {
    public VirtualizingMutator(ObfuscatorContext base, ConfigurationSection config) {
        super(base, config);
    }

    @Override
    public void run(ObfuscatorContext base) {
        for (JClassNode jcn : base.getClasses()) {
            for (MethodNode method : jcn.methods) {
                int leeway = BytecodeUtil.leeway(method);
                for (AbstractInsnNode ain : method.instructions) {
                    if (leeway < 30000)
                        break;
                    if (BytecodeUtil.isInteger(ain)) {
                        int stack = method.maxLocals++;
                        int sp = method.maxLocals++;
                        int op = method.maxLocals++;

                        int value = BytecodeUtil.getInteger(ain);
                        VirtualMachine vm = createVm(value, stack, sp, op);

                        var list = new InsnList();
                        list.add(BytecodeUtil.makeInteger(VirtualMachine.PUSH32));
                        list.add(new VarInsnNode(ISTORE, op));
                        list.add(vm.generate());

                        method.instructions.insertBefore(ain, list);
                        method.instructions.remove(ain);
                    }

                    leeway = BytecodeUtil.leeway(method);
                }
            }
        }
    }

    @NotNull
    private static VirtualMachine createVm(int value, int stack, int sp, int op) {
        byte[] integer = new byte[]{
                (byte) (value >>> 24),
                (byte) (value >>> 16),
                (byte) (value >>> 8),
                (byte) value
        };

        VirtualMachineInterface vmi = new VirtualMachineInterface(stack, sp, op);
        return new VirtualMachine(
                vmi,
                integer
        );
    }
}
