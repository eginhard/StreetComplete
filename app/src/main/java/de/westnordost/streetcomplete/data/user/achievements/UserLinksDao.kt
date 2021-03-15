package de.westnordost.streetcomplete.data.user.achievements

import de.westnordost.streetcomplete.data.Database
import de.westnordost.streetcomplete.data.user.achievements.UserLinksTable.Columns.LINK
import de.westnordost.streetcomplete.data.user.achievements.UserLinksTable.NAME

import javax.inject.Inject

/** Stores which link ids have been unlocked by the user */
class UserLinksDao @Inject constructor(private val db: Database) {

    suspend fun getAll(): List<String> =
        db.query(NAME) { it.getString(LINK) }

    suspend fun clear() {
        db.delete(NAME)
    }

    suspend fun add(link: String) {
        db.insertOrIgnore(NAME, listOf(LINK to link))
    }

    suspend fun addAll(links: List<String>) {
        if (links.isEmpty()) return
        db.insertOrIgnoreMany(NAME,
            arrayOf(LINK),
            links.map { arrayOf(it) }
        )
    }
}
