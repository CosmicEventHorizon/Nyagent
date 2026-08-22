package com.pirouette.nyagent.application.interfaces

import com.pirouette.nyagent.application.model.LinuxLogEntryModel

/** Contract for the app's private Linux environment used for tool calling. */
interface IEnvironmentService {
    val isInstalled: Boolean

    /** Immutable install status log kept since the last install attempt. */
    val installLog: List<LinuxLogEntryModel>

    /** Installs (or reinstalls) the bundled Linux environment. */
    fun install(): Boolean

    /** Runs a single [command] inside the Linux environment and returns its output. */
    fun execute(command: String): String
}
