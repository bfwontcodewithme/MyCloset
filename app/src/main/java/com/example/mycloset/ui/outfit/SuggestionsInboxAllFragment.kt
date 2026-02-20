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
import com.example.mycloset.data.model.OutfitSuggestion
import com.example.mycloset.data.repository.OutfitsRepository
import com.example.mycloset.data.repository.SuggestionsRepository
import com.google.firebase.auth.FirebaseAuth
import kotlinx.coroutines.launch

class SuggestionsInboxAllFragment : Fragment(R.layout.fragment_suggestions_inbox) {

    private val suggestionsRepo = SuggestionsRepository()
    private val outfitsRepo = OutfitsRepository()

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
            onOpen = { s ->
                val b = Bundle().apply {
                    putString("ownerUid", s.ownerUid)
                    putString("outfitId", s.outfitId)
                    putString("suggestionId", s.suggestionId)
                }
                findNavController().navigate(R.id.nav_suggestion_details, b)
            },
            onAccept = { s -> acceptSuggestionCreateNewOutfit(s) },
            onReject = { s -> updateStatus(s.suggestionId, "REJECTED") }
        )

        rv.layoutManager = LinearLayoutManager(requireContext())
        rv.adapter = adapter

        loadAllPendingSuggestions()
    }

    override fun onResume() {
        super.onResume()
        loadAllPendingSuggestions()
    }

    private fun loadAllPendingSuggestions() {
        val ownerUid = FirebaseAuth.getInstance().currentUser?.uid
        if (ownerUid == null) {
            tvEmpty.visibility = View.VISIBLE
            tvEmpty.text = "Please login first"
            return
        }

        lifecycleScope.launch {
            try {
                progress.visibility = View.VISIBLE
                tvEmpty.visibility = View.GONE

                val list: List<OutfitSuggestion> = suggestionsRepo.getOwnerInbox(ownerUid)

                adapter.submitList(list)
                tvEmpty.visibility = if (list.isEmpty()) View.VISIBLE else View.GONE
                if (list.isEmpty()) tvEmpty.text = "No pending suggestions ✅"

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
                loadAllPendingSuggestions()
            } catch (e: Exception) {
                Toast.makeText(requireContext(), "Error: ${e.message}", Toast.LENGTH_LONG).show()
            } finally {
                progress.visibility = View.GONE
            }
        }
    }

    private fun acceptSuggestionCreateNewOutfit(s: OutfitSuggestion) {
        val ownerUid = FirebaseAuth.getInstance().currentUser?.uid ?: return
        if (s.ownerUid != ownerUid) return

        lifecycleScope.launch {
            try {
                progress.visibility = View.VISIBLE

                val newName = if (s.note.isNotBlank()) {
                    "From friend • ${s.note.take(18)}"
                } else {
                    "From friend"
                }

                val newOutfit = Outfit(
                    ownerUid = ownerUid,
                    name = newName,
                    itemIds = s.suggestedItemIds
                )

                outfitsRepo.addOutfit(ownerUid, newOutfit)
                suggestionsRepo.updateStatus(s.suggestionId, "ACCEPTED")

                Toast.makeText(requireContext(), "Accepted ✅ New outfit created", Toast.LENGTH_SHORT).show()
                loadAllPendingSuggestions()

            } catch (e: Exception) {
                Toast.makeText(requireContext(), "Error: ${e.message}", Toast.LENGTH_LONG).show()
            } finally {
                progress.visibility = View.GONE
            }
        }
    }
}
