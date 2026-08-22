package com.pirouette.nyagent.application.dto

import com.google.gson.annotations.SerializedName

/** Response body of the Ollama /api/tags endpoint. */
data class OllamaTagsResponseModel(
    @SerializedName("models") val models: List<OllamaArtifactModel> = emptyList()
)
