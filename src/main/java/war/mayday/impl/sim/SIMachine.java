package war.mayday.impl.sim;

import lombok.Getter;
import war.mayday.Machine;
import war.mayday.impl.sim.typed.SIMPoly;
import war.mayday.impl.sim.typed.impl.SIMParity;
import war.mayday.intrin.MachinePrinter;
import war.mayday.intrin.expr.Expression;
import war.mayday.intrin.expr.impl.*;
import war.mayday.intrin.match.MatchArm;
import war.mayday.intrin.match.SwitchStmt;
import war.metaphor.util.interfaces.IRandom;

import java.security.SecureRandom;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;

import static war.mayday.intrin.CType.INT;
import static war.mayday.intrin.CType.UCHAR;

/**
 * Implementation of a SIM (Single Instruction Machine)
 */
public class SIMachine implements Machine, IRandom {
    private final SIMSpec spec;
    private final MachinePrinter printer = getPrinter();
    private static int overlap = 0;
    private final boolean usePolymorphicMath;
    private final List<SIMPoly> polyOps = new ArrayList<>();
    private final SecureRandom random = new SecureRandom();

    private final Map<SIMMode, Integer> modeToOpcode = new HashMap<>();

    @Getter
    private final String outputName = overlapGuard("__SIM_OUT");
    private final String arg0Name = overlapGuard("__SIM_ARG0");
    private final String arg1Name = overlapGuard("__SIM_ARG1");
    private final String modeName = overlapGuard("__SIM_MODE");
    private final String vmName = overlapGuard("__SIM_ENTER");

    public SIMachine(boolean usePolymorphicMath, SIMSpec spec) {
        this.spec = spec;
        this.usePolymorphicMath = usePolymorphicMath;

        for (var mode : SIMMode.values()) {
            int op;
            do {
                op = RANDOM.nextInt();
            } while (modeToOpcode.containsValue(op));

            modeToOpcode.put(mode, op);
        }

        if (usePolymorphicMath) {
            int opCount = random.nextInt(3) + 2;
            for (int i = 0; i < opCount; i++) {
                polyOps.add(new SIMParity());
            }
        }
        
        define();
    }

    private void define() {
        printer.varDecl(INT, outputName, "0");
        printer.varDecl(INT, arg0Name, spec.arg0());
        printer.varDecl(INT, arg1Name, spec.arg1());
        printer.varDecl(UCHAR, modeName, modeToOpcode.get(spec.mode()));

        SwitchStmt sw = new SwitchStmt(modeName);
        defineArms(sw, spec.mode());

        printer.label(vmName);
        printer.buildSwitch(sw);
    }

    private void defineArms(SwitchStmt stmt, SIMMode mode) {
        var arm = new MatchArm(modeToOpcode.get(mode));
        Consumer<MachinePrinter> bodyBuilder;

        if (usePolymorphicMath) {
            bodyBuilder = printer -> {
                for (SIMPoly math : polyOps) {
                    printer.appendIndented(math.compute(printer, outputName))
                          .append(";")
                          .newLine();
                }

                assignOperation(printer, mode);
            };
        } else {
            bodyBuilder = printer -> assignOperation(printer, mode);
        }

        arm.setAssignment(outputName, createExpression(mode));
        stmt.arms.add(arm);
    }
    
    private Expression createExpression(SIMMode mode) {
        return switch (mode) {
            case ADD -> new AddExpr(arg0Name, arg1Name);
            case SUB -> new SubExpr(arg0Name, arg1Name);
            case DIV -> new DivExpr(arg0Name, arg1Name);
            case MUL -> new MulExpr(arg0Name, arg1Name);
            case REMAINDER -> new RemExpr(arg0Name, arg1Name);
            case SHIFT_LEFT -> new ShiftLeftExpr(arg0Name, arg1Name);
            case SHIFT_RIGHT -> new ShiftRightExpr(arg0Name, arg1Name);
            case USHIFT_RIGHT -> new UnsignedShiftRightExpr(arg0Name, arg1Name);
            case AND -> new AndExpr(arg0Name, arg1Name);
            case OR -> new OrExpr(arg0Name, arg1Name);
            case XOR -> new XorExpr(arg0Name, arg1Name);
        };
    }
    
    private void assignOperation(MachinePrinter printer, SIMMode mode) {
        Expression expr = createExpression(mode);
        printer.assign(outputName, expr);
    }

    /*
    Prevents names from overlapping by concatenating a overlap value after them.
     */
    private String overlapGuard(String base) {
        overlap++;
        return base + "_" + overlap;
    }

    public static SIMachine generateConstant(int value) {

        SecureRandom random = new SecureRandom();
        random.setSeed(value);
        
        int val1 = random.nextInt(200);
        int val2;
        SIMMode mode;
        
        if (val1 > value) {
            mode = SIMMode.SUB;
            val2 = val1 - value;
        } else if (val1 < value) {
            mode = SIMMode.ADD;
            val2 = value - val1;
        } else {
            mode = SIMMode.MUL;
            val2 = 1;
        }
        
        return new SIMachine(true, new SIMSpec(val1, val2, mode));
    }

    @Override
    public String get() {
        return printer.toString();
    }
}
