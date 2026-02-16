package com.example.mycloset.ui.outfit

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.example.mycloset.R
import com.example.mycloset.data.model.Outfit

data class OutfitRowUi(
    val outfit: Outfit,
    val pendingCount: Int = 0
)

class OutfitAdapter(
    private val onClick: (OutfitRowUi) -> Unit,
    private val onOpenSuggestions: (OutfitRowUi) -> Unit
) : ListAdapter<OutfitRowUi, OutfitAdapter.VH>(DIFF) {

    companion object {
        private val DIFF = object : DiffUtil.ItemCallback<OutfitRowUi>() {
            override fun areItemsTheSame(oldItem: OutfitRowUi, newItem: OutfitRowUi): Boolean {
                return oldItem.outfit.outfitId == newItem.outfit.outfitId
            }

            override fun areContentsTheSame(oldItem: OutfitRowUi, newItem: OutfitRowUi): Boolean {
                return oldItem == newItem
            }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): VH {
        val v = LayoutInflater.from(parent.context).inflate(R.layout.item_outfit, parent, false)
        return VH(v)
    }

    override fun onBindViewHolder(holder: VH, position: Int) {
        holder.bind(getItem(position))
    }

    inner class VH(itemView: View) : RecyclerView.ViewHolder(itemView) {

        private val tvName: TextView = itemView.findViewById(R.id.tvOutfitName)
        private val tvSub: TextView = itemView.findViewById(R.id.tvOutfitSub)
        private val tvBadge: TextView = itemView.findViewById(R.id.tvPendingBadge)

        fun bind(row: OutfitRowUi) {
            tvName.text = row.outfit.name.ifBlank { "Outfit" }

            if (row.pendingCount > 0) {
                tvBadge.visibility = View.VISIBLE
                tvBadge.text = row.pendingCount.toString()
                tvSub.text = "${row.pendingCount} suggestion(s) pending"

                tvBadge.setOnClickListener { onOpenSuggestions(row) }
            } else {
                tvBadge.visibility = View.GONE
                tvSub.text = "No pending suggestions"
                tvBadge.setOnClickListener(null)
            }

            itemView.setOnClickListener { onClick(row) }
        }
    }
}
