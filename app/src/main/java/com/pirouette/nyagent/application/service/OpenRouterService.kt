package com.pirouette.nyagent.application.service

import com.pirouette.nyagent.application.interfaces.IOpenRouterCreditsProxy
import com.pirouette.nyagent.application.interfaces.IOpenRouterModelProxy

/** High-level OpenRouter operations: model listing and credit lookups. */
class OpenRouterService(
    private val modelProxy: IOpenRouterModelProxy,
    private val creditsProxy: IOpenRouterCreditsProxy
) {

    fun fetchModels(apiKey: String, onResult: (List<String>) -> Unit, onError: (String) -> Unit) {
        modelProxy.fetchModels(apiKey, onResult, onError)
    }

    fun fetchCredits(apiKey: String, onResult: (Double?) -> Unit, onError: (String) -> Unit) {
        creditsProxy.fetchCredits(apiKey, onResult, onError)
    }
}
