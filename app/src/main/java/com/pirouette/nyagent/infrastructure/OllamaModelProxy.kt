package com.pirouette.nyagent.infrastructure

import android.os.Handler
import android.os.Looper
import com.google.gson.Gson
import com.pirouette.nyagent.application.dto.OllamaTagsResponseModel
import com.pirouette.nyagent.application.interfaces.IOllamaModelProxy
import java.io.IOException

/** [IOllamaModelProxy] backed by [OllamaApiClient], listing models over HTTP. */
class OllamaModelProxy(
    private val apiClient: OllamaApiClient,
    private val gson: Gson = Gson(),
    private val mainHandler: Handler = Handler(Looper.getMainLooper())
) : IOllamaModelProxy {

    override fun fetchModels(
        serverAddress: String,
        port: String,
        onResult: (List<String>) -> Unit,
        onError: (String) -> Unit
    ) {
        Thread {
            try {
                val (responseCode, body) = apiClient.get(serverAddress, port, PATH_TAGS)
                if (responseCode !in 200..299) {
                    throw IOException(body.ifEmpty { "HTTP $responseCode" })
                }

                val models = gson.fromJson(body, OllamaTagsResponseModel::class.java)
                    ?.models
                    ?.map { it.name }
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
        const val PATH_TAGS = "/api/tags"
    }
}
