package com.example.mycloset.ui.closet

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageButton
import android.widget.PopupMenu
import android.widget.TextView
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.example.mycloset.R
import com.example.mycloset.data.model.Closet

class ClosetAdapter(
    private val onClick: (Closet) -> Unit = {},
    private val onRename: (Closet) -> Unit = {},
    private val onDelete: (Closet) -> Unit = {}
) : ListAdapter<Closet, ClosetAdapter.VH>(DIFF) {

    companion object {
        private val DIFF = object : DiffUtil.ItemCallback<Closet>() {
            override fun areItemsTheSame(oldItem: Closet, newItem: Closet) =
                oldItem.closetId == newItem.closetId

            override fun areContentsTheSame(oldItem: Closet, newItem: Closet) =
                oldItem == newItem
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): VH {
        val v = LayoutInflater.from(parent.context).inflate(R.layout.item_closet_row, parent, false)
        return VH(v)
    }

    override fun onBindViewHolder(holder: VH, position: Int) = holder.bind(getItem(position))

    inner class VH(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val tv = itemView.findViewById<TextView>(R.id.tvClosetName)
        private val menuBtn = itemView.findViewById<ImageButton>(R.id.btnClosetMenu)

        fun bind(c: Closet) {
            tv.text = c.closetName

            itemView.setOnClickListener { onClick(c) }

            menuBtn.setOnClickListener { anchor ->
                val popup = PopupMenu(anchor.context, anchor)
                popup.menu.add("Rename")
                popup.menu.add("Delete")
                popup.setOnMenuItemClickListener { item ->
                    when (item.title.toString()) {
                        "Rename" -> onRename(c)
                        "Delete" -> onDelete(c)
                    }
                    true
                }
                popup.show()
            }
        }
    }
}
