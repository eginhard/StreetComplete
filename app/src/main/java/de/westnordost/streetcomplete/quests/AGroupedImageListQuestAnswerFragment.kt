package de.westnordost.streetcomplete.quests

import android.content.Context
import android.os.Bundle
import androidx.appcompat.app.AlertDialog
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
import android.view.View
import androidx.core.view.postDelayed
import androidx.preference.PreferenceManager

import de.westnordost.streetcomplete.R
import de.westnordost.streetcomplete.databinding.QuestGenericListBinding
import de.westnordost.streetcomplete.view.image_select.GroupableDisplayItem
import de.westnordost.streetcomplete.view.image_select.GroupedImageSelectAdapter
import kotlin.math.max

/**
 * Abstract class for quests with a grouped list of images and one to select.
 *
 * Saving and restoring state is not implemented
 */
abstract class AGroupedImageListQuestAnswerFragment<I,T> : AbstractQuestFormAnswerFragment<T>() {

    final override val contentLayoutResId = R.layout.quest_generic_list
    private val binding by contentViewBinding(QuestGenericListBinding::bind)

    override val defaultExpanded = false

    protected lateinit var imageSelector: GroupedImageSelectAdapter<I>

    protected abstract val allItems: List<GroupableDisplayItem<I>>
    protected abstract val topItems: List<GroupableDisplayItem<I>>

    internal lateinit var favs: LastPickedValuesStore<GroupableDisplayItem<I>>

    private val selectedItem get() = imageSelector.selectedItem

    protected open val itemsPerRow = 3

    override fun onAttach(ctx: Context) {
        super.onAttach(ctx)
        val validSuggestions = allItems.mapNotNull { it.items }.flatten()
        val stringToItem = validSuggestions.associateBy { it.value.toString() }
        favs = LastPickedValuesStore(
            PreferenceManager.getDefaultSharedPreferences(ctx.applicationContext),
            key = javaClass.simpleName,
            serialize = { item -> item.value.toString() },
            deserialize = { value -> stringToItem[value] }
        )
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        imageSelector = GroupedImageSelectAdapter(GridLayoutManager(activity, itemsPerRow))
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding.list.layoutManager = imageSelector.gridLayoutManager
        binding.list.isNestedScrollingEnabled = false

        binding.showMoreButton.setOnClickListener {
            imageSelector.items = allItems
            binding.showMoreButton.visibility = View.GONE
        }

        binding.selectHintLabel.setText(R.string.quest_select_hint_most_specific)

        imageSelector.listeners.add { checkIsFormComplete() }
        imageSelector.registerAdapterDataObserver(object : RecyclerView.AdapterDataObserver() {
            override fun onItemRangeInserted(positionStart: Int, itemCount: Int) {
                super.onItemRangeInserted(positionStart, itemCount)
                scrollTo(positionStart)
            }
        })
        checkIsFormComplete()

        imageSelector.items = getInitialItems()
        binding.list.adapter = imageSelector
    }

    private fun scrollTo(index: Int) {
        val item = imageSelector.gridLayoutManager.getChildAt(max(0, index - 1))
        if (item != null) {
            val itemPos = IntArray(2)
            item.getLocationInWindow(itemPos)
            val scrollViewPos = IntArray(2)
            scrollView.getLocationInWindow(scrollViewPos)

            scrollView.postDelayed(250) {
                scrollView.smoothScrollTo(0, itemPos[1] - scrollViewPos[1])
            }
        }
    }

    private fun getInitialItems(): List<GroupableDisplayItem<I>> {
        return favs.get().mostCommonWithin(6, 30).padWith(topItems).toList()
    }

    override fun onClickOk() {
        val item = selectedItem!!
        val itemValue = item.value

        if (itemValue == null) {
            context?.let {
                AlertDialog.Builder(it)
                .setMessage(R.string.quest_generic_item_invalid_value)
                .setPositiveButton(android.R.string.ok, null)
                .show()
            }
        } else {
            if (item.isGroup) {
                context?.let {
                    AlertDialog.Builder(it)
                        .setMessage(R.string.quest_generic_item_confirmation)
                        .setNegativeButton(R.string.quest_generic_confirmation_no, null)
                        .setPositiveButton(R.string.quest_generic_confirmation_yes) { _, _ ->
                            favs.add(item)
                            onClickOk(itemValue)
                        }
                        .show()
                }
            }
            else {
                favs.add(item)
                onClickOk(itemValue)
            }
        }
    }

    abstract fun onClickOk(value: I)

    override fun isFormComplete() = selectedItem != null
}
