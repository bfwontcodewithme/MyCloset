package com.example.mycloset.ui.chat

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.example.mycloset.R
import java.text.SimpleDateFormat
import java.util.Locale

class ChatAdapter(
    private val myUid: String
) : ListAdapter<ChatMessage, ChatAdapter.VH>(Diff) {

    object Diff : DiffUtil.ItemCallback<ChatMessage>() {

        override fun areItemsTheSame(
            oldItem: ChatMessage,
            newItem: ChatMessage
        ): Boolean {
            // מספיק לצ'אט פשוט
            return oldItem.text == newItem.text &&
                    oldItem.senderId == newItem.senderId &&
                    oldItem.createdAt == newItem.createdAt
        }

        override fun areContentsTheSame(
            oldItem: ChatMessage,
            newItem: ChatMessage
        ): Boolean {
            return oldItem == newItem
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): VH {
        val v = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_chat_message, parent, false)
        return VH(v)
    }

    override fun onBindViewHolder(holder: VH, position: Int) {
        holder.bind(getItem(position), myUid)
    }

    class VH(itemView: View) : RecyclerView.ViewHolder(itemView) {

        private val tvSender: TextView =
            itemView.findViewById(R.id.tvMsgSender)

        private val tvText: TextView =
            itemView.findViewById(R.id.tvMsgText)

        private val tvTime: TextView =
            itemView.findViewById(R.id.tvMsgTime)

        private val fmt =
            SimpleDateFormat("HH:mm", Locale.getDefault())

        fun bind(m: ChatMessage, myUid: String) {

            val isMe = m.senderId == myUid

            // מי שלח
            tvSender.text =
                if (isMe) "Me" else m.senderRole.ifBlank { "Other" }

            // תוכן
            tvText.text = m.text

            // שעה
            tvTime.text =
                m.createdAt?.toDate()?.let { fmt.format(it) } ?: ""

            // רקע פשוט לפי צד
            val bg = if (isMe)
                R.drawable.bg_chat_me
            else
                R.drawable.bg_chat_other

            itemView.setBackgroundResource(bg)
        }
    }
}
