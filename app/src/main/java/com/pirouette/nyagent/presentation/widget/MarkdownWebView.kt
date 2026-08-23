package com.pirouette.nyagent.presentation.widget

import android.content.Context
import android.graphics.Color
import android.webkit.WebView
import android.webkit.WebViewClient
import java.io.IOException

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
        const val HEIGHT_MEASURE_JS =
            "(function(){var d=document.getElementById('content');" +
            "return Math.max(document.body.scrollHeight, document.documentElement.scrollHeight, 1);})();"
    }

    init {
        settings.javaScriptEnabled = true
        settings.loadsImagesAutomatically = true
        isVerticalScrollBarEnabled = false
        isHorizontalScrollBarEnabled = false
        isScrollContainer = false
        setBackgroundColor(Color.TRANSPARENT)
        webViewClient = object : WebViewClient() {
            override fun onPageFinished(view: WebView, url: String) {
                resizeToContent()
            }
        }
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

    /** Re-renders this WebView with new [content]. */
    fun setContent(content: String, textColor: String) {
        render(content, textColor)
    }

    /** Measures the rendered content height and resizes this WebView to fit it,
     * so a WRAP_CONTENT bubble doesn't collapse to zero height. */
    private fun resizeToContent() {
        evaluateJavascript(HEIGHT_MEASURE_JS) { value ->
            try {
                val height = value
                    .trim()
                    .let { if (it.startsWith("\"") && it.endsWith("\"")) it.substring(1, it.length - 1) else it }
                    .toDoubleOrNull()
                    ?.toInt()
                    ?: return@evaluateJavascript
                post {
                    val params = layoutParams
                    if (params != null) {
                        params.height = height
                        layoutParams = params
                        requestLayout()
                    }
                }
            } catch (e: Exception) {
                // Leave the existing height unchanged.
            }
        }
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
        return try {
            val stream = context.assets.open("markdown/renderer.html")
            stream.use {
                val buffer = java.io.ByteArrayOutputStream()
                val chunk = ByteArray(4096)
                while (true) {
                    val n = it.read(chunk)
                    if (n < 0) break
                    buffer.write(chunk, 0, n)
                }
                String(buffer.toByteArray(), Charsets.UTF_8)
            }
        } catch (e: IOException) {
            throw IllegalStateException("Failed to read bundled renderer.html", e)
        }
    }
}
