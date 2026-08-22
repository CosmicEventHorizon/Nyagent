package com.pirouette.nyagent.application.model

import com.google.gson.annotations.SerializedName
import java.io.Serializable

/** A message submitted to a chat API (role + content + optional tool call fields). */
data class OllamaMessageModel(
    @SerializedName("role") val role: String,
    @SerializedName("content") val content: String,
    @SerializedName("tool_calls") val toolCalls: List<OllamaToolCallModel>? = null,
    @SerializedName("tool_call_id") val toolCallId: String? = null
) : Serializable

/** A tool invocation requested by an assistant message (OpenAI tool-call shape). */
data class OllamaToolCallModel(
    @SerializedName("id") val id: String,
    @SerializedName("type") val type: String = "function",
    @SerializedName("function") val function: OllamaToolFunctionModel
) : Serializable

/** The function name + arguments string inside an [OllamaToolCallModel]. */
data class OllamaToolFunctionModel(
    @SerializedName("name") val name: String,
    @SerializedName("arguments") val arguments: String
) : Serializable
