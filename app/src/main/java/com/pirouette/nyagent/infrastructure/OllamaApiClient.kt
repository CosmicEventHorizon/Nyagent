package com.pirouette.nyagent.infrastructure

import java.io.IOException
import java.net.HttpURLConnection
import java.net.URI
import java.net.URL

/** Low-level helper for talking to an Ollama server over HTTP. */
class OllamaApiClient {

    /**
     * Returns a URL for [path] on the given server, adding `http://` when the
     * address has no scheme and falling back to [port] if it is invalid.
     */
    private fun urlFor(serverAddress: String, port: String?, path: String): String {
        val base = if (serverAddress.startsWith("http://") || serverAddress.startsWith("https://")) {
            serverAddress
        } else {
            "http://$serverAddress"
        }

        val uri = try {
            URI(base)
        } catch (e: Exception) {
            throw IllegalArgumentException("Invalid server address")
        }

        val portInt = port?.toIntOrNull() ?: uri.port
        return URI(uri.scheme, null, uri.host, portInt, path, null, null).toString()
    }

    private fun openConnection(
        serverAddress: String,
        port: String?,
        path: String,
        method: String,
        body: String? = null,
        connectTimeout: Int = DEFAULT_TIMEOUT_MS,
        readTimeout: Int = DEFAULT_TIMEOUT_MS
    ): HttpURLConnection {
        val connection = URL(urlFor(serverAddress, port, path)).openConnection() as HttpURLConnection
        connection.requestMethod = method
        connection.connectTimeout = connectTimeout
        connection.readTimeout = readTimeout
        connection.doInput = true
        if (body != null) {
            connection.doOutput = true
            connection.setRequestProperty("Content-Type", "application/json")
            connection.outputStream.use { it.write(body.toByteArray()) }
        }
        return connection
    }

    /** Streams each non-blank response line of [path] (via POST with [requestBody]) to [onLine]. */
    fun streamLines(
        serverAddress: String,
        port: String?,
        path: String,
        requestBody: String,
        onLine: (String) -> Unit
    ) {
        val connection = openConnection(
            serverAddress,
            port,
            path,
            METHOD_POST,
            requestBody,
            connectTimeout = CHAT_TIMEOUT_MS,
            readTimeout = CHAT_TIMEOUT_MS
        )
        try {
            val responseCode = connection.responseCode
            if (responseCode !in 200..299) {
                val error = connection.errorStream?.bufferedReader()?.readText() ?: "HTTP $responseCode"
                throw IOException(error)
            }

            connection.inputStream.bufferedReader().forEachLine { line ->
                if (line.isNotBlank()) {
                    onLine(line)
                }
            }
        } finally {
            connection.disconnect()
        }
    }

    /** Performs a GET to [path] and returns the response code and body. */
    fun get(serverAddress: String, port: String?, path: String): Pair<Int, String> {
        val connection = openConnection(serverAddress, port, path, METHOD_GET)
        return try {
            val responseCode = connection.responseCode
            if (responseCode !in 200..299) {
                val error = connection.errorStream?.bufferedReader()?.readText() ?: "HTTP $responseCode"
                throw IOException(error)
            }
            responseCode to connection.inputStream.bufferedReader().readText()
        } finally {
            connection.disconnect()
        }
    }

    /**
     * Performs a blocking POST of [requestBody] to [path] and returns the whole
     * response body. Used for non-streaming chat completions (including tool
     * calling) so the harness can inspect the full reply.
     */
    fun postChat(
        serverAddress: String,
        port: String?,
        path: String,
        requestBody: String
    ): String {
        val connection = openConnection(
            serverAddress,
            port,
            path,
            METHOD_POST,
            requestBody,
            connectTimeout = CHAT_TIMEOUT_MS,
            readTimeout = CHAT_TIMEOUT_MS
        )
        return try {
            val responseCode = connection.responseCode
            if (responseCode !in 200..299) {
                val error = connection.errorStream?.bufferedReader()?.readText() ?: "HTTP $responseCode"
                throw IOException(error)
            }
            connection.inputStream.bufferedReader().readText()
        } finally {
            connection.disconnect()
        }
    }

    private companion object {
        const val METHOD_GET = "GET"
        const val METHOD_POST = "POST"
        const val DEFAULT_TIMEOUT_MS = 10_000
        const val CHAT_TIMEOUT_MS = 600_000
    }
}
