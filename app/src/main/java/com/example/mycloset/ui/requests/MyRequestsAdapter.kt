package com.example.mycloset.ui.requests

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.TextView
import androidx.navigation.Navigation
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.example.mycloset.R
import java.text.SimpleDateFormat
import java.util.Locale

class MyRequestsAdapter(
    private val onCancel: (MyRequestUI) -> Unit
) : ListAdapter<MyRequestUI, MyRequestsAdapter.VH>(Diff) {

    object Diff : DiffUtil.ItemCallback<MyRequestUI>() {
        override fun areItemsTheSame(oldItem: MyRequestUI, newItem: MyRequestUI) =
            oldItem.docId == newItem.docId

        override fun areContentsTheSame(oldItem: MyRequestUI, newItem: MyRequestUI) =
            oldItem == newItem
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): VH {
        val v = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_my_request, parent, false)
        return VH(v)
    }

    override fun onBindViewHolder(holder: VH, position: Int) {
        holder.bind(getItem(position), onCancel)
    }

    class VH(itemView: View) : RecyclerView.ViewHolder(itemView) {

        private val tvStatus: TextView = itemView.findViewById(R.id.tvMyReqStatus)
        private val tvDate: TextView = itemView.findViewById(R.id.tvMyReqDate)
        private val tvNote: TextView = itemView.findViewById(R.id.tvMyReqNote)
        private val tvStylist: TextView = itemView.findViewById(R.id.tvMyReqStylist)

        private val btnCancel: Button = itemView.findViewById(R.id.btnCancelRequest)

        // ✅ חשוב: ב-item_my_request.xml צריך להיות כפתור עם id הזה
        private val btnChat: Button = itemView.findViewById(R.id.btnOpenChat)

        private val fmt = SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault())

        fun bind(r: MyRequestUI, onCancel: (MyRequestUI) -> Unit) {

            tvStatus.text = "Status: ${r.status}"

            val dateText = r.createdAt?.toDate()?.let { fmt.format(it) } ?: "-"
            tvDate.text = "Date: $dateText"

            tvNote.text = if (r.note.isBlank()) "Note: (empty)" else "Note: ${r.note}"

            val stylistText = if (r.stylistEmail.isBlank()) "(unknown stylist)" else r.stylistEmail
            tvStylist.text = "Stylist: $stylistText"

            // Cancel only when OPEN
            if (r.status == "OPEN") {
                btnCancel.visibility = View.VISIBLE
                btnCancel.isEnabled = true
                btnCancel.setOnClickListener { onCancel(r) }
            } else {
                btnCancel.visibility = View.GONE
                btnCancel.setOnClickListener(null)
            }

            // ✅ Chat
            btnChat.setOnClickListener {
                val b = Bundle().apply {
                    putString("requestId", r.docId) // docId של הבקשה
                }

                Navigation.findNavController(itemView)
                    .navigate(R.id.nav_chat, b)

            }
        }
    }
}
