package war.jnt.utility.mapping.impl

import war.jnt.utility.mapping.AbstractIdentity
import war.jnt.utility.mapping.annotation.Section

/**
 * @author etho
 */
@Section("member")
data class MemberIdentity(
    val owner: String,
    val name: String,
    val desc: String
) : AbstractIdentity {
    override fun test(identity: AbstractIdentity): Boolean {
        (identity as MemberIdentity)
        return owner == identity.owner && name == identity.name
    }

    override fun toString(): String {
        return "${owner.replace('/', '.')}.$name$desc"
    }
}