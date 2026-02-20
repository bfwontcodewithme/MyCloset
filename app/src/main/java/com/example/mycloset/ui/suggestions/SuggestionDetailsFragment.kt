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

        val suggestionId = arguments?.getString("suggestionId").orEmpty()
        if (suggestionId.isBlank()) {
            Toast.makeText(requireContext(), "Missing suggestionId", Toast.LENGTH_SHORT).show()
            findNavController().popBackStack()
            return
        }

        val tvTitle = view.findViewById<TextView>(R.id.tvSugTitle)
        val tvSub = view.findViewById<TextView>(R.id.tvSugSub)
        val btnAccept = view.findViewById<Button>(R.id.btnSugAccept)
        val btnReject = view.findViewById<Button>(R.id.btnSugReject)
        val progress = view.findViewById<ProgressBar>(R.id.progressSugDetails)

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

                // ✅ Fallback למסמכים ישנים
                val tType = if (s.targetType.isNotBlank()) s.targetType else "OUTFIT"
                val tId = if (s.targetId.isNotBlank()) s.targetId else s.outfitId

                tvTitle.text = "Suggestion • ${s.status}"
                tvSub.text =
                    "From: ${s.suggesterUid.ifBlank { "Friend" }}\n" +
                            "Target: $tType • $tId\n" +
                            "Items: ${s.suggestedItemIds.size}\n" +
                            "Note: ${s.note.ifBlank { "-" }}"

                val pending = s.status == "PENDING"
                val isOwner = myUid == s.ownerUid

                btnAccept.isEnabled = isOwner && pending
                btnReject.isEnabled = isOwner && pending

                btnAccept.setOnClickListener {
                    if (!isOwner || !pending) return@setOnClickListener

                    lifecycleScope.launch {
                        try {
                            setLoading(true)

                            val newName = when (tType) {
                                "CLOSET" -> if (s.note.isNotBlank()) "Closet suggestion • ${s.note.take(18)}" else "Closet suggestion"
                                else -> if (s.note.isNotBlank()) "Outfit suggestion • ${s.note.take(18)}" else "Outfit suggestion"
                            }

                            val newOutfit = Outfit(
                                ownerUid = s.ownerUid,
                                name = newName,
                                itemIds = s.suggestedItemIds
                            )

                            outfitsRepo.addOutfit(s.ownerUid, newOutfit)
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
                    if (!isOwner || !pending) return@setOnClickListener

                    lifecycleScope.launch {
                        try {
                            setLoading(true)
                            repo.updateStatus(suggestionId, "DECLINED")
                            Toast.makeText(requireContext(), "Declined ✅", Toast.LENGTH_SHORT).show()
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
