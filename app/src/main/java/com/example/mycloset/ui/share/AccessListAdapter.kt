package com.example.mycloset.ui.share

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.example.mycloset.R
import com.example.mycloset.data.model.AccessGrant

class AccessListAdapter(
    private val onRevoke: (AccessGrant) -> Unit
) : RecyclerView.Adapter<AccessListAdapter.VH>() {

    private val list = mutableListOf<AccessGrant>()

    fun submitList(newList: List<AccessGrant>) {
        list.clear()
        list.addAll(newList)
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): VH {
        val v = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_access_grant, parent, false)
        return VH(v)
    }

    override fun onBindViewHolder(holder: VH, position: Int) {
        holder.bind(list[position])
    }

    override fun getItemCount() = list.size

    inner class VH(itemView: View) : RecyclerView.ViewHolder(itemView) {

        private val tvEmail: TextView = itemView.findViewById(R.id.tvGrantEmail)
        private val btnRevoke: Button = itemView.findViewById(R.id.btnRevoke)

        fun bind(grant: AccessGrant) {
            tvEmail.text = grant.granteeEmail.ifBlank { grant.granteeUid }
            btnRevoke.setOnClickListener { onRevoke(grant) }
        }
    }
}
