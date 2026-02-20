package com.example.mycloset.ui.shared

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.example.mycloset.R
import com.google.android.material.button.MaterialButton

class SharedOutfitAdapter(
    private val onOpenDetails: (SharedOutfitRow) -> Unit,
    private val onSuggest: (SharedOutfitRow) -> Unit
) : ListAdapter<SharedOutfitRow, SharedOutfitAdapter.VH>(DIFF) {

    companion object {
        private val DIFF = object : DiffUtil.ItemCallback<SharedOutfitRow>() {
            override fun areItemsTheSame(oldItem: SharedOutfitRow, newItem: SharedOutfitRow): Boolean {
                return oldItem.ownerUid == newItem.ownerUid && oldItem.outfitId == newItem.outfitId
            }
            override fun areContentsTheSame(oldItem: SharedOutfitRow, newItem: SharedOutfitRow): Boolean {
                return oldItem == newItem
            }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): VH {
        val v = LayoutInflater.from(parent.context).inflate(R.layout.item_shared_outfit, parent, false)
        return VH(v)
    }

    override fun onBindViewHolder(holder: VH, position: Int) {
        holder.bind(getItem(position))
    }

    inner class VH(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val tvTitle: TextView = itemView.findViewById(R.id.tvSharedOutfitTitle)
        private val tvSub: TextView = itemView.findViewById(R.id.tvSharedOutfitSub)
        private val btnSuggest: MaterialButton = itemView.findViewById(R.id.btnSuggestOutfit)

        fun bind(row: SharedOutfitRow) {
            tvTitle.text = row.outfitName.ifBlank { "Shared Outfit" }
            tvSub.text = "Shared by: ${row.sharedBy.ifBlank { row.ownerUid }}"

            itemView.setOnClickListener { onOpenDetails(row) }
            btnSuggest.setOnClickListener { onSuggest(row) }
        }
    }
}
