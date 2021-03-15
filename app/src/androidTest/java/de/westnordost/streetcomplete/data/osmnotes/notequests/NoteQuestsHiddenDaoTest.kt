package de.westnordost.streetcomplete.data.osmnotes.notequests

import de.westnordost.streetcomplete.data.ApplicationDbTestCase
import de.westnordost.streetcomplete.ktx.containsExactlyInAnyOrder
import kotlinx.coroutines.runBlocking
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test

class NoteQuestsHiddenDaoTest : ApplicationDbTestCase() {
    private lateinit var dao: NoteQuestsHiddenDao

    @Before fun createDao() {
        dao = NoteQuestsHiddenDao(database)
    }

    @Test fun getButNothingIsThere() = runBlocking {
        assertFalse(dao.contains(123L))
    }

    @Test fun addAndGet() = runBlocking {
        dao.add(123L)
        assertTrue(dao.contains(123L))
    }

    @Test fun getAll() = runBlocking {
        dao.add(1L)
        dao.add(2L)
        assertTrue(dao.getAll().containsExactlyInAnyOrder(listOf(1L,2L)))
    }

    @Test fun deleteAll() = runBlocking {
        assertEquals(0, dao.deleteAll())
        dao.add(1L)
        dao.add(2L)
        assertEquals(2, dao.deleteAll())
        assertFalse(dao.contains(1L))
        assertFalse(dao.contains(2L))
    }
}
