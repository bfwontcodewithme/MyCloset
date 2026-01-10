package com.example.mycloset.ui.item

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.example.mycloset.R
import com.example.mycloset.data.model.Item

class ItemsAdapter(
    private val onClick: (Item) -> Unit = {}   // ✅ ברירת מחדל
) : ListAdapter<Item, ItemsAdapter.ItemVH>(DIFF) {

    companion object {
        private val DIFF = object : DiffUtil.ItemCallback<Item>() {
            override fun areItemsTheSame(oldItem: Item, newItem: Item): Boolean =
                oldItem.itemId == newItem.itemId

            override fun areContentsTheSame(oldItem: Item, newItem: Item): Boolean =
                oldItem == newItem
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ItemVH {
        val v = LayoutInflater.from(parent.context).inflate(R.layout.item_closet, parent, false)
        return ItemVH(v)
    }

    override fun onBindViewHolder(holder: ItemVH, position: Int) {
        holder.bind(getItem(position))
    }

    inner class ItemVH(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val tvName = itemView.findViewById<TextView>(R.id.tvItemName)
        private val tvMeta = itemView.findViewById<TextView>(R.id.tvItemMeta)
        private val img = itemView.findViewById<ImageView>(R.id.ivItem)

        fun bind(item: Item) {
            tvName.text = item.name
            tvMeta.text = "${item.type} • ${item.color} • ${item.season}"

            if (item.itemImageUri.isNotBlank()) {
                Glide.with(itemView)
                    .load(item.itemImageUri)
                    .placeholder(R.drawable.ic_launcher_foreground)
                    .error(R.drawable.ic_launcher_foreground)
                    .into(img)
            } else {
                img.setImageResource(R.drawable.ic_launcher_foreground)
            }

            itemView.setOnClickListener { onClick(item) }
        }
    }
}