package com.pirouette.nyagent.application.service

import com.pirouette.nyagent.application.interfaces.IStoryRepository
import com.pirouette.nyagent.application.model.SavedStoryModel

/** High-level operations over the saved-story library. */
class StoryService(private val repository: IStoryRepository) {

    /** All saved stories, newest first. */
    fun loadAll(): List<SavedStoryModel> = repository.loadAll()

    fun save(story: SavedStoryModel) = repository.save(story)

    fun delete(index: Int) = repository.delete(index)

    fun deleteByName(name: String) = repository.deleteByName(name)
}
