package com.pirouette.nyagent.application.interfaces.harness

/**
 * Structured result of executing a tool: combined stdout/stderr, exit code and
 * status. [pending] is true while the tool still runs in the background after a
 * timeout; [timedOut] is true when the result was produced by an elapsed
 * 30-second deadline rather than normal process completion.
 */
data class ToolResultModel(
    val output: String,
    val exitCode: Int,
    val truncated: Boolean = false,
    val pending: Boolean = false,
    val timedOut: Boolean = false
) {
    val isSuccess: Boolean get() = exitCode == 0
}
