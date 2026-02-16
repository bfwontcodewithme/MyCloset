package com.example.mycloset.ui.outfit

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
import com.example.mycloset.data.model.Outfit
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await

class OutfitListFragment : Fragment(R.layout.fragment_outfit_list) {

    private val db by lazy { FirebaseFirestore.getInstance() }

    private lateinit var rv: RecyclerView
    private lateinit var progress: ProgressBar
    private lateinit var tvEmpty: TextView
    private lateinit var adapter: OutfitAdapter   // ✅ חשוב – לא OutfitListAdapter

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        rv = view.findViewById(R.id.rvOutfits)
        progress = view.findViewById(R.id.progressOutfits)
        tvEmpty = view.findViewById(R.id.tvEmptyOutfits)

        adapter = OutfitAdapter(
            onClick = { row ->
                val b = Bundle().apply {
                    putString("outfitId", row.outfit.outfitId)
                }
                findNavController().navigate(R.id.nav_outfit_details, b)
            },
            onOpenSuggestions = { row ->
                val b = Bundle().apply {
                    putString("outfitId", row.outfit.outfitId)
                }
                findNavController().navigate(R.id.nav_suggestions_inbox, b)
            }
        )

        rv.layoutManager = LinearLayoutManager(requireContext())
        rv.adapter = adapter

        loadOutfitsWithBadges()
    }

    override fun onResume() {
        super.onResume()
        loadOutfitsWithBadges()
    }

    private fun setLoading(loading: Boolean) {
        progress.visibility = if (loading) View.VISIBLE else View.GONE
    }

    private fun loadOutfitsWithBadges() {
        val ownerUid = FirebaseAuth.getInstance().currentUser?.uid
        if (ownerUid == null) {
            tvEmpty.visibility = View.VISIBLE
            tvEmpty.text = "Please login first"
            return
        }

        lifecycleScope.launch {
            try {
                setLoading(true)
                tvEmpty.visibility = View.GONE

                val snap = db.collection("users")
                    .document(ownerUid)
                    .collection("outfits")
                    .orderBy("createdAt")
                    .get()
                    .await()

                val outfits = snap.documents.mapNotNull { doc ->
                    doc.toObject(Outfit::class.java)
                        ?.copy(outfitId = doc.id, ownerUid = ownerUid)
                }

                if (outfits.isEmpty()) {
                    adapter.submitList(emptyList())
                    tvEmpty.visibility = View.VISIBLE
                    tvEmpty.text = "No outfits yet"
                    return@launch
                }

                // ✅ חשוב – coroutineScope כדי ש־async יעבוד תקין
                val rows = coroutineScope {
                    outfits.map { o ->
                        async {
                            val sSnap = db.collection("users")
                                .document(ownerUid)
                                .collection("outfits")
                                .document(o.outfitId)
                                .collection("suggestions")
                                .whereEqualTo("status", "PENDING")
                                .get()
                                .await()

                            OutfitRowUi(
                                outfit = o,
                                pendingCount = sSnap.size()
                            )
                        }
                    }.awaitAll()
                }

                adapter.submitList(rows)

            } catch (e: Exception) {
                Toast.makeText(requireContext(), "Error: ${e.message}", Toast.LENGTH_LONG).show()
            } finally {
                setLoading(false)
            }
        }
    }
}
