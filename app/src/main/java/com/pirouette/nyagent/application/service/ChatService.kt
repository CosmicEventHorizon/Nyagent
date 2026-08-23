package com.pirouette.nyagent.application.service

import android.os.Handler
import android.os.Looper
import com.pirouette.nyagent.application.interfaces.IEnvironmentService
import com.pirouette.nyagent.application.interfaces.ISettingsRepository
import com.pirouette.nyagent.application.interfaces.harness.IHarnessListener
import com.pirouette.nyagent.application.interfaces.harness.IHarnessLoop
import com.pirouette.nyagent.application.model.ChatMessageModel
import com.pirouette.nyagent.application.model.MessageAuthorModel
import com.pirouette.nyagent.application.model.OllamaMessageModel
import com.pirouette.nyagent.application.model.SavedStoryModel
import java.util.concurrent.atomic.AtomicBoolean
import kotlin.math.min

/** Orchestrates a chat session: message history, harness replies, and story snapshots. */
class ChatService(
    private val settings: ISettingsRepository,
    private val environment: IEnvironmentService,
    private val harness: IHarnessLoop,
    private val compaction: CompactionService
) {

    interface Listener {
        fun onMessagesChanged()
        fun onAssistantDelta(content: String)
        fun onError(message: String)
        fun onContextChanged(percentUsed: Int)
        /** Fired once per new story-relevant message so the host can persist it. */
        fun onStoryChanged()
    }

    private val _messages = ArrayList<ChatMessageModel>()
    private val _conversationLog = ArrayList<OllamaMessageModel>()
    private val _running = AtomicBoolean(false)
    private val _compacting = AtomicBoolean(false)
    private val mainHandler = Handler(Looper.getMainLooper())

    val messages: List<ChatMessageModel> get() = _messages
    val conversationLog: List<OllamaMessageModel> get() = _conversationLog
    val isRunning: Boolean get() = _running.get()
    val isCompacting: Boolean get() = _compacting.get()

    /** Percentage of the configured max context window currently consumed. */
    val contextPercentUsed: Int
        get() {
            if (settings.maxContextTokens <= 0) return 0
            val used = estimateTokens()
            return kotlin.math.min(100, used * 100 / settings.maxContextTokens)
        }

    var listener: Listener? = null

    private fun notifyChanged() = mainHandler.post { listener?.onMessagesChanged() }
    private fun notifyStory() = mainHandler.post { listener?.onStoryChanged() }
    private fun notifyDelta(content: String) = mainHandler.post { listener?.onAssistantDelta(content) }
    private fun notifyError(message: String) = mainHandler.post { listener?.onError(message) }
    private fun notifyContext() = mainHandler.post { listener?.onContextChanged(contextPercentUsed) }

    private fun estimateTokens(): Int {
        var chars = 0
        for (message in _conversationLog) {
            chars += message.content.length
            message.toolCalls?.forEach { call ->
                chars += call.function.name.length + call.function.arguments.length
            }
        }
        return chars / 4
    }

    /** Builds a story snapshot of the current conversation. */
    fun createStorySnapshot(name: String): SavedStoryModel =
        SavedStoryModel(
            name = name,
            displayMessages = _messages.toList(),
            conversationLog = _conversationLog.toList()
        )

    /** Requests the in-flight harness loop (if any) to stop. */
    fun stop() {
        if (_running.get()) {
            harness.stop()
        }
    }

    /** Sends the user's [content] and starts a (possibly compacted) harness reply. */
    fun sendUserMessage(content: String) {
        if (_running.get() || _compacting.get()) {
            return
        }
        removeTrailingError()
        _messages.add(ChatMessageModel(MessageAuthorModel.USER, content))
        _conversationLog.add(OllamaMessageModel("user", content))
        notifyStory()
        notifyContext()
        notifyChanged()
        if (shouldCompact()) {
            runCompaction(content)
        } else {
            streamReply()
        }
    }

    /** True when the estimated token usage exceeds the compact threshold. */
    private fun shouldCompact(): Boolean {
        if (settings.compactThresholdTokens <= 0) return false
        return estimateTokens() >= settings.compactThresholdTokens
    }

    /**
     * Compacts the conversation: disables input, surfaces a status message,
     * asks the selected provider to summarise the history, then continues with
     * the compacted summary plus the user message that triggered it.
     */
    private fun runCompaction(triggerContent: String) {
        _compacting.set(true)
        _messages.add(ChatMessageModel(MessageAuthorModel.SYSTEM, "Compaction started"))
        _messages.add(ChatMessageModel(MessageAuthorModel.SYSTEM, "Compacting…"))
        notifyChanged()
        notifyContext()

        compaction.compact(
            history = _conversationLog.toList(),
            onResult = { summary ->
                _conversationLog.clear()
                _conversationLog.add(OllamaMessageModel("system", summary))
                _conversationLog.add(OllamaMessageModel("user", triggerContent))
                _messages.clear()
                _messages.add(ChatMessageModel(MessageAuthorModel.SYSTEM, summary))
                _messages.add(ChatMessageModel(MessageAuthorModel.USER, triggerContent))
                _compacting.set(false)
                notifyContext()
                notifyChanged()
                streamReply()
            },
            onError = { message ->
                _compacting.set(false)
                reportError("Compaction failed: $message")
                notifyContext()
            }
        )
    }

    /**
     * Removes a trailing error message and the failed exchange it refers to:
     * the system message, the user message below it, and the matching log entry.
     */
    fun removeTrailingError() {
        if (_messages.lastOrNull()?.author != MessageAuthorModel.SYSTEM) {
            return
        }
        _messages.removeAt(_messages.lastIndex) // system message
        if (_messages.isNotEmpty() && _messages.last().author == MessageAuthorModel.USER) {
            _messages.removeAt(_messages.lastIndex) // user message
        }
        _conversationLog.removeLastOrNull()
        notifyChanged()
    }

    /** Deletes the last assistant + user message pair, or the trailing error if present. */
    fun deleteLastMessage() {
        if (_running.get()) {
            return
        }
        if (_messages.lastOrNull()?.author == MessageAuthorModel.SYSTEM) {
            removeTrailingError()
            return
        }
        if (_messages.isNotEmpty()) {
            _messages.removeAt(_messages.lastIndex) // assistant message
            if (_messages.isNotEmpty()) {
                _messages.removeAt(_messages.lastIndex) // user message
            }
            _conversationLog.removeLastOrNull()
            _conversationLog.removeLastOrNull()
            notifyChanged()
        }
    }

    /**
     * Truncates the conversation at the [index]-th visible message, edits that
     * message's content, and restarts the reply from there. The visible message
     * at [index] must be a user message; the log is rebuilt to keep every entry
     * up to (and including) that user message, with the new content substituted.
     */
    fun editAndRestart(index: Int, newContent: String) {
        if (_running.get()) {
            return
        }
        val target = _messages[index]
        if (target.author != MessageAuthorModel.USER) return

        _messages.subList(index + 1, _messages.size).clear()
        _messages[index] = ChatMessageModel(MessageAuthorModel.USER, newContent)

        var userId = -1
        for (i in _conversationLog.indices) {
            if (_conversationLog[i].role == "user") userId = i
        }
        if (userId >= 0) {
            _conversationLog.subList(userId, _conversationLog.size).clear()
        }
        _conversationLog.add(OllamaMessageModel("user", newContent))
        notifyChanged()
        notifyContext()
        streamReply()
    }

    /** Clears the whole conversation. */
    fun clearConversation() {
        if (_running.get()) {
            return
        }
        _messages.clear()
        _conversationLog.clear()
        notifyChanged()
        notifyContext()
    }

    /** Replaces the current conversation with a loaded [story]. */
    fun loadStory(story: SavedStoryModel) {
        if (_running.get()) {
            return
        }
        _messages.clear()
        _messages.addAll(story.displayMessages)
        _conversationLog.clear()
        _conversationLog.addAll(story.conversationLog)
        notifyChanged()
        notifyContext()
    }

    /** Restores message and log state captured for process recreation. */
    fun restoreState(
        messages: List<ChatMessageModel>,
        conversationLog: List<OllamaMessageModel>
    ) {
        _messages.clear()
        _messages.addAll(messages)
        _conversationLog.clear()
        _conversationLog.addAll(conversationLog)
        notifyChanged()
        notifyContext()
    }

    private fun streamReply() {
        if (!environment.isInstalled) {
            reportError("Linux environment is not installed. Install it from Settings first.")
            return
        }
        runHarness()
    }

    /**
     * Runs the harness on a background thread. The harness prepends the system
     * prompt (plus tools), iterates tool calls, and reports each via the listener
     * bridge; the final response is appended as an assistant bubble. The provider
     * is selected by settings, so Ollama and OpenRouter behave identically.
     */
    private fun runHarness() {
        _running.set(true)
        harness.listener = harnessListener
        Thread {
            var finalText = ""
            try {
                finalText = harness.run(_conversationLog)
            } catch (error: Exception) {
                reportError("Failed to connect to API: " + (error.message ?: error.toString()))
            } finally {
                if (_messages.lastOrNull()?.author != MessageAuthorModel.ASSISTANT
                    && finalText.isNotBlank()) {
                    _messages.add(ChatMessageModel(MessageAuthorModel.ASSISTANT, finalText))
                    _conversationLog.add(OllamaMessageModel("assistant", finalText))
                    notifyStory()
                    notifyContext()
                    notifyChanged()
                    notifyDelta(finalText)
                }
                _running.set(false)
                notifyChanged()
            }
        }.start()
    }
    /** Bridges harness events into the chat UI and conversation log. */
    private val harnessListener: IHarnessListener = object : IHarnessListener {
        override fun onToolCall(name: String, arguments: String) {
            showToolCall(name, arguments)
        }

        override fun onSpawnedAgent(task: String) {
            showSpawnedAgent(task)
        }

        override fun onFinalResponse(response: String) {
            _messages.add(ChatMessageModel(MessageAuthorModel.ASSISTANT, response))
            _conversationLog.add(OllamaMessageModel("assistant", response))
            notifyStory()
            notifyContext()
            notifyChanged()
            notifyDelta(response)
        }

        override fun onError(message: String) {
            reportError(message)
        }
    }

    /**
     * Surfaces a tool call the assistant requested on screen (name + JSON args)
     * without adding it to the LLM conversation log.
     */
    private fun showToolCall(name: String, arguments: String) {
        _messages.add(ChatMessageModel(MessageAuthorModel.TOOL, "Tool: " + toollabel(name, arguments)))
        notifyStory()
        notifyChanged()
    }

    /** Surfaces a spawn_agent invocation on screen. */
    private fun showSpawnedAgent(task: String) {
        _messages.add(ChatMessageModel(MessageAuthorModel.TOOL, "Tool: spawn_agent($task)"))
        notifyStory()
        notifyChanged()
    }

    private fun toollabel(name: String, arguments: String): String = "$name($arguments)"

    private fun reportError(message: String) {
        _messages.add(ChatMessageModel(MessageAuthorModel.SYSTEM, message))
        notifyChanged()
        notifyError(message)
    }

}
