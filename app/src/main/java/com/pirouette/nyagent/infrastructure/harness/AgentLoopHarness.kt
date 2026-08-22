package com.pirouette.nyagent.infrastructure.harness

import com.google.gson.Gson
import com.google.gson.JsonElement
import com.google.gson.JsonObject
import com.google.gson.JsonParser
import com.pirouette.nyagent.application.constants.ToolRegistry
import com.pirouette.nyagent.application.interfaces.harness.IHarnessListener
import com.pirouette.nyagent.application.interfaces.harness.IChildAgent
import com.pirouette.nyagent.application.interfaces.harness.IHarnessLoop
import com.pirouette.nyagent.application.interfaces.harness.ICompletionSender
import com.pirouette.nyagent.application.interfaces.harness.IToolHarness
import com.pirouette.nyagent.application.interfaces.harness.IToolRunCoordinator
import com.pirouette.nyagent.application.interfaces.harness.ToolResultModel
import com.pirouette.nyagent.application.model.OllamaMessageModel
import com.pirouette.nyagent.application.model.ToolModel
import com.pirouette.nyagent.application.prompts.HARNESS_CHILD_SYSTEM_PROMPT
import com.pirouette.nyagent.application.prompts.HARNESS_SYSTEM_PROMPT
import java.util.ArrayList
import java.util.LinkedHashMap
import java.util.concurrent.atomic.AtomicBoolean

/**
 * Drives the LLM agent loop: sends the conversation plus tool definitions to the
 * completion provider, observes any tool calls the model makes, executes them,
 * appends the results as tool messages, and repeats until the model returns a
 * normal final response.
 *
 * Tool execution is coordinated through a single [IToolRunCoordinator] so only one
 * tool runs at a time. Slow tools may be reported as pending/timed-out while their
 * process stays alive in the background; the model can call tool_wait to keep
 * waiting or tool_kill to cancel.
 *
 * The loop notifies a [Listener] whenever a tool call is requested or a final
 * response is reached so the UI can render it. If [stopRequested] becomes true,
 * the loop aborts and returns the most recent assistant text. Each network call is
 * bounded by the completion provider's 30-second timeout.
 */
