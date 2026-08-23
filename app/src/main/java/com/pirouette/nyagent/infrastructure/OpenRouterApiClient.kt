package com.pirouette.nyagent.infrastructure

import java.io.IOException
import java.net.HttpURLConnection
import java.net.URL

/** Low-level helper for talking to the OpenRouter API over HTTPS. */
class OpenRouterApiClient {

    private fun openConnection(
        path: String,
        method: String,
        apiKey: String,
        body: String? = null,
        connectTimeout: Int = DEFAULT_TIMEOUT_MS,
        readTimeout: Int = DEFAULT_TIMEOUT_MS
    ): HttpURLConnection {
        val connection = URL(BASE_URL + path).openConnection() as HttpURLConnection
        connection.requestMethod = method
        connection.connectTimeout = connectTimeout
        connection.readTimeout = readTimeout
        connection.doInput = true
        connection.setRequestProperty("Authorization", "Bearer $apiKey")
        if (body != null) {
            connection.doOutput = true
            connection.setRequestProperty("Content-Type", "application/json")
            connection.outputStream.use { it.write(body.toByteArray()) }
        }
        return connection
    }

    private fun parseResponse(connection: HttpURLConnection): String {
        val responseCode = connection.responseCode
        if (responseCode !in 200..299) {
            val error = connection.errorStream?.bufferedReader()?.readText() ?: "HTTP $responseCode"
            throw IOException(error)
        }
        return connection.inputStream.bufferedReader().readText()
    }

    /** Performs a GET to [path] and returns the response body. */
    fun get(path: String, apiKey: String): String {
        val connection = openConnection(path, METHOD_GET, apiKey)
        return try {
            parseResponse(connection)
        } finally {
            connection.disconnect()
        }
    }

    /** Performs a POST of [requestBody] to [path] with long timeouts for chat completions. */
    fun postChat(path: String, apiKey: String, requestBody: String): String {
        val connection = openConnection(
            path,
            METHOD_POST,
            apiKey,
            body = requestBody,
            connectTimeout = CHAT_TIMEOUT_MS,
            readTimeout = CHAT_TIMEOUT_MS
        )
        return try {
            parseResponse(connection)
        } finally {
            connection.disconnect()
        }
    }

    private companion object {
        const val BASE_URL = "https://openrouter.ai/api/v1"
        const val METHOD_GET = "GET"
        const val METHOD_POST = "POST"
        const val DEFAULT_TIMEOUT_MS = 10_000
        const val CHAT_TIMEOUT_MS = 100_000
    }
}
