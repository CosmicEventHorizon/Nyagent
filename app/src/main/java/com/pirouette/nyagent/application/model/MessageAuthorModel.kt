package com.pirouette.nyagent.application.model

import java.io.Serializable

/** Who wrote a chat message shown on screen. */
enum class MessageAuthorModel {
    USER,
    ASSISTANT,
    TOOL,
    SYSTEM
}
