package com.pirouette.nyagent.infrastructure.harness

import java.io.File

/**
 * Result of executing a command in the Alpine environment. [exitCode] is the
 * process's exit code (0 = success) and [output] is the captured combined output.
 */
data class CommandResult(
    val output: String,
    val exitCode: Int
) {
    val isSuccess: Boolean get() = exitCode == 0
}

/**
 * Executes shell/guest commands inside the app's Alpine Linux environment by
 * spawning proot per invocation. The guest rootfs persists on disk, so each call
 * boots the same minimal environment, runs the command, and returns output.
 *
 * [execute]/[executeGuest] block until the process finishes and are used for
 * install/validation. [startBackground] launches a [ControllableProcess] whose
 * output is captured incrementally, so the harness can impose its own deadline,
 * keep the process alive in the background, and kill it later (tool_kill).
 *
 * PRoot is bundled as a native library (libproot.so) and runs directly from the
 * package's native library directory to avoid writable-storage executability
 * restrictions. The companion loader is exported through PROOT_LOADER.
 */
class CodeExecutor(
    private val rootfsDir: File,
    private val prootExecutable: File,
    private val prootLoader: File,
    private val cacheDir: File
) {

    /** Runs [command] via the guest's /bin/sh and blocks until it finishes. */
    fun execute(command: String, extraEnv: Map<String, String> = mapOf()): CommandResult =
        run(prootArgs() + listOf("/bin/sh", "-c", command), extraEnv)

    /** Runs a guest program directly (no shell trampoline) and blocks for output. */
    fun executeGuest(arguments: List<String>, extraEnv: Map<String, String> = mapOf()): CommandResult =
        run(prootArgs() + arguments, extraEnv)

    /**
     * Launches [command] in the background and returns a live handle. The process
     * keeps running after this returns; callers poll [ControllableProcess.isAlive]
     * and can call [ControllableProcess.kill].
     */
    fun startBackground(command: String): ControllableProcess =
        startControllable(prootArgs() + listOf("/bin/sh", "-c", command))

    /** Base PRoot argv: enter the rootfs and expose host /dev, /proc and /sys. */
    private fun prootArgs(): List<String> =
        listOf(
            prootExecutable.absolutePath,
            "-w", "/root",
            "-r", rootfsDir.absolutePath,
            "-0",
            "-b", "/dev",
            "-b", "/proc",
            "-b", "/sys"
        )

    private fun startControllable(arguments: List<String>, extraEnv: Map<String, String> = mapOf()): ControllableProcess {
        cacheDir.mkdirs()
        val processBuilder = ProcessBuilder(arguments)
        processBuilder.directory(prootExecutable.parentFile)
        processBuilder.redirectErrorStream(true)
        val environment = processBuilder.environment()
        environment.put("PROOT_LOADER", prootLoader.absolutePath)
        environment.put("PROOT_TMP_DIR", cacheDir.absolutePath)
        environment.put("PATH", "/bin:/usr/bin:/sbin:/usr/sbin")
        for (entry in extraEnv) {
            environment.put(entry.key, entry.value)
        }
        val process = processBuilder.start()
        return ControllableProcess(process, arguments.joinToString(" "))
    }

    private fun run(arguments: List<String>, extraEnv: Map<String, String>): CommandResult {
        val controllable = try {
            startControllable(arguments, extraEnv)
        } catch (e: Exception) {
            return CommandResult("Error starting proot: ${e.message}", 1)
        }
        val exitCode = controllable.awaitExit()
        return CommandResult(controllable.output, exitCode)
    }
}
