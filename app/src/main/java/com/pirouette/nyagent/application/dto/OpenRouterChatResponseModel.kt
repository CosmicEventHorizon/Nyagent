package com.pirouette.nyagent.application.dto

import com.google.gson.annotations.SerializedName

/** Response body of the OpenRouter /api/v1/chat/completions endpoint. */
data class OpenRouterChatResponseModel(
    @SerializedName("choices") val choices: List<OpenRouterChoiceModel> = emptyList(),
    @SerializedName("usage") val usage: OpenRouterUsageModel? = null
)
