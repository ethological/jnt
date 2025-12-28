package war.metaphor.mutator.data.strings.poly2.decryptionMethod.args.impl.integer;

import java.util.concurrent.ThreadLocalRandom;

public class ByteArgument extends IntegerArgument
{
    @Override
    public String getDescriptor()
    {
        return "B";
    }

    @Override
    public Object makeRandomValue()
    {
        return ThreadLocalRandom.current().nextInt(Byte.MIN_VALUE, Byte.MAX_VALUE);
    }
}
