package com.pirouette.nyagent.application.service

import android.os.Handler
import android.os.Looper
import com.pirouette.nyagent.application.interfaces.harness.ICompletionSender
import com.pirouette.nyagent.application.model.MessageAuthorModel
import com.pirouette.nyagent.application.model.OllamaMessageModel
import com.pirouette.nyagent.application.model.SavedStoryModel
import com.pirouette.nyagent.application.prompts.CONVERSATION_TITLE_SYSTEM_PROMPT
import com.pirouette.nyagent.persistence.repository.ConversationTitleRepository
import java.util.concurrent.ConcurrentHashMap

/** Generates and persists short, provider-selected titles without invoking tools. */
class ConversationTitleService(
    private val completion: ICompletionSender,
    private val repository: ConversationTitleRepository,
    private val mainHandler: Handler = Handler(Looper.getMainLooper())
) {

    private val pendingIds = ConcurrentHashMap.newKeySet<String>()

    /** Stored LLM title, or a readable first-message preview while one is unavailable. */
    fun displayTitle(story: SavedStoryModel): String =
        repository.get(story.name) ?: fallbackTitle(
            story.displayMessages.firstOrNull { it.author == MessageAuthorModel.USER }?.content.orEmpty()
        )

    /** Requests one title for a newly-created conversation. Duplicate requests are ignored. */
    fun generate(conversationId: String, firstUserMessage: String, onReady: () -> Unit) {
        if (repository.get(conversationId) != null || !pendingIds.add(conversationId)) {
            return
        }
        Thread {
            val title = try {
                val reply = completion.complete(
                    listOf(
                        OllamaMessageModel("system", CONVERSATION_TITLE_SYSTEM_PROMPT.trim()),
                        OllamaMessageModel("user", firstUserMessage)
                    ),
                    emptyList()
                )
                normalizeTitle(
                    reply.firstOrNull { it.role == "assistant" && it.content.isNotBlank() }?.content.orEmpty()
                ).ifBlank { fallbackTitle(firstUserMessage) }
            } catch (_: Exception) {
                fallbackTitle(firstUserMessage)
            }
            // delete() removes the pending marker. In that case discard a
            // late result instead of recreating title data for a deleted chat.
            if (pendingIds.remove(conversationId)) {
                repository.save(conversationId, title)
                mainHandler.post(onReady)
            }
        }.start()
    }

    fun delete(conversationId: String) {
        pendingIds.remove(conversationId)
        repository.delete(conversationId)
    }

    private fun normalizeTitle(raw: String): String {
        val singleLine = raw.lineSequence().firstOrNull { it.isNotBlank() }.orEmpty().trim()
        return singleLine
            .removePrefix("Title:")
            .removePrefix("title:")
            .trim()
            .trim('"', '\'', ' ', '.', '!', '?', ':', ';', '*', '#', '`')
            .take(MAX_TITLE_LENGTH)
            .trimEnd()
    }

    private fun fallbackTitle(message: String): String {
        val compact = message.replace(Regex("\\s+"), " ").trim()
        if (compact.isBlank()) return DEFAULT_TITLE
        return if (compact.length <= MAX_TITLE_LENGTH) compact
        else compact.take(MAX_TITLE_LENGTH - 1).trimEnd() + "…"
    }

    private companion object {
        const val MAX_TITLE_LENGTH = 56
        const val DEFAULT_TITLE = "New conversation"
    }
}
