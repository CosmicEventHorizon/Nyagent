package com.pirouette.nyagent.application.model

import java.io.Serializable

/** A single message rendered in the chat, tagged with its author. */
data class ChatMessageModel(
    val author: MessageAuthorModel,
    val content: String
) : Serializable
