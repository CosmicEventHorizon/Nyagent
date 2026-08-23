package com.pirouette.nyagent.persistence.repository

import android.content.Context
import com.pirouette.nyagent.application.interfaces.IStoryRepository
import com.pirouette.nyagent.application.model.SavedStoryModel
import java.io.IOException
import java.io.ObjectInputStream
import java.io.ObjectOutputStream

/** [IStoryRepository] backed by Java object serialization to a private file. */
class StoryRepository(private val context: Context) : IStoryRepository {

    private val file by lazy {
        context.getFileStreamPath(FILE_NAME)
    }

    override fun loadAll(): List<SavedStoryModel> {
        return try {
            if (!file.exists()) {
                return emptyList()
            }
            ObjectInputStream(context.openFileInput(FILE_NAME)).use { stream ->
                @Suppress("UNCHECKED_CAST")
                (stream.readObject() as ArrayList<SavedStoryModel>)
            }
        } catch (e: Exception) {
            e.printStackTrace()
            emptyList()
        }
    }

    /**
     * Saves [story], replacing any existing story with the same name so a
     * conversation keeps a single entry that is always refreshed on each send.
     * The saved entry is moved to the end so it reads as the newest first.
     */
    override fun save(story: SavedStoryModel) {
        try {
            val stories = ArrayList(loadAll().filterNot { it.name == story.name })
            stories.add(story)
            ObjectOutputStream(context.openFileOutput(FILE_NAME, Context.MODE_PRIVATE)).use { stream ->
                stream.writeObject(stories)
            }
        } catch (e: IOException) {
            e.printStackTrace()
        }
    }

    override fun delete(index: Int) {
        val stories = ArrayList(loadAll())
        if (index in stories.indices) {
            stories.removeAt(index)
            try {
                ObjectOutputStream(context.openFileOutput(FILE_NAME, Context.MODE_PRIVATE)).use { stream ->
                    stream.writeObject(stories)
                }
            } catch (e: IOException) {
                e.printStackTrace()
            }
        }
    }

    override fun deleteByName(name: String) {
        val stories = loadAll().filterNot { it.name == name }
        if (stories.size != loadAll().size) {
            try {
                ObjectOutputStream(context.openFileOutput(FILE_NAME, Context.MODE_PRIVATE)).use { stream ->
                    stream.writeObject(stories)
                }
            } catch (e: IOException) {
                e.printStackTrace()
            }
        }
    }

    companion object {
        private const val FILE_NAME = "array_data"
    }
}
