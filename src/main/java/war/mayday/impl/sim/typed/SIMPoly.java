package war.mayday.impl.sim.typed;

import war.mayday.intrin.MachinePrinter;

public interface SIMPoly {
    String compute(MachinePrinter printer, String outputName);
    String reverse(MachinePrinter printer, String outputName);
}
