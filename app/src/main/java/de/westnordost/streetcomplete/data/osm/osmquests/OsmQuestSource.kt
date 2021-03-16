package de.westnordost.streetcomplete.data.osm.osmquests

import de.westnordost.osmapi.map.data.BoundingBox

interface OsmQuestSource {

    interface Listener {
        fun onUpdated(addedQuests: Collection<OsmQuest>, deletedQuestIds: Collection<Long>)
    }

    /** get single quest by id */
    suspend fun get(questId: Long): OsmQuest?

    /** Get count of all quests in given bounding box */
    suspend fun getAllInBBoxCount(bbox: BoundingBox): Int

    /** Get all quests of optionally the given types in given bounding box */
    suspend fun getAllVisibleInBBox(bbox: BoundingBox, questTypes: Collection<String>? = null): List<OsmQuest>

    fun addListener(listener: Listener)
    fun removeListener(listener: Listener)
}
