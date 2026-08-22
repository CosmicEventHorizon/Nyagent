package com.pirouette.nyagent.application.interfaces

/** Contract for fetching the list of available Ollama models. */
interface IOllamaModelProxy {
    /** Fetches model names, calling [onResult] on the main thread. */
    fun fetchModels(serverAddress: String, port: String, onResult: (List<String>) -> Unit, onError: (String) -> Unit)
}
