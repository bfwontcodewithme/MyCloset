package com.example.mycloset.ui.shared

import android.os.Bundle
import android.view.View
import android.widget.Button
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
import com.example.mycloset.data.model.Item
import com.example.mycloset.data.model.OutfitSuggestion
import com.example.mycloset.data.repository.GrantsRepository
import com.example.mycloset.data.repository.SuggestionsRepository
import com.example.mycloset.ui.outfit.PickItemsAdapter
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await

class SharedClosetDetailsFragment : Fragment(R.layout.fragment_shared_closet_details) {

    private val db by lazy { FirebaseFirestore.getInstance() }
    private val grantsRepo = GrantsRepository()
    private val suggestionsRepo = SuggestionsRepository()

    private lateinit var rv: RecyclerView
    private lateinit var progress: ProgressBar
    private lateinit var tvTitle: TextView
    private lateinit var tvSub: TextView
    private lateinit var btnSend: Button

    private lateinit var adapter: PickItemsAdapter

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        rv = view.findViewById(R.id.rvSharedClosetItems)
        progress = view.findViewById(R.id.progressSharedClosetDetails)
        tvTitle = view.findViewById(R.id.tvSharedClosetDetailsTitle)
        tvSub = view.findViewById(R.id.tvSharedClosetDetailsSub)
        btnSend = view.findViewById(R.id.btnSendClosetSuggestion)

        val ownerUid = arguments?.getString("ownerUid").orEmpty()
        val closetId = arguments?.getString("closetId").orEmpty()

        val myUid = FirebaseAuth.getInstance().currentUser?.uid.orEmpty()
        if (ownerUid.isBlank() || closetId.isBlank() || myUid.isBlank()) {
            Toast.makeText(requireContext(), "Missing data", Toast.LENGTH_SHORT).show()
            findNavController().popBackStack()
            return
        }

        adapter = PickItemsAdapter(
            onSelectionChanged = {
                val c = adapter.getSelectedIds().size
                btnSend.text = if (c > 0) "Send Suggestion ($c)" else "Send Suggestion"
                btnSend.isEnabled = c > 0
            }
        )

        rv.layoutManager = LinearLayoutManager(requireContext())
        rv.adapter = adapter

        btnSend.isEnabled = false
        btnSend.setOnClickListener {
            val selected = adapter.getSelectedIds()
            if (selected.isEmpty()) return@setOnClickListener

            lifecycleScope.launch {
                try {
                    setLoading(true)

                    //  יצירת suggestion גלובלי
                    val suggestion = OutfitSuggestion(
                        suggestionId = "",
                        ownerUid = ownerUid,
                        targetType = "CLOSET",
                        targetId = closetId,
                        suggesterUid = myUid,
                        suggestedItemIds = selected,
                        note = "Suggestion from shared closet subset",
                        status = "PENDING",
                        createdAt = com.google.firebase.Timestamp.now()
                    )


                    suggestionsRepo.createSuggestion(suggestion)


                    Toast.makeText(requireContext(), "Suggestion sent ", Toast.LENGTH_SHORT).show()
                    findNavController().popBackStack()

                } catch (e: Exception) {
                    Toast.makeText(requireContext(), "Error: ${e.message}", Toast.LENGTH_LONG).show()
                } finally {
                    setLoading(false)
                }
            }
        }

        loadSubsetItems(ownerUid, closetId, myUid)
    }

    private fun setLoading(loading: Boolean) {
        progress.visibility = if (loading) View.VISIBLE else View.GONE
        rv.isEnabled = !loading
        btnSend.isEnabled = !loading && adapter.getSelectedIds().isNotEmpty()
    }

    private fun loadSubsetItems(ownerUid: String, closetId: String, myUid: String) {
        lifecycleScope.launch {
            try {
                setLoading(true)

                //  להביא grant כדי לדעת איזה itemIds מותר לראות
                val grants: List<AccessGrant> = grantsRepo.getSharedClosetGrantsForMe(myUid)
                val g = grants.firstOrNull { it.ownerUid == ownerUid && it.resourceId == closetId }

                val allowedIds = g?.itemIds ?: emptyList()
                if (allowedIds.isEmpty()) {
                    adapter.submitList(emptyList())
                    tvTitle.text = "Shared Closet"
                    tvSub.text = "No shared items (subset is empty)"
                    return@launch
                }

                tvTitle.text = "Shared Closet"
                tvSub.text = "Pick items to suggest (${allowedIds.size} available)"

                // 2) להביא כל Item לפי id (רק אלו שב־subset)
                val items = allowedIds.mapNotNull { itemId ->
                    val doc = db.collection("users")
                        .document(ownerUid)
                        .collection("items")
                        .document(itemId)
                        .get()
                        .await()

                    doc.toObject(Item::class.java)?.copy(id = doc.id)
                }

                adapter.submitList(items)

            } catch (e: Exception) {
                Toast.makeText(requireContext(), "Error: ${e.message}", Toast.LENGTH_LONG).show()
            } finally {
                setLoading(false)
            }
        }
    }
}
