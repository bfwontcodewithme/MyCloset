package com.example.mycloset.ui.shared

import android.os.Bundle
import android.view.View
import android.widget.ProgressBar
import android.widget.TextView
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.mycloset.R
import com.example.mycloset.data.model.AccessGrant
import com.example.mycloset.data.repository.GrantsRepository
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await

class SharedClosetsFragment : Fragment(R.layout.fragment_shared_closets) {

    private val grantsRepo = GrantsRepository()
    private val db by lazy { FirebaseFirestore.getInstance() }

    private lateinit var rv: RecyclerView
    private lateinit var progress: ProgressBar
    private lateinit var tvEmpty: TextView
    private lateinit var adapter: SharedClosetAdapter

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        rv = view.findViewById(R.id.rvSharedClosets)
        progress = view.findViewById(R.id.progressSharedClosets)
        tvEmpty = view.findViewById(R.id.tvEmptySharedClosets)

        adapter = SharedClosetAdapter { row ->
            val args = Bundle().apply {
                putString("ownerUid", row.ownerUid)
                putString("closetId", row.closetId)
            }
            findNavController().navigate(R.id.nav_shared_closet_details, args)
        }

        rv.layoutManager = LinearLayoutManager(requireContext())
        rv.adapter = adapter

        loadSharedClosets()
    }

    override fun onResume() {
        super.onResume()
        loadSharedClosets()
    }

    private fun setLoading(loading: Boolean) {
        progress.visibility = if (loading) View.VISIBLE else View.GONE
    }

    private fun loadSharedClosets() {
        val myUid = FirebaseAuth.getInstance().currentUser?.uid
        if (myUid == null) {
            tvEmpty.visibility = View.VISIBLE
            tvEmpty.text = "Please login first"
            return
        }

        lifecycleScope.launch {
            try {
                setLoading(true)
                tvEmpty.visibility = View.GONE

                // 1) Grants שקיבלתי ל-CLOSET עם VIEW_ITEMS
                val grants: List<AccessGrant> = grantsRepo.getSharedClosetGrantsForMe(myUid)

                if (grants.isEmpty()) {
                    adapter.submitList(emptyList())
                    tvEmpty.visibility = View.VISIBLE
                    tvEmpty.text = "No shared closets yet"
                    return@launch
                }

                // 2) לכל grant נטען closetName מה-owner
                val rows = grants.map { g ->
                    async {
                        val closetDoc = db.collection("users")
                            .document(g.ownerUid)
                            .collection("closets")
                            .document(g.resourceId)
                            .get()
                            .await()

                        val closetName = closetDoc.getString("name")
                            ?: closetDoc.getString("closetName")
                            ?: g.resourceId

                        SharedClosetRow(
                            ownerUid = g.ownerUid,
                            closetId = g.resourceId,
                            closetName = closetName,
                            sharedBy = g.ownerUid
                        )
                    }
                }.awaitAll()

                adapter.submitList(rows)
                tvEmpty.visibility = if (rows.isEmpty()) View.VISIBLE else View.GONE

            } catch (e: Exception) {
                Toast.makeText(requireContext(), "Error: ${e.message}", Toast.LENGTH_LONG).show()
            } finally {
                setLoading(false)
            }
        }
    }
}
