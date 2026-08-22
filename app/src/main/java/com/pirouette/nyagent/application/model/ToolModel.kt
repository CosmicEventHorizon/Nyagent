package com.pirouette.nyagent.application.model

import com.google.gson.annotations.SerializedName

/** A tool an LLM may call, described as a name, description and JSON argument schema. */
data class ToolModel(
    @SerializedName("type") val type: String = "function",
    @SerializedName("function") val function: ToolFunctionModel
)

/** The function descriptor inside a [ToolModel] (OpenAI-style tool calling shape). */
data class ToolFunctionModel(
    @SerializedName("name") val name: String,
    @SerializedName("description") val description: String,
    @SerializedName("parameters") val parameters: Map<String, Any?>
)
