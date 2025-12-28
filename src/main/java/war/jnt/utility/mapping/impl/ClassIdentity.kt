package war.jnt.utility.mapping.impl

import war.jnt.utility.mapping.AbstractIdentity
import war.jnt.utility.mapping.annotation.Section

/**
 * @author etho
 */
@Section("class")
data class ClassIdentity(val name: String) : AbstractIdentity {
    override fun test(identity: AbstractIdentity): Boolean {
        (identity as ClassIdentity)
        return name == identity.name
    }

    override fun toString(): String {
        return name.replace('/', '.')
    }
}