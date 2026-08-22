package com.pirouette.nyagent.persistence.entity

/** A system prompt persisted for reuse. */
data class SavedPromptEntity(
    val name: String,
    val prompt: String
)
