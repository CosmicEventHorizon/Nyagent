package com.pirouette.nyagent.presentation.activity

import android.os.Bundle
import android.view.View
import android.widget.AdapterView
import android.widget.ArrayAdapter
import android.widget.Button
import android.widget.EditText
import android.widget.Spinner
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.pirouette.nyagent.NyagentApplication
import com.pirouette.nyagent.R
import com.pirouette.nyagent.application.model.SavedPromptModel
import com.pirouette.nyagent.application.service.OllamaModelService
import com.pirouette.nyagent.application.service.SettingsService
import com.pirouette.nyagent.presentation.ServiceLocator

class OllamaActivity : AppCompatActivity() {

    private lateinit var serviceLocator: ServiceLocator
    private lateinit var settingsService: SettingsService
    private lateinit var modelService: OllamaModelService

    private lateinit var etOllamaIPAdd: EditText
    private lateinit var etOllamaPort: EditText
    private lateinit var etOllamaModel: EditText
    private lateinit var etOllamaPromptName: EditText
    private lateinit var etOllamaSystemPrompt: EditText
    private lateinit var btnOllamaBack: Button
    private lateinit var btnOllamaSave: Button
    private lateinit var btnOllamaReset: Button
    private lateinit var btnOllamaPullModels: Button
    private lateinit var btnOllamaApplyModel: Button
    private lateinit var spOllamaSystemPrompts: Spinner
    private lateinit var spOllamaModels: Spinner
    private lateinit var tvSavedPrompts: TextView

