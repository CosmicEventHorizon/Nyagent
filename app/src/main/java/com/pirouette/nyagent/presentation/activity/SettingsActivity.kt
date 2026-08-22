package com.pirouette.nyagent.presentation.activity

import android.content.Intent
import android.os.Bundle
import android.widget.Button
import android.widget.RadioButton
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.pirouette.nyagent.NyagentApplication
import com.pirouette.nyagent.R
import com.pirouette.nyagent.application.service.SettingsService

class SettingsActivity : AppCompatActivity() {

    private lateinit var settingsService: SettingsService

    private lateinit var btnOllama: Button
    private lateinit var btnOpenRouter: Button
    private lateinit var btnLinux: Button
    private lateinit var btnModelConfig: Button
    private lateinit var btnSettingsBack: Button
    private lateinit var btnSettingsSave: Button
    private lateinit var rbOllama: RadioButton
    private lateinit var rbOpenRouter: RadioButton

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_settings)

        settingsService = (application as NyagentApplication).serviceLocator.settingsService
        initializeWidgets()
        loadData()

        btnOllama.setOnClickListener {
            startActivity(Intent(this, OllamaActivity::class.java))
        }
        btnOpenRouter.setOnClickListener {
            startActivity(Intent(this, OpenRouterActivity::class.java))
        }
        btnLinux.setOnClickListener {
            startActivity(Intent(this, LinuxSettingsActivity::class.java))
        }
        btnModelConfig.setOnClickListener {
            startActivity(Intent(this, ModelConfigurationActivity::class.java))
        }
        btnSettingsBack.setOnClickListener { finish() }
        btnSettingsSave.setOnClickListener {
            saveData()
            Toast.makeText(applicationContext, "Data Saved!", Toast.LENGTH_SHORT).show()
        }
        rbOllama.setOnClickListener {
            rbOpenRouter.isChecked = !rbOllama.isChecked
        }
        rbOpenRouter.setOnClickListener {
            rbOllama.isChecked = !rbOpenRouter.isChecked
        }
    }

    private fun initializeWidgets() {
        btnOllama = findViewById(R.id.btnOllama)
        btnOpenRouter = findViewById(R.id.btnOpenRouter)
        btnLinux = findViewById(R.id.btnLinux)
        btnModelConfig = findViewById(R.id.btnModelConfig)
        btnSettingsBack = findViewById(R.id.btnSettingsBack)
        btnSettingsSave = findViewById(R.id.btnSettingsSave)
        rbOllama = findViewById(R.id.rbOllama)
        rbOpenRouter = findViewById(R.id.rbOpenRouter)
    }

    private fun loadData() {
        val useOllama = settingsService.useOllama
        rbOllama.isChecked = useOllama
        rbOpenRouter.isChecked = !useOllama
    }

    private fun saveData() {
        settingsService.useOllama = rbOllama.isChecked
    }
}
