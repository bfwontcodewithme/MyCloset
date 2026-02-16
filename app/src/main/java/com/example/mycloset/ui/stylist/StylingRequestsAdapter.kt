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
    private val onChat: (StylingRequestWithId) -> Unit
) : ListAdapter<StylingRequestWithId, StylingRequestsAdapter.VH>(Diff) {

    object Diff : DiffUtil.ItemCallback<StylingRequestWithId>() {
        override fun areItemsTheSame(oldItem: StylingRequestWithId, newItem: StylingRequestWithId) =
            oldItem.docId == newItem.docId

        override fun areContentsTheSame(oldItem: StylingRequestWithId, newItem: StylingRequestWithId) =
            oldItem == newItem
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): VH {
        val v = LayoutInflater.from(parent.context)
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

        // ✅ new
        private val tvLastMessage: TextView = itemView.findViewById(R.id.tvReqLastMessage)
        private val tvLastTime: TextView = itemView.findViewById(R.id.tvReqLastTime)
        private val tvUnread: TextView = itemView.findViewById(R.id.tvReqUnread)

        private val btnAccept: Button = itemView.findViewById(R.id.btnAccept)
        private val btnDone: Button = itemView.findViewById(R.id.btnDone)
        private val btnChat: Button = itemView.findViewById(R.id.btnChat)

        private val fmtDate = SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault())
        private val fmtTime = SimpleDateFormat("HH:mm", Locale.getDefault())

        fun bind(
            item: StylingRequestWithId,
            onAccept: (String) -> Unit,
            onDone: (String) -> Unit,
            onChat: (StylingRequestWithId) -> Unit
        ) {
            val docId = item.docId
            val r = item.req

            tvEmail.text = r.fromEmail.ifBlank { "(unknown)" }
            tvStatus.text = "Status: ${r.status}"

            val dateText = r.createdAt?.toDate()?.let { fmtDate.format(it) } ?: "-"
            tvDate.text = "Date: $dateText"

            if (r.note.isBlank()) {
                tvNote.visibility = View.GONE
            } else {
                tvNote.visibility = View.VISIBLE
                tvNote.text = "Note: ${r.note}"
            }

            // ✅ last message preview
            tvLastMessage.text = if (r.lastMessage.isBlank()) "(no messages yet)" else r.lastMessage

            val t = r.lastMessageAt?.toDate()
            if (t == null) {
                tvLastTime.visibility = View.GONE
            } else {
                tvLastTime.visibility = View.VISIBLE
                tvLastTime.text = fmtTime.format(t)
            }

            // ✅ unread badge for stylist
            val unread = r.unreadForStylist
            if (unread > 0) {
                tvUnread.visibility = View.VISIBLE
                tvUnread.text = unread.toString()
            } else {
                tvUnread.visibility = View.GONE
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

            btnChat.setOnClickListener { onChat(item) }
        }
    }
}
