package de.westnordost.streetcomplete.quests.postbox_collection_times

import android.content.Context
import android.text.format.DateFormat
import androidx.recyclerview.widget.RecyclerView
import android.view.LayoutInflater
import android.view.ViewGroup

import de.westnordost.streetcomplete.data.meta.CountryInfo
import de.westnordost.streetcomplete.databinding.QuestTimesWeekdayRowBinding
import de.westnordost.streetcomplete.quests.opening_hours.model.Weekdays
import de.westnordost.streetcomplete.quests.opening_hours.WeekdaysPickerDialog
import de.westnordost.streetcomplete.view.dialogs.TimePickerDialog
import kotlinx.serialization.Serializable

@Serializable
data class WeekdaysTimesRow(var weekdays: Weekdays, var minutes: Int)

private fun List<WeekdaysTimesRow>.toWeekdaysTimesList(): List<WeekdaysTimes> {
    val result = mutableListOf<WeekdaysTimes>()
    var last: WeekdaysTimes? = null
    for (row in this) {
        if (row.weekdays == last?.weekdays) {
            last.minutesList.add(row.minutes)
        } else {
            last = WeekdaysTimes(row.weekdays, mutableListOf(row.minutes))
            result.add(last)
        }
    }
    return result
}

class CollectionTimesAdapter(
    initialCollectionTimeRows: List<WeekdaysTimesRow>,
    private val context: Context,
    private val countryInfo: CountryInfo
) : RecyclerView.Adapter<CollectionTimesAdapter.ViewHolder>() {

    var collectionTimesRows: MutableList<WeekdaysTimesRow> = initialCollectionTimeRows.toMutableList()
        private set

    fun createCollectionTimes() = collectionTimesRows.toWeekdaysTimesList()

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val inflater = LayoutInflater.from(parent.context)
        return ViewHolder(QuestTimesWeekdayRowBinding.inflate(inflater, parent, false))
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val times = collectionTimesRows[position]
        val previousTimes = if (position > 0) collectionTimesRows[position - 1] else null
        holder.update(times, previousTimes)
    }

    override fun getItemCount() = collectionTimesRows.size

    /* ------------------------------------------------------------------------------------------ */

    private fun remove(position: Int) {
        collectionTimesRows.removeAt(position)
        notifyItemRemoved(position)
        // if not last weekday removed -> element after this one may need to be updated
        // because it may need to show the weekdays now
        if (position < collectionTimesRows.size) notifyItemChanged(position)
    }

    fun addNewWeekdays() {
        val isFirst = collectionTimesRows.isEmpty()
        openSetWeekdaysDialog(getWeekdaysSuggestion(isFirst)) { weekdays ->
            openSetTimeDialog(12 * 60) { minutes ->
                add(weekdays, minutes) }
        }
    }

    fun addNewHours() {
        val rowAbove = if (collectionTimesRows.size > 0) collectionTimesRows[collectionTimesRows.size - 1] else null
        if (rowAbove == null) return
        openSetTimeDialog(12 * 60) { minutes ->
            add(rowAbove.weekdays, minutes)
        }
    }

    private fun add(weekdays: Weekdays, minutes: Int) {
        val insertIndex = itemCount
        collectionTimesRows.add(WeekdaysTimesRow(weekdays, minutes))
        notifyItemInserted(insertIndex)
    }

    /* ------------------------------------ weekdays select --------------------------------------*/

    inner class ViewHolder(
        private val binding: QuestTimesWeekdayRowBinding
    ) : RecyclerView.ViewHolder(binding.root) {

        init {
            binding.deleteButton.setOnClickListener {
                val index = adapterPosition
                if (index != RecyclerView.NO_POSITION) remove(adapterPosition)
            }
        }

        fun update(times: WeekdaysTimesRow, previousTimes: WeekdaysTimesRow?) {
            if (previousTimes != null && times.weekdays == previousTimes.weekdays) {
                binding.weekdaysLabel.text = ""
            } else {
                binding.weekdaysLabel.text = times.weekdays.toLocalizedString(context.resources)
            }

            binding.weekdaysLabel.setOnClickListener {
                openSetWeekdaysDialog(times.weekdays) { weekdays ->
                    times.weekdays = weekdays
                    notifyItemChanged(adapterPosition)
                }
            }
            binding.hoursLabel.text = "%02d:%02d".format(times.minutes / 60, times.minutes % 60)
            binding.hoursLabel.setOnClickListener {
                openSetTimeDialog(times.minutes) { minutes ->
                    times.minutes = minutes
                    notifyItemChanged(adapterPosition)
                }
            }
        }
    }

    private fun getWeekdaysSuggestion(isFirst: Boolean): Weekdays {
        if (isFirst) {
            val firstWorkDayIdx = Weekdays.getWeekdayIndex(countryInfo.firstDayOfWorkweek)
            val result = BooleanArray(Weekdays.OSM_ABBR_WEEKDAYS.size)
            for (i in 0 until countryInfo.workweekDays) {
                result[(i + firstWorkDayIdx) % Weekdays.WEEKDAY_COUNT] = true
            }
            return Weekdays(result)
        }
        return Weekdays()
    }

    private fun openSetWeekdaysDialog(weekdays: Weekdays, callback: (weekdays: Weekdays) -> Unit) {
        WeekdaysPickerDialog.show(context, weekdays, callback)
    }

    private fun openSetTimeDialog(minutes: Int, callback: (minutes: Int) -> Unit) {
        TimePickerDialog(context, minutes / 60, minutes % 60, DateFormat.is24HourFormat(context)) { hourOfDay, minute ->
            callback(hourOfDay * 60 + minute)
        }.show()
    }
}
