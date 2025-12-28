package war.metaphor.util.asm;

import org.objectweb.asm.Type;

public interface TypeResolver {
    Type common(Type var1, Type var2);

    Type commonException(Type var1, Type var2);
}
