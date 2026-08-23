package com.pirouette.nyagent.presentation.adapter

import android.graphics.Typeface
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.RecyclerView
import com.pirouette.nyagent.R
import com.pirouette.nyagent.application.model.SavedStoryModel

/**
 * Renders saved conversations in the left pane. A tap on a row invokes
 * [onSelect] so the host can load that conversation.
 */
class ConversationAdapter(
    private val conversations: List<SavedStoryModel>,
    private val activeConversationId: String?,
    private val titleFor: (SavedStoryModel) -> String,
    private val onSelect: (SavedStoryModel) -> Unit,
    private val onDelete: (SavedStoryModel) -> Unit
) : RecyclerView.Adapter<ConversationAdapter.ViewHolder>() {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.list_item, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val conversation = conversations[position]
        holder.textView.text = titleFor(conversation)
        val isActive = conversation.name == activeConversationId
        holder.itemView.isSelected = isActive
        holder.textView.setTypeface(null, if (isActive) Typeface.BOLD else Typeface.NORMAL)
        holder.textView.setTextColor(
            ContextCompat.getColor(holder.itemView.context, R.color.panel_item_text)
        )
    }

    override fun getItemCount(): Int = conversations.size

    inner class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val textView: TextView = itemView.findViewById(R.id.tvLoadItem)

        init {
            itemView.setOnClickListener {
                val position = adapterPosition
                if (position != RecyclerView.NO_POSITION) {
                    onSelect(conversations[position])
                }
            }
            itemView.setOnLongClickListener {
                val position = adapterPosition
                if (position != RecyclerView.NO_POSITION) {
                    onDelete(conversations[position])
                }
                true
            }
        }
    }
}
