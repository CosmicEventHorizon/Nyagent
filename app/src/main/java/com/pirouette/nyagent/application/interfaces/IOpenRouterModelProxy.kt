package com.pirouette.nyagent.application.interfaces

/** Contract for fetching the list of available OpenRouter models. */
interface IOpenRouterModelProxy {
    /** Fetches model ids, calling [onResult] on the main thread. */
    fun fetchModels(apiKey: String, onResult: (List<String>) -> Unit, onError: (String) -> Unit)
}
