package com.pirouette.nyagent.presentation.adapter

import android.content.ClipboardManager
import android.content.Context
import android.graphics.Color
import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.FrameLayout
import android.widget.TextView
import android.widget.Toast
import com.google.gson.JsonParser
import androidx.recyclerview.widget.RecyclerView
import com.pirouette.nyagent.R
import com.pirouette.nyagent.presentation.widget.MarkdownWebView
import com.pirouette.nyagent.application.model.ChatMessageModel
import com.pirouette.nyagent.application.model.MessageAuthorModel

/**
 * Renders a list of [ChatMessageModel] as messenger-style bubbles. User bubbles
 * are right-aligned with a green tint; everything else is left-aligned. Every
 * bubble remains selectable so text can be drag-selected and copied. A long
 * press on a user bubble invokes [onUserMessageEdit] so the host can edit that
 * message and restart from there; a long press on any other bubble copies.
 */
class ChatAdapter(
    private val messages: List<ChatMessageModel>,
    private val onUserMessageEdit: ((Int) -> Unit)? = null
) : RecyclerView.Adapter<ChatAdapter.ViewHolder>() {

    private companion object {
        const val MAX_PREVIEW_LENGTH = 25
    }
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.chat_message, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val message = messages[position]
        if (message.author == MessageAuthorModel.ASSISTANT) {
            bindMarkdown(holder, message)
            return
        }
        holder.markdownContainer.visibility = View.GONE
        val existing = holder.markdownContainer.getTag(R.id.markdown_webview_tag) as MarkdownWebView?
        if (existing != null) {
            holder.markdownContainer.removeView(existing)
            holder.markdownContainer.setTag(R.id.markdown_webview_tag, null)
            holder.markdownContainer.setTag(R.id.markdown_content_tag, null)
        }
        holder.textView.visibility = View.VISIBLE
        holder.textView.text = displayText(message)
        holder.textView.setTextColor(Color.parseColor(message.author.hexColor()))
        holder.textView.setBackgroundResource(message.author.backgroundResource())

        val params = holder.textView.layoutParams as FrameLayout.LayoutParams
        params.gravity = if (message.author == MessageAuthorModel.USER) Gravity.END else Gravity.START
        holder.textView.layoutParams = params

        holder.textView.setOnLongClickListener {
            if (message.author == MessageAuthorModel.USER) {
                onUserMessageEdit?.invoke(position)
            } else {
                copyToClipboard(holder.textView.getContext(), message.content)
            }
            true
        }
    }

    /**
     * Shows [message] inside a Markdown + LaTeX WebView. The container uses the
     * assistant bubble background and holds a single WebView that is created once
     * and reused across binds of the same row.
     */
    private fun bindMarkdown(holder: ViewHolder, message: ChatMessageModel) {
        holder.textView.visibility = View.GONE
        holder.markdownContainer.visibility = View.VISIBLE
        val container = holder.markdownContainer
        val webView = container.getTag(R.id.markdown_webview_tag) as MarkdownWebView?
        val content = message.content
        if (webView != null && container.getTag(R.id.markdown_content_tag) == content) {
            return
        }
        if (webView != null) {
            container.removeView(webView)
        }
        val fresh = MarkdownWebView(container.getContext(), content, "#111111")
        // MATCH_PARENT width gives the WebView a real, non-collapsing measurement
        // so it renders instead of shrinking to an empty sliver.
        val lp = android.view.ViewGroup.LayoutParams(
            android.view.ViewGroup.LayoutParams.MATCH_PARENT,
            android.view.ViewGroup.LayoutParams.WRAP_CONTENT
        )
        container.addView(fresh, lp)
        container.setTag(R.id.markdown_webview_tag, fresh)
        container.setTag(R.id.markdown_content_tag, content)
    }


    override fun getItemCount(): Int = messages.size

    private fun displayText(message: ChatMessageModel): String {
        if (message.author == MessageAuthorModel.SYSTEM) {
            return "System: " + message.content
        }
        if (message.author != MessageAuthorModel.TOOL) {
            return message.content
        }
        val rest = message.content.removePrefix("Tool: ").trim()
        val open = rest.indexOf('(')
        val close = rest.lastIndexOf(')')
        val name = if (open > 0) rest.substring(0, open).trim() else rest
        val args = if (open > 0 && close > open) rest.substring(open + 1, close) else ""
        val value = extractValue(args)
        val preview = if (value.isNotEmpty()) "Tool: $name - $value" else "Tool: $name"
        return crop(preview)
    }

    /**
     * Extracts the payload value(s) from a JSON-encoded tool argument object,
     * dropping the braces and the key names so the bubble reads e.g.
     * `Tool: curl - https://google.com`.
     */
    private fun extractValue(args: String): String {
        val trimmed = args.trim()
        if (!trimmed.startsWith("{")) {
            return trimmed
        }
        return try {
            val obj = JsonParser.parseString(trimmed).asJsonObject
            val values = ArrayList<String>()
            for (entry in obj.entrySet()) {
                val raw = entry.value.toString()
                values.add(raw.removePrefix("\"").removeSuffix("\""))
            }
            values.joinToString(", ")
        } catch (e: Exception) {
            trimmed
        }
    }

    /** Truncates [text] to MAX_PREVIEW_LENGTH characters and appends an ellipsis. */
    private fun crop(text: String): String {
        if (text.length <= MAX_PREVIEW_LENGTH) {
            return text
        }
        return text.substring(0, MAX_PREVIEW_LENGTH).trimEnd() + "..."
    }


    /** Dark, readable bubble text colors per author. */
    private fun MessageAuthorModel.hexColor(): String = when (this) {
        MessageAuthorModel.USER -> "#113311"
        MessageAuthorModel.ASSISTANT -> "#111111"
        MessageAuthorModel.TOOL -> "#111111"
        MessageAuthorModel.SYSTEM -> "#333333"
    }

    /** Bubble background drawable resource per author. */
    private fun MessageAuthorModel.backgroundResource(): Int = when (this) {
        MessageAuthorModel.USER -> R.drawable.bubble_user_background
        MessageAuthorModel.ASSISTANT -> R.drawable.bubble_assistant_background
        MessageAuthorModel.TOOL -> R.drawable.bubble_tool_background
        MessageAuthorModel.SYSTEM -> R.drawable.bubble_system_background
    }

    private fun copyToClipboard(context: Context, text: String) {
        try {
            val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
            clipboard.setPrimaryClip(android.content.ClipData.newPlainText("message", text))
            Toast.makeText(context, "Message copied", Toast.LENGTH_SHORT).show()
        } catch (e: Exception) {
            Toast.makeText(context, "Copy failed", Toast.LENGTH_SHORT).show()
        }
    }

    inner class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val textView: TextView = itemView.findViewById(R.id.tvMessages)
        val markdownContainer: android.view.ViewGroup = itemView.findViewById(R.id.markdownContainer)
    }
}
