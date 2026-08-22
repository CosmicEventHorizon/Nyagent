package com.pirouette.nyagent.application.interfaces.harness

import com.pirouette.nyagent.application.model.OllamaMessageModel

/** Callbacks the harness loop fires so the chat UI can render progress. */
interface IHarnessListener {
    fun onToolCall(name: String, arguments: String)
    fun onSpawnedAgent(task: String)
    fun onFinalResponse(response: String)
    fun onError(message: String)
}

/**
 * Runs the tool-enabled agent loop until the model finishes or is stopped.
 * Implemented by the infrastructure harness and called by chat services.
 */
interface IHarnessLoop {
    var listener: IHarnessListener?

    /** Runs the loop for [history] and returns the final assistant response. */
    fun run(history: List<OllamaMessageModel>): String

    /** Requests the current loop to stop after the in-flight tool call returns. */
    fun stop()
}
