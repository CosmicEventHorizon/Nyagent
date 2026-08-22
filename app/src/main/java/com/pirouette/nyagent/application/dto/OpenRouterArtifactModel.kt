package com.pirouette.nyagent.application.dto

import com.google.gson.annotations.SerializedName

/** A single model listed by the OpenRouter /api/v1/models endpoint. */
data class OpenRouterArtifactModel(
    @SerializedName("id") val id: String = ""
)
