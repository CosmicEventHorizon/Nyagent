package com.pirouette.nyagent.application.service

import com.pirouette.nyagent.application.interfaces.IOllamaModelProxy

/** High-level model listing operations for the Ollama settings screen. */
class OllamaModelService(private val proxy: IOllamaModelProxy) {

    fun fetchModels(
        serverAddress: String,
        port: String,
        onResult: (List<String>) -> Unit,
        onError: (String) -> Unit
    ) {
        proxy.fetchModels(serverAddress, port, onResult, onError)
    }
}
