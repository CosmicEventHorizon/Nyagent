package com.pirouette.nyagent.application.interfaces.harness

import com.pirouette.nyagent.application.model.ToolModel

/** Runs tools by name on behalf of an LLM and returns structured results. */
interface IToolHarness {
    val tools: List<ToolModel>

    /**
     * Launches [tool] with [arguments] in the background and waits up to the
     * 30-second deadline. Returns the finished result, or a pending/timed-out
     * result while the process keeps running in the background.
     */
    fun startTool(tool: String, arguments: Map<String, Any?>): ToolResultModel

    /**
     * Restarts the 30-second deadline on the currently running tool and blocks
     * until it finishes or times out again. If no tool is running it returns a
     * "nothing to wait on" result.
     */
    fun waitOnCurrent(): ToolResultModel

    /** Kills the currently running tool and the processes it started. */
    fun killCurrent(): ToolResultModel

    /** The current accumulated output of the running tool. */
    fun currentOutput(): String

    /** True while a tool is running (or pending) in the background. */
    val hasRunningTool: Boolean
}
