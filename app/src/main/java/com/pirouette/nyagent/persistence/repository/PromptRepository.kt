package com.pirouette.nyagent.persistence.repository

import android.content.SharedPreferences
import com.pirouette.nyagent.application.interfaces.IPromptRepository
import com.pirouette.nyagent.application.model.SavedPromptModel
import com.pirouette.nyagent.persistence.entity.SavedPromptEntity
import org.json.JSONArray
import org.json.JSONObject

/** [IPromptRepository] backed by a JSON array stored in SharedPreferences. */
class PromptRepository(private val prefs: SharedPreferences) : IPromptRepository {

    override fun loadPrompts(): List<SavedPromptModel> {
        val raw = prefs.getString(KEY_PROMPT_LIST, null)
        val prompts = ArrayList<SavedPromptModel>()

        if (!raw.isNullOrEmpty()) {
            val array = JSONArray(raw)
            for (index in 0 until array.length()) {
                array.opt(index)?.let { item ->
                    val entity = when (item) {
                        is JSONObject -> SavedPromptEntity(
                            item.optString(FIELD_NAME).trim(),
                            item.optString(FIELD_PROMPT)
                        )
                        else -> SavedPromptEntity(
                            SavedPromptModel.buildLabel(array.optString(index)),
                            array.optString(index)
                        )
                    }
                    if (entity.name.isNotEmpty() && entity.prompt.isNotEmpty()) {
                        prompts.add(entity.toModel())
                    }
                }
            }
        }
        return prompts
    }

    override fun savePrompts(prompts: List<SavedPromptModel>) {
        val array = JSONArray()
        prompts.forEach { prompt ->
            if (prompt.name.isNotEmpty() && prompt.prompt.isNotEmpty()) {
                array.put(
                    JSONObject()
                        .put(FIELD_NAME, prompt.name)
                        .put(FIELD_PROMPT, prompt.prompt)
                )
            }
        }
        prefs.edit().putString(KEY_PROMPT_LIST, array.toString()).apply()
    }

    private fun SavedPromptEntity.toModel() = SavedPromptModel(name, prompt)

    companion object {
        const val PREFS_NAME = "saved_ollama_settings"
        private const val KEY_PROMPT_LIST = "SYSTEM_PROMPT_LIST"
        private const val FIELD_NAME = "name"
        private const val FIELD_PROMPT = "prompt"
    }
}
