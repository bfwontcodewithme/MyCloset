package com.example.mycloset.ui.stylist

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.TextView
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.example.mycloset.R
import java.text.SimpleDateFormat
import java.util.Locale

class StylingRequestsAdapter(
    private val onAccept: (String) -> Unit,
    private val onDone: (String) -> Unit,
    private val onChat: (String) -> Unit
) : ListAdapter<StylingRequestWithId, StylingRequestsAdapter.VH>(Diff) {

    object Diff : DiffUtil.ItemCallback<StylingRequestWithId>() {
        override fun areItemsTheSame(oldItem: StylingRequestWithId, newItem: StylingRequestWithId) =
            oldItem.docId == newItem.docId

        override fun areContentsTheSame(oldItem: StylingRequestWithId, newItem: StylingRequestWithId) =
            oldItem == newItem
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): VH {
        val v = LayoutInflater.from(parent.context)
            // ✅ לפי התיקייה שלך: res/layout/item_styling_request.xml
            .inflate(R.layout.item_styling_request, parent, false)
        return VH(v)
    }

    override fun onBindViewHolder(holder: VH, position: Int) {
        holder.bind(getItem(position), onAccept, onDone, onChat)
    }

    class VH(itemView: View) : RecyclerView.ViewHolder(itemView) {

        private val tvEmail: TextView = itemView.findViewById(R.id.tvReqEmail)
        private val tvStatus: TextView = itemView.findViewById(R.id.tvReqStatus)
        private val tvDate: TextView = itemView.findViewById(R.id.tvReqDate)
        private val tvNote: TextView = itemView.findViewById(R.id.tvReqNote)

        private val btnAccept: Button = itemView.findViewById(R.id.btnAccept)
        private val btnDone: Button = itemView.findViewById(R.id.btnDone)
        private val btnChat: Button = itemView.findViewById(R.id.btnChat)

        private val fmt = SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault())

        fun bind(
            item: StylingRequestWithId,
            onAccept: (String) -> Unit,
            onDone: (String) -> Unit,
            onChat: (String) -> Unit
        ) {
            val docId = item.docId
            val r = item.req

            tvEmail.text = r.fromEmail.ifBlank { "(unknown)" }
            tvStatus.text = "Status: ${r.status}"

            val dateText = r.createdAt?.toDate()?.let { fmt.format(it) } ?: "-"
            tvDate.text = "Date: $dateText"

            if (r.note.isBlank()) {
                tvNote.visibility = View.GONE
            } else {
                tvNote.visibility = View.VISIBLE
                tvNote.text = "Note: ${r.note}"
            }

            when (r.status) {
                "OPEN" -> {
                    btnAccept.visibility = View.VISIBLE
                    btnDone.visibility = View.GONE
                    btnAccept.setOnClickListener { onAccept(docId) }
                    btnDone.setOnClickListener(null)
                }
                "IN_PROGRESS" -> {
                    btnAccept.visibility = View.GONE
                    btnDone.visibility = View.VISIBLE
                    btnDone.setOnClickListener { onDone(docId) }
                    btnAccept.setOnClickListener(null)
                }
                else -> {
                    btnAccept.visibility = View.GONE
                    btnDone.visibility = View.GONE
                    btnAccept.setOnClickListener(null)
                    btnDone.setOnClickListener(null)
                }
            }

            btnChat.setOnClickListener { onChat(docId) }
        }
    }
}
