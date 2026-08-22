package com.pirouette.nyagent.infrastructure.harness

import com.pirouette.nyagent.application.interfaces.harness.IToolHarness
import com.pirouette.nyagent.application.interfaces.harness.IToolRunCoordinator
import com.pirouette.nyagent.application.interfaces.harness.ToolResultModel

/**
 * Enforces the single-running-tool rule shared across the agent loop and child
 * agents. Normal tool calls delegate to [IToolHarness.startTool], which blocks up
 * to the 30-second deadline and keeps a slow process alive in the background.
 * While a tool is running (or pending) every other non-control tool call is
 * rejected; tool_wait and tool_kill operate on the same live process.
 */
class ToolRunCoordinator(
    private val toolHarness: IToolHarness
) : IToolRunCoordinator {

    override val isToolRunning: Boolean
        get() = toolHarness.hasRunningTool

    override fun start(tool: String, arguments: Map<String, Any?>): ToolResultModel {
        if (toolHarness.hasRunningTool) {
            return blockedResult()
        }
        return try {
            toolHarness.startTool(tool, arguments)
        } catch (e: Exception) {
            ToolResultModel("Tool error: " + (e.message ?: e.toString()), 1)
        }
    }

    override fun waitMore(): ToolResultModel =
        try {
            toolHarness.waitOnCurrent()
        } catch (e: Exception) {
            ToolResultModel("Tool wait error: " + (e.message ?: e.toString()), 1)
        }

    override fun kill(): ToolResultModel =
        try {
            toolHarness.killCurrent()
        } catch (e: Exception) {
            ToolResultModel("Tool kill error: " + (e.message ?: e.toString()), 1)
        }

    override fun currentOutput(): String = toolHarness.currentOutput()

    private fun blockedResult(): ToolResultModel =
        ToolResultModel(
            "A tool is still running in the background. Use tool_wait to keep waiting or tool_kill to cancel.",
            1,
            pending = true
        )
}
