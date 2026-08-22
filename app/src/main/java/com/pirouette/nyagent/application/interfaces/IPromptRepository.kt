package com.pirouette.nyagent.application.interfaces

import com.pirouette.nyagent.application.model.SavedPromptModel

/** Contract for persisting the saved system-prompt library. */
interface IPromptRepository {
    fun loadPrompts(): List<SavedPromptModel>
    fun savePrompts(prompts: List<SavedPromptModel>)
}
