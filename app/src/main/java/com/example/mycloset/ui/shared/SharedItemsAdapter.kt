package com.example.mycloset.ui.shared

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import coil.load
import com.example.mycloset.R
import com.example.mycloset.data.model.SharedItem

class SharedItemsAdapter : RecyclerView.Adapter<SharedItemsAdapter.VH>() {

    private val items = mutableListOf<SharedItem>()

    fun submitList(newList: List<SharedItem>) {
        items.clear()
        items.addAll(newList)
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): VH {
        val v = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_shared_item, parent, false)
        return VH(v)
    }

    override fun onBindViewHolder(holder: VH, position: Int) {
        holder.bind(items[position])
    }

    override fun getItemCount() = items.size

    inner class VH(itemView: View) : RecyclerView.ViewHolder(itemView) {

        private val ivImage: ImageView = itemView.findViewById(R.id.ivItemImage)
        private val tvName: TextView = itemView.findViewById(R.id.tvItemName)
        private val tvSub: TextView = itemView.findViewById(R.id.tvItemSub)

        fun bind(item: SharedItem) {
            tvName.text = item.name
            tvSub.text = "${item.type} • ${item.color}"

            ivImage.load(item.imageUrl) {
                placeholder(R.drawable.ic_launcher_background)
                crossfade(true)
            }
        }
    }
}
