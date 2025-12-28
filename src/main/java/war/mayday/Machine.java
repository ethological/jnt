package war.mayday;

import war.mayday.intrin.MachinePrinter;

public interface Machine {
    default MachinePrinter getPrinter() {
        return new MachinePrinter();
    }

    String get();
}
