package war.jnt.utility.mapping

import war.jnt.utility.mapping.annotation.Section
import java.util.Optional

/**
 * @author etho
 */
class MappingRepository {
    private val mappings = mutableListOf<Mapping>()

    fun add(mapping: Mapping) {
        mappings.add(mapping)
    }

    fun isEmpty(): Boolean = this.mappings.isEmpty()

    fun former(current: AbstractIdentity): Optional<AbstractIdentity> {
        for (mapping in mappings) {
            val cm = mapping.current
            if (cm.test(current)) {
                return Optional.of(cm)
            }
        }
        return Optional.empty()
    }

    override fun toString(): String {
        val sb = StringBuilder()

        val groupedMappings = mappings.groupBy {
            it.former::class.annotations.find {
                a -> a is Section
            }?.let {
                (it as Section).type
            } ?: ""
        }

        groupedMappings.forEach { (section, mappingsInSection) ->
            sb.appendLine(".section $section")
            mappingsInSection.forEach { mapping ->
                sb.appendLine("${mapping.former} -> ${mapping.current}")
            }
            sb.appendLine()
        }

        return sb.toString()
    }
}