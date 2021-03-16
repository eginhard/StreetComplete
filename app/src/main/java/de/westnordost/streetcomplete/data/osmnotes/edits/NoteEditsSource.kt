package de.westnordost.streetcomplete.data.osmnotes.edits

import de.westnordost.osmapi.map.data.BoundingBox
import de.westnordost.osmapi.map.data.LatLon

interface NoteEditsSource {

    interface Listener {
        fun onAddedEdit(edit: NoteEdit)
        fun onSyncedEdit(edit: NoteEdit)
        fun onDeletedEdit(edit: NoteEdit)
    }

    /** Count of unsynced a.k.a to-be-uploaded edits */
    suspend fun getUnsyncedCount(): Int

    suspend fun getAllUnsynced(): List<NoteEdit>

    suspend fun getAllUnsynced(bbox: BoundingBox): List<NoteEdit>

    suspend fun getAllUnsyncedForNote(noteId: Long): List<NoteEdit>

    suspend fun getAllUnsyncedForNotes(noteIds: Collection<Long>): List<NoteEdit>

    suspend fun getAllUnsyncedPositions(bbox: BoundingBox): List<LatLon>

    fun addListener(listener: Listener)
    fun removeListener(listener: Listener)

}
