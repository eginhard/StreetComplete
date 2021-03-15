package de.westnordost.streetcomplete.data.visiblequests

import org.junit.Before
import org.junit.Test

import de.westnordost.streetcomplete.data.ApplicationDbTestCase
import kotlinx.coroutines.runBlocking

import org.junit.Assert.*

class VisibleQuestTypeDaoTest : ApplicationDbTestCase() {
    private lateinit var dao: VisibleQuestTypeDao

    @Before fun createDao() {
        dao = VisibleQuestTypeDao(database)
    }

    @Test fun defaultEnabledQuest() = runBlocking {
        assertTrue(dao.get("something"))
    }

    @Test fun disableQuest() = runBlocking {
        dao.put("no", false)
        assertFalse(dao.get("no"))
    }

    @Test fun enableQuest() = runBlocking {
        dao.put("no", false)
        dao.put("no", true)
        assertTrue(dao.get("no"))
    }

    @Test fun reset() = runBlocking {
        dao.put("blurb", false)
        assertFalse(dao.get("blurb"))
        dao.clear()
        assertTrue(dao.get("blurb"))
    }
}
