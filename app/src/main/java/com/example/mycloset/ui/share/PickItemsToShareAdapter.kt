package com.example.mycloset.ui.share
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.CheckBox
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.example.mycloset.R
import com.example.mycloset.data.model.Item

class PickItemsAdapter(


        private val onSelectionChanged: (Set<String>) -> Unit
    ) : RecyclerView.Adapter<PickItemsAdapter.VH>() {

        private val items = mutableListOf<Item>()
        private val selectedIds = mutableSetOf<String>()

        fun submitList(newList: List<Item>) {
            items.clear()
            items.addAll(newList)
            selectedIds.clear()
            notifyDataSetChanged()
            onSelectionChanged(selectedIds)
        }

        fun getSelectedIds(): List<String> = selectedIds.toList()

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): VH {
            val v = LayoutInflater.from(parent.context).inflate(R.layout.item_pick_share, parent, false)
            return VH(v)
        }

        override fun getItemCount(): Int = items.size

        override fun onBindViewHolder(holder: VH, position: Int) {
            holder.bind(items[position])
        }

        inner class VH(itemView: View) : RecyclerView.ViewHolder(itemView) {
            private val cb: CheckBox = itemView.findViewById(R.id.cbPickItem)
            private val tvTitle: TextView = itemView.findViewById(R.id.tvPickItemTitle)
            private val tvSub: TextView = itemView.findViewById(R.id.tvPickItemSub)

            fun bind(item: Item) {
                tvTitle.text = item.name.ifBlank { item.id }
                tvSub.text = "${item.type} • ${item.color} • ${item.season}"

                cb.setOnCheckedChangeListener(null)
                cb.isChecked = selectedIds.contains(item.id)

                cb.setOnCheckedChangeListener { _, isChecked ->
                    if (isChecked) selectedIds.add(item.id) else selectedIds.remove(item.id)
                    onSelectionChanged(selectedIds)
                }

                itemView.setOnClickListener {
                    cb.isChecked = !cb.isChecked
                }
            }
        }
    }

