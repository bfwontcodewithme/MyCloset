package com.example.mycloset.ui.requests

import android.os.Bundle
import android.view.View
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.mycloset.R
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration
import com.google.firebase.firestore.Query

class MyRequestsFragment : Fragment(R.layout.fragment_my_requests) {

    private val auth by lazy { FirebaseAuth.getInstance() }
    private val db by lazy { FirebaseFirestore.getInstance() }

    private lateinit var rv: RecyclerView
    private lateinit var tvEmpty: TextView
    private lateinit var adapter: MyRequestsAdapter

    // ✅ NEW: filter buttons
    private lateinit var btnActive: Button
    private lateinit var btnHistory: Button

    private var reg: ListenerRegistration? = null

    // cache: stylistId -> email
    private val stylistEmailCache = mutableMapOf<String, String>()

    // ✅ NEW: keep full list and filter on UI
    private var allUi: List<MyRequestUI> = emptyList()
    private var currentFilter: String = "ACTIVE" // "ACTIVE" / "HISTORY"

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        rv = view.findViewById(R.id.rvMyRequests)
        tvEmpty = view.findViewById(R.id.tvEmptyMyRequests)

        btnActive = view.findViewById(R.id.btnMyReqActive)
        btnHistory = view.findViewById(R.id.btnMyReqHistory)

        adapter = MyRequestsAdapter(
            onCancel = { item -> confirmCancel(item) },
            onOpenChat = { item -> openChat(item.docId) }
        )

        rv.layoutManager = LinearLayoutManager(requireContext())
        rv.adapter = adapter

        // default
        setSelectedFilterUI()

        btnActive.setOnClickListener {
            currentFilter = "ACTIVE"
            setSelectedFilterUI()
            applyFilterAndRender()
        }

        btnHistory.setOnClickListener {
            currentFilter = "HISTORY"
            setSelectedFilterUI()
            applyFilterAndRender()
        }

        listenToMyRequests()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        reg?.remove()
        reg = null
    }

    private fun listenToMyRequests() {
        val uid = auth.currentUser?.uid ?: run {
            tvEmpty.visibility = View.VISIBLE
            tvEmpty.text = "Not logged in"
            rv.visibility = View.GONE
            return
        }

        tvEmpty.visibility = View.VISIBLE
        tvEmpty.text = "Loading..."
        rv.visibility = View.GONE

        reg?.remove()
        reg = db.collection("styling_requests")
            .whereEqualTo("fromUserId", uid)
            .orderBy("createdAt", Query.Direction.DESCENDING)
            .addSnapshotListener { snap, err ->
                if (err != null) {
                    tvEmpty.visibility = View.VISIBLE
                    tvEmpty.text = "Failed: ${err.message}"
                    rv.visibility = View.GONE
                    return@addSnapshotListener
                }

                val docs = snap?.documents.orEmpty()
                val raw = docs.map { doc ->
                    val r = doc.toObject(MyRequestItem::class.java) ?: MyRequestItem()
                    doc.id to r
                }

                val stylistIds = raw.map { it.second.stylistId }
                    .filter { it.isNotBlank() }
                    .distinct()

                fetchStylistEmailsIfNeeded(stylistIds) {
                    val uiList = raw.map { (docId, r) ->
                        MyRequestUI(
                            docId = docId, // ✅ requestId for chat
                            stylistId = r.stylistId,
                            stylistEmail = stylistEmailCache[r.stylistId].orEmpty(),
                            status = r.status,
                            note = r.note,
                            createdAt = r.createdAt,

                            // ✅ IMPORTANT: map chat summary + unread too
                            lastMessage = r.lastMessage,
                            lastMessageAt = r.lastMessageAt,
                            lastSenderId = r.lastSenderId,
                            unreadForUser = r.unreadForUser,
                            unreadForStylist = r.unreadForStylist
                        )
                    }

                    allUi = uiList
                    applyFilterAndRender()
                }
            }
    }

    private fun applyFilterAndRender() {
        val filtered = when (currentFilter) {
            "ACTIVE" -> allUi.filter { it.status == "OPEN" || it.status == "IN_PROGRESS" }
            "HISTORY" -> allUi.filter { it.status == "DONE" || it.status == "CANCELLED" }
            else -> allUi
        }

        adapter.submitList(filtered)

        val empty = filtered.isEmpty()
        rv.visibility = if (empty) View.GONE else View.VISIBLE
        tvEmpty.visibility = if (empty) View.VISIBLE else View.GONE

        if (empty) {
            tvEmpty.text = when (currentFilter) {
                "ACTIVE" -> "No active requests"
                "HISTORY" -> "No past requests"
                else -> "No requests"
            }
        }
    }

    private fun setSelectedFilterUI() {
        // optional UI feedback without changing styles.xml:
        btnActive.isEnabled = currentFilter != "ACTIVE"
        btnHistory.isEnabled = currentFilter != "HISTORY"
    }

    private fun fetchStylistEmailsIfNeeded(stylistIds: List<String>, onDone: () -> Unit) {
        val toFetch = stylistIds.filter { id -> !stylistEmailCache.containsKey(id) }
        if (toFetch.isEmpty()) {
            onDone()
            return
        }

        var remaining = toFetch.size
        for (id in toFetch) {
            db.collection("users").document(id).get()
                .addOnSuccessListener { doc ->
                    stylistEmailCache[id] = doc.getString("email").orEmpty()
                }
                .addOnFailureListener {
                    stylistEmailCache[id] = ""
                }
                .addOnCompleteListener {
                    remaining--
                    if (remaining == 0) onDone()
                }
        }
    }

    private fun openChat(requestId: String) {
        val args = Bundle().apply {
            putString("requestId", requestId)
        }
        findNavController().navigate(R.id.nav_chat, args)
    }

    private fun confirmCancel(item: MyRequestUI) {
        AlertDialog.Builder(requireContext())
            .setTitle("Cancel request?")
            .setMessage("Are you sure you want to cancel this request?\nThis cannot be undone.")
            .setPositiveButton("Yes, cancel") { _, _ ->
                cancelRequest(item.docId)
            }
            .setNegativeButton("No", null)
            .show()
    }

    private fun cancelRequest(docId: String) {
        db.collection("styling_requests")
            .document(docId)
            .update("status", "CANCELLED")
            .addOnSuccessListener {
                toast("Request cancelled ✅")
                // optional: switch to history so user sees it moved
                // currentFilter = "HISTORY"; setSelectedFilterUI(); applyFilterAndRender()
            }
            .addOnFailureListener { e -> toast("Failed: ${e.message}") }
    }

    private fun toast(msg: String) {
        Toast.makeText(requireContext(), msg, Toast.LENGTH_SHORT).show()
    }
}
