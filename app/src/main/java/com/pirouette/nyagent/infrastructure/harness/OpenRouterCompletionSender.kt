package com.pirouette.nyagent.infrastructure.harness

import com.google.gson.Gson
import com.pirouette.nyagent.application.dto.OpenRouterChatRequestModel
import com.pirouette.nyagent.application.dto.OpenRouterChatResponseModel
import com.pirouette.nyagent.application.interfaces.ISettingsRepository
import com.pirouette.nyagent.application.interfaces.harness.ICompletionSender
import com.pirouette.nyagent.application.model.OllamaMessageModel
import com.pirouette.nyagent.application.model.ToolModel
import com.pirouette.nyagent.infrastructure.OpenRouterApiClient

/**
 * [ICompletionSender] backed by OpenRouter. Reads the current API key and model
 * from settings on each call, sends a blocking completion with tool definitions,
 * and returns the assistant message including any tool calls the model requested.
 */
class OpenRouterCompletionSender(
    private val apiClient: OpenRouterApiClient,
    private val settings: ISettingsRepository,
    private val gson: Gson = Gson()
) : ICompletionSender {

    override fun complete(
        messages: List<OllamaMessageModel>,
        tools: List<ToolModel>
    ): List<OllamaMessageModel> {
        val request = OpenRouterChatRequestModel(
            model = settings.openRouterModel,
            messages = messages,
            tools = tools,
            toolChoice = "auto",
            stream = false
        )
        val body = apiClient.postChat(PATH_COMPLETIONS, settings.openRouterApiKey, gson.toJson(request))
        val response = gson.fromJson(body, OpenRouterChatResponseModel::class.java)
        val message = response.choices.firstOrNull()?.message
            ?: return listOf(OllamaMessageModel("assistant", ""))
        return listOf(
            OllamaMessageModel(
                role = "assistant",
                content = message.content ?: "",
                toolCalls = message.toolCalls
            )
        )
    }

    private companion object {
        const val PATH_COMPLETIONS = "/chat/completions"
    }
}
