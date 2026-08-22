package com.pirouette.nyagent.application.dto

import com.google.gson.annotations.SerializedName

/** Credit information returned by the OpenRouter /api/v1/credits endpoint. */
data class OpenRouterCreditsModel(
    @SerializedName("total_credits") val totalCredits: Double? = null,
    @SerializedName("total_usage") val totalUsage: Double? = null
) {
    /** Money left in the account, or null when credits/usage are unavailable. */
    val remainingCredit: Double?
        get() = if (totalCredits != null) totalCredits - (totalUsage ?: 0.0) else null
}
