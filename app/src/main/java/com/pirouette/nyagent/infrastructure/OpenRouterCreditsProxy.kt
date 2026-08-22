package com.pirouette.nyagent.infrastructure

import android.os.Handler
import android.os.Looper
import com.google.gson.Gson
import com.pirouette.nyagent.application.dto.OpenRouterCreditsResponseModel
import com.pirouette.nyagent.application.interfaces.IOpenRouterCreditsProxy
import java.io.IOException

/** [IOpenRouterCreditsProxy] backed by [OpenRouterApiClient], fetching the account balance. */
class OpenRouterCreditsProxy(
    private val apiClient: OpenRouterApiClient,
    private val gson: Gson = Gson(),
    private val mainHandler: Handler = Handler(Looper.getMainLooper())
) : IOpenRouterCreditsProxy {

    override fun fetchCredits(
        apiKey: String,
        onResult: (Double?) -> Unit,
        onError: (String) -> Unit
    ) {
        Thread {
            try {
                val body = apiClient.get(PATH_CREDITS, apiKey)
                val credits = gson.fromJson(body, OpenRouterCreditsResponseModel::class.java)?.data
                    ?: throw IOException("No credit info returned")
                mainHandler.post { onResult(credits.remainingCredit) }
            } catch (error: Exception) {
                mainHandler.post { onError(error.message ?: error.toString()) }
            }
        }.start()
    }

    private companion object {
        const val PATH_CREDITS = "/credits"
    }
}
