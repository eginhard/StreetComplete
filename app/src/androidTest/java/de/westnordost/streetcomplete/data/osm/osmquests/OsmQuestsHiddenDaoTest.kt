package de.westnordost.streetcomplete.data.osm.osmquests

import de.westnordost.osmapi.map.data.Element
import de.westnordost.streetcomplete.data.ApplicationDbTestCase
import de.westnordost.streetcomplete.ktx.containsExactlyInAnyOrder
import kotlinx.coroutines.runBlocking
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test

class OsmQuestsHiddenDaoTest : ApplicationDbTestCase() {
    private lateinit var dao: OsmQuestsHiddenDao

    @Before fun createDao() {
        dao = OsmQuestsHiddenDao(database)
    }

    @Test fun getButNothingIsThere() = runBlocking {
        assertFalse(dao.contains(OsmQuestKey(Element.Type.NODE, 0L, "bla")))
    }

    @Test fun addAndGet() = runBlocking {
        val key = OsmQuestKey(Element.Type.NODE, 123L, "bla")
        dao.add(key)
        assertTrue(dao.contains(key))
    }

    @Test fun getAll() = runBlocking {
        val keys = listOf(
            OsmQuestKey(Element.Type.NODE, 123L, "bla"),
            OsmQuestKey(Element.Type.NODE, 124L, "bla")
        )
        keys.forEach { dao.add(it) }
        assertTrue(dao.getAll().containsExactlyInAnyOrder(keys))
    }

    @Test fun deleteAll() = runBlocking {
        assertEquals(0, dao.deleteAll())
        val keys = listOf(
            OsmQuestKey(Element.Type.NODE, 123L, "bla"),
            OsmQuestKey(Element.Type.NODE, 124L, "bla")
        )
        keys.forEach { dao.add(it) }
        assertEquals(2, dao.deleteAll())
        assertFalse(dao.contains(keys[0]))
        assertFalse(dao.contains(keys[1]))
    }
}
