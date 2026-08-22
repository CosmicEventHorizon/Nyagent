package com.pirouette.nyagent.application.model

import java.io.Serializable

/**
 * A conversation saved to long-term storage.
 * Acts as both the application model and the serialized on-disk shape.
 */
data class SavedStoryModel(
    val name: String,
    /** Messages as rendered on screen. */
    val displayMessages: List<ChatMessageModel>,
    /** The raw user/assistant log sent to the API. */
    val conversationLog: List<OllamaMessageModel>
) : Serializable
