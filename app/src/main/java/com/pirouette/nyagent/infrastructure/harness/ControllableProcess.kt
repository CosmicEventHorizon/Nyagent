package com.pirouette.nyagent.infrastructure.harness

import java.io.BufferedReader
import java.io.InputStreamReader
import java.nio.charset.StandardCharsets

/**
 * A running OS process with incremental output capture and the ability to kill
 * it. Created by [CodeExecutor.startBackground]; callers observe [output] and
 * poll [isAlive] so they can impose their own deadline while the process keeps
 * running in the background.
 */
class ControllableProcess(
    private val process: Process,
    private val description: String
) {

    private val stateLock = Object()
    private val buffer = StringBuilder()

    private val readerThread: Thread = Thread {
        try {
            val reader = InputStreamReader(process.inputStream, StandardCharsets.UTF_8)
            val buffered = BufferedReader(reader)
            var line = buffered.readLine()
            while (line != null) {
                synchronized (stateLock) {
                    buffer.append(line).append("\n")
                }
                line = buffered.readLine()
            }
        } catch (e: Exception) {
            // Pipe closed when the process is killed; ignore.
        }
    }

    init {
        readerThread.isDaemon = true
        readerThread.start()
    }

    /** Accumulated combined stdout and stderr text produced so far. */
    val output: String
        get() = synchronized (stateLock) { buffer.toString() }

    /** True while the OS process is still running. */
    val isAlive: Boolean get() = process.isAlive

    /** Blocks until the process exits and returns its exit code. */
    fun awaitExit(): Int {
        val code = process.waitFor()
        awaitReader()
        return code
    }

    /** Kills the process and returns once it has terminated. */
    fun kill(): Int {
        process.destroyForcibly()
        val code = process.waitFor()
        awaitReader()
        return code
    }

    private fun awaitReader() {
        if (readerThread.isAlive && Thread.currentThread() !== readerThread) {
            readerThread.join(2000)
        }
    }

    /** A short human description of what this process was running. */
    override fun toString(): String = description
}
