package war.metaphor.sim

import java.util.function.BiFunction

/**
 * Integer math
 * @author jan
 */
enum class MathType(val func: BiFunction<Value, Value, Int>) {
    ADD({ a: Value, b: Value -> (a.value as Int) + (b.value as Int) }),
    SUB({ a: Value, b: Value -> (a.value as Int) - (b.value as Int) }),
    DIV({ a: Value, b: Value -> (a.value as Int) / (b.value as Int) }),
    MUL({ a: Value, b: Value -> (a.value as Int) * (b.value as Int) }),
    XOR({ a: Value, b: Value -> (a.value as Int) xor (b.value as Int) }),
    SHL({ a: Value, b: Value -> (a.value as Int) shl (b.value as Int) }),
    SHR({ a: Value, b: Value -> (a.value as Int) shr (b.value as Int) }),
    AND({ a: Value, b: Value -> (a.value as Int) and (b.value as Int) }),
    OR({ a: Value, b: Value -> (a.value as Int) or (b.value as Int) }),
    REM({ a: Value, b: Value -> (a.value as Int) % (b.value as Int) }),
    USHR({ a: Value, b: Value -> (a.value as Int) ushr (b.value as Int) })
}