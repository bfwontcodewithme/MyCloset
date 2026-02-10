package com.example.mycloset.ui.requests

import android.os.Bundle
import android.view.View
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.Fragment
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

    private var reg: ListenerRegistration? = null

    // cache: stylistId -> email
    private val stylistEmailCache = mutableMapOf<String, String>()

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        rv = view.findViewById(R.id.rvMyRequests)
        tvEmpty = view.findViewById(R.id.tvEmptyMyRequests)

        adapter = MyRequestsAdapter { item ->
            confirmCancel(item)
        }

        rv.layoutManager = LinearLayoutManager(requireContext())
        rv.adapter = adapter

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

                val stylistIds = raw.map { it.second.stylistId }.filter { it.isNotBlank() }.distinct()
                fetchStylistEmailsIfNeeded(stylistIds) {
                    val uiList = raw.map { (docId, r) ->
                        MyRequestUI(
                            docId = docId,
                            stylistId = r.stylistId,
                            stylistEmail = stylistEmailCache[r.stylistId].orEmpty(),
                            status = r.status,
                            note = r.note,
                            createdAt = r.createdAt
                        )
                    }

                    adapter.submitList(uiList)

                    val empty = uiList.isEmpty()
                    rv.visibility = if (empty) View.GONE else View.VISIBLE
                    tvEmpty.visibility = if (empty) View.VISIBLE else View.GONE
                    if (empty) tvEmpty.text = "No requests yet"
                }
            }
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

    private fun confirmCancel(item: MyRequestUI) {
        AlertDialog.Builder(requireContext())
            .setTitle("Cancel request?")
            .setMessage("Are you sure you want to cancel this request?\nThis cannot be undone.")
            .setPositiveButton("Yes, cancel") { _, _ ->
                deleteRequest(item.docId)
            }
            .setNegativeButton("No", null)
            .show()
    }

    private fun deleteRequest(docId: String) {
        db.collection("styling_requests")
            .document(docId)
            .delete()
            .addOnSuccessListener { toast("Request cancelled ✅") }
            .addOnFailureListener { e -> toast("Failed: ${e.message}") }
    }

    private fun toast(msg: String) {
        Toast.makeText(requireContext(), msg, Toast.LENGTH_SHORT).show()
    }
}
