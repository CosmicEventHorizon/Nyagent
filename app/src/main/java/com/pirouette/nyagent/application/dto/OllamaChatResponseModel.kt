package com.pirouette.nyagent.application.dto

import com.google.gson.annotations.SerializedName

/** Response body of a non-streaming Ollama /api/chat request. */
data class OllamaChatResponseModel(
    @SerializedName("message") val message: OllamaMessageResponseModel? = null
)

/** The assistant message inside an Ollama chat response. */
data class OllamaMessageResponseModel(
    @SerializedName("role") val role: String = "assistant",
    @SerializedName("content") val content: String? = null,
    @SerializedName("tool_calls") val toolCalls: List<OllamaToolCallResponseModel>? = null
)

/** A tool invocation requested by an Ollama assistant message. */
data class OllamaToolCallResponseModel(
    @SerializedName("function") val function: OllamaToolFunctionResponseModel? = null
)

/** The function name + JSON arguments inside an [OllamaToolCallResponseModel]. */
data class OllamaToolFunctionResponseModel(
    @SerializedName("name") val name: String = "",
    @SerializedName("arguments") val arguments: Any? = null
)
