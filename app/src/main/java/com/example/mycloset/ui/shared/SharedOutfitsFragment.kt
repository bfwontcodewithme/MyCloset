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
import com.example.mycloset.data.model.Outfit
import com.example.mycloset.data.repository.GrantsRepository
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await

class SharedOutfitsFragment : Fragment(R.layout.fragment_shared_outfits) {

    private val grantsRepo = GrantsRepository()
    private val db by lazy { FirebaseFirestore.getInstance() }

    private lateinit var rv: RecyclerView
    private lateinit var progress: ProgressBar
    private lateinit var tvEmpty: TextView

    private lateinit var adapter: SharedOutfitAdapter

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        rv = view.findViewById(R.id.rvSharedOutfits)
        progress = view.findViewById(R.id.progressSharedOutfits)
        tvEmpty = view.findViewById(R.id.tvEmptySharedOutfits)

        adapter = SharedOutfitAdapter { row ->
            val args = Bundle().apply {
                putString("ownerUid", row.ownerUid)
                putString("outfitId", row.outfitId)
            }
            findNavController().navigate(R.id.nav_shared_outfit_details, args)
        }

        rv.layoutManager = LinearLayoutManager(requireContext())
        rv.adapter = adapter

        loadSharedOutfits()
    }

    override fun onResume() {
        super.onResume()
        loadSharedOutfits()
    }

    private fun setLoading(loading: Boolean) {
        progress.visibility = if (loading) View.VISIBLE else View.GONE
    }

    private fun loadSharedOutfits() {
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

                // 1) להביא Grants שקיבלתי לאאוטפיטים עם SUGGEST_OUTFIT
                val grants: List<AccessGrant> = grantsRepo.getSharedOutfitGrantsForMe(myUid)

                if (grants.isEmpty()) {
                    adapter.submitList(emptyList())
                    tvEmpty.visibility = View.VISIBLE
                    tvEmpty.text = "No shared outfits yet"
                    return@launch
                }

                // 2) לכל grant → נטען את האאוטפיט עצמו מה-owner
                val rows = grants.map { g ->
                    async {
                        val doc = db.collection("users")
                            .document(g.ownerUid)
                            .collection("outfits")
                            .document(g.resourceId)
                            .get()
                            .await()

                        val outfit = doc.toObject(Outfit::class.java)
                        if (outfit == null) null
                        else SharedOutfitRow(
                            ownerUid = g.ownerUid,
                            outfitId = g.resourceId,
                            outfitName = outfit.name,
                            sharedBy = g.ownerUid // בשלב 1 מציגים ownerUid (אפשר לשפר לשם/מייל בשלב 2)
                        )
                    }
                }.awaitAll().filterNotNull()

                adapter.submitList(rows)

                tvEmpty.visibility = if (rows.isEmpty()) View.VISIBLE else View.GONE
                if (rows.isEmpty()) tvEmpty.text = "No shared outfits found"

            } catch (e: Exception) {
                Toast.makeText(requireContext(), "Error: ${e.message}", Toast.LENGTH_LONG).show()
            } finally {
                setLoading(false)
            }
        }
    }
}
