package com.pirouette.nyagent.application.constants

import com.pirouette.nyagent.application.model.ToolFunctionModel
import com.pirouette.nyagent.application.model.ToolModel
import java.util.ArrayList
import java.util.LinkedHashMap

/**
 * The tools the harness exposes to any connected LLM. Each tool is an
 * OpenAI-style function whose parameters follow JSON Schema: properties are
 * plain type/description maps and the required names live at the object level
 * as a list of strings. This shape is accepted by both Ollama and OpenRouter.
 */
object ToolRegistry {

    val tools: List<ToolModel> = listOf(
        ToolModel(function = ToolFunctionModel(
            name = "shell",
            description = "Run a shell command in the Alpine environment and capture its combined stdout/stderr.",
            parameters = objectSchema(mapOf("command" to stringParam("The shell command to run.")))
        )),
        ToolModel(function = ToolFunctionModel(
            name = "ls",
            description = "List the files and directories at a path inside the Alpine environment.",
            parameters = objectSchema(mapOf("path" to stringParam("Absolute path inside the Alpine rootfs.")))
        )),
        ToolModel(function = ToolFunctionModel(
            name = "curl",
            description = "Make an HTTP request from inside the Alpine environment using curl.",
            parameters = objectSchema(
                mapOf(
                    "url" to stringParam("Target HTTP(S) URL."),
                    "method" to stringParam("HTTP method (default GET)."),
                    "data" to stringParam("Optional request body."),
                    "headers" to stringParam("Optional headers as JSON or newline-separated key: value.")
                ),
                required = listOf("url")
            )
        )),
        ToolModel(function = ToolFunctionModel(
            name = "read_file",
            description = "Read UTF-8 text content of a file inside the Alpine environment.",
            parameters = objectSchema(mapOf("path" to stringParam("Absolute path of the file to read.")))
        )),
        ToolModel(function = ToolFunctionModel(
            name = "write_file",
            description = "Write UTF-8 text content to a file inside the Alpine environment.",
            parameters = objectSchema(
                mapOf(
                    "path" to stringParam("Absolute path of the file to write."),
                    "content" to stringParam("Full text content to write.")
                )
            )
        )),
        ToolModel(function = ToolFunctionModel(
            name = "spawn_agent",
            description = "Start a child agent with its own independent message history and its task, returning the child's final response.",
            parameters = objectSchema(mapOf("task" to stringParam("The task for the child agent.")))
        )),
        ToolModel(function = ToolFunctionModel(
            name = "tool_wait",
            description = "Keep waiting on the single tool currently running in the background and restart its 30-second deadline. Use it when a tool reported that it is still running (e.g. a slow download). Returns the tool's final output once it finishes, or another timeout notice if it still needs more time.",
            parameters = objectSchema(mapOf())
        )),
        ToolModel(function = ToolFunctionModel(
            name = "tool_kill",
            description = "Kill the tool currently running in the background and the processes it started. If no tool is running it returns 'nothing to kill'.",
            parameters = objectSchema(mapOf())
        ))
    )

    private fun stringParam(description: String): Map<String, Any?> {
        val spec = LinkedHashMap<String, Any?>()
        spec["type"] = "string"
        spec["description"] = description
        return spec
    }

    /**
     * Builds an object schema. Every property is optional unless listed in
     * [required]. The required list is kept at the object level as a JSON
     * Schema array of strings, which both Ollama and OpenRouter accept.
     */
    private fun objectSchema(
        properties: Map<String, Map<String, Any?>>,
        required: List<String>? = null
    ): Map<String, Any?> {
        val spec = LinkedHashMap<String, Any?>()
        spec["type"] = "object"
        spec["properties"] = properties
        val requiredNames = required ?: properties.keys.toList()
        if (requiredNames.isNotEmpty()) {
            val requiredList = ArrayList<String>()
            requiredList.addAll(requiredNames)
            spec["required"] = requiredList
        }
        return spec
    }
}
