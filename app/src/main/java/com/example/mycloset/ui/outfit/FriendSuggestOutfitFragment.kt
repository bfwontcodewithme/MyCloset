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
import com.example.mycloset.data.model.OutfitSuggestion
import com.example.mycloset.data.repository.SuggestionsRepository
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await

class FriendSuggestOutfitFragment : Fragment(R.layout.fragment_friend_suggest_outfit) {

    private val suggestionsRepo = SuggestionsRepository()
    private val db by lazy { FirebaseFirestore.getInstance() }

    private lateinit var rv: RecyclerView
    private lateinit var progress: ProgressBar
    private lateinit var tvEmpty: TextView
    private lateinit var adapter: SuggestionsAdapter

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        rv = view.findViewById(R.id.rvSuggestions)
        progress = view.findViewById(R.id.progressSuggestions)
        tvEmpty = view.findViewById(R.id.tvEmptySuggestions)

        adapter = SuggestionsAdapter(
            onOpen = { sug ->
                val b = Bundle().apply {
                    putString("ownerUid", sug.ownerUid)
                    putString("outfitId", sug.outfitId)
                    putString("suggestionId", sug.suggestionId)
                }
                findNavController().navigate(R.id.nav_suggestion_details, b)
            },
            onAccept = { sug ->
                applySuggestion(
                    ownerUid = sug.ownerUid,
                    outfitId = sug.outfitId,
                    suggestionId = sug.suggestionId,
                    suggestedItemIds = sug.suggestedItemIds
                )
            },
            onReject = { sug ->
                updateStatus(sug.suggestionId, "REJECTED")
            }
        )

        rv.layoutManager = LinearLayoutManager(requireContext())
        rv.adapter = adapter

        loadInbox()
    }

    override fun onResume() {
        super.onResume()
        loadInbox()
    }

    private fun loadInbox() {
        val ownerUid = FirebaseAuth.getInstance().currentUser?.uid
        if (ownerUid == null) {
            tvEmpty.visibility = View.VISIBLE
            tvEmpty.text = "Please login first"
            return
        }

        lifecycleScope.launch {
            try {
                progress.visibility = View.VISIBLE

                val list: List<OutfitSuggestion> = suggestionsRepo.getOwnerInbox(ownerUid)
                adapter.submitList(list)

                tvEmpty.visibility = if (list.isEmpty()) View.VISIBLE else View.GONE
                if (list.isEmpty()) tvEmpty.text = "No suggestions"

            } catch (e: Exception) {
                Toast.makeText(requireContext(), "Error: ${e.message}", Toast.LENGTH_LONG).show()
            } finally {
                progress.visibility = View.GONE
            }
        }
    }

    private fun updateStatus(suggestionId: String, status: String) {
        lifecycleScope.launch {
            try {
                progress.visibility = View.VISIBLE
                suggestionsRepo.updateStatus(suggestionId, status)
                loadInbox()
            } catch (e: Exception) {
                Toast.makeText(requireContext(), "Error: ${e.message}", Toast.LENGTH_LONG).show()
            } finally {
                progress.visibility = View.GONE
            }
        }
    }

    private fun applySuggestion(
        ownerUid: String,
        outfitId: String,
        suggestionId: String,
        suggestedItemIds: List<String>
    ) {
        lifecycleScope.launch {
            try {
                progress.visibility = View.VISIBLE

                // 1) Update outfit items
                db.collection("users")
                    .document(ownerUid)
                    .collection("outfits")
                    .document(outfitId)
                    .update("itemIds", suggestedItemIds)
                    .await()

                // 2) Mark suggestion accepted
                suggestionsRepo.updateStatus(suggestionId, "ACCEPTED")

                Toast.makeText(requireContext(), "Suggestion applied ✅", Toast.LENGTH_SHORT).show()
                loadInbox()

            } catch (e: Exception) {
                Toast.makeText(requireContext(), "Error: ${e.message}", Toast.LENGTH_LONG).show()
            } finally {
                progress.visibility = View.GONE
            }
        }
    }
}
