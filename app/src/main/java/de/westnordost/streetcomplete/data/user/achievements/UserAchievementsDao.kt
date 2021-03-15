package de.westnordost.streetcomplete.data.user.achievements

import de.westnordost.streetcomplete.data.Database
import de.westnordost.streetcomplete.data.user.achievements.UserAchievementsTable.Columns.ACHIEVEMENT
import de.westnordost.streetcomplete.data.user.achievements.UserAchievementsTable.Columns.LEVEL
import de.westnordost.streetcomplete.data.user.achievements.UserAchievementsTable.NAME

import javax.inject.Inject

/** Stores which achievement ids have been unlocked by the user and at which level */
class UserAchievementsDao @Inject constructor(private val db: Database) {

    suspend fun getAll(): Map<String, Int> =
        db.query(NAME) { it.getString(ACHIEVEMENT) to it.getInt(LEVEL) }.toMap()

    suspend fun clear() {
        db.delete(NAME)
    }

    suspend fun put(achievement: String, level: Int) {
        db.replace(NAME, listOf(
            ACHIEVEMENT to achievement,
            LEVEL to level
        ))
    }
}
