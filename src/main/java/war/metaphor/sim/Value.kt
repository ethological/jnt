package war.metaphor.sim

import org.objectweb.asm.tree.AbstractInsnNode

/**
 * Holds data
 * @author jan
 */
data class Value(var value: Any?, var resolved: Boolean, var creatorNodes: Array<AbstractInsnNode>?) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as Value

        if (resolved != other.resolved) return false
        if (value != other.value) return false
        if (!creatorNodes.contentEquals(other.creatorNodes)) return false

        return true
    }

    override fun hashCode(): Int {
        var result = resolved.hashCode()
        result = 31 * result + (value?.hashCode() ?: 0)
        result = 31 * result + creatorNodes.contentHashCode()
        return result
    }
}
