package com.pirouette.nyagent.application.dto

import com.google.gson.annotations.SerializedName
import com.pirouette.nyagent.application.model.OllamaMessageModel
import com.pirouette.nyagent.application.model.ToolModel

/** Body of a POST to the Ollama /api/chat endpoint. */
data class OllamaChatRequestModel(
    @SerializedName("model") val model: String?,
    @SerializedName("messages") val messages: List<OllamaMessageModel>,
    @SerializedName("tools") val tools: List<ToolModel> = emptyList(),
    @SerializedName("stream") val stream: Boolean
)
