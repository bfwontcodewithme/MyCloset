package com.example.mycloset.ui.outfit

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.example.mycloset.R
import com.example.mycloset.data.model.Item

class PickItemsAdapter(
    private val onSelectionChanged: (() -> Unit)? = null
) : ListAdapter<Item, PickItemsAdapter.VH>(DIFF) {

    private val selectedIds = linkedSetOf<String>()

    companion object {
        private val DIFF = object : DiffUtil.ItemCallback<Item>() {
            override fun areItemsTheSame(oldItem: Item, newItem: Item): Boolean =
                oldItem.itemId == newItem.itemId

            override fun areContentsTheSame(oldItem: Item, newItem: Item): Boolean =
                oldItem == newItem
        }
    }

    fun getSelectedIds(): List<String> = selectedIds.toList()

    fun setSelectedIds(ids: Set<String>) {
        selectedIds.clear()
        selectedIds.addAll(ids)
        notifyDataSetChanged()
        onSelectionChanged?.invoke()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): VH {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_pick_simple, parent, false)
        return VH(view)
    }

    override fun onBindViewHolder(holder: VH, position: Int) {
        holder.bind(getItem(position))
    }

    inner class VH(itemView: View) : RecyclerView.ViewHolder(itemView) {

        private val tvPickItem: TextView =
            itemView.findViewById(R.id.tvPickItem)

        fun bind(item: Item) {
            val isSelected = selectedIds.contains(item.itemId)

            tvPickItem.text =
                "${item.name} (${item.type}, ${item.color}, ${item.season})"

            itemView.alpha = if (isSelected) 0.6f else 1.0f
            itemView.setBackgroundResource(
                if (isSelected) R.drawable.bg_pick_selected
                else R.drawable.bg_pick_normal
            )

            itemView.setOnClickListener {
                if (selectedIds.contains(item.itemId)) {
                    selectedIds.remove(item.itemId)
                } else {
                    selectedIds.add(item.itemId)
                }

                val pos = adapterPosition
                if (pos != RecyclerView.NO_POSITION) {
                    notifyItemChanged(pos)
                }

                onSelectionChanged?.invoke()
            }
        }
    }
}