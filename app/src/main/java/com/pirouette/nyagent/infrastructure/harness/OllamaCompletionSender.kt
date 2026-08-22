package com.pirouette.nyagent.infrastructure.harness

import com.google.gson.Gson
import com.google.gson.JsonArray
import com.google.gson.JsonElement
import com.google.gson.JsonObject
import com.google.gson.JsonParser
import com.pirouette.nyagent.application.dto.OllamaChatResponseModel
import com.pirouette.nyagent.application.interfaces.ISettingsRepository
import com.pirouette.nyagent.application.interfaces.harness.ICompletionSender
import com.pirouette.nyagent.application.model.OllamaMessageModel
import com.pirouette.nyagent.application.model.OllamaToolCallModel
import com.pirouette.nyagent.application.model.OllamaToolFunctionModel
import com.pirouette.nyagent.application.model.ToolModel
import com.pirouette.nyagent.infrastructure.OllamaApiClient

/**
 * [ICompletionSender] backed by Ollama. Reads the current model and server
 * settings on each call, sends a blocking non-streaming chat request with tool
 * definitions, and normalises any Ollama tool calls into the OpenAI-shaped
 * [OllamaToolCallModel] the harness understands.
 */
class OllamaCompletionSender(
    private val apiClient: OllamaApiClient,
    private val settings: ISettingsRepository,
    private val gson: Gson = Gson()
) : ICompletionSender {

    override fun complete(
        messages: List<OllamaMessageModel>,
        tools: List<ToolModel>
    ): List<OllamaMessageModel> {
        val body = apiClient.postChat(
            serverAddress = settings.serverAddress,
            port = settings.serverPort,
            path = PATH_CHAT,
            requestBody = buildNativeBody(messages, tools)
        )
        val response = gson.fromJson(body, OllamaChatResponseModel::class.java)
        val message = response.message ?: return listOf(OllamaMessageModel("assistant", ""))
        val toolCalls = message.toolCalls?.mapIndexed { index, call ->
            OllamaToolCallModel(
                id = "call_$index",
                type = "function",
                function = OllamaToolFunctionModel(
                    name = call.function?.name.orEmpty(),
                    arguments = stringifyArguments(call.function?.arguments)
                )
            )
        }
        return listOf(
            OllamaMessageModel(
                role = "assistant",
                content = message.content.orEmpty(),
                toolCalls = toolCalls?.takeIf { it.isNotEmpty() }
            )
        )
    }

    private companion object {
        const val PATH_CHAT = "/api/chat"
    }

    /**
     * Ollama's native /api/chat contract requires every tool-call "arguments"
     * field to be an actual JSON object, whereas the shared harness model keeps
     * them as JSON strings (the OpenAI/OpenRouter shape). Building the body here
     * parses each arguments string back into an object so Ollama never sees a
     * quoted "{"..."}".
     */
    private fun buildNativeBody(
        messages: List<OllamaMessageModel>,
        tools: List<ToolModel>
    ): String {
        val root = JsonObject()
        root.addProperty("model", settings.model)
        root.addProperty("stream", false)
        if (tools.isNotEmpty()) {
            root.add("tools", gson.toJsonTree(tools))
        }

        val messageArray = JsonArray()
        for (message in messages) {
            val obj = JsonObject()
            obj.addProperty("role", message.role)
            obj.addProperty("content", message.content)
            if (message.toolCallId != null) {
                obj.addProperty("tool_call_id", message.toolCallId)
            }
            if (message.toolCalls != null && message.toolCalls.isNotEmpty()) {
                val calls = JsonArray()
                for (call in message.toolCalls) {
                    val callObj = JsonObject()
                    callObj.addProperty("id", call.id)
                    callObj.addProperty("type", call.type)
                    val fn = JsonObject()
                    fn.addProperty("name", call.function.name)
                    fn.add("arguments", parseArguments(call.function.arguments))
                    callObj.add("function", fn)
                    calls.add(callObj)
                }
                obj.add("tool_calls", calls)
            }
            messageArray.add(obj)
        }
        root.add("messages", messageArray)
        return root.toString()
    }

    /**
     * Parses the stored arguments JSON string back into a JSON element so it
     * nests as an object rather than a quoted string. Falls back to "{}".
     */
    private fun parseArguments(arguments: String): JsonElement {
        return try {
            JsonParser.parseString(arguments)
        } catch (e: Exception) {
            JsonObject()
        }
    }

    /**
     * Ollama returns tool-call arguments as a JSON object (or a JSON string).
     * Serialises either form back into a JSON string so the harness sees the
     * same OpenAI-shaped arguments it expects from any provider.
     */
    private fun stringifyArguments(arguments: Any?): String {
        return if (arguments is String) {
            arguments
        } else if (arguments != null) {
            gson.toJson(arguments)
        } else {
            "{}"
        }
    }
}
