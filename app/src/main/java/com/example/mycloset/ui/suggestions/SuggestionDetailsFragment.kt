package com.example.mycloset.ui.suggestions

import android.os.Bundle
import android.view.View
import android.widget.Button
import android.widget.ProgressBar
import android.widget.TextView
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import com.example.mycloset.R
import com.example.mycloset.data.model.Outfit
import com.example.mycloset.data.repository.OutfitsRepository
import com.example.mycloset.data.repository.SuggestionsRepository
import com.google.firebase.auth.FirebaseAuth
import kotlinx.coroutines.launch

class SuggestionDetailsFragment : Fragment(R.layout.fragment_suggestion_details) {

    private val repo = SuggestionsRepository()
    private val outfitsRepo = OutfitsRepository()

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val ownerUid = arguments?.getString("ownerUid").orEmpty()
        val outfitId = arguments?.getString("outfitId").orEmpty()
        val suggestionId = arguments?.getString("suggestionId").orEmpty()

        val tvTitle = view.findViewById<TextView>(R.id.tvSugTitle)
        val tvSub = view.findViewById<TextView>(R.id.tvSugSub)
        val btnAccept = view.findViewById<Button>(R.id.btnSugAccept)
        val btnReject = view.findViewById<Button>(R.id.btnSugReject)
        val progress = view.findViewById<ProgressBar>(R.id.progressSugDetails)

        if (suggestionId.isBlank() || ownerUid.isBlank() || outfitId.isBlank()) {
            Toast.makeText(requireContext(), "Missing data", Toast.LENGTH_SHORT).show()
            findNavController().popBackStack()
            return
        }

        val myUid = FirebaseAuth.getInstance().currentUser?.uid.orEmpty()

        fun setLoading(isLoading: Boolean) {
            progress.visibility = if (isLoading) View.VISIBLE else View.GONE
            btnAccept.isEnabled = !isLoading
            btnReject.isEnabled = !isLoading
        }

        lifecycleScope.launch {
            try {
                setLoading(true)

                val s = repo.getById(suggestionId)
                if (s == null) {
                    Toast.makeText(requireContext(), "Suggestion not found", Toast.LENGTH_SHORT).show()
                    findNavController().popBackStack()
                    return@launch
                }

                tvTitle.text = "Suggestion • ${s.status}"
                tvSub.text =
                    "From UID: ${s.suggesterUid.ifBlank { "Friend" }}\n" +
                            "Items: ${s.suggestedItemIds.size}\n" +
                            "Note: ${s.note.ifBlank { "-" }}\n" +
                            "OutfitId: ${s.outfitId}"

                val pending = s.status == "PENDING"
                val isOwner = (myUid == ownerUid) && (s.ownerUid == ownerUid)

                btnAccept.isEnabled = isOwner && pending
                btnReject.isEnabled = isOwner && pending

                btnAccept.setOnClickListener {
                    if (!isOwner) return@setOnClickListener

                    lifecycleScope.launch {
                        try {
                            setLoading(true)

                            // ✅ Create NEW outfit (do not overwrite existing)
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

                            // ✅ mark accepted
                            repo.updateStatus(suggestionId, "ACCEPTED")

                            Toast.makeText(requireContext(), "Accepted ✅ New outfit created", Toast.LENGTH_SHORT).show()
                            findNavController().popBackStack()

                        } catch (e: Exception) {
                            Toast.makeText(requireContext(), "Error: ${e.message}", Toast.LENGTH_LONG).show()
                        } finally {
                            setLoading(false)
                        }
                    }
                }

                btnReject.setOnClickListener {
                    if (!isOwner) return@setOnClickListener

                    lifecycleScope.launch {
                        try {
                            setLoading(true)
                            repo.updateStatus(suggestionId, "REJECTED")
                            Toast.makeText(requireContext(), "Rejected ✅", Toast.LENGTH_SHORT).show()
                            findNavController().popBackStack()
                        } catch (e: Exception) {
                            Toast.makeText(requireContext(), "Error: ${e.message}", Toast.LENGTH_LONG).show()
                        } finally {
                            setLoading(false)
                        }
                    }
                }

            } catch (e: Exception) {
                Toast.makeText(requireContext(), "Error: ${e.message}", Toast.LENGTH_LONG).show()
            } finally {
                setLoading(false)
            }
        }
    }
}
