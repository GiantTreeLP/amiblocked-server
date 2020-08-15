package de.gianttree.amiblocked

import org.jetbrains.exposed.dao.IntEntity
import org.jetbrains.exposed.dao.IntEntityClass
import org.jetbrains.exposed.dao.id.EntityID
import org.jetbrains.exposed.dao.id.IntIdTable


class BlockedUserDTO(
    val username: String,
    val snowflake: String,
    val note: String,
    val blocked: Boolean
) {
    constructor(blockedUser: BlockedUser) : this(
        blockedUser.username,
        blockedUser.snowflake,
        blockedUser.note,
        blockedUser.blocked
    )

    companion object {
        fun noResult(searchParam: String) = BlockedUserDTO(
            searchParam,
            "",
            "",
            false
        )
    }
}

class BlockedUser(id: EntityID<Int>) : IntEntity(id) {

    val username by BlockedUsers.username
    val snowflake by BlockedUsers.snowflake
    val note by BlockedUsers.note
    val blocked by BlockedUsers.blocked

    companion object : IntEntityClass<BlockedUser>(BlockedUsers)

}

object BlockedUsers : IntIdTable() {
    val username = varchar("username", 32 + 1 + 4).index()
    val snowflake = varchar("snowflake", 20).uniqueIndex()
    val note = varchar("note", 256)
    val blocked = bool("blocked")
}
