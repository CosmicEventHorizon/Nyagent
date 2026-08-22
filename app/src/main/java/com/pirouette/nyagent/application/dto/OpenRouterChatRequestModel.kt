package com.pirouette.nyagent.application.dto

import com.google.gson.annotations.SerializedName
import com.pirouette.nyagent.application.model.OllamaMessageModel
import com.pirouette.nyagent.application.model.ToolModel

/** Body of a POST to the OpenRouter /api/v1/chat/completions endpoint. */
data class OpenRouterChatRequestModel(
    @SerializedName("model") val model: String?,
    @SerializedName("messages") val messages: List<OllamaMessageModel>,
    @SerializedName("tools") val tools: List<ToolModel> = emptyList(),
    @SerializedName("tool_choice") val toolChoice: String = "auto",
    @SerializedName("stream") val stream: Boolean = false
)
