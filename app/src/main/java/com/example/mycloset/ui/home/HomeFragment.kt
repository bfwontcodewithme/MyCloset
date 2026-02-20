package com.example.mycloset.ui.home

import android.os.Bundle
import android.view.View
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import com.example.mycloset.R
import com.google.android.material.button.MaterialButton
import com.google.android.material.card.MaterialCardView
import com.google.android.material.textview.MaterialTextView
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await

class HomeFragment : Fragment(R.layout.fragment_home) {

    private val db by lazy { FirebaseFirestore.getInstance() }
    private val auth by lazy { FirebaseAuth.getInstance() }

    private lateinit var tvSuggestionsSubtitle: MaterialTextView

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        tvSuggestionsSubtitle = view.findViewById(R.id.tvSuggestionsSubtitle)

        view.findViewById<MaterialCardView>(R.id.cardClosets)
            .setOnClickListener { findNavController().navigate(R.id.nav_closet_list) }

        view.findViewById<MaterialCardView>(R.id.cardOutfits)
            .setOnClickListener { findNavController().navigate(R.id.nav_outfit_list) }

        view.findViewById<MaterialCardView>(R.id.cardAddItem)
            .setOnClickListener { findNavController().navigate(R.id.nav_add_item) }

        // ✅ FIX: My Items -> open ItemListFragment in "All items" mode (closetId="")
        view.findViewById<MaterialCardView>(R.id.cardMyItems)
            .setOnClickListener {
                val b = Bundle().apply {
                    putString("closetId", "")          // All items
                    putString("closetName", "My Items")
                }
                findNavController().navigate(R.id.nav_items_list, b)
            }

        view.findViewById<MaterialCardView>(R.id.cardRequestStylist)
            .setOnClickListener { findNavController().navigate(R.id.nav_stylist_list) }

        view.findViewById<MaterialCardView>(R.id.cardMyRequests)
            .setOnClickListener { findNavController().navigate(R.id.nav_my_requests) }

        view.findViewById<MaterialCardView>(R.id.cardSharedWithMe)
            .setOnClickListener { findNavController().navigate(R.id.nav_shared_outfits) }

        view.findViewById<MaterialCardView>(R.id.cardSuggestions)
            .setOnClickListener { findNavController().navigate(R.id.nav_suggestions_inbox_all) }

        view.findViewById<MaterialCardView>(R.id.cardSupport)
            .setOnClickListener { findNavController().navigate(R.id.nav_support) }

        view.findViewById<MaterialButton>(R.id.btnLogout)
            .setOnClickListener { logout() }

        updateSuggestionsSubtitle()
    }

    override fun onResume() {
        super.onResume()
        updateSuggestionsSubtitle()
    }

    private fun logout() {
        auth.signOut()
        findNavController().navigate(R.id.action_global_login)
    }

    private fun updateSuggestionsSubtitle() {
        val myUid = auth.currentUser?.uid ?: run {
            tvSuggestionsSubtitle.text = "No pending"
            return
        }

        lifecycleScope.launch {
            tvSuggestionsSubtitle.text = "Loading..."
            try {
                val suggestionsSnap = db.collection("outfit_suggestions")
                    .whereEqualTo("ownerUid", myUid)
                    .whereEqualTo("status", "PENDING")
                    .get()
                    .await()

                val totalPending = suggestionsSnap.size()
                tvSuggestionsSubtitle.text =
                    if (totalPending > 0) "$totalPending pending" else "No pending"

            } catch (_: Exception) {
                tvSuggestionsSubtitle.text = "No pending"
            }
        }
    }
}
