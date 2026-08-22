package com.pirouette.nyagent.application.dto

import com.google.gson.annotations.SerializedName

/** Response body of the OpenRouter /api/v1/models endpoint. */
data class OpenRouterArtifactResponseModel(
    @SerializedName("data") val data: List<OpenRouterArtifactModel> = emptyList()
)
