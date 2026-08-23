package com.pirouette.nyagent.presentation.widget

import android.content.Context
import android.graphics.Color
import android.webkit.WebView
import android.webkit.WebViewClient
import java.io.IOException

/**
 * A reusable Markdown + LaTeX renderer built on a local [WebView]. KaTeX and
 * markdown-it are inlined into the bundled `assets/markdown/renderer.html` so
 * everything renders fully offline with no dependency on asset-relative loading.
 *
 * Each instance renders one assistant response. The renderer HTML is read from
 * the packaged APK assets and the model text is embedded as a JSON string
 * literal, then the document is loaded. If the JavaScript renderer ever fails,
 * the page falls back to showing the plain text so the bubble is never empty.
 */
class MarkdownWebView(context: Context, content: String, textColor: String) : WebView(context) {

    private companion object {
        const val BASE_URL = "file:///android_asset/markdown/"
        const val MIME_TYPE = "text/html"
        const val ENCODING = "utf-8"
        const val HEIGHT_MEASURE_JS =
            "(function(){var d=document.getElementById('content');" +
            "var h=Math.ceil(document.body.scrollHeight);" +
            "if(!h||h<10){h=Math.ceil(d.getBoundingClientRect().height);}" +
            "return h>0?h:400;})();"
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
                // Layout may not be final yet; give the renderer a beat to
                // run, then measure and size the bubble to its content.
                view.postDelayed({ resizeToContent() }, 250L)
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

    /**
     * Measures the rendered content height and resizes this WebView to fit it,
     * so the wrapping bubble grows to the markdown instead of collapsing empty.
     */
    private fun resizeToContent() {
        evaluateJavascript(HEIGHT_MEASURE_JS) { value ->
            try {
                val height = parseMeasuredHeight(value)
                if (height <= 0) return@evaluateJavascript
                post {
                    val params = layoutParams
                    if (params != null) {
                        params.height = height
                        layoutParams = params
                        requestLayout()
                    }
                }
            } catch (e: Exception) {
                // Leave the existing height unchanged; the plain-text fallback
                // in the page means the bubble still shows content.
            }
        }
    }

    /** Parses the JS measurement result, tolerating JSON-style quotes. */
    private fun parseMeasuredHeight(value: String): Int {
        val cleaned = value.trim()
            .let { if (it.startsWith("\"") && it.endsWith("\"")) it.substring(1, it.length - 1) else it }
            .trim()
        return cleaned.toDoubleOrNull()?.toInt() ?: 0
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
                val chunk = ByteArray(8192)
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
