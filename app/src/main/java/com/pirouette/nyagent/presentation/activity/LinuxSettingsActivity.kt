package com.pirouette.nyagent.presentation.activity

import android.graphics.Color
import android.os.Bundle
import android.text.SpannableStringBuilder
import android.text.style.ForegroundColorSpan
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.pirouette.nyagent.NyagentApplication
import com.pirouette.nyagent.R
import com.pirouette.nyagent.application.model.LinuxLogEntryModel
import com.pirouette.nyagent.infrastructure.linux.LinuxEnvironmentService
import java.util.Timer
import java.util.TimerTask

class LinuxSettingsActivity : AppCompatActivity() {

    private companion object {
        val COLOR_OUTPUT = Color.rgb(255, 255, 255)
        val COLOR_SUCCESS = Color.rgb(0, 255, 0)
        val COLOR_ERROR = Color.rgb(255, 0, 0)
    }

    private lateinit var environmentService: LinuxEnvironmentService
    private lateinit var tvStatus: TextView
    private lateinit var tvShell: TextView
    private lateinit var btnInstall: Button
    private lateinit var btnReinstall: Button
    private lateinit var btnBack: Button
    private var installing = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_linux_settings)

        environmentService = (application as NyagentApplication)
            .serviceLocator.environmentService

        initializeWidgets()
        refreshStatus()
        renderInstallLog()

        btnBack.setOnClickListener { finish() }
        btnInstall.setOnClickListener { installEnvironment() }
        btnReinstall.setOnClickListener { installEnvironment() }
    }

    private fun initializeWidgets() {
        tvStatus = findViewById(R.id.tvLinuxStatus)
        tvShell = findViewById(R.id.tvLinuxShell)
        btnInstall = findViewById(R.id.btnLinuxInstall)
        btnReinstall = findViewById(R.id.btnLinuxReinstall)
        btnBack = findViewById(R.id.btnLinuxBack)
    }

    private fun refreshStatus() {
        tvStatus.text = if (installing) "Installing..." else if (environmentService.isInstalled) "Installed" else "Not installed"
        btnInstall.isEnabled = !installing && !environmentService.isInstalled
        btnReinstall.isEnabled = !installing && environmentService.isInstalled
    }

    private fun renderInstallLog() {
        val styled = SpannableStringBuilder()
        for (entry in environmentService.installLog) {
            val line = entry.message + "\n"
            val start = styled.length
            styled.append(line)
            val color = if (entry.isError) COLOR_ERROR else if (entry.isSuccess) COLOR_SUCCESS else COLOR_OUTPUT
            styled.setSpan(ForegroundColorSpan(color), start, styled.length, 0)
        }
        tvShell.text = styled
    }

    private fun installEnvironment() {
        if (installing) return
        installing = true
        refreshStatus()
        Toast.makeText(applicationContext, "Installing Linux environment...", Toast.LENGTH_SHORT).show()

        val progress = Timer()
        progress.scheduleAtFixedRate(object : TimerTask() {
            override fun run() {
                runOnUiThread {
                    if (installing) {
                        renderInstallLog()
                    }
                }
            }
        }, 200, 200)

        val installer = Thread {
            val success = environmentService.install()
            runOnUiThread {
                progress.cancel()
                installing = false
                refreshStatus()
                renderInstallLog()
                Toast.makeText(
                    applicationContext,
                    if (success) "Linux environment installed" else "Linux environment installation failed",
                    Toast.LENGTH_LONG
                ).show()
            }
        }
        installer.isDaemon = true
        installer.start()
    }
}
