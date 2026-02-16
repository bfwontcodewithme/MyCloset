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

    private var myRole: String = "REGULAR"
    private var requestIdArg: String = ""      // מה שמגיע ב-args
    private var requestDocId: String = ""      // ה-docId האמיתי ב-Firestore

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

        requestIdArg = arguments?.getString("requestId").orEmpty()
        if (requestIdArg.isBlank()) {
            toast("Missing requestId")
            findNavController().navigateUp()
            return
        }

        adapter = ChatAdapter(myUid)
        rv.layoutManager = LinearLayoutManager(requireContext()).apply { stackFromEnd = true }
        rv.adapter = adapter

        showStatus("Opening chat...")

        loadMyRole(myUid)

        // ✅ כאן הקסם: פותחים צ'אט גם אם requestId הוא "legacy" עם _timestamp
        resolveRequestDocIdThenListen(myUid, requestIdArg)

        btnSend.setOnClickListener {
            val text = etMsg.text?.toString()?.trim().orEmpty()
            if (text.isBlank()) return@setOnClickListener
            if (requestDocId.isBlank()) {
                toast("Chat not ready yet")
                return@setOnClickListener
            }
            sendMessage(requestDocId, myUid, text)
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        reg?.remove()
        reg = null
    }

    private fun loadMyRole(myUid: String) {
        db.collection("users")
            .document(myUid)
            .get()
            .addOnSuccessListener { doc ->
                myRole = doc.getString("role") ?: "REGULAR"
            }
            .addOnFailureListener {
                myRole = "REGULAR"
            }
    }

    /**
     * ✅ 1) ננסה לפתוח ישירות לפי requestIdArg
     * ✅ 2) אם אין מסמך, ובקלט יש "_" (legacy id) → נפתור ל-docId אמיתי לפי fromUserId+stylistId
     */
    private fun resolveRequestDocIdThenListen(myUid: String, requestIdArg: String) {
        val directRef = db.collection("styling_requests").document(requestIdArg)

        directRef.get()
            .addOnSuccessListener { doc ->
                if (doc.exists()) {
                    requestDocId = doc.id
                    verifyParticipantThenListen(myUid, doc)
                } else {
                    // fallback: legacy id כמו from_stylist_timestamp
                    resolveLegacyIdByQuery(myUid, requestIdArg)
                }
            }
            .addOnFailureListener { e ->
                showStatus("Failed opening request: ${e.message}")
            }
    }

    private fun resolveLegacyIdByQuery(myUid: String, legacy: String) {
        val parts = legacy.split("_")
        if (parts.size < 2) {
            showStatus("Request not found\nrequestId=$legacy")
            return
        }

        val fromUserId = parts[0]
        val stylistId = parts[1]

        showStatus("Resolving chat...\n(fromUserId/stylistId)")

        db.collection("styling_requests")
            .whereEqualTo("fromUserId", fromUserId)
            .whereEqualTo("stylistId", stylistId)
            .orderBy("createdAt", Query.Direction.DESCENDING)
            .limit(1)
            .get()
            .addOnSuccessListener { snap ->
                val bestDoc = snap.documents.firstOrNull()
                if (bestDoc == null) {
                    showStatus("Request not found (legacy)\nrequestId=$legacy")
                    return@addOnSuccessListener
                }

                requestDocId = bestDoc.id
                verifyParticipantThenListen(myUid, bestDoc)
            }
            .addOnFailureListener { e ->
                showStatus("Failed resolving legacy id: ${e.message}")
            }
    }

    private fun verifyParticipantThenListen(myUid: String, doc: com.google.firebase.firestore.DocumentSnapshot) {
        val fromUserId = doc.getString("fromUserId").orEmpty()
        val stylistId = doc.getString("stylistId").orEmpty()

        val isParticipant = (myUid == fromUserId) || (myUid == stylistId)
        if (!isParticipant) {
            showStatus(
                "No permission for this chat\n" +
                        "myUid=$myUid\n" +
                        "fromUserId=$fromUserId\n" +
                        "stylistId=$stylistId"
            )
            return
        }

        // reset unread כשנכנסים
        val resetField = if (myUid == stylistId) "unreadForStylist" else "unreadForUser"
        db.collection("styling_requests").document(doc.id).update(resetField, 0)

        listenMessages(doc.id)
    }

    private fun listenMessages(requestDocId: String) {
        showStatus("Loading messages...")

        reg?.remove()
        reg = db.collection("styling_requests")
            .document(requestDocId)
            .collection("messages")
            .orderBy("createdAt", Query.Direction.ASCENDING)
            .addSnapshotListener { snap, err ->
                if (err != null) {
                    showStatus("Failed: ${err.message}")
                    return@addSnapshotListener
                }

                val list = snap?.documents.orEmpty().mapNotNull { d ->
                    d.toObject(ChatMessage::class.java)?.copy(id = d.id)
                }

                adapter.submitList(list)

                if (list.isEmpty()) {
                    showStatus("No messages yet")
                } else {
                    tvEmpty.visibility = View.GONE
                    rv.visibility = View.VISIBLE
                    rv.scrollToPosition(list.size - 1)
                }
            }
    }

    private fun sendMessage(requestDocId: String, myUid: String, text: String) {
        val msg = hashMapOf(
            "senderId" to myUid,
            "senderRole" to myRole,
            "text" to text,
            "createdAt" to FieldValue.serverTimestamp(),
            "seenBy" to listOf(myUid)
        )

        val reqRef = db.collection("styling_requests").document(requestDocId)

        reqRef.collection("messages")
            .add(msg)
            .addOnSuccessListener {
                etMsg.setText("")

                // ✅ summary + unread counter (לא קריטי לצ'אט עצמו)
                val unreadField = if (myRole == "STYLIST") "unreadForUser" else "unreadForStylist"
                reqRef.update(
                    mapOf(
                        "lastMessage" to text,
                        "lastMessageAt" to FieldValue.serverTimestamp(),
                        "lastSenderId" to myUid,
                        unreadField to FieldValue.increment(1)
                    )
                )
            }
            .addOnFailureListener { e ->
                toast("Send failed: ${e.message}")
            }
    }

    private fun showStatus(msg: String) {
        tvEmpty.visibility = View.VISIBLE
        tvEmpty.text = msg
        rv.visibility = View.GONE
    }

    private fun toast(msg: String) {
        Toast.makeText(requireContext(), msg, Toast.LENGTH_SHORT).show()
    }
}
