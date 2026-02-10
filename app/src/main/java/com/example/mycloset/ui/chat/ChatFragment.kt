package com.example.mycloset.ui.chat

import android.os.Bundle
import android.view.View
import android.widget.EditText
import android.widget.ImageButton
import android.widget.TextView
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.mycloset.R
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration
import com.google.firebase.firestore.Query

class ChatFragment : Fragment(R.layout.fragment_chat) {

    private val auth by lazy { FirebaseAuth.getInstance() }
    private val db by lazy { FirebaseFirestore.getInstance() }

    private lateinit var rv: RecyclerView
    private lateinit var tvEmpty: TextView
    private lateinit var etMsg: EditText
    private lateinit var btnSend: ImageButton

    private var reg: ListenerRegistration? = null
    private lateinit var adapter: ChatAdapter

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        rv = view.findViewById(R.id.rvChatMessages)
        tvEmpty = view.findViewById(R.id.tvChatEmpty)
        etMsg = view.findViewById(R.id.etChatMessage)
        btnSend = view.findViewById(R.id.btnChatSend)

        val myUid = auth.currentUser?.uid
        if (myUid == null) {
            toast("Not logged in")
            findNavController().navigateUp()
            return
        }

        val requestId = arguments?.getString("requestId").orEmpty()
        if (requestId.isBlank()) {
            toast("Missing requestId (chat can't open)")
            findNavController().navigateUp()
            return
        }

        adapter = ChatAdapter(myUid)
        rv.layoutManager = LinearLayoutManager(requireContext()).apply { stackFromEnd = true }
        rv.adapter = adapter

        listenMessages(requestId)

        btnSend.setOnClickListener {
            val text = etMsg.text?.toString()?.trim().orEmpty()
            if (text.isBlank()) return@setOnClickListener
            sendMessage(requestId, myUid, text)
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        reg?.remove()
        reg = null
    }

    private fun listenMessages(requestId: String) {
        tvEmpty.visibility = View.VISIBLE
        tvEmpty.text = "Loading chat..."
        rv.visibility = View.GONE

        reg?.remove()
        reg = db.collection("styling_requests")
            .document(requestId)
            .collection("messages")
            .orderBy("createdAt", Query.Direction.ASCENDING)
            .addSnapshotListener { snap, err ->
                if (err != null) {
                    tvEmpty.visibility = View.VISIBLE
                    tvEmpty.text = "Failed: ${err.message}"
                    rv.visibility = View.GONE
                    return@addSnapshotListener
                }

                val list = snap?.documents.orEmpty().mapNotNull { d ->
                    d.toObject(ChatMessage::class.java)
                }

                adapter.submitList(list)

                val empty = list.isEmpty()
                tvEmpty.visibility = if (empty) View.VISIBLE else View.GONE
                rv.visibility = if (empty) View.GONE else View.VISIBLE
                if (empty) tvEmpty.text = "No messages yet"

                if (!empty) rv.scrollToPosition(list.size - 1)
            }
    }

    private fun sendMessage(requestId: String, myUid: String, text: String) {
        val role = "STYLIST" // אם את רוצה דיוק לפי משתמש – תגידי ואני אעשה fetch מה-users/{uid}

        val data = hashMapOf(
            "senderId" to myUid,
            "senderRole" to role,
            "text" to text,
            "createdAt" to FieldValue.serverTimestamp()
        )

        db.collection("styling_requests")
            .document(requestId)
            .collection("messages")
            .add(data)
            .addOnSuccessListener {
                etMsg.setText("")
            }
            .addOnFailureListener { e ->
                toast("Send failed: ${e.message}")
            }
    }

    private fun toast(msg: String) {
        Toast.makeText(requireContext(), msg, Toast.LENGTH_SHORT).show()
    }
}
