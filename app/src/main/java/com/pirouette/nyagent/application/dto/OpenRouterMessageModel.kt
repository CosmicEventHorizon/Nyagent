package com.pirouette.nyagent.application.dto

import com.google.gson.annotations.SerializedName
import com.pirouette.nyagent.application.model.OllamaToolCallModel

/** The assistant message inside an OpenRouter chat completion choice. */
data class OpenRouterMessageModel(
    @SerializedName("content") val content: String? = null,
    @SerializedName("tool_calls") val toolCalls: List<OllamaToolCallModel>? = null
)
