package com.pirouette.nyagent.application.interfaces.harness

import com.pirouette.nyagent.application.model.OllamaMessageModel
import com.pirouette.nyagent.application.model.ToolModel

/** Spawns an independent child agent loop for the harness. */
interface IChildAgent {

    /** Runs a fresh agent loop with [history] and returns the final text response. */
    fun run(history: List<OllamaMessageModel>, tools: List<ToolModel>): String
}
