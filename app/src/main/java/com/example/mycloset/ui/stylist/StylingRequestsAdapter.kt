package com.example.mycloset.ui.stylist

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.example.mycloset.R
import java.text.SimpleDateFormat
import java.util.Locale

class StylingRequestsAdapter(
    private val items: MutableList<StylingRequest>
) : RecyclerView.Adapter<StylingRequestsAdapter.VH>() {

    private val df = SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault())

    class VH(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val tvEmail: TextView = itemView.findViewById(R.id.tvReqEmail)
        val tvStatus: TextView = itemView.findViewById(R.id.tvReqStatus)
        val tvDate: TextView = itemView.findViewById(R.id.tvReqDate)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): VH {
        val v = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_styling_request, parent, false)
        return VH(v)
    }

    override fun onBindViewHolder(holder: VH, position: Int) {
        val item = items[position]
        holder.tvEmail.text = item.fromEmail.ifBlank { "(no email)" }
        holder.tvStatus.text = "Status: ${item.status}"
        val dateText = item.createdAt?.toDate()?.let { df.format(it) } ?: "-"
        holder.tvDate.text = "Date: $dateText"
    }

    override fun getItemCount() = items.size

    fun setData(newItems: List<StylingRequest>) {
        items.clear()
        items.addAll(newItems)
        notifyDataSetChanged()
    }
}
