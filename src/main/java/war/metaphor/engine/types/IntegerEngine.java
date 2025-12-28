package war.metaphor.engine.types;

import war.metaphor.engine.Engine;
import war.metaphor.engine.modules.*;
import war.metaphor.engine.modules.Module;

import java.util.Arrays;
import java.util.List;
import java.util.Random;

public class IntegerEngine extends Engine {

    public IntegerEngine(int numModules) {
        Random rand = new Random();
        List<Class<? extends Module>> modules = Arrays.asList(
                XorMod.class,
                AddMod.class,
                SubMod.class
        );
        for (int i = 0; i < numModules; i++) {
            Class<? extends Module> module =
                    modules.get(rand.nextInt(modules.size()));
            addModule(module);
        }
        randomise(Integer.MAX_VALUE);
    }


}
