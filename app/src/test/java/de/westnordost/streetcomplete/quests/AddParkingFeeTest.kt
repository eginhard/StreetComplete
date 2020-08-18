package de.westnordost.streetcomplete.quests

import de.westnordost.streetcomplete.data.meta.toCheckDateString
import de.westnordost.streetcomplete.data.osm.changes.StringMapEntryAdd
import de.westnordost.streetcomplete.data.osm.changes.StringMapEntryDelete
import de.westnordost.streetcomplete.data.osm.changes.StringMapEntryModify
import de.westnordost.streetcomplete.mock
import de.westnordost.streetcomplete.quests.opening_hours.model.*
import de.westnordost.streetcomplete.quests.parking_fee.*
import org.junit.Test
import java.util.*

class AddParkingFeeTest {

    private val questType = AddParkingFee(mock(), mock())

    private val openingHours = listOf(
        OpeningMonths(CircularSection(0,11), listOf(
            listOf(OpeningWeekdays(
                Weekdays(booleanArrayOf(true)),
                mutableListOf(TimeRange(0, 12*60))
            )),
            listOf(OpeningWeekdays(
                Weekdays(booleanArrayOf(false, true)),
                mutableListOf(TimeRange(12*60, 24*60))
            ))
        ))
    )
    private val openingHoursString = "Mo 00:00-12:00; Tu 12:00-24:00"

    @Test fun `apply yes answer`() {
        questType.verifyAnswer(HasFee, StringMapEntryAdd("fee", "yes"))
    }

    @Test fun `apply no answer`() {
        questType.verifyAnswer(HasNoFee, StringMapEntryAdd("fee", "no"))
    }

    @Test fun `apply only at hours answer`() {
        questType.verifyAnswer(
            HasFeeAtHours(openingHours),
            StringMapEntryAdd("fee", "no"),
            StringMapEntryAdd("fee:conditional", "yes @ ($openingHoursString)")
        )
    }

    @Test fun `apply yes except at hours answer`() {
        questType.verifyAnswer(
            HasFeeExceptAtHours(openingHours),
            StringMapEntryAdd("fee", "yes"),
            StringMapEntryAdd("fee:conditional", "no @ ($openingHoursString)")
        )
    }

    @Test fun `apply yes answer if before was conditional`() {
        questType.verifyAnswer(
            mapOf("fee:conditional" to "someval", "fee" to "no"),
            HasFee,
            StringMapEntryModify("fee", "no", "yes"),
            StringMapEntryDelete("fee:conditional", "someval")
        )
    }

    @Test fun `apply conditional answer if before was yes`() {
        questType.verifyAnswer(
            mapOf("fee" to "yes"),
            HasFeeExceptAtHours(openingHours),
            StringMapEntryAdd("fee:conditional", "no @ ($openingHoursString)"),
            StringMapEntryAdd("check_date:fee", Date().toCheckDateString())
        )
    }
}
