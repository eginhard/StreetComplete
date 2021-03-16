package de.westnordost.streetcomplete.quests.oneway_suspects

import de.westnordost.osmapi.map.data.OsmWay
import de.westnordost.streetcomplete.data.ApplicationDbTestCase
import de.westnordost.streetcomplete.data.osm.mapdata.WayDao
import de.westnordost.streetcomplete.quests.oneway_suspects.data.WayTrafficFlowDao
import de.westnordost.streetcomplete.util.KryoSerializer
import kotlinx.coroutines.runBlocking
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test

class WayTrafficFlowDaoTest : ApplicationDbTestCase() {

    private lateinit var dao: WayTrafficFlowDao

    @Before fun createDao() {
        dao = WayTrafficFlowDao(database)
    }

    @Test fun putGetTrue() = runBlocking {
        dao.put(123L, true)
        assertTrue(dao.isForward(123L)!!)
    }

    @Test fun putGetFalse() = runBlocking {
        dao.put(123L, false)
        assertFalse(dao.isForward(123L)!!)
    }

    @Test fun getNull() = runBlocking {
        assertNull(dao.isForward(123L))
    }

    @Test fun delete() = runBlocking {
        dao.put(123L, false)
        dao.delete(123L)
        assertNull(dao.isForward(123L))
    }

    @Test fun overwrite() = runBlocking {
        dao.put(123L, true)
        dao.put(123L, false)
        assertFalse(dao.isForward(123L)!!)
    }

    @Test fun deleteUnreferenced() = runBlocking {
        val wayDao = WayDao(database, KryoSerializer())

        wayDao.put(OsmWay(1, 0, mutableListOf(), null))
        wayDao.put(OsmWay(2, 0, mutableListOf(), null))

        dao.put(1, true)
        dao.put(3, true)

        dao.deleteUnreferenced()

        assertTrue(dao.isForward(1)!!)
        assertNull(dao.isForward(3))
    }
}
