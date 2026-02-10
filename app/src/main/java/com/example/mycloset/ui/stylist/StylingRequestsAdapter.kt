package com.example.mycloset.ui.stylist

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.example.mycloset.R
import java.text.SimpleDateFormat
import java.util.Locale
import android.content.Intent
import android.net.Uri


class StylingRequestsAdapter(
    private val onAccept: (docId: String) -> Unit,
    private val onDone: (docId: String) -> Unit
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
        holder.bind(getItem(position), onAccept, onDone)
    }

    class VH(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val tvEmail: TextView = itemView.findViewById(R.id.tvReqEmail)
        private val tvStatus: TextView = itemView.findViewById(R.id.tvReqStatus)
        private val tvDate: TextView = itemView.findViewById(R.id.tvReqDate)
        private val tvNote: TextView = itemView.findViewById(R.id.tvReqNote)

        private val btnCopyEmail: Button = itemView.findViewById(R.id.btnCopyEmail)
        private val btnAccept: Button = itemView.findViewById(R.id.btnAccept)
        private val btnDone: Button = itemView.findViewById(R.id.btnDone)

        private val fmt = SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault())
        private val btnEmailCustomer: Button = itemView.findViewById(R.id.btnEmailCustomer)


        fun bind(item: StylingRequestWithId, onAccept: (String) -> Unit, onDone: (String) -> Unit) {
            val r = item.req
            val ctx = itemView.context

            val email = r.fromEmail.trim()
            tvEmail.text = if (email.isBlank()) "(no email)" else email
            tvStatus.text = "Status: ${r.status}"

            val dateText = r.createdAt?.toDate()?.let { fmt.format(it) } ?: "-"
            tvDate.text = "Date: $dateText"

            // NOTE
            val noteText = r.note.trim()
            if (noteText.isBlank()) {
                tvNote.visibility = View.GONE
            } else {
                tvNote.visibility = View.VISIBLE
                tvNote.text = "Note: $noteText"
            }

            //  Copy Email
            btnCopyEmail.isEnabled = email.isNotBlank()
            btnCopyEmail.setOnClickListener {
                if (email.isBlank()) return@setOnClickListener
                copyToClipboard(ctx, "email", email)
                Toast.makeText(ctx, "Email copied ✅", Toast.LENGTH_SHORT).show()
            }
            btnEmailCustomer.isEnabled = email.isNotBlank()
            btnEmailCustomer.setOnClickListener {
                if (email.isBlank()) return@setOnClickListener

                val subject = Uri.encode("MyCloset Styling Request")
                val body = Uri.encode(
                    "Hi,\n\nI received your styling request.\n\n" +
                            "Your note: ${if (noteText.isBlank()) "(empty)" else noteText}\n\n" +
                            "Reply here with any details you want to add.\n\n" +
                            "— Stylist"
                )

                val uri = Uri.parse("mailto:$email?subject=$subject&body=$body")
                val intent = Intent(Intent.ACTION_SENDTO, uri)

                // פותח אפליקציית מייל אם קיימת
                if (intent.resolveActivity(ctx.packageManager) != null) {
                    ctx.startActivity(intent)
                } else {
                    Toast.makeText(ctx, "No email app found", Toast.LENGTH_SHORT).show()
                }
            }


            // reset actions
            btnAccept.text = "Accept"
            btnDone.text = "Done"
            btnAccept.setOnClickListener(null)
            btnDone.setOnClickListener(null)

            when (r.status) {
                "OPEN" -> {
                    btnAccept.visibility = View.VISIBLE
                    btnDone.visibility = View.GONE
                    btnAccept.isEnabled = true
                    btnAccept.setOnClickListener { onAccept(item.docId) }
                }
                "IN_PROGRESS" -> {
                    btnAccept.visibility = View.GONE
                    btnDone.visibility = View.VISIBLE
                    btnDone.isEnabled = true
                    btnDone.text = "Done"
                    btnDone.setOnClickListener { onDone(item.docId) }
                }
                "DONE" -> {
                    btnAccept.visibility = View.GONE
                    btnDone.visibility = View.VISIBLE
                    btnDone.text = "Done ✓"
                    btnDone.isEnabled = false
                }
                else -> {
                    btnAccept.visibility = View.GONE
                    btnDone.visibility = View.GONE
                }
            }
        }

        private fun copyToClipboard(context: Context, label: String, text: String) {
            val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
            clipboard.setPrimaryClip(ClipData.newPlainText(label, text))
        }
    }
}
