package com.pirouette.nyagent.application.dto

import com.google.gson.annotations.SerializedName

/** Token usage reported by an OpenRouter chat completion. */
data class OpenRouterUsageModel(
    @SerializedName("prompt_tokens") val promptTokens: Int = 0,
    @SerializedName("completion_tokens") val completionTokens: Int = 0,
    @SerializedName("total_tokens") val totalTokens: Int = 0
)
