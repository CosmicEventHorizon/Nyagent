package com.pirouette.nyagent.presentation.activity

import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.Button
import android.widget.EditText
import android.widget.ListView
import android.widget.PopupWindow
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.pirouette.nyagent.NyagentApplication
import com.pirouette.nyagent.R
import com.pirouette.nyagent.application.service.OpenRouterService
import com.pirouette.nyagent.application.service.SettingsService

class OpenRouterActivity : AppCompatActivity() {

    private lateinit var settingsService: SettingsService
    private lateinit var openRouterService: OpenRouterService

    private lateinit var etApiKey: EditText
    private lateinit var etModel: EditText
    private lateinit var tvCredits: TextView
    private lateinit var btnBack: Button
    private lateinit var btnSave: Button
    private lateinit var btnReset: Button
    private lateinit var btnFetchCredits: Button
    private lateinit var btnChooseModel: Button

    private var modelPickerPopup: PopupWindow? = null
    private val allModels = ArrayList<String>()
    private var selectedModel = ""

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_openrouter)

        val locator = (application as NyagentApplication).serviceLocator
        settingsService = locator.settingsService
        openRouterService = locator.openRouterService

        initializeWidgets()
        loadData()

        btnBack.setOnClickListener { finish() }
        btnSave.setOnClickListener {
            saveData()
            Toast.makeText(applicationContext, "Data Saved!", Toast.LENGTH_SHORT).show()
        }
        btnReset.setOnClickListener {
            clearForm()
            Toast.makeText(applicationContext, "Loaded Default Settings", Toast.LENGTH_SHORT).show()
        }
        btnFetchCredits.setOnClickListener { fetchCredits() }
        btnChooseModel.setOnClickListener { toggleModelPicker() }
    }

    private fun initializeWidgets() {
        etApiKey = findViewById(R.id.etOpenRouterApiKey)
        etModel = findViewById(R.id.etOpenRouterModel)
        tvCredits = findViewById(R.id.tvOpenRouterCredits)
        btnBack = findViewById(R.id.btnOpenBack)
        btnSave = findViewById(R.id.btnOpenSave)
        btnReset = findViewById(R.id.btnOpenReset)
        btnFetchCredits = findViewById(R.id.btnOpenFetchCredits)
        btnChooseModel = findViewById(R.id.btnOpenChooseModel)
    }

    private fun loadData() {
        etApiKey.setText(settingsService.openRouterApiKey)
        etModel.setText(settingsService.openRouterModel)
    }

    private fun saveData() {
        settingsService.openRouterApiKey = etApiKey.text.toString().trim()
        settingsService.openRouterModel = etModel.text.toString().trim()
    }

    private fun clearForm() {
        etApiKey.setText("")
        etModel.setText("")
        tvCredits.text = "Balance: --"
        saveData()
    }

    private fun fetchCredits() {
        val apiKey = etApiKey.text.toString().trim()
        if (apiKey.isEmpty()) {
            Toast.makeText(applicationContext, "Enter an API key first", Toast.LENGTH_SHORT).show()
            return
        }
        openRouterService.fetchCredits(
            apiKey = apiKey,
            onResult = { balance ->
                tvCredits.text = if (balance == null) {
                    "Balance: Unavailable"
                } else {
                    "Balance: \$${"%.2f".format(balance)}"
                }
            },
            onError = { message ->
                Toast.makeText(applicationContext, message, Toast.LENGTH_SHORT).show()
            }
        )
    }

    private fun toggleModelPicker() {
        val apiKey = etApiKey.text.toString().trim()
        if (apiKey.isEmpty()) {
            Toast.makeText(applicationContext, "Enter an API key first", Toast.LENGTH_SHORT).show()
            return
        }
        if (modelPickerPopup?.isShowing == true) {
            modelPickerPopup?.dismiss()
        } else {
            fetchAndShowModelPicker(apiKey)
        }
    }

    private fun fetchAndShowModelPicker(apiKey: String) {
        openRouterService.fetchModels(
            apiKey = apiKey,
            onResult = { models ->
                allModels.clear()
                allModels.addAll(models)
                showModelPicker()
            },
            onError = { message ->
                Toast.makeText(applicationContext, message, Toast.LENGTH_SHORT).show()
            }
        )
    }

    private fun showModelPicker() {
        if (allModels.isEmpty()) {
            Toast.makeText(applicationContext, "No models returned", Toast.LENGTH_SHORT).show()
            return
        }

        val inflater = LayoutInflater.from(this)
        val popupView: View = inflater.inflate(R.layout.popup_model_picker, null)
        val etSearch: EditText = popupView.findViewById(R.id.etModelSearch)
        val listView: ListView = popupView.findViewById(R.id.lvAvailableModels)
        val btnClose: Button = popupView.findViewById(R.id.btnCloseModelPicker)

        var adapter = ArrayAdapter(this, android.R.layout.simple_list_item_1, allModels)
        listView.adapter = adapter

        fun refresh(query: String) {
            val trimmed = query.trim()
            val filtered = when {
                trimmed.isEmpty() -> allModels
                else -> {
                    val pattern = try {
                        Regex(trimmed, RegexOption.IGNORE_CASE)
                    } catch (e: Exception) {
                        Regex(Regex.escape(trimmed), RegexOption.IGNORE_CASE)
                    }
                    allModels.filter { pattern.containsMatchIn(it) }
                }
            }
            adapter = ArrayAdapter(this, android.R.layout.simple_list_item_1, ArrayList(filtered))
            listView.adapter = adapter
        }

        etSearch.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                refresh(s?.toString().orEmpty())
            }
            override fun afterTextChanged(s: Editable?) {}
        })

        listView.setOnItemClickListener { _, _, position, _ ->
            selectedModel = listView.getItemAtPosition(position).toString()
            etModel.setText(selectedModel)
            saveData()
            closeModelPicker()
            Toast.makeText(applicationContext, "Model chosen: $selectedModel", Toast.LENGTH_SHORT).show()
        }

        btnClose.setOnClickListener { closeModelPicker() }

        val popupWindow = PopupWindow(
            popupView,
            ViewGroup.LayoutParams.WRAP_CONTENT,
            ViewGroup.LayoutParams.WRAP_CONTENT,
            true
        )
        popupWindow.isOutsideTouchable = true
        popupWindow.elevation = 10f
        popupWindow.setOnDismissListener { modelPickerPopup = null }
        popupWindow.showAtLocation(btnChooseModel, android.view.Gravity.CENTER, 0, 0)
        modelPickerPopup = popupWindow
    }

    private fun closeModelPicker() {
        modelPickerPopup?.dismiss()
        modelPickerPopup = null
    }
}
