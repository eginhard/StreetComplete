package de.westnordost.streetcomplete.data.osmnotes.notequests

import de.westnordost.osmapi.map.data.BoundingBox
import de.westnordost.osmapi.notes.Note
import de.westnordost.osmapi.notes.NoteComment
import de.westnordost.streetcomplete.ApplicationConstants
import de.westnordost.streetcomplete.data.osmnotes.edits.NotesWithEditsSource
import de.westnordost.streetcomplete.data.user.LoginStatusSource
import de.westnordost.streetcomplete.data.user.UserLoginStatusListener
import de.westnordost.streetcomplete.data.user.UserStore
import java.util.concurrent.CopyOnWriteArrayList
import javax.inject.Inject
import javax.inject.Singleton

/** Used to get visible osm note quests */
@Singleton class OsmNoteQuestController @Inject constructor(
    private val noteSource: NotesWithEditsSource,
    private val hiddenDB: NoteQuestsHiddenDao,
    private val loginStatusSource: LoginStatusSource,
    private val userStore: UserStore,
    private val notesPreferences: NotesPreferences,
): OsmNoteQuestSource {
    /* Must be a singleton because there is a listener that should respond to a change in the
     *  database table */

    private val listeners: MutableList<OsmNoteQuestSource.Listener> = CopyOnWriteArrayList()

    private val userId: Long? get() = userStore.userId.takeIf { it != -1L }

    private val showOnlyNotesPhrasedAsQuestions: Boolean get() =
        notesPreferences.showOnlyNotesPhrasedAsQuestions

    private val noteUpdatesListener = object : NotesWithEditsSource.Listener {
        @Synchronized override fun onUpdated(added: Collection<Note>, updated: Collection<Note>, deleted: Collection<Long>) {
            val hiddenNoteIds = getNoteIdsHidden()

            val quests = mutableListOf<OsmNoteQuest>()
            val deletedQuestIds = ArrayList(deleted)
            for (note in added) {
                val q = createQuestForNote(note, hiddenNoteIds)
                if (q != null) quests.add(q)
            }
            for (note in updated) {
                val q = createQuestForNote(note, hiddenNoteIds)
                if (q != null) quests.add(q)
                else deletedQuestIds.add(note.id)
            }
            onUpdated(quests, deletedQuestIds)
        }
    }

    private val userLoginStatusListener = object : UserLoginStatusListener {
        @Synchronized override fun onLoggedIn() {
            // notes created by the user in this app or commented on by this user should not be shown
            onInvalidated()
        }
        override fun onLoggedOut() {}
    }

    private val notesPreferencesListener = object : NotesPreferences.Listener {
        @Synchronized override fun onNotesPreferencesChanged() {
            // a lot of notes become visible/invisible if this option is changed
            onInvalidated()
        }
    }

    init {
        noteSource.addListener(noteUpdatesListener)
        loginStatusSource.addLoginStatusListener(userLoginStatusListener)
        notesPreferences.listener = notesPreferencesListener
    }

    override suspend fun get(questId: Long): OsmNoteQuest? {
        if (isNoteHidden(questId)) return null
        return noteSource.get(questId)?.let { createQuestForNote(it) }
    }

    override suspend fun getAllVisibleInBBox(bbox: BoundingBox): List<OsmNoteQuest> {
        return createQuestsForNotes(noteSource.getAll(bbox))
    }

    @Synchronized fun hide(questId: Long) {
        hiddenDB.add(questId)
        onUpdated(deletedQuestIds = listOf(questId))
    }

    @Synchronized fun unhideAll(): Int {
        val previouslyHiddenNotes = noteSource.getAll(hiddenDB.getAll())
        val result = hiddenDB.deleteAll()

        val unhiddenNoteQuests = previouslyHiddenNotes.mapNotNull { createQuestForNote(it, emptySet()) }

        onUpdated(quests = unhiddenNoteQuests)
        return result
    }

    private fun createQuestsForNotes(notes: Collection<Note>): List<OsmNoteQuest> {
        val blockedNoteIds = getNoteIdsHidden()
        return notes.mapNotNull { createQuestForNote(it, blockedNoteIds) }
    }

    private fun createQuestForNote(note: Note, blockedNoteIds: Set<Long> = setOf()): OsmNoteQuest? =
        if(note.shouldShowAsQuest(userId, showOnlyNotesPhrasedAsQuestions, blockedNoteIds))
            OsmNoteQuest(note.id, note.position)
        else null

    private suspend fun isNoteHidden(noteId: Long): Boolean = hiddenDB.contains(noteId)

    private suspend fun getNoteIdsHidden(): Set<Long> = hiddenDB.getAll().toSet()

    /* ---------------------------------------- Listener ---------------------------------------- */

    override fun addListener(listener: OsmNoteQuestSource.Listener) {
        listeners.add(listener)
    }
    override fun removeListener(listener: OsmNoteQuestSource.Listener) {
        listeners.remove(listener)
    }

    private fun onUpdated(
        quests: Collection<OsmNoteQuest> = emptyList(),
        deletedQuestIds: Collection<Long> = emptyList()
    ) {
        if (quests.isEmpty() && deletedQuestIds.isEmpty()) return
        listeners.forEach { it.onUpdated(quests, deletedQuestIds) }
    }

    private fun onInvalidated() {
        listeners.forEach { it.onInvalidated() }
    }
}

