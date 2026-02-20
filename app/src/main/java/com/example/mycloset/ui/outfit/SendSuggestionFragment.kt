package com.example.mycloset.ui.outfit

import android.os.Bundle
import android.view.View
import android.widget.Button
import android.widget.EditText
import android.widget.ProgressBar
import android.widget.TextView
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import com.example.mycloset.R
import com.example.mycloset.data.model.Outfit
import com.example.mycloset.data.model.OutfitSuggestion
import com.example.mycloset.data.repository.GrantsRepository
import com.example.mycloset.data.repository.SuggestionsRepository
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await

class SendSuggestionFragment : Fragment(R.layout.fragment_send_suggestion) {

    private val db by lazy { FirebaseFirestore.getInstance() }
    private val suggestionsRepo = SuggestionsRepository()
    private val grantsRepo = GrantsRepository()

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val ownerUid = arguments?.getString("ownerUid").orEmpty()
        val outfitId = arguments?.getString("outfitId").orEmpty()

        val tvTitle = view.findViewById<TextView>(R.id.tvSendSugTitle)
        val tvSub = view.findViewById<TextView>(R.id.tvSendSugSub)
        val etNote = view.findViewById<EditText>(R.id.etSendSugNote)
        val btnSend = view.findViewById<Button>(R.id.btnSendSug)
        val progress = view.findViewById<ProgressBar>(R.id.progressSendSug)

        if (ownerUid.isBlank() || outfitId.isBlank()) {
            Toast.makeText(requireContext(), "Missing ownerUid/outfitId", Toast.LENGTH_SHORT).show()
            findNavController().popBackStack()
            return
        }

        val myUid = FirebaseAuth.getInstance().currentUser?.uid
        if (myUid.isNullOrBlank()) {
            Toast.makeText(requireContext(), "Please login first", Toast.LENGTH_SHORT).show()
            findNavController().popBackStack()
            return
        }

        tvTitle.text = "Send Suggestion"
        tvSub.text = "Suggest changes to this outfit"

        fun setLoading(loading: Boolean) {
            progress.visibility = if (loading) View.VISIBLE else View.GONE
            btnSend.isEnabled = !loading
        }

        btnSend.setOnClickListener {
            lifecycleScope.launch {
                try {
                    setLoading(true)

                    //   Permission check (must have SUGGEST_OUTFIT)
                    val allowed = grantsRepo.hasGrant(
                        ownerUid = ownerUid,
                        granteeUid = myUid,
                        resourceType = "OUTFIT",
                        resourceId = outfitId,
                        permission = "SUGGEST_OUTFIT"
                    )

                    if (!allowed) {
                        Toast.makeText(requireContext(), "No permission to suggest for this outfit", Toast.LENGTH_LONG).show()
                        findNavController().popBackStack()
                        return@launch
                    }

                    //  Load outfit (owner side) to copy itemIds
                    val outfitDoc = db.collection("users")
                        .document(ownerUid)
                        .collection("outfits")
                        .document(outfitId)
                        .get()
                        .await()

                    val outfit = outfitDoc.toObject(Outfit::class.java)
                    val suggestedItemIds = outfit?.itemIds ?: emptyList()

                    if (suggestedItemIds.isEmpty()) {
                        Toast.makeText(requireContext(), "Outfit has no items", Toast.LENGTH_SHORT).show()
                        return@launch
                    }

                    //  Create suggestion
                    val suggestion = OutfitSuggestion(
                        suggestionId = "",
                        ownerUid = ownerUid,
                        targetType = "OUTFIT",
                        targetId = outfitId,
                        suggesterUid = myUid,
                        suggestedItemIds = suggestedItemIds,
                        note = etNote.text.toString().trim(),
                        status = "PENDING",
                        createdAt = com.google.firebase.Timestamp.now()
                    )


                    suggestionsRepo.createSuggestion(suggestion)

                    Toast.makeText(requireContext(), "Suggestion sent ✅", Toast.LENGTH_SHORT).show()
                    findNavController().popBackStack()

                } catch (e: Exception) {
                    Toast.makeText(requireContext(), "Error: ${e.message}", Toast.LENGTH_LONG).show()
                } finally {
                    setLoading(false)
                }
            }
        }
    }
}
