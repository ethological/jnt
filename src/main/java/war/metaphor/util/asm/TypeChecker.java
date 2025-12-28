package war.metaphor.util.asm;

import org.objectweb.asm.Type;

import java.util.function.BiPredicate;

public interface TypeChecker extends BiPredicate<Type, Type> {
}
