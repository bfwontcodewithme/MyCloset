package com.example.mycloset.ui.friends

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.example.mycloset.R
import com.example.mycloset.data.model.User

class PickFriendAdapter(
    private val onPick: (User) -> Unit
) : ListAdapter<User, PickFriendAdapter.VH>(DIFF) {

    companion object {
        private val DIFF = object : DiffUtil.ItemCallback<User>() {
            override fun areItemsTheSame(oldItem: User, newItem: User) = oldItem.userUid == newItem.userUid
            override fun areContentsTheSame(oldItem: User, newItem: User) = oldItem == newItem
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): VH {
        val v = LayoutInflater.from(parent.context).inflate(R.layout.item_pick_friend, parent, false)
        return VH(v)
    }

    override fun onBindViewHolder(holder: VH, position: Int) = holder.bind(getItem(position))

    inner class VH(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val tvTitle: TextView = itemView.findViewById(R.id.tvFriendTitle)
        private val tvSub: TextView = itemView.findViewById(R.id.tvFriendSub)

        fun bind(u: User) {
            tvTitle.text = u.userName.ifBlank { u.userEmail.ifBlank { "Friend" } }
            tvSub.text = u.userEmail.ifBlank { u.userUid }
            itemView.setOnClickListener { onPick(u) }
        }
    }
}
