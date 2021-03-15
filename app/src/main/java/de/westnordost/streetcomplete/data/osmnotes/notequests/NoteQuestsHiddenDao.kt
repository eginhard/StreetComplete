package de.westnordost.streetcomplete.data.osmnotes.notequests

import de.westnordost.streetcomplete.data.Database
import de.westnordost.streetcomplete.data.osmnotes.notequests.NoteQuestsHiddenTable.Columns.NOTE_ID
import de.westnordost.streetcomplete.data.osmnotes.notequests.NoteQuestsHiddenTable.Columns.TIMESTAMP
import de.westnordost.streetcomplete.data.osmnotes.notequests.NoteQuestsHiddenTable.NAME
import java.lang.System.currentTimeMillis
import javax.inject.Inject

/** Persists which note ids should be hidden (because the user selected so) in the note quest */
class NoteQuestsHiddenDao @Inject constructor(private val db: Database) {

    suspend fun add(noteId: Long) {
        db.insert(NAME, listOf(
            NOTE_ID to noteId,
            TIMESTAMP to currentTimeMillis()
        ))
    }

    suspend fun contains(noteId: Long): Boolean =
        db.queryOne(NAME, where = "$NOTE_ID = $noteId") { true } ?: false

    suspend fun getAll(): List<Long> =
        db.query(NAME) { it.getLong(NOTE_ID) }

    suspend fun deleteAll(): Int =
        db.delete(NAME)
}
