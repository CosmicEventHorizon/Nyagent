package com.pirouette.nyagent.application.model

/** A saved system prompt, as represented in the application layer. */
data class SavedPromptModel(
    val name: String,
    val prompt: String
) {
    companion object {
        /** Builds a short, human-readable label for a prompt. */
        fun buildLabel(prompt: String): String {
            if (prompt.isBlank()) {
                return "System Prompt"
            }
            val singleLine = prompt.replace("\n", " ").trim()
            return if (singleLine.length > 24) {
                singleLine.take(24) + "..."
            } else {
                singleLine
            }
        }
    }
}
