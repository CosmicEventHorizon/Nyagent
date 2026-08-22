package com.pirouette.nyagent.application.model

/** Color-schemed line shown in the read-only Linux install status view. */
data class LinuxLogEntryModel(
    val message: String,
    val isError: Boolean = false,
    val isSuccess: Boolean = false
)
