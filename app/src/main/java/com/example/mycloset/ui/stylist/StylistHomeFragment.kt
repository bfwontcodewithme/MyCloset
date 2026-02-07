package com.example.mycloset.ui.stylist

import android.os.Bundle
import android.view.View
import android.widget.Button
import android.widget.TextView
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.mycloset.R
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query

class StylistHomeFragment : Fragment(R.layout.fragment_stylist_home) {

    private val auth by lazy { FirebaseAuth.getInstance() }
    private val db by lazy { FirebaseFirestore.getInstance() }

    private lateinit var rv: RecyclerView
    private lateinit var tvEmpty: TextView
    private lateinit var adapter: StylingRequestsAdapter

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val btnLogout = view.findViewById<Button>(R.id.btnStylistLogout)

        rv = view.findViewById(R.id.rvStylingRequests)
        tvEmpty = view.findViewById(R.id.tvEmptyRequests)

        adapter = StylingRequestsAdapter(mutableListOf<StylingRequest>())
        rv.layoutManager = LinearLayoutManager(requireContext())
        rv.adapter = adapter

        btnLogout.setOnClickListener {
            auth.signOut()
            findNavController().navigate(R.id.action_global_login)
        }

        loadMyRequests()
    }

    private fun loadMyRequests() {
        val myUid = auth.currentUser?.uid ?: return

        db.collection("styling_requests")
            .whereEqualTo("stylistId", myUid)
            .orderBy("createdAt", Query.Direction.DESCENDING)
            .get()
            .addOnSuccessListener { snap ->
                val requests = snap.toObjects(StylingRequest::class.java)

                adapter.setData(requests)

                val isEmpty = requests.isEmpty()
                tvEmpty.visibility = if (isEmpty) View.VISIBLE else View.GONE
                rv.visibility = if (isEmpty) View.GONE else View.VISIBLE
            }

            .addOnFailureListener { e ->
                tvEmpty.visibility = View.VISIBLE
                rv.visibility = View.GONE
                tvEmpty.text = "Failed: ${e.message}"
            }

    }
}
