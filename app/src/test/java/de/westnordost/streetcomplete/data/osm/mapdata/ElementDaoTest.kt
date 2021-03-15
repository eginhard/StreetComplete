package de.westnordost.streetcomplete.data.osm.mapdata

import org.junit.Before
import org.junit.Test

import de.westnordost.osmapi.map.data.Element
import de.westnordost.streetcomplete.testutils.mock
import de.westnordost.streetcomplete.testutils.node
import de.westnordost.streetcomplete.testutils.way
import de.westnordost.streetcomplete.testutils.rel
import kotlinx.coroutines.runBlocking

import org.mockito.Mockito.*

class ElementDaoTest {
    private lateinit var nodeDao: NodeDao
    private lateinit var wayDao: WayDao
    private lateinit var relationDao: RelationDao
    private lateinit var dao: ElementDao

    @Before fun setUp() {
        nodeDao = mock()
        wayDao = mock()
        relationDao = mock()
        dao = ElementDao(nodeDao, wayDao, relationDao)
    }

    @Test fun putNode(): Unit = runBlocking {
        val node = node(1)
        dao.put(node)
        verify(nodeDao).put(node)
    }

    @Test fun getNode(): Unit = runBlocking {
        dao.get(Element.Type.NODE, 1L)
        verify(nodeDao).get(1L)
    }

    @Test fun deleteNode(): Unit = runBlocking {
        dao.delete(Element.Type.NODE, 1L)
        verify(nodeDao).delete(1L)
    }

    @Test fun putWay(): Unit = runBlocking {
        val way = way()
        dao.put(way)
        verify(wayDao).put(way)
    }

    @Test fun getWay(): Unit = runBlocking {
        dao.get(Element.Type.WAY, 1L)
        verify(wayDao).get(1L)
    }

    @Test fun deleteWay(): Unit = runBlocking {
        dao.delete(Element.Type.WAY, 1L)
        verify(wayDao).delete(1L)
    }

    @Test fun putRelation(): Unit = runBlocking {
        val relation = rel()
        dao.put(relation)
        verify(relationDao).put(relation)
    }

    @Test fun getRelation(): Unit = runBlocking {
        dao.get(Element.Type.RELATION, 1L)
        verify(relationDao).get(1L)
    }

    @Test fun deleteRelation(): Unit = runBlocking {
        dao.delete(Element.Type.RELATION, 1L)
        verify(relationDao).delete(1L)
    }

    @Test fun putAllRelations(): Unit = runBlocking {
        dao.putAll(listOf(rel()))
        verify(relationDao).putAll(anyCollection())
    }

    @Test fun putAllWays(): Unit = runBlocking {
        dao.putAll(listOf(way()))
        verify(wayDao).putAll(anyCollection())
    }

    @Test fun putAllNodes(): Unit = runBlocking {
        dao.putAll(listOf(node()))
        verify(nodeDao).putAll(anyCollection())
    }

    @Test fun putAllElements(): Unit = runBlocking {
        dao.putAll(listOf(node(), way(), rel()))

        verify(nodeDao).putAll(anyCollection())
        verify(wayDao).putAll(anyCollection())
        verify(relationDao).putAll(anyCollection())
    }

    @Test fun deleteAllElements(): Unit = runBlocking {
        dao.deleteAll(listOf(
            ElementKey(Element.Type.NODE,0),
            ElementKey(Element.Type.WAY,0),
            ElementKey(Element.Type.RELATION,0)
        ))

        verify(nodeDao).deleteAll(listOf(0L))
        verify(wayDao).deleteAll(listOf(0L))
        verify(relationDao).deleteAll(listOf(0L))
    }

    @Test fun getAllElements(): Unit = runBlocking {
        dao.getAll(listOf(
            ElementKey(Element.Type.NODE,0),
            ElementKey(Element.Type.WAY,0),
            ElementKey(Element.Type.RELATION,0)
        ))

        verify(nodeDao).getAll(listOf(0L))
        verify(wayDao).getAll(listOf(0L))
        verify(relationDao).getAll(listOf(0L))
    }
}
