package com.example.mycloset.ui.friends

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.TextView
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.example.mycloset.R
import com.example.mycloset.data.model.FriendRequest

class FriendRequestsAdapter(
    private val onAccept: (FriendRequest) -> Unit,
    private val onDecline: (FriendRequest) -> Unit
) : ListAdapter<FriendRequest, FriendRequestsAdapter.VH>(DIFF) {

    companion object {
        private val DIFF = object : DiffUtil.ItemCallback<FriendRequest>() {
            override fun areItemsTheSame(oldItem: FriendRequest, newItem: FriendRequest) =
                oldItem.requestId == newItem.requestId

            override fun areContentsTheSame(oldItem: FriendRequest, newItem: FriendRequest) =
                oldItem == newItem
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): VH {
        val v = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_request_row, parent, false)
        return VH(v)
    }

    override fun onBindViewHolder(holder: VH, position: Int) {
        holder.bind(getItem(position))
    }

    inner class VH(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val tvTitle: TextView = itemView.findViewById(R.id.tvReqTitle)
        private val tvSub: TextView = itemView.findViewById(R.id.tvReqSub)
        private val btnAccept: Button = itemView.findViewById(R.id.btnAccept)
        private val btnDecline: Button = itemView.findViewById(R.id.btnDecline)

        fun bind(r: FriendRequest) {
            tvTitle.text = "Request from ${r.fromEmail.ifBlank { r.fromUid }}"
            tvSub.text = "Status: ${r.status}"

            val pending = r.status == "PENDING"
            btnAccept.isEnabled = pending
            btnDecline.isEnabled = pending

            btnAccept.setOnClickListener { onAccept(r) }
            btnDecline.setOnClickListener { onDecline(r) }
        }
    }
}
