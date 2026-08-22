package com.pirouette.nyagent.infrastructure

import android.os.Handler
import android.os.Looper
import com.google.gson.Gson
import com.pirouette.nyagent.application.dto.OpenRouterArtifactResponseModel
import com.pirouette.nyagent.application.interfaces.IOpenRouterModelProxy
import java.io.IOException

/** [IOpenRouterModelProxy] backed by [OpenRouterApiClient], listing models over HTTPS. */
class OpenRouterModelProxy(
    private val apiClient: OpenRouterApiClient,
    private val gson: Gson = Gson(),
    private val mainHandler: Handler = Handler(Looper.getMainLooper())
) : IOpenRouterModelProxy {

    override fun fetchModels(
        apiKey: String,
        onResult: (List<String>) -> Unit,
        onError: (String) -> Unit
    ) {
        Thread {
            try {
                val body = apiClient.get(PATH_MODELS, apiKey)
                val models = gson.fromJson(body, OpenRouterArtifactResponseModel::class.java)
                    ?.data
                    ?.map { it.id }
                    ?.filter { it.isNotBlank() }
                    ?: emptyList()

                if (models.isEmpty()) {
                    throw IOException("No models found")
                }

                mainHandler.post { onResult(models) }
            } catch (error: Exception) {
                mainHandler.post { onError(error.message ?: error.toString()) }
            }
        }.start()
    }

    private companion object {
        const val PATH_MODELS = "/models"
    }
}
