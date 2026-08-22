package com.pirouette.nyagent.application.service

import android.os.Handler
import android.os.Looper
import com.pirouette.nyagent.application.interfaces.harness.ICompletionSender
import com.pirouette.nyagent.application.model.OllamaMessageModel
import com.pirouette.nyagent.application.prompts.COMPACTION_SYSTEM_PROMPT

/**
 * Produces compacted summaries of a conversation using whichever chat provider
 * is currently selected, plus a lightweight token estimator. Compaction never
 * involves tools; it only asks the model to compress the supplied history into
 * a continuing summary.
 */
class CompactionService(
    private val completion: ICompletionSender,
    private val mainHandler: Handler = Handler(Looper.getMainLooper())
) {

    /**
     * Compacts [history] into a single summary message. Calls the selected
     * provider without tools and posts the summary via [onResult] on the main
     * thread. On any failure [onError] is called with a message.
     */
    fun compact(
        history: List<OllamaMessageModel>,
        onResult: (String) -> Unit,
        onError: (String) -> Unit
    ) {
        val messages = ArrayList<OllamaMessageModel>()
        messages.add(OllamaMessageModel("system", COMPACTION_SYSTEM_PROMPT))
        messages.add(OllamaMessageModel("user", serializeHistory(history)))
        Thread {
            try {
                val reply = completion.complete(messages, emptyList())
                val summary = firstAssistantText(reply)
                mainHandler.post { onResult(summary) }
            } catch (error: Exception) {
                mainHandler.post { onError(error.message ?: error.toString()) }
            }
        }.start()
    }

    /** Returns the first non-blank assistant text in [reply], or the default notice. */
    private fun firstAssistantText(reply: List<OllamaMessageModel>): String {
        for (message in reply) {
            if (message.role == "assistant" && message.content.isNotBlank()) {
                return message.content
            }
        }
        return "Compaction failed: the model returned no summary."
    }

    /** Builds a compact human-readable rendering of the conversation for the LLM. */
    private fun serializeHistory(history: List<OllamaMessageModel>): String {
        val sb = StringBuilder()
        for (message in history) {
            val role = if (message.role.isEmpty()) "" else
                message.role.substring(0, 1).uppercase() + message.role.substring(1)
            sb.append(role).append(": ").append(message.content).append("\n")
        }
        return sb.toString()
    }
}