    private val promptList = ArrayList<SavedPromptModel>()
    private val modelList = ArrayList<String>()
    private var selectedModel = ""

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_ollama)
        serviceLocator = (application as NyagentApplication).serviceLocator
        settingsService = serviceLocator.settingsService
        modelService = serviceLocator.modelService
        initializeWidgets()
        loadData()

        btnOllamaBack.setOnClickListener { finish() }
        btnOllamaSave.setOnClickListener {
            saveData()
            Toast.makeText(applicationContext, "Data Saved!", Toast.LENGTH_SHORT).show()
        }
        btnOllamaPullModels.setOnClickListener { pullModels() }
        btnOllamaApplyModel.setOnClickListener { applySelectedModel() }
        btnOllamaReset.setOnClickListener {
            resetDefaults()
            Toast.makeText(applicationContext, "Loaded Default Settings", Toast.LENGTH_SHORT).show()
        }
    }

    private fun initializeWidgets() {
        etOllamaIPAdd = findViewById(R.id.etOllamaIPAdd)
        etOllamaPort = findViewById(R.id.etOllamaPort)
        etOllamaModel = findViewById(R.id.etOllamaModel)
        etOllamaPromptName = findViewById(R.id.etOllamaPromptName)
        etOllamaSystemPrompt = findViewById(R.id.etOllamaSystemPrompt)
        spOllamaSystemPrompts = findViewById(R.id.spOllamaSystemPrompts)
        spOllamaModels = findViewById(R.id.spOllamaModels)
        tvSavedPrompts = findViewById(R.id.tvSavedPrompts)
        btnOllamaBack = findViewById(R.id.btnOllamaBack)
        btnOllamaSave = findViewById(R.id.btnOllamaSave)
        btnOllamaReset = findViewById(R.id.btnOllamaReset)
        btnOllamaPullModels = findViewById(R.id.btnOllamaPullModels)
        btnOllamaApplyModel = findViewById(R.id.btnOllamaApplyModel)

        spOllamaSystemPrompts.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {
                if (position in promptList.indices) {
                    etOllamaPromptName.setText(promptList[position].name)
                    etOllamaSystemPrompt.setText(promptList[position].prompt)
                }
            }

            override fun onNothingSelected(parent: AdapterView<*>?) {}
        }

        spOllamaModels.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {
                if (position in modelList.indices) {
                    selectedModel = modelList[position]
                }
            }

            override fun onNothingSelected(parent: AdapterView<*>?) {}
        }
    }

    private fun loadData() {
        etOllamaIPAdd.setText(settingsService.serverAddress)
        etOllamaPort.setText(settingsService.serverPort)
        etOllamaModel.setText(settingsService.model)
        etOllamaPromptName.setText(settingsService.systemPromptName)
        etOllamaSystemPrompt.setText(settingsService.systemPrompt)

        promptList.clear()
        promptList.addAll(settingsService.promptLibrary)
        loadPromptSpinner(settingsService.systemPromptName)

        resetModelList()
    }

    private fun resetDefaults() {
        settingsService.resetServerDefaults()
        etOllamaPort.setText(settingsService.serverPort)
        etOllamaIPAdd.setText(settingsService.serverAddress)
        etOllamaModel.setText(settingsService.model)
        etOllamaPromptName.setText("")
        etOllamaSystemPrompt.setText("")
        resetModelList()
    }

    private fun saveData() {
        settingsService.saveCurrentSettings(
            etOllamaPromptName.text.toString().trim(),
            etOllamaSystemPrompt.text.toString()
        )
        etOllamaIPAdd.text.toString().let { settingsService.serverAddress = it }
        etOllamaPort.text.toString().let { settingsService.serverPort = it }
        etOllamaModel.text.toString().let { settingsService.model = it }
        promptList.clear()
        promptList.addAll(settingsService.promptLibrary)
        loadPromptSpinner(settingsService.systemPromptName)
    }

    private fun resetModelList() {
        modelList.clear()
        selectedModel = ""
        spOllamaModels.adapter = null
        spOllamaModels.visibility = View.GONE
    }

    private fun pullModels() {
        val ipAddress = etOllamaIPAdd.text.toString().trim()
        val portNumber = etOllamaPort.text.toString().trim()
        modelService.fetchModels(
            serverAddress = ipAddress,
            port = portNumber,
            onResult = { models ->
                modelList.clear()
                modelList.addAll(models)
                selectedModel = modelList.firstOrNull().orEmpty()
                val adapter = ArrayAdapter(this, android.R.layout.simple_spinner_item, modelList)
                adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
                spOllamaModels.adapter = adapter
                spOllamaModels.visibility = View.VISIBLE
                Toast.makeText(applicationContext, "Models Pulled!", Toast.LENGTH_SHORT).show()
            },
            onError = { message ->
                Toast.makeText(applicationContext, message, Toast.LENGTH_SHORT).show()
            }
        )
    }

    private fun applySelectedModel() {
        if (modelList.isEmpty()) {
            Toast.makeText(applicationContext, "Pull models first", Toast.LENGTH_SHORT).show()
            return
        }
        selectedModel = spOllamaModels.selectedItem?.toString().orEmpty()
        if (selectedModel.isEmpty()) {
            Toast.makeText(applicationContext, "Choose a model first", Toast.LENGTH_SHORT).show()
            return
        }
        etOllamaModel.setText(selectedModel)
        saveData()
        Toast.makeText(applicationContext, "Model Applied!", Toast.LENGTH_SHORT).show()
    }

    private fun loadPromptSpinner(selectedPromptName: String) {
        if (promptList.isEmpty()) {
            spOllamaSystemPrompts.visibility = View.GONE
            tvSavedPrompts.visibility = View.GONE
            return
        }
        spOllamaSystemPrompts.visibility = View.VISIBLE
        tvSavedPrompts.visibility = View.VISIBLE
        val promptAdapter = ArrayAdapter(
            this,
            android.R.layout.simple_spinner_item,
            promptList.map { it.name }
        )
        promptAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        spOllamaSystemPrompts.adapter = promptAdapter
        val selectedIndex = promptList.indexOfFirst { it.name == selectedPromptName }
        if (selectedIndex >= 0) {
            spOllamaSystemPrompts.setSelection(selectedIndex)
        }
    }
}
