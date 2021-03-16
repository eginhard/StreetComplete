package de.westnordost.streetcomplete.data.osmnotes.edits

import de.westnordost.osmapi.map.data.BoundingBox
import de.westnordost.osmapi.map.data.LatLon
import de.westnordost.osmapi.notes.Note
import java.lang.System.currentTimeMillis
import java.util.concurrent.CopyOnWriteArrayList
import javax.inject.Inject
import javax.inject.Singleton

@Singleton class NoteEditsController @Inject constructor(
    private val editsDB: NoteEditsDao
): NoteEditsSource {
    /* Must be a singleton because there is a listener that should respond to a change in the
     * database table */

    private val listeners: MutableList<NoteEditsSource.Listener> = CopyOnWriteArrayList()

    @Synchronized fun add(
        noteId: Long,
        action: NoteEditAction,
        position: LatLon,
        text: String? = null,
        imagePaths: List<String> = emptyList()
    ) {

        val edit = NoteEdit(
            0,
            noteId,
            position,
            action,
            text,
            imagePaths,
            currentTimeMillis(),
            false,
            imagePaths.isNotEmpty()
        )
        editsDB.add(edit)
        onAddedEdit(edit)
    }

    override suspend fun getAllUnsynced(): List<NoteEdit> =
        editsDB.getAllUnsynced()

    suspend fun getOldestUnsynced(): NoteEdit? =
        editsDB.getOldestUnsynced()

    override suspend fun getUnsyncedCount(): Int =
        editsDB.getUnsyncedCount()

    override suspend fun getAllUnsyncedForNote(noteId: Long): List<NoteEdit> =
        editsDB.getAllUnsyncedForNote(noteId)

    override suspend fun getAllUnsyncedForNotes(noteIds: Collection<Long>): List<NoteEdit> =
        editsDB.getAllUnsyncedForNotes(noteIds)

    override suspend fun getAllUnsynced(bbox: BoundingBox): List<NoteEdit> =
        editsDB.getAllUnsynced(bbox)

    override suspend fun getAllUnsyncedPositions(bbox: BoundingBox): List<LatLon> =
        editsDB.getAllUnsyncedPositions(bbox)

    suspend fun getOldestNeedingImagesActivation(): NoteEdit? =
        editsDB.getOldestNeedingImagesActivation()

    @Synchronized fun imagesActivated(id: Long): Boolean =
        editsDB.markImagesActivated(id)

    @Synchronized fun synced(edit: NoteEdit, note: Note) {
        if (edit.noteId != note.id) {
            editsDB.updateNoteId(edit.noteId, note.id)
        }
        if (editsDB.markSynced(edit.id)) {
            onSyncedEdit(edit)
        }
    }

    @Synchronized fun syncFailed(edit: NoteEdit) {
        delete(edit)
    }

    @Synchronized fun deleteSyncedOlderThan(timestamp: Long): Int =
        editsDB.deleteSyncedOlderThan(timestamp)

    private suspend fun delete(edit: NoteEdit) {
        if (editsDB.delete(edit.id)) {
            onDeletedEdit(edit)
        }
    }

    /* ------------------------------------ Listeners ------------------------------------------- */

    override fun addListener(listener: NoteEditsSource.Listener) {
        listeners.add(listener)
    }
    override fun removeListener(listener: NoteEditsSource.Listener) {
        listeners.remove(listener)
    }

    private fun onAddedEdit(edit: NoteEdit) {
        listeners.forEach { it.onAddedEdit(edit) }
    }

    private fun onSyncedEdit(edit: NoteEdit) {
        listeners.forEach { it.onSyncedEdit(edit) }
    }

    private fun onDeletedEdit(edit: NoteEdit) {
        listeners.forEach { it.onDeletedEdit(edit) }
    }
}
