package com.pirouette.nyagent.presentation.adapter

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.RecyclerView
import com.pirouette.nyagent.R
import com.pirouette.nyagent.application.model.SavedStoryModel

/** Renders a list of saved stories with a currently selected one highlighted. */
class StoryAdapter(
    private val stories: List<SavedStoryModel>
) : RecyclerView.Adapter<StoryAdapter.ViewHolder>() {

    var selectedPosition = RecyclerView.NO_POSITION

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.list_item, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.textView.text = stories[position].name
        holder.itemView.setBackgroundColor(
            ContextCompat.getColor(
                holder.itemView.context,
                if (selectedPosition == position) R.color.purple_200 else android.R.color.black
            )
        )
    }

    override fun getItemCount(): Int = stories.size

    inner class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val textView: TextView = itemView.findViewById(R.id.tvLoadItem)

        init {
            itemView.setOnClickListener {
                val position = adapterPosition
                if (position != RecyclerView.NO_POSITION) {
                    selectedPosition = position
                    notifyDataSetChanged()
                }
            }
        }
    }
}
