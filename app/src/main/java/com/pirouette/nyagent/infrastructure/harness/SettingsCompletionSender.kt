package com.pirouette.nyagent.infrastructure.harness

import com.pirouette.nyagent.application.interfaces.ISettingsRepository
import com.pirouette.nyagent.application.interfaces.harness.ICompletionSender
import com.pirouette.nyagent.application.model.OllamaMessageModel
import com.pirouette.nyagent.application.model.ToolModel

/**
 * [ICompletionSender] that forwards every completion request to the provider
 * currently selected in settings (Ollama or OpenRouter), so the harness behaves
 * identically regardless of which chat backend the user picked.
 */
class SettingsCompletionSender(
    private val settings: ISettingsRepository,
    private val ollama: ICompletionSender,
    private val openRouter: ICompletionSender
) : ICompletionSender {

    override fun complete(
        messages: List<OllamaMessageModel>,
        tools: List<ToolModel>
    ): List<OllamaMessageModel> {
        val sender = if (settings.useOllama) ollama else openRouter
        return sender.complete(messages, tools)
    }
}
