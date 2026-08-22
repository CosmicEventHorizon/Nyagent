package com.pirouette.nyagent.infrastructure.linux

import android.content.Context
import android.content.pm.ApplicationInfo
import com.pirouette.nyagent.R
import com.pirouette.nyagent.application.interfaces.IEnvironmentService
import com.pirouette.nyagent.application.interfaces.ISettingsRepository
import com.pirouette.nyagent.application.model.LinuxLogEntryModel
import java.io.File
import java.nio.file.Files
import com.pirouette.nyagent.infrastructure.harness.CodeExecutor

/**
 * Owns the app's private ARM64 Alpine Linux environment. The Alpine rootfs is
 * bundled as a raw resource and unpacked into app-private storage on first boot
 * (or when the user reinstalls from settings). PRoot itself is packaged as a
 * native library and executes directly from the package's native library
 * directory, which avoids writable-storage executability restrictions.
 */
class LinuxEnvironmentService(
    private val context: Context,
    private val settings: ISettingsRepository
) : IEnvironmentService {

    private val applicationInfo: ApplicationInfo = context.applicationInfo
    private val nativeLibDir: File = File(applicationInfo.nativeLibraryDir)
    private val prootExecutable: File = File(nativeLibDir, "libproot.so")
    private val prootLoader: File = File(nativeLibDir, "libproot-loader.so")

    private val rootDir: File = File(context.getFilesDir(), "linux")
    private val rootfsDir: File = File(rootDir, "rootfs")
    private val legacyProotDir: File = File(rootDir, "proot")
    private val rootfsMarker: File = File(rootDir, ".installed")
    private val cacheDir: File = File(context.getCacheDir(), "linux")
    private val _installLog = ArrayList<LinuxLogEntryModel>()

    override val isInstalled: Boolean
        get() = settings.linuxInstalled && rootfsMarker.exists()

    override val installLog: List<LinuxLogEntryModel>
        get() = _installLog.toList()

    /** Reusable command executor targeting this environment. */
    val codeExecutor by lazy {
        CodeExecutor(rootfsDir, prootExecutable, prootLoader, cacheDir)
    }

    override fun install(): Boolean {
        _installLog.clear()
        _installLog.add(LinuxLogEntryModel("Linux installation started", isSuccess = true))
        return try {
            // PRoot no longer lives here; remove any copy a previous install left behind.
            legacyProotDir.deleteRecursively()

            extractBundle(rootfsDir, R.raw.alpine_rootfs_bundle)
            verifyProotAvailable()

            // Run a full startup validation through PRoot. Only if every check passes
            // is the Alpine userspace considered healthy enough to use.
            val validation = AlpineValidator(rootfsDir, prootExecutable, prootLoader, cacheDir).validate()
            for (entry in validation.entries) {
                _installLog.add(entry)
            }
            if (!validation.allPassed) {
                throw Exception("Validation failed: " + validation.failureMessage)
            }

            seedResolvConf()

            // Demonstrate the guest is live by running an actual ls / through PRoot.
            // Only once it succeeds is the install considered successful.
            _installLog.add(LinuxLogEntryModel("testing ls /"))
            val listing = codeExecutor.execute("ls /")
            if (!listing.isSuccess) {
                throw Exception("Guest ls failed: " + listing.output)
            }

            rootfsMarker.writeText("installed")
            settings.linuxInstalled = true

            // Show the ls / result first, then the success message.
            if (listing.output.trim().isNotEmpty()) {
                _installLog.add(LinuxLogEntryModel(listing.output))
            }
            _installLog.add(LinuxLogEntryModel("Linux installed successfully", isSuccess = true))
            true
        } catch (e: Exception) {
            settings.linuxInstalled = false
            _installLog.add(
                LinuxLogEntryModel("Installation failed: ${e.message ?: e.toString()}", isError = true)
            )
            false
        }
    }

    override fun execute(command: String): String {
        if (!isInstalled) {
            return "Linux environment is not installed"
        }
        return codeExecutor.execute(command).output
    }

    private fun verifyProotAvailable() {
        val exists = prootExecutable.exists()
        _installLog.add(LinuxLogEntryModel("Proot path: " + prootExecutable.absolutePath))
        _installLog.add(LinuxLogEntryModel("Proot exists: " + exists))
        if (!exists) {
            throw Exception("Proot native library missing: ${prootExecutable.absolutePath}")
        }
        val loaderExists = prootLoader.exists()
        _installLog.add(LinuxLogEntryModel("Proot loader path: " + prootLoader.absolutePath))
        _installLog.add(LinuxLogEntryModel("Proot loader exists: " + loaderExists))
        if (!loaderExists) {
            throw Exception("Proot loader missing: ${prootLoader.absolutePath}")
        }

        prootExecutable.setExecutable(true)
        _installLog.add(LinuxLogEntryModel("Proot canExecute: " + prootExecutable.canExecute()))
        if (!prootExecutable.canExecute()) {
            throw Exception("Proot native library is not executable")
        }
    }

    private fun extractBundle(destination: File, resourceId: Int) {
        val extractor = GzipTarExtractor()
        extractor.onEntry = { entry ->
            if (_installLog.size > 1) {
                _installLog.removeAt(_installLog.lastIndex)
            }
            _installLog.add(LinuxLogEntryModel(entry))
        }
        context.getResources().openRawResource(resourceId).use { input ->
            extractor.extract(input, destination)
        }
    }

    /** The minimal rootfs ships without a resolver, so seed one for DNS. */
    private fun seedResolvConf() {
        val etcDir = File(rootfsDir, "etc")
        etcDir.mkdirs()
        val resolv = File(etcDir, "resolv.conf")
        if (!resolv.exists()) {
            resolv.writeText("nameserver 8.8.8.8\n")
        }
    }
}
