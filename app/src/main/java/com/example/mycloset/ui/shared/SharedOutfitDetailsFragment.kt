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
import com.example.mycloset.data.model.Item
import com.example.mycloset.data.model.Outfit
import com.example.mycloset.data.model.SharedItem
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await

class SharedOutfitDetailsFragment : Fragment(R.layout.fragment_shared_outfit_details) {

    private val db by lazy { FirebaseFirestore.getInstance() }

    private lateinit var adapter: SharedItemsAdapter

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val tvTitle = view.findViewById<TextView>(R.id.tvSharedOutfitTitle)
        val rv = view.findViewById<RecyclerView>(R.id.rvSharedItems)
        val btnSuggest = view.findViewById<Button>(R.id.btnSuggest)
        val progress = view.findViewById<ProgressBar>(R.id.progressSharedOutfitDetails)

        adapter = SharedItemsAdapter()
        rv.layoutManager = LinearLayoutManager(requireContext())
        rv.adapter = adapter

        val ownerUid = arguments?.getString("ownerUid").orEmpty()
        val outfitId = arguments?.getString("outfitId").orEmpty()

        if (ownerUid.isBlank() || outfitId.isBlank()) {
            Toast.makeText(requireContext(), "Missing data", Toast.LENGTH_SHORT).show()
            return
        }

        btnSuggest.setOnClickListener {
            val args = Bundle().apply {
                putString("ownerUid", ownerUid)
                putString("outfitId", outfitId)
            }
            findNavController().navigate(R.id.nav_send_suggestion, args)
        }

        lifecycleScope.launch {
            try {
                progress.visibility = View.VISIBLE

                // 1️⃣ Load Outfit
                val outfitDoc = db.collection("users")
                    .document(ownerUid)
                    .collection("outfits")
                    .document(outfitId)
                    .get()
                    .await()

                val outfit = outfitDoc.toObject(Outfit::class.java)
                if (outfit == null) {
                    Toast.makeText(requireContext(), "Outfit not found", Toast.LENGTH_SHORT).show()
                    return@launch
                }

                // 2️⃣ Load Owner Name
                val ownerDoc = db.collection("users")
                    .document(ownerUid)
                    .get()
                    .await()

                val ownerName = ownerDoc.getString("userName")
                    ?: ownerDoc.getString("userEmail")
                    ?: ownerUid

                tvTitle.text = "${outfit.name}\nby $ownerName"

                // 3️⃣ Load Items
                val items = outfit.itemIds.map { id ->
                    async {
                        db.collection("users")
                            .document(ownerUid)
                            .collection("items")
                            .document(id)
                            .get()
                            .await()
                            .toObject(Item::class.java)
                    }
                }.awaitAll().filterNotNull()

                if (items.isEmpty()) {
                    Toast.makeText(requireContext(), "No items in outfit", Toast.LENGTH_SHORT).show()
                }

                val sharedItems = items.map {
                    SharedItem(
                        itemId = it.id,
                        name = it.name,
                        type = it.type,
                        color = it.color,
                        imageUrl = it.imageUrl
                    )
                }

                adapter.submitList(sharedItems)

            } catch (e: Exception) {
                Toast.makeText(requireContext(), "Error: ${e.message}", Toast.LENGTH_LONG).show()
            } finally {
                progress.visibility = View.GONE
            }
        }
    }
}
