package com.pirouette.nyagent.application.dto

import com.google.gson.annotations.SerializedName

/** A single model listed by the Ollama /api/tags endpoint. */
data class OllamaArtifactModel(
    @SerializedName("name") val name: String = ""
)
