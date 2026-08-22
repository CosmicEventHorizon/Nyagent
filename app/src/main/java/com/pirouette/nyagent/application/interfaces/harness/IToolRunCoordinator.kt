package com.pirouette.nyagent.application.interfaces.harness

/**
 * Owns the single background tool execution slot shared by the agent loop and
 * child agents. Because only one tool runs at a time, the coordinator controls
 * the live process, its 30-second deadline, and the wait/kill operations the LLM
 * triggers through the tool_wait and tool_kill tools.
 */
interface IToolRunCoordinator {

    /**
     * Starts [tool] with [arguments] and waits up to the 30-second deadline. If
     * no other tool is pending it returns the finished result, otherwise a
     * pending/timed-out result while the process keeps running in the background.
     */
    fun start(tool: String, arguments: Map<String, Any?>): ToolResultModel

    /**
     * Restarts the 30-second deadline on the running tool and blocks until it
     * finishes (or times out again). Returns the final result or another
     * pending/timed-out result.
     */
    fun waitMore(): ToolResultModel

    /** Kills the running tool and any processes it started, then clears the slot. */
    fun kill(): ToolResultModel

    /** True while a tool is running or pending in the background. */
    val isToolRunning: Boolean

    /** The current accumulated output of the running tool. */
    fun currentOutput(): String
}
