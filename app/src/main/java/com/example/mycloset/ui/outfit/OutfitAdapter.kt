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

class OutfitAdapter(
    private val onClick: (Outfit) -> Unit = {}
) : ListAdapter<Outfit, OutfitAdapter.VH>(DIFF) {

    companion object {
        private val DIFF = object : DiffUtil.ItemCallback<Outfit>() {
            override fun areItemsTheSame(oldItem: Outfit, newItem: Outfit) = oldItem.outfitId == newItem.outfitId
            override fun areContentsTheSame(oldItem: Outfit, newItem: Outfit) = oldItem == newItem
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): VH {
        val v = LayoutInflater.from(parent.context).inflate(R.layout.item_outfit, parent, false)
        return VH(v)
    }

    override fun onBindViewHolder(holder: VH, position: Int) = holder.bind(getItem(position))

    inner class VH(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val tvName = itemView.findViewById<TextView>(R.id.tvOutfitName)
        private val tvMeta = itemView.findViewById<TextView>(R.id.tvOutfitMeta)

        fun bind(outfit: Outfit) {
            tvName.text = outfit.name
            tvMeta.text = "Items: ${outfit.itemIds.size}"
            itemView.setOnClickListener { onClick(outfit) }
        }
    }
}