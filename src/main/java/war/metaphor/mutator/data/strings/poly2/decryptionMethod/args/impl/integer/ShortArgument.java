package war.metaphor.mutator.data.strings.poly2.decryptionMethod.args.impl.integer;

import org.objectweb.asm.tree.InsnList;

import java.util.concurrent.ThreadLocalRandom;

public class ShortArgument extends IntegerArgument
{
    @Override
    public String getDescriptor()
    {
        return "S";
    }

    @Override
    public Object makeRandomValue()
    {
        return ThreadLocalRandom.current().nextInt(Short.MIN_VALUE, Short.MAX_VALUE);
    }
}
