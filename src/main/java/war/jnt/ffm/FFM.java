package war.jnt.ffm;

import java.lang.foreign.*;
import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;

/**
 * @author etho
 * A test for the new FFM API.
 */
public class FFM {
    private final Arena arena;

    public FFM() {
        arena = Arena.ofShared(); // shared allocation arena (global thread-wide access)
    }

    public Object call(NativeFunction func, MemorySegment... arguments) {
        try (Arena arena = Arena.ofConfined()) {
            Linker linker = Linker.nativeLinker();

            MemorySegment address = linker.defaultLookup().find(func.getName()).get();

            MethodHandle handle;
            if (func.getReturnLayout() == null) {
                handle = linker.downcallHandle(
                        address,
                        FunctionDescriptor.ofVoid(func.getArgLayouts())
                );
            } else {
                handle = linker.downcallHandle(
                        address,
                        FunctionDescriptor.of(func.getReturnLayout(), func.getArgLayouts())
                );
            }

            return handle.invokeWithArguments(arguments);
        } catch (Throwable t) {
            t.printStackTrace(System.err);
        }
        // dealloc
        throw new RuntimeException("what");
    }

    public Arena getArena() {
        return arena;
    }
}
