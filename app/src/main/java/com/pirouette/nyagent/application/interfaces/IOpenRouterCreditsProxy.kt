package com.pirouette.nyagent.application.interfaces

/** Contract for fetching the remaining credits on an OpenRouter API key. */
interface IOpenRouterCreditsProxy {
    /**
     * Fetches the remaining credits, calling [onResult] on the main thread.
     * A null value means the key is on the free tier or has no limit.
     */
    fun fetchCredits(apiKey: String, onResult: (Double?) -> Unit, onError: (String) -> Unit)
}
