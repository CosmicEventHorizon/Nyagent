package com.pirouette.nyagent.presentation

import android.content.Context
import com.pirouette.nyagent.application.service.ChatService
import com.pirouette.nyagent.application.service.CompactionService
import com.pirouette.nyagent.application.service.OllamaModelService
import com.pirouette.nyagent.application.service.OpenRouterService
import com.pirouette.nyagent.application.service.SettingsService
import com.pirouette.nyagent.application.service.StoryService
import com.pirouette.nyagent.infrastructure.OllamaApiClient
import com.pirouette.nyagent.infrastructure.OllamaModelProxy
import com.pirouette.nyagent.infrastructure.OpenRouterApiClient
import com.pirouette.nyagent.infrastructure.OpenRouterCreditsProxy
import com.pirouette.nyagent.infrastructure.OpenRouterModelProxy
import com.pirouette.nyagent.infrastructure.harness.AgentLoopHarness
import com.pirouette.nyagent.infrastructure.harness.LinuxToolHarness
import com.pirouette.nyagent.infrastructure.harness.OllamaCompletionSender
import com.pirouette.nyagent.infrastructure.harness.OpenRouterCompletionSender
import com.pirouette.nyagent.infrastructure.harness.SettingsCompletionSender
import com.pirouette.nyagent.infrastructure.linux.LinuxEnvironmentService
import com.pirouette.nyagent.persistence.repository.PromptRepository
import com.pirouette.nyagent.persistence.repository.SettingsRepository
import com.pirouette.nyagent.persistence.repository.StoryRepository

/**
 * Simple service locator that wires the concrete repository and proxy
 * implementations used by the app. Keeps the UI decoupled from how the
 * application's dependencies are constructed.
 */
class ServiceLocator(context: Context) {

    private val appContext = context.applicationContext

    private val settingsRepository by lazy { SettingsRepository(appContext) }

    private val promptRepository by lazy {
        PromptRepository(
            appContext.getSharedPreferences(PromptRepository.PREFS_NAME, Context.MODE_PRIVATE)
        )
    }

    private val storyRepository by lazy { StoryRepository(appContext) }

    private val ollamaApiClient = OllamaApiClient()
    private val modelProxy = OllamaModelProxy(ollamaApiClient)
    private val openRouterApiClient = OpenRouterApiClient()
    private val openRouterModelProxy = OpenRouterModelProxy(openRouterApiClient)
    private val openRouterCreditsProxy = OpenRouterCreditsProxy(openRouterApiClient)

    val environmentService by lazy {
        LinuxEnvironmentService(appContext, settingsRepository)
    }

    val toolHarness by lazy {
        LinuxToolHarness(environmentService, environmentService.codeExecutor)
    }

    /** Completion sender that dispatches to the provider currently selected in settings. */
    val completionSender: SettingsCompletionSender by lazy {
        SettingsCompletionSender(
            settingsRepository,
            OllamaCompletionSender(ollamaApiClient, settingsRepository),
            OpenRouterCompletionSender(openRouterApiClient, settingsRepository)
        )
    }

    val harnessLoop by lazy {
        AgentLoopHarness(completionSender, toolHarness)
    }

    val settingsService: SettingsService by lazy {
        SettingsService(settingsRepository, promptRepository)
    }

    val compactionService: CompactionService by lazy {
        CompactionService(completionSender)
    }

    val chatService: ChatService by lazy {
        ChatService(
            appContext,
            settingsRepository,
            environmentService,
            harnessLoop,
            compactionService
        )
    }

    val storyService: StoryService by lazy {
        StoryService(storyRepository)
    }

    val modelService: OllamaModelService by lazy {
        OllamaModelService(modelProxy)
    }

    val openRouterService: OpenRouterService by lazy {
        OpenRouterService(openRouterModelProxy, openRouterCreditsProxy)
    }
}
