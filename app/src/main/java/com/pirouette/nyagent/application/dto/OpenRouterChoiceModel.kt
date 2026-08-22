package com.pirouette.nyagent.application.dto

import com.google.gson.annotations.SerializedName

/** A single choice returned by an OpenRouter chat completion. */
data class OpenRouterChoiceModel(
    @SerializedName("message") val message: OpenRouterMessageModel? = null
)
