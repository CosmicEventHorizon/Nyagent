package com.pirouette.nyagent.presentation.widget

import android.content.Context
import android.graphics.Color
import android.webkit.WebView
import java.io.IOException
import java.nio.charset.StandardCharsets

/**
 * A reusable Markdown + LaTeX renderer built on a local [WebView]. KaTeX and
 * markdown-it are bundled under `assets/markdown/` so everything renders offline.
 *
 * Each instance renders one assistant response. The renderer HTML is read from
 * the packaged APK assets, the model text is embedded as a JSON string literal,
 * and the combined document is loaded with a base URL of
 * `file:///android_asset/markdown/` so the relative `lib/` and `fonts/`
 * references resolve from the packaged assets. Inline math (`$...$`, `\(...\)`)
 * and block math (`$$...$$`, `\[...\]`) are rendered by KaTeX.
 */
class MarkdownWebView(context: Context, content: String, textColor: String) : WebView(context) {

    private companion object {
        const val BASE_URL = "file:///android_asset/markdown/"
        const val MIME_TYPE = "text/html"
        const val ENCODING = "utf-8"
    }

    init {
        settings.javaScriptEnabled = true
        settings.loadsImagesAutomatically = true
        isVerticalScrollBarEnabled = false
        isHorizontalScrollBarEnabled = false
        isScrollContainer = false
        setBackgroundColor(Color.TRANSPARENT)
        render(content, textColor)
    }

    /** Renders [content] as Markdown + LaTeX inside this WebView. */
    fun render(content: String, textColor: String) {
        val template = readAssetHtml()
        val page = template
            .replace("/*__CONTENT__*/", jsString(content))
            .replace("/*__TEXT_COLOR__*/", jsString(textColor))
        loadDataWithBaseURL(BASE_URL, page, MIME_TYPE, ENCODING, null)
    }

    /** JSON-encodes [value] so it is safe as a JavaScript string literal. */
    private fun jsString(value: String): String {
        val sb = StringBuilder("\"")
        for (ch in value) {
            when (ch) {
                '\\' -> sb.append("\\\\")
                '"' -> sb.append("\\\"")
                '\n' -> sb.append("\\n")
                '\r' -> sb.append("\\r")
                '\t' -> sb.append("\\t")
                '\b' -> sb.append("\\b")
                '\u000C' -> sb.append("\\f")
                else -> {
                    if (ch < ' ') {
                        sb.append("\\u").append(String.format("%04x", ch.code))
                    } else {
                        sb.append(ch)
                    }
                }
            }
        }
        sb.append('"')
        return sb.toString()
    }

    /** Reads the bundled renderer.html from the APK package assets. */
    private fun readAssetHtml(): String {
        val stream = MarkdownWebView::class.java.getResourceAsStream("/assets/markdown/renderer.html")
            ?: throw IllegalStateException("Missing bundled renderer.html under assets/markdown/")
        try {
            return String(stream.readAllBytesStdlib(), StandardCharsets.UTF_8)
        } catch (e: IOException) {
            throw IllegalStateException("Failed to read bundled renderer.html", e)
        }
    }

    /** Reads all bytes from [stream] into a byte array, handling older Android. */
    private fun java.io.InputStream.readAllBytesStdlib(): ByteArray {
        val buffer = java.io.ByteArrayOutputStream()
        val chunk = ByteArray(4096)
        while (true) {
            val n = read(chunk)
            if (n < 0) break
            buffer.write(chunk, 0, n)
        }
        return buffer.toByteArray()
    }
}