private fun Note.shouldShowAsQuest(
    userId: Long?,
    showOnlyNotesPhrasedAsQuestions: Boolean,
    blockedNoteIds: Set<Long>
): Boolean {

    // don't show a note if user already contributed to it
    if (userId != null) {
        if (containsCommentFromUser(userId) || probablyCreatedByUserInThisApp(userId)) return false
    }
    // a note comment pending to be uploaded also counts as contribution
    if (id in blockedNoteIds) return false

    /* many notes are created to report problems on the map that cannot be resolved
     * through an on-site survey.
     * Likely, if something is posed as a question, the reporter expects someone to
     * answer/comment on it, possibly an information on-site is missing, so let's only show these */
    if (showOnlyNotesPhrasedAsQuestions) {
        if (!probablyContainsQuestion() && !containsSurveyRequiredMarker()) return false
    }

    return true
}

private fun Note.probablyContainsQuestion(): Boolean {
    /* from left to right (if smartass IntelliJ wouldn't mess up left-to-right):
       - latin question mark
       - greek question mark (a different character than semikolon, though same appearance)
       - semikolon (often used instead of proper greek question mark)
       - mirrored question mark (used in script written from right to left, like Arabic)
       - armenian question mark
       - ethopian question mark
       - full width question mark (often used in modern Chinese / Japanese)
       (Source: https://en.wikipedia.org/wiki/Question_mark)

        NOTE: some languages, like Thai, do not use any question mark, so this would be more
        difficult to determine.
   */
    val questionMarksAroundTheWorld = "[?;;؟՞፧？]"

    val text = comments?.firstOrNull()?.text
    return text?.matches(".*$questionMarksAroundTheWorld.*".toRegex()) ?: false
}

private fun Note.containsSurveyRequiredMarker(): Boolean {
    val surveyRequiredMarker = "#surveyme"
    return comments.any { it.text?.matches(".*$surveyRequiredMarker.*".toRegex()) == true }
}

private fun Note.containsCommentFromUser(userId: Long): Boolean =
    comments.any { it.isFromUser(userId) && it.isComment  }

private fun Note.probablyCreatedByUserInThisApp(userId: Long): Boolean {
    val firstComment = comments.first()
    val isViaApp = firstComment.text.contains("via " + ApplicationConstants.NAME)
    return firstComment.isFromUser(userId) && isViaApp
}

private val NoteComment.isComment: Boolean get() =
    action == NoteComment.Action.COMMENTED

private fun NoteComment.isFromUser(userId: Long): Boolean =
    user?.id == userId
