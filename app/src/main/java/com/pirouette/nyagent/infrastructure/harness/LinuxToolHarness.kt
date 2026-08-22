package com.pirouette.nyagent.infrastructure.harness

import com.pirouette.nyagent.application.constants.ToolRegistry
import com.pirouette.nyagent.application.interfaces.harness.IToolHarness
import com.pirouette.nyagent.application.interfaces.harness.ToolResultModel
import com.pirouette.nyagent.application.model.ToolModel
import com.pirouette.nyagent.infrastructure.linux.LinuxEnvironmentService
import java.util.ArrayList

/**
 * Executes the harness tools inside the app's Alpine environment. Each tool boots
 * PRoot via [CodeExecutor]. A tool normally finishes and returns its combined
 * stdout/stderr plus the exit code, but if it outlives the 30-second deadline the
 * process is kept alive in the background and later calls can wait on it or kill
 * it. Only one tool runs at a time.
 *
 * Shell and curl are launched in the background and polled until done or timeout;
 * [waitOnCurrent] blocks again on the same process.
 */
class LinuxToolHarness(
    private val environment: LinuxEnvironmentService,
    private val codeExecutor: CodeExecutor,
    private val maxOutputChars: Int = 100_000,
    private val maxCommandLength: Int = 8_000
) : IToolHarness {

    private companion object {
        const val DEADLINE_MILLIS: Long = 30_000L
    }

    private val lock = Object()
    private var currentProcess: ControllableProcess? = null
    private var currentDescription = ""

    override val tools: List<ToolModel>
        get() = ToolRegistry.tools

    override val hasRunningTool: Boolean
        get() = synchronized (lock) { currentProcess?.isAlive == true }

    override fun startTool(tool: String, arguments: Map<String, Any?>): ToolResultModel {
        synchronized (lock) {
            if (currentProcess?.isAlive == true) {
                return blockedResult()
            }
        }
        if (!environment.isInstalled) {
            return ToolResultModel("Linux environment is not installed", 3)
        }
        val command = commandFor(tool, arguments)
        if (command == null) {
            return ToolResultModel("Unknown tool: " + tool, 2)
        }
        if (command.length > maxCommandLength) {
            return ToolResultModel("Command too long (" + command.length + " chars)", 2)
        }

        val process = try {
            codeExecutor.startBackground(command)
        } catch (e: Exception) {
            return ToolResultModel("Error starting tool: ${e.message}", 1)
        }
        synchronized (lock) {
            currentProcess = process
            currentDescription = tool
        }
        return awaitDeadline(process)
    }

    override fun waitOnCurrent(): ToolResultModel {
        val process = currentProcess
        if (process == null || !process.isAlive) {
            return ToolResultModel("nothing to wait on", 0)
        }
        return awaitDeadline(process)
    }

    override fun killCurrent(): ToolResultModel {
        val process = currentProcess
        if (process == null || !process.isAlive) {
            return ToolResultModel("nothing to kill", 0)
        }
        synchronized (lock) {
            if (currentProcess == process) {
                currentProcess = null
                currentDescription = ""
            }
        }
        val code = process.kill()
        return ToolResultModel("killed tool (exit " + code + ")", 1)
    }

    override fun currentOutput(): String {
        val process = currentProcess
        return process?.output ?: ""
    }

    /** Blocks until [process] finishes or the 30-second deadline elapses. */
    private fun awaitDeadline(process: ControllableProcess): ToolResultModel {
        val launched = System.currentTimeMillis()
        while (process.isAlive) {
            if (System.currentTimeMillis() - launched >= DEADLINE_MILLIS) {
                return timedOutResult(process.output)
            }
            try {
                Thread.sleep(250)
            } catch (e: InterruptedException) {
                Thread.currentThread().interrupt()
                return timedOutResult(process.output)
            }
        }
        clearIfCurrent(process)
        val exit = process.awaitExit()
        return doneResult(process.output, exit)
    }

    private fun doneResult(output: String, exitCode: Int): ToolResultModel =
        ToolResultModel(truncate(output, maxOutputChars), exitCode)

    private fun timedOutResult(output: String): ToolResultModel =
        ToolResultModel(
            truncate(output, maxOutputChars) + "\n[30 seconds passed: tool is still running. Use tool_wait to keep waiting or tool_kill to cancel.]",
            1,
            pending = true,
            timedOut = true
        )

    private fun blockedResult(): ToolResultModel =
        ToolResultModel(
            "A tool is still running in the background. Use tool_wait to keep waiting or tool_kill to cancel.",
            1,
            pending = true
        )

    /** Clears [process] from the slot only if it is still the active one. */
    private fun clearIfCurrent(process: ControllableProcess) {
        synchronized (lock) {
            if (currentProcess === process) {
                currentProcess = null
                currentDescription = ""
            }
        }
    }

    /** Builds the shell command for [tool]; returns null for unknown tools. */
    private fun commandFor(tool: String, arguments: Map<String, Any?>): String? =
        when(tool) {
            "shell" -> stringArg(arguments, "command")
            "ls" -> "ls \"" + escaped(stringArg(arguments, "path", "/")) + "\""
            "curl" -> buildCurl(arguments)
            "read_file" -> "cat \"" + escaped(stringArg(arguments, "path")) + "\""
            "write_file" -> buildWrite(arguments)
            "spawn_agent" -> "echo spawn-agent"
            else -> null
        }

    private fun buildCurl(arguments: Map<String, Any?>): String {
        val url = stringArg(arguments, "url")
        val method = stringArg(arguments, "method", "GET")
        val data = stringArg(arguments, "data", null)
        val headers = stringArg(arguments, "headers", null)
        val parts = ArrayList<String>()
        parts.add("curl")
        if (hasArg(arguments, "headers")) parts.add("-H \"" + escaped(headers) + "\"")
        if (hasArg(arguments, "data")) parts.add("--data \"" + escaped(data) + "\"")
        parts.add(method.lowercase())
        parts.add("\"" + escaped(url) + "\"")
        return parts.joinToString(" ")
    }

    private fun buildWrite(arguments: Map<String, Any?>): String {
        val path = stringArg(arguments, "path")
        val content = stringArg(arguments, "content")
        return "mkdir -p \"" + dirname(path) + "\" && cat > '" + path + "' <<'CHIBIEOF'\n" + content + "\nCHIBIEOF"
    }

    private fun dirname(path: String): String {
        val idx = path.lastIndexOf('/')
        return if (idx < 0) "." else path.substring(0, idx)
    }

    private fun escaped(value: String): String = value.replace("\"", "\\\"")
    private fun truncate(value: String, max: Int): String =
        if (value.length <= max) value else value.substring(0, max) + "\n... [truncated]"

    private fun stringArg(arguments: Map<String, Any?>, key: String, default: String? = null): String =
        arguments[key]?.toString() ?: default ?: ""

    /** True when [key] was supplied in [arguments]. */
    private fun hasArg(arguments: Map<String, Any?>, key: String): Boolean =
        arguments.containsKey(key) && arguments[key] != null
}
