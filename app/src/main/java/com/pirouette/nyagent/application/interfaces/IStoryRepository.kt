package com.pirouette.nyagent.application.interfaces

import com.pirouette.nyagent.application.model.SavedStoryModel

/** Contract for persisting saved conversations. */
interface IStoryRepository {
    /** All saved stories, newest first. */
    fun loadAll(): List<SavedStoryModel>

    fun save(story: SavedStoryModel)

    fun delete(index: Int)
}
