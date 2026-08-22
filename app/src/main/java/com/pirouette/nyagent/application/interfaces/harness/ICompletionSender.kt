package com.pirouette.nyagent.application.interfaces.harness

import com.pirouette.nyagent.application.model.OllamaMessageModel
import com.pirouette.nyagent.application.model.ToolModel

/** Sends a completion request so the harness stays decoupled from any single LLM provider. */
interface ICompletionSender {

    /**
     * Requests a non-streaming completion and returns the raw assistant reply
     * (already parsed to messages so the harness can inspect tool calls).
     */
    fun complete(messages: List<OllamaMessageModel>, tools: List<ToolModel>): List<OllamaMessageModel>
}