class AgentLoopHarness(
    private val completion: ICompletionSender,
    private val toolHarness: IToolHarness,
    private val systemPrompt: String = HARNESS_SYSTEM_PROMPT,
    private val childSystemPrompt: String = HARNESS_CHILD_SYSTEM_PROMPT,
    private val gson: Gson = Gson(),
    private val maxIterations: Int = 16,
    private val runCoordinator: IToolRunCoordinator? = null
) : IHarnessLoop, IChildAgent {

    private val coordinator: IToolRunCoordinator = runCoordinator ?: ToolRunCoordinator(toolHarness)

    override var listener: IHarnessListener? = null
    private val stopFlag = AtomicBoolean(false)

    /** Requests the current run to stop; kills the running tool so the loop can exit. */
    override fun stop() {
        stopFlag.set(true)
        if (coordinator.isToolRunning) {
            coordinator.kill()
        }
    }

    override fun run(history: List<OllamaMessageModel>): String =
        runWithSystemPrompt(history, ToolRegistry.tools, systemPrompt)

    /** IChildAgent entry point; [tools] are the tool definitions to advertise. */
    override fun run(history: List<OllamaMessageModel>, tools: List<ToolModel>): String =
        runWithSystemPrompt(history, tools, systemPrompt)

    private fun runWithSystemPrompt(
        history: List<OllamaMessageModel>,
        tools: List<ToolModel>,
        prompt: String
    ): String {
        val conversation = ArrayList<OllamaMessageModel>()
        conversation.add(OllamaMessageModel("system", buildSystemPrompt(prompt, tools)))
        conversation.addAll(history)
        val toolSet = tools

        for (iteration in 0..maxIterations) {
            if (stopFlag.get()) {
                return snapshotFinalText(conversation)
            }
            val reply = completion.complete(conversation, toolSet)
            if (stopFlag.get()) {
                return snapshotFinalText(conversation)
            }
            val toolCalls = findToolCalls(reply)
            if (toolCalls.isEmpty()) {
                val finalText = lastText(reply)
                conversation.addAll(reply)
                if (finalText != null) {
                    listener?.onFinalResponse(finalText)
                    return finalText
                }
                val empty = "Agent returned no final response."
                listener?.onError(empty)
                return empty
            }

            conversation.addAll(reply)
            for (call in toolCalls) {
                if (stopFlag.get()) {
                    return snapshotFinalText(conversation)
                }
                val args = parseArgs(call.arguments)
                val result = if (call.name == "spawn_agent") {
                    val task = args["task"]?.toString() ?: ""
                    listener?.onSpawnedAgent(task)
                    runChildAgent(task, tools, childSystemPrompt)
                } else {
                    listener?.onToolCall(call.name, call.arguments)
                    executeCoordinatedTool(call.name, args)
                }
                conversation.add(
                    OllamaMessageModel(
                        role = "tool",
                        content = toolResultPayload(result),
                        toolCallId = call.id
                    )
                )
            }
            if (stopFlag.get()) {
                return snapshotFinalText(conversation)
            }
        }
        val exceeded = "Agent loop exceeded $maxIterations iterations."
        listener?.onError(exceeded)
        return exceeded
    }

    /** Routes a tool call through the coordinator: wait, kill, or run. */
    private fun executeCoordinatedTool(name: String, args: Map<String, Any?>): ToolResultModel =
        when(name) {
            "tool_kill" -> coordinator.kill()
            "tool_wait" -> coordinator.waitMore()
            else -> coordinator.start(name, args) // rejected as blocked when already running
        }

    /** Concatenates the base prompt with the advertised tool descriptions. */
    private fun buildSystemPrompt(base: String, tools: List<ToolModel>): String {
        if (tools.isEmpty()) return base
        val descriptions = tools.map { tool ->
            val fn = tool.function
            "${fn.name}: ${fn.description}"
        }
        return base + "\n\nAvailable tools:\n" + descriptions.joinToString("\n")
    }

    /** Runs an independent child loop with its own history and structured prompt. */
    private fun runChildAgent(task: String, tools: List<ToolModel>, prompt: String): ToolResultModel {
        val childHistory = ArrayList<OllamaMessageModel>()
        childHistory.add(OllamaMessageModel("user", task))
        val childLoop = AgentLoopHarness(
            completion = completion,
            toolHarness = toolHarness,
            systemPrompt = prompt,
            childSystemPrompt = childSystemPrompt,
            gson = gson,
            maxIterations = maxIterations,
            runCoordinator = coordinator
        )
        // Child agents run with their own listener chain (unset) so their tool
        // calls and messages stay out of the parent's chat window.
        val childReply = childLoop.runWithSystemPrompt(childHistory, tools, childSystemPrompt)
        return ToolResultModel(childReply, 0)
    }

    /** Serialises a tool result into a single-line JSON string for the tool message. */
    private fun toolResultPayload(result: ToolResultModel): String {
        val obj = JsonObject()
        obj.addProperty("output", result.output)
        obj.addProperty("exit_code", result.exitCode)
        obj.addProperty("truncated", result.truncated)
        obj.addProperty("is_success", result.isSuccess)
        obj.addProperty("pending", result.pending)
        obj.addProperty("timed_out", result.timedOut)
        return obj.toString()
    }

    /** Parses the assistant reply and returns any tool calls it requested. */
    private fun findToolCalls(reply: List<OllamaMessageModel>): List<ToolCall> {
        val calls = ArrayList<ToolCall>()
        for (message in reply) {
            if (message.role != "assistant") continue

            // Prefer the structured tool_calls field when present.
            if (message.toolCalls != null && message.toolCalls.isNotEmpty()) {
                for (tc in message.toolCalls) {
                    calls.add(
                        ToolCall(
                            tc.id,
                            tc.function.name,
                            tc.function.arguments
                        )
                    )
                }
                continue
            }

            // Fallback: the content may itself be a JSON tool_calls envelope.
            val parsed = try {
                JsonParser.parseString(message.content).asJsonObject
            } catch (e: Exception) {
                null
            }
            val toolCalls = parsed?.getAsJsonArray("tool_calls") ?: continue
            for (element in toolCalls) {
                if (!element.isJsonObject) continue
                val obj = element.asJsonObject
                val fn = if (obj.has("function")) obj.getAsJsonObject("function") else null
                if (fn == null) continue
                calls.add(
                    ToolCall(
                        obj.get("id")?.asString ?: "",
                        fn.get("name")?.asString ?: "",
                        fn.get("arguments")?.asString ?: "{}"
                    )
                )
            }
        }
        return calls
    }

    /** Returns the final non-blank assistant text message in [reply]. */
    private fun lastText(reply: List<OllamaMessageModel>): String? {
        var latest: String? = null
        for (message in reply) {
            if (message.role == "assistant" && message.content.isNotBlank()) {
                latest = message.content
            }
        }
        return latest
    }

    /** Retries the last assistant text from the whole conversation after a stop. */
    private fun snapshotFinalText(conversation: List<OllamaMessageModel>): String {
        var latest = "Agent stopped by user."
        for (message in conversation) {
            if (message.role == "assistant" && message.content.isNotBlank()) {
                latest = message.content
            }
        }
        return latest
    }

    /** Converts a JSON object string into a Kotlin map of primitive values. */
    private fun parseArgs(raw: String): Map<String, Any?> {
        return try {
            val obj = JsonParser.parseString(raw).asJsonObject
            val out = LinkedHashMap<String, Any?>()
            for ((key, value) in obj.entrySet()) {
                out[key] = jsonToValue(value)
            }
            out
        } catch (e: Exception) {
            mapOf()
        }
    }

    private fun jsonToValue(element: JsonElement): Any? {
        if (element.isJsonPrimitive) {
            val p = element.asJsonPrimitive
            return if (p.isBoolean) p.asBoolean else if (p.isNumber) p.asNumber else p.asString
        }
        if (element.isJsonArray) {
            val mapped = ArrayList<Any?>()
            for (item in element.asJsonArray) {
                mapped.add(jsonToValue(item))
            }
            return mapped
        }
        return element.toString()
    }

    private data class ToolCall(
        val id: String,
        val name: String,
        val arguments: String
    )
}
