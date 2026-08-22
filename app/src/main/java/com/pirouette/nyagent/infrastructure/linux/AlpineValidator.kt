package com.pirouette.nyagent.infrastructure.linux

import com.pirouette.nyagent.application.model.LinuxLogEntryModel
import java.io.File
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.Paths
import java.util.HashMap
import java.util.HashSet
import com.pirouette.nyagent.infrastructure.harness.CodeExecutor

/**
 * Runs a startup validation phase through PRoot before the normal Alpine shell is
 * launched. Every failing check reports FAIL (red) with the exact reason; healthy
 * checks are only summarised. If any check fails the caller aborts startup so a
 * broken rootfs is never used. The working PRoot/native loader setup is untouched.
 */
class AlpineValidator(
    private val rootfsDir: File,
    private val prootExecutable: File,
    private val prootLoader: File,
    private val cacheDir: File
) {

    private companion object {
        val REQUIRED_DIRS = listOf("/bin", "/usr/bin", "/sbin", "/usr/sbin", "/etc", "/tmp")
        val SAMPLE_APPLETS = listOf("ls", "cat", "mkdir", "chmod", "env", "sh")
        val APPLET_PATHS = listOf(
            "/bin/sh", "/bin/ls", "/bin/cat", "/bin/rm", "/bin/mkdir",
            "/bin/cp", "/bin/mv", "/bin/chmod", "/bin/echo", "/usr/bin/env"
        )
        val PROBE_VAR = "CHIBI_CHAT_VALIDATION"
        val PROBE_VALUE = "roundtrip-ok"
        val TMP_FILE = "/tmp/proot_probe"
        val PROBE_TEXT = "hello-proot"
    }

    /** One probe result: which check, which category, and why it failed. */
    private data class Probe(
        val name: String,
        val category: String,
        val passed: Boolean,
        val detail: String = ""
    )

    /** Outcome of the whole validation: log entries plus a specific failure label. */
    data class Outcome(
        val entries: List<LinuxLogEntryModel>,
        val allPassed: Boolean,
        val failureMessage: String?
    )

    /** Runs every check and returns coloured FAIL entries plus a specific summary. */
    fun validate(): Outcome {
        val executor = CodeExecutor(rootfsDir, prootExecutable, prootLoader, cacheDir)
        val probes = ArrayList<Probe>()

        // 1. PRoot can enter the Alpine rootfs.
        val busyboxList = executor.executeGuest(listOf("/bin/busybox", "--list"))
        probes.add(
            Probe(
                "PRoot can enter the Alpine rootfs",
                "PRoot",
                busyboxList.isSuccess,
                if (busyboxList.isSuccess) "" else slim(busyboxList.output)
            )
        )

        // 2. /bin/busybox exists and actually executes via a valid applet call.
        val busyboxFile = File(rootfsDir, "bin/busybox")
        val busyboxProbe = probeBusybox(executor, busyboxFile)
        probes.add(busyboxProbe)

        // 3. /bin/sh exists and executes a command.
        val shProbe = executor.execute("echo sh-ok")
        probes.add(
            Probe(
                "/bin/sh exists and executes a command",
                "shell execution",
                shProbe.isSuccess && shProbe.output.contains("sh-ok"),
                if (shProbe.isSuccess && shProbe.output.contains("sh-ok")) "" else slim(shProbe.output)
            )
        )

        // 4. Critical directories exist.
        for (dir in REQUIRED_DIRS) {
            val hostDir = File(rootfsDir, dir.removePrefix("/"))
            probes.add(
                Probe(
                    "$dir exists",
                    "rootfs extraction",
                    hostDir.isDirectory(),
                    if (hostDir.isDirectory()) "" else "directory missing on disk"
                )
            )
        }

        // 5. BusyBox applets are available.
        var appletsOk = busyboxList.isSuccess
        for (name in SAMPLE_APPLETS) {
            if (appletsOk && !busyboxList.output.contains(name)) {
                appletsOk = false
            }
        }
        probes.add(
            Probe(
                "BusyBox applets are available",
                "busybox",
                appletsOk,
                if (appletsOk) "" else "one or more expected applets missing from --list"
            )
        )

        // 6. BusyBox symlinks/links resolve for the important applet paths.
        val symlinkProbes = probeSymlinks()
        probes.addAll(symlinkProbes)

        // 7. PATH is set and valid.
        val pathProbe = executor.execute("echo PATH=\$PATH")
        val pathText = pathProbe.output
        val pathOk = pathProbe.isSuccess && pathText.contains("PATH=")
            && pathText.contains("/bin:") && pathText.contains("/sbin:")
        probes.add(
            Probe(
                "PATH is valid",
                "PATH",
                pathOk,
                if (pathOk) "" else slim(pathProbe.output)
            )
        )

        // 8. Commands resolve through PATH.
        val bareLs = executor.execute("ls /bin/sh")
        probes.add(
            Probe(
                "commands resolve through PATH",
                "PATH",
                bareLs.isSuccess && bareLs.output.contains("/bin/sh"),
                if (bareLs.isSuccess && bareLs.output.contains("/bin/sh")) "" else slim(bareLs.output)
            )
        )

        // 9. Executable permissions are intact inside the rootfs.
        val permsProbe = executor.execute("/bin/ls /bin/sh")
        val permsOk = permsProbe.isSuccess && busyboxFile.canExecute()
        probes.add(
            Probe(
                "executable permissions intact",
                "permissions",
                permsOk,
                if (permsOk) "" else slim(permsProbe.output)
            )
        )

        // 10. /etc/alpine-release is readable.
        val releaseProbe = executor.executeGuest(listOf("/bin/busybox", "cat", "/etc/alpine-release"))
        val releaseOk = releaseProbe.isSuccess && releaseProbe.output.trim().isNotEmpty()
        probes.add(
            Probe(
                "/etc/alpine-release is readable",
                "rootfs",
                releaseOk,
                if (releaseOk) "" else slim(releaseProbe.output)
            )
        )

        // 11. /tmp is writable: creation, write, read and cleanup checked separately.
        probes.addAll(probeTmp(executor))

        // 12. Environment variables are passed into the guest.
        probes.add(probeEnv(executor))

        // 13. Dynamic linker/interpreter requirements are satisfied (musl on arm64).
        probes.add(probeLinker(executor))

        val entries = ArrayList<LinuxLogEntryModel>()
        for (probe in probes) {
            val line = if (probe.passed) "PASS: ${probe.name}" else "FAIL: ${probe.name}"
            val detail = if (probe.detail.isNotEmpty()) ": ${probe.detail}" else ""
            entries.add(LinuxLogEntryModel(line + detail, isError = !probe.passed, isSuccess = probe.passed))
        }

        var failureMessage: String? = null
        for (probe in probes) {
            if (!probe.passed) {
                failureMessage = "${probe.name}${if (probe.detail.isNotEmpty()) " (" + probe.detail + ")" else ""}"
                break
            }
        }

        return Outcome(entries, failureMessage == null, failureMessage)
    }

    /** /bin/busybox is a real binary; run it with a valid applet, not "--version". */
    private fun probeBusybox(executor: CodeExecutor, busyboxFile: File): Probe {
        if (!busyboxFile.isFile() || !busyboxFile.canExecute()) {
            return Probe("BusyBox cannot execute", "busybox", false,
                "not a regular file or not executable on disk")
        }
        // "true" is a valid BusyBox applet that exits 0 on success.
        val run = executor.executeGuest(listOf("/bin/busybox", "true"))
        if (!run.isSuccess) {
            return Probe("BusyBox cannot execute", "busybox", false, slim(run.output))
        }
        return Probe("/bin/busybox exists and executes", "busybox", true)
    }

    /**
     * Checks every important applet path individually. A passing check prints
     * nothing; only failures are reported. If all pass, a single summary appears.
     */
    private fun probeSymlinks(): List<Probe> {
        val result = ArrayList<Probe>()
        var allOk = true
        for (path in APPLET_PATHS) {
            val file = File(rootfsDir, path.removePrefix("/"))
            val probe = verifyApplet(path, file)
            if (probe != null) {
                result.add(probe)
                allOk = false
            }
        }
        if (allOk) {
            result.add(Probe("BusyBox symlinks valid", "symlinks", true))
        }
        return result
    }

    /** Verifies a single applet path; returns null (no output) when valid. */
    private fun verifyApplet(path: String, file: File): Probe? {
        if (Files.isSymbolicLink(file.toPath())) {
            val target = Files.readSymbolicLink(file.toPath()).toString()
            val resolved = resolveInsideGuest(file.toPath())
            if (resolved == null) {
                return Probe("$path broken symlink -> $target", "symlinks", false)
            }
            if (!Files.isRegularFile(resolved.toPath())) {
                return Probe("$path unexpected target -> $target", "symlinks", false,
                    "resolves to a non-file")
            }
            if (!resolved.canExecute()) {
                return Probe("$path unexpected target -> $target", "symlinks", false,
                    "resolved target not executable")
            }
            return null
        }
        if (!file.isFile() || !file.canExecute()) {
            return Probe("$path not a regular file and not executable", "symlinks", false)
        }
        return null
    }

    /**
     * Resolves a symlink entirely within the guest rootfs, never against the host
     * filesystem. Absolute targets are rebased under [rootfsDir]; relative targets
     * are resolved from the symlink's parent directory (also under the rootfs).
     * Returns null when the chain loops, escapes the rootfs, or a link is broken.
     */
    private fun resolveInsideGuest(link: Path): File? {
        val rootPrefix = rootfsDir.absolutePath + File.separator
        val seen = HashSet<String>()
        var current = link
        var steps = 0
        while (Files.isSymbolicLink(current)) {
            val key = current.toAbsolutePath().normalize().toString()
            if (seen.contains(key) || steps > 32 || !key.startsWith(rootPrefix)) {
                return null
            }
            seen.add(key)
            val target = Files.readSymbolicLink(current)
            val next = if (target.isAbsolute) {
                // Rebase guest-absolute targets (e.g. "/bin/busybox") onto the rootfs.
                Paths.get(rootfsDir.absolutePath).resolve(Paths.get(target.toString().removePrefix("/")))
            } else {
                // Relative targets resolve from the symlink's parent inside the rootfs.
                current.parent.resolve(target)
            }
            val nextKey = next.toAbsolutePath().normalize().toString()
            if (!nextKey.startsWith(rootPrefix)) {
                return null
            }
            current = next
            steps++
        }
        val normalized = current.normalize().toAbsolutePath()
        if (!normalized.toString().startsWith(rootPrefix)) {
            return null
        }
        return normalized.toFile()
    }

    /**
     * /tmp tests: each of creation, writing, reading, cleanup is reported
     * separately. Later steps only run once the prior one succeeded, so cleanup
     * is not blamed when creation already failed.
     */
    private fun probeTmp(executor: CodeExecutor): List<Probe> {
        val result = ArrayList<Probe>()

        // creation
        val creation = executor.executeGuest(listOf("/bin/busybox", "sh", "-c", "touch $TMP_FILE"))
        if (!creation.isSuccess) {
            result.add(Probe("/tmp file creation failed", "permissions", false, slim(creation.output)))
            return result
        }

        // write
        val write = executor.executeGuest(listOf("/bin/busybox", "sh", "-c", "echo $PROBE_VALUE > $TMP_FILE"))
        if (!write.isSuccess) {
            result.add(Probe("/tmp write failed", "permissions", false, slim(write.output)))
            return result
        }

        // read
        val read = executor.executeGuest(listOf("/bin/busybox", "cat", TMP_FILE))
        if (!read.isSuccess || !read.output.contains(PROBE_VALUE)) {
            result.add(Probe("/tmp read failed", "permissions", false, slim(read.output)))
            return result
        }

        // cleanup
        val cleanup = executor.executeGuest(listOf("/bin/busybox", "rm", TMP_FILE))
        if (!cleanup.isSuccess) {
            result.add(Probe("/tmp cleanup failed", "permissions", false, slim(cleanup.output)))
            return result
        }

        result.add(Probe("/tmp is writable", "permissions", true))
        return result
    }

    /** Confirms a custom environment variable round-trips into the guest. */
    private fun probeEnv(executor: CodeExecutor): Probe {
        val env = HashMap<String, String>()
        env[PROBE_VAR] = PROBE_VALUE
        val out = executor.executeGuest(listOf("/bin/busybox", "env"), env)
        val ok = out.isSuccess && out.output.contains("$PROBE_VAR=$PROBE_VALUE")
        return Probe(
            "environment variables are passed",
            "PRoot",
            ok,
            if (ok) "" else slim(out.output)
        )
    }

    /** Confirms the musl dynamic linker/libc exist and guest binaries actually run. */
    private fun probeLinker(executor: CodeExecutor): Probe {
        val libDir = File(rootfsDir, "lib")
        val ld = File(libDir, "ld-musl-aarch64.so.1")
        val libc = File(libDir, "libc.musl-aarch64.so.1")
        if (!ld.exists()) {
            return Probe("musl dynamic linker missing", "dynamic linker", false,
                "/lib/ld-musl-aarch64.so.1 not found")
        }
        if (!libc.exists()) {
            return Probe("musl libc missing", "dynamic linker", false,
                "/lib/libc.musl-aarch64.so.1 not found")
        }
        // A real run exercises the ELF loader/ld runtime end to end.
        val run = executor.executeGuest(listOf("/bin/busybox", "true"))
        if (!run.isSuccess) {
            return Probe("musl dynamic linker cannot run binaries", "dynamic linker", false,
                slim(run.output))
        }
        return Probe("musl dynamic linker present", "dynamic linker", true)
    }

    /** Collapses probe output so a FAIL reason stays on one readable line. */
    private fun slim(text: String): String =
        text.trim().replace("\n", " | ").replace("\r", "").take(500)
}
