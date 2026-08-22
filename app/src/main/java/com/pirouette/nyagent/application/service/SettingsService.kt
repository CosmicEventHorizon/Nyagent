package com.pirouette.nyagent.application.service

import com.pirouette.nyagent.application.interfaces.IPromptRepository
import com.pirouette.nyagent.application.interfaces.ISettingsRepository
import com.pirouette.nyagent.application.model.SavedPromptModel

/** High-level operations over application + prompt settings. */
class SettingsService(
    private val settings: ISettingsRepository,
    private val prompts: IPromptRepository
) {

    var serverAddress: String
        get() = settings.serverAddress
        set(value) { settings.serverAddress = value }

    var serverPort: String
        get() = settings.serverPort
        set(value) { settings.serverPort = value }

    var model: String
        get() = settings.model
        set(value) { settings.model = value }

    var systemPromptName: String
        get() = settings.systemPromptName
        set(value) { settings.systemPromptName = value }

    var systemPrompt: String
        get() = settings.systemPrompt
        set(value) { settings.systemPrompt = value }

    var openRouterApiKey: String
        get() = settings.openRouterApiKey
        set(value) { settings.openRouterApiKey = value }

    var openRouterModel: String
        get() = settings.openRouterModel
        set(value) { settings.openRouterModel = value }

    var useOllama: Boolean
        get() = settings.useOllama
        set(value) { settings.useOllama = value }

    var maxContextTokens: Int
        get() = settings.maxContextTokens
        set(value) { settings.maxContextTokens = value }

    var compactThresholdTokens: Int
        get() = settings.compactThresholdTokens
        set(value) { settings.compactThresholdTokens = value }

    /** The whole saved prompt library. */
    val promptLibrary: List<SavedPromptModel>
        get() = prompts.loadPrompts()

    /**
     * Persists the current form values. The on-screen system prompt becomes the
     * active prompt and is moved to the front of the saved library.
     */
    fun saveCurrentSettings(promptName: String, promptText: String) {
        systemPromptName = promptName
        systemPrompt = promptText
        if (promptName.isNotEmpty() && promptText.isNotEmpty()) {
            val existing = prompts.loadPrompts().toMutableList()
            existing.removeAll { it.name == promptName || it.prompt == promptText }
            existing.add(0, SavedPromptModel(promptName, promptText))
            prompts.savePrompts(existing)
        }
    }

    /** Sets the active system prompt from the library without reordering. */
    fun selectSystemPrompt(prompt: SavedPromptModel) {
        systemPromptName = prompt.name
        systemPrompt = prompt.prompt
    }

    fun resetServerDefaults() {
        serverPort = ISettingsRepository.DEFAULT_SERVER_PORT
        serverAddress = ISettingsRepository.DEFAULT_SERVER_ADDRESS
        model = ISettingsRepository.DEFAULT_MODEL
    }
}
