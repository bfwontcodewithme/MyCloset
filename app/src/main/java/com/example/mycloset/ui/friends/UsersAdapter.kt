package com.example.mycloset.ui.friends

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.example.mycloset.R
import com.example.mycloset.data.model.User

class UsersAdapter(
    private var items: List<User> = emptyList(),
    private val isFriend: (User) -> Boolean,
    private val onAdd: (User) -> Unit,
    private val onRemove: (User) -> Unit,
    private val onPick: ((User) -> Unit)? = null // לשימוש במסך Share
) : RecyclerView.Adapter<UsersAdapter.VH>() {

    fun submitList(newItems: List<User>) {
        items = newItems
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): VH {
        val v = LayoutInflater.from(parent.context).inflate(R.layout.item_user_row, parent, false)
        return VH(v)
    }

    override fun onBindViewHolder(holder: VH, position: Int) = holder.bind(items[position])
    override fun getItemCount(): Int = items.size

    inner class VH(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val tvName: TextView = itemView.findViewById(R.id.tvName)
        private val tvEmail: TextView = itemView.findViewById(R.id.tvEmail)
        private val btn: Button = itemView.findViewById(R.id.btnAction)

        fun bind(u: User) {
            tvName.text = u.userName.ifBlank { "No name" }
            tvEmail.text = u.userEmail

            val friend = isFriend(u)
            btn.text = if (friend) "Remove" else "Add"

            btn.setOnClickListener {
                if (friend) onRemove(u) else onAdd(u)
            }

            itemView.setOnClickListener {
                onPick?.invoke(u)
            }
        }
    }
}
