package com.pirouette.nyagent.application.dto

import com.google.gson.annotations.SerializedName

/** Response body of the OpenRouter /api/v1/credits endpoint. */
data class OpenRouterCreditsResponseModel(
    @SerializedName("data") val data: OpenRouterCreditsModel? = null
)
