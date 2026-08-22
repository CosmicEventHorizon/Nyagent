package com.pirouette.nyagent.presentation.activity

import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.pirouette.nyagent.NyagentApplication
import com.pirouette.nyagent.R
import com.pirouette.nyagent.application.service.SettingsService

/** Edits the model context window and auto-compaction threshold. */
class ModelConfigurationActivity : AppCompatActivity() {

    private companion object {
        const val DEFAULT_MAX_CONTEXT_TOKENS = 8192
        const val DEFAULT_COMPACT_THRESHOLD_TOKENS = 6144
    }

    private lateinit var settingsService: SettingsService
    private lateinit var etMaxContext: EditText
    private lateinit var etCompactThreshold: EditText
    private lateinit var btnBack: Button
    private lateinit var btnSave: Button
    private lateinit var btnReset: Button

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_model_config)

        settingsService = (application as NyagentApplication).serviceLocator.settingsService
        initializeWidgets()
        loadData()

        btnBack.setOnClickListener { finish() }
        btnSave.setOnClickListener {
            saveData()
            Toast.makeText(applicationContext, "Data Saved!", Toast.LENGTH_SHORT).show()
        }
        btnReset.setOnClickListener {
            resetDefaults()
            Toast.makeText(applicationContext, "Loaded Default Settings", Toast.LENGTH_SHORT).show()
        }
    }

    private fun initializeWidgets() {
        etMaxContext = findViewById(R.id.etMaxContext)
        etCompactThreshold = findViewById(R.id.etCompactThreshold)
        btnBack = findViewById(R.id.btnModelConfigBack)
        btnSave = findViewById(R.id.btnModelConfigSave)
        btnReset = findViewById(R.id.btnModelConfigReset)
    }

    private fun loadData() {
        etMaxContext.setText(settingsService.maxContextTokens.toString())
        etCompactThreshold.setText(settingsService.compactThresholdTokens.toString())
    }

    private fun resetDefaults() {
        settingsService.maxContextTokens = DEFAULT_MAX_CONTEXT_TOKENS
        settingsService.compactThresholdTokens = DEFAULT_COMPACT_THRESHOLD_TOKENS
        loadData()
    }

    private fun saveData() {
        val maxContext = etMaxContext.text.toString().trim().toIntOrNull()
        val threshold = etCompactThreshold.text.toString().trim().toIntOrNull()
        if (maxContext != null && maxContext > 0) {
            settingsService.maxContextTokens = maxContext
        }
        if (threshold != null && threshold > 0) {
            settingsService.compactThresholdTokens = threshold
        }
        loadData()
    }
}
