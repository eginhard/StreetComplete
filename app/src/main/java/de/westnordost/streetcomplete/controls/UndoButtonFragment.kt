package de.westnordost.streetcomplete.controls

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.TextView
import androidx.appcompat.app.AlertDialog
import androidx.core.view.isGone
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import de.westnordost.osmfeatures.FeatureDictionary
import de.westnordost.streetcomplete.Injector
import de.westnordost.streetcomplete.R
import de.westnordost.streetcomplete.data.osm.edits.ElementEdit
import de.westnordost.streetcomplete.data.osm.edits.ElementEditsController
import de.westnordost.streetcomplete.data.osm.edits.ElementEditsSource
import de.westnordost.streetcomplete.data.osm.edits.MapDataWithEditsSource
import de.westnordost.streetcomplete.data.upload.UploadProgressListener
import de.westnordost.streetcomplete.data.upload.UploadProgressSource
import de.westnordost.streetcomplete.ktx.popIn
import de.westnordost.streetcomplete.ktx.popOut
import de.westnordost.streetcomplete.quests.getHtmlQuestTitle
import kotlinx.coroutines.launch
import java.util.concurrent.FutureTask
import javax.inject.Inject

/** Fragment that shows (and hides) the undo button, based on whether there is anything to undo */
class UndoButtonFragment : Fragment(R.layout.fragment_undo_button) {

    @Inject internal lateinit var elementEditsController: ElementEditsController
    @Inject internal lateinit var mapDataSource: MapDataWithEditsSource
    @Inject internal lateinit var uploadProgressSource: UploadProgressSource
    @Inject internal lateinit var featureDictionaryFutureTask: FutureTask<FeatureDictionary>

    private val undoButton get() = view as ImageButton

    /* undo button is not shown when there is nothing to undo */
    private val osmElementChangesListener = object : ElementEditsSource.Listener {
        override fun onAddedEdit(edit: ElementEdit) { lifecycleScope.launch { animateInIfAnythingToUndo() }}
        override fun onSyncedEdit(edit: ElementEdit) { lifecycleScope.launch { animateOutIfNothingLeftToUndo() }}
        override fun onDeletedEdit(edit: ElementEdit) { lifecycleScope.launch { animateOutIfNothingLeftToUndo() }}
    }

    /* Don't allow undoing while uploading. Should prevent race conditions. (Undoing quest while
    *  also uploading it at the same time) */
    private val uploadProgressListener = object : UploadProgressListener {
        override fun onStarted() { lifecycleScope.launch { updateUndoButtonEnablement(false) }}
        override fun onFinished() { lifecycleScope.launch { updateUndoButtonEnablement(true) }}
    }

    /* --------------------------------------- Lifecycle ---------------------------------------- */

    init {
        Injector.applicationComponent.inject(this)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        undoButton.setOnClickListener {
            undoButton.isEnabled = false
            lifecycleScope.launch {
                val change = elementEditsController.getMostRecentUndoableEdit()
                if (change != null) confirmUndo(change)
            }
        }
    }

    override fun onStart() {
        super.onStart()
        lifecycleScope.launch {
            updateUndoButtonVisibility()
        }
        updateUndoButtonEnablement(true)
        elementEditsController.addListener(osmElementChangesListener)
        uploadProgressSource.addUploadProgressListener(uploadProgressListener)
    }

    override fun onStop() {
        super.onStop()
        elementEditsController.removeListener(osmElementChangesListener)
        uploadProgressSource.removeUploadProgressListener(uploadProgressListener)
    }

    /* ------------------------------------------------------------------------------------------ */

    private suspend fun confirmUndo(edit: ElementEdit) {
        val ctx = context ?: return
        val element = mapDataSource.get(edit.elementType, edit.elementId)

        val inner = LayoutInflater.from(ctx).inflate(R.layout.dialog_undo, null, false)
        val icon = inner.findViewById<ImageView>(R.id.icon)
        icon.setImageResource(edit.questType.icon)
        val text = inner.findViewById<TextView>(R.id.text)
        text.text = resources.getHtmlQuestTitle(edit.questType, element, featureDictionaryFutureTask)

        AlertDialog.Builder(ctx)
            .setTitle(R.string.undo_confirm_title)
            .setView(inner)
            .setPositiveButton(R.string.undo_confirm_positive) { _, _ ->
                elementEditsController.undo(edit.id)
                updateUndoButtonEnablement(true)
            }
            .setNegativeButton(R.string.undo_confirm_negative) { _, _ -> updateUndoButtonEnablement(true) }
            .setOnCancelListener { updateUndoButtonEnablement(true) }
            .show()
    }

    private suspend fun updateUndoButtonVisibility() {
        view?.isGone = elementEditsController.getMostRecentUndoableEdit() == null
    }

    private fun updateUndoButtonEnablement(enable: Boolean) {
        undoButton.isEnabled = enable && !uploadProgressSource.isUploadInProgress
    }

    private suspend fun animateInIfAnythingToUndo() {
        if (!undoButton.isVisible && elementEditsController.getMostRecentUndoableEdit() != null) {
            undoButton.popIn()
        }
    }

    private suspend fun animateOutIfNothingLeftToUndo() {
        if (undoButton.isVisible && elementEditsController.getMostRecentUndoableEdit() == null) {
            undoButton.popOut().withEndAction { undoButton.visibility = View.INVISIBLE }
        }
    }
}
