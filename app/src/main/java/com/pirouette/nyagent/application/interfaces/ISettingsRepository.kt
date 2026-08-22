package com.pirouette.nyagent.application.interfaces

/** Contract for persisting the server connection + system prompt settings. */
interface ISettingsRepository {
    var serverAddress: String
    var serverPort: String
    var model: String
    var systemPromptName: String
    var systemPrompt: String
    var useOllama: Boolean
    var openRouterApiKey: String
    var openRouterModel: String
    var linuxInstalled: Boolean
    var maxContextTokens: Int
    var compactThresholdTokens: Int

    companion object Defaults {
        const val DEFAULT_SERVER_ADDRESS = "127.0.0.1"
        const val DEFAULT_SERVER_PORT = "11434"
        const val DEFAULT_MODEL = "null"
    }
}
