package com.example.mycloset.ui.outfit

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.TextView
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.example.mycloset.R
import com.example.mycloset.data.model.OutfitSuggestion

class SuggestionsAdapter(
    private val onOpen: (OutfitSuggestion) -> Unit,
    private val onAccept: (OutfitSuggestion) -> Unit,
    private val onReject: (OutfitSuggestion) -> Unit
) : ListAdapter<OutfitSuggestion, SuggestionsAdapter.VH>(DIFF) {

    companion object {
        private val DIFF = object : DiffUtil.ItemCallback<OutfitSuggestion>() {
            override fun areItemsTheSame(oldItem: OutfitSuggestion, newItem: OutfitSuggestion) =
                oldItem.suggestionId == newItem.suggestionId

            override fun areContentsTheSame(oldItem: OutfitSuggestion, newItem: OutfitSuggestion) =
                oldItem == newItem
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): VH {
        val v = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_suggestion, parent, false)
        return VH(v)
    }

    override fun onBindViewHolder(holder: VH, position: Int) {
        holder.bind(getItem(position))
    }

    inner class VH(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val tvTitle: TextView = itemView.findViewById(R.id.tvSuggestionRowTitle)
        private val tvSub: TextView = itemView.findViewById(R.id.tvSuggestionRowSub)
        private val btnOpen: Button = itemView.findViewById(R.id.btnOpenSuggestion)
        private val btnAccept: Button = itemView.findViewById(R.id.btnAcceptRow)
        private val btnReject: Button = itemView.findViewById(R.id.btnRejectRow)

        fun bind(s: OutfitSuggestion) {
            val from = s.suggesterUid.ifBlank { "Friend" }
            tvTitle.text = "From: $from • ${s.status}"

            val notePart = if (s.note.isBlank()) "" else " • ${s.note}"
            tvSub.text = "Items: ${s.suggestedItemIds.size}$notePart"

            btnOpen.setOnClickListener { onOpen(s) }

            val pending = (s.status == "PENDING")
            btnAccept.isEnabled = pending
            btnReject.isEnabled = pending

            btnAccept.setOnClickListener { onAccept(s) }
            btnReject.setOnClickListener { onReject(s) }
        }
    }
}
