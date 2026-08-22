package com.pirouette.nyagent.persistence.repository

import android.content.Context
import android.content.SharedPreferences
import com.pirouette.nyagent.application.interfaces.ISettingsRepository

/**
 * [ISettingsRepository] backed by Android SharedPreferences.
 *
 * Server + system prompt settings live in `saved_ollama_settings` while the
 * generic "use Ollama" toggle and Linux environment flag live in
 * `saved_server_settings`, matching the original storage so existing
 * installations keep their data.
 */
class SettingsRepository(context: Context) : ISettingsRepository {

    private val ollamaPrefs: SharedPreferences = context.getSharedPreferences(
        PREFS_OLLAMA,
        Context.MODE_PRIVATE
    )

    private val serverPrefs: SharedPreferences = context.getSharedPreferences(
        PREFS_SERVER,
        Context.MODE_PRIVATE
    )

    override var serverAddress: String
        get() = ollamaPrefs.getString(KEY_SERVER_ADDRESS, ISettingsRepository.DEFAULT_SERVER_ADDRESS).orEmpty()
        set(value) = ollamaPrefs.edit().putString(KEY_SERVER_ADDRESS, value).apply()

    override var serverPort: String
        get() = ollamaPrefs.getString(KEY_SERVER_PORT, ISettingsRepository.DEFAULT_SERVER_PORT).orEmpty()
        set(value) = ollamaPrefs.edit().putString(KEY_SERVER_PORT, value).apply()

    override var model: String
        get() = ollamaPrefs.getString(KEY_MODEL, ISettingsRepository.DEFAULT_MODEL).orEmpty()
        set(value) = ollamaPrefs.edit().putString(KEY_MODEL, value).apply()

    override var systemPromptName: String
        get() = ollamaPrefs.getString(KEY_SYSTEM_PROMPT_NAME, "").orEmpty()
        set(value) = ollamaPrefs.edit().putString(KEY_SYSTEM_PROMPT_NAME, value).apply()

    override var systemPrompt: String
        get() = ollamaPrefs.getString(KEY_SYSTEM_PROMPT, "").orEmpty()
        set(value) = ollamaPrefs.edit().putString(KEY_SYSTEM_PROMPT, value).apply()

    override var openRouterApiKey: String
        get() = ollamaPrefs.getString(KEY_OPENROUTER_API_KEY, "").orEmpty()
        set(value) = ollamaPrefs.edit().putString(KEY_OPENROUTER_API_KEY, value).apply()

    override var openRouterModel: String
        get() = ollamaPrefs.getString(KEY_OPENROUTER_MODEL, "").orEmpty()
        set(value) = ollamaPrefs.edit().putString(KEY_OPENROUTER_MODEL, value).apply()

    override var linuxInstalled: Boolean
        get() = serverPrefs.getBoolean(KEY_LINUX_INSTALLED, false)
        set(value) = serverPrefs.edit().putBoolean(KEY_LINUX_INSTALLED, value).apply()

    override var useOllama: Boolean
        get() = serverPrefs.getBoolean(KEY_USE_OLLAMA, true)
        set(value) = serverPrefs.edit().putBoolean(KEY_USE_OLLAMA, value).apply()

    override var maxContextTokens: Int
        get() = serverPrefs.getInt(KEY_MAX_CONTEXT_TOKENS, DEFAULT_MAX_CONTEXT_TOKENS)
        set(value) = serverPrefs.edit().putInt(KEY_MAX_CONTEXT_TOKENS, value).apply()

    override var compactThresholdTokens: Int
        get() = serverPrefs.getInt(KEY_COMPACT_THRESHOLD_TOKENS, DEFAULT_COMPACT_THRESHOLD_TOKENS)
        set(value) = serverPrefs.edit().putInt(KEY_COMPACT_THRESHOLD_TOKENS, value).apply()

    private companion object {
        const val PREFS_OLLAMA = "saved_ollama_settings"
        const val PREFS_SERVER = "saved_server_settings"

        const val KEY_SERVER_ADDRESS = "IP_ADDRESS"
        const val KEY_SERVER_PORT = "PORT"
        const val KEY_MODEL = "MODEL"
        const val KEY_SYSTEM_PROMPT_NAME = "SYSTEM_PROMPT_NAME"
        const val KEY_SYSTEM_PROMPT = "SYSTEM_PROMPT"
        const val KEY_USE_OLLAMA = "OLLAMA"
        const val KEY_OPENROUTER_API_KEY = "OPENROUTER_API_KEY"
        const val KEY_OPENROUTER_MODEL = "OPENROUTER_MODEL"
        const val KEY_LINUX_INSTALLED = "LINUX_INSTALLED"
        const val KEY_MAX_CONTEXT_TOKENS = "MAX_CONTEXT_TOKENS"
        const val KEY_COMPACT_THRESHOLD_TOKENS = "COMPACT_THRESHOLD_TOKENS"

        const val DEFAULT_MAX_CONTEXT_TOKENS = 8192
        const val DEFAULT_COMPACT_THRESHOLD_TOKENS = 6144
    }
}
