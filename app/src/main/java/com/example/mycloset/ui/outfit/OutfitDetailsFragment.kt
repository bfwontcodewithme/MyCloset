package com.example.mycloset.ui.outfit

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
import com.example.mycloset.data.repository.ItemsRepository
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await

class OutfitDetailsFragment : Fragment(R.layout.fragment_outfit_details) {

    private val db by lazy { FirebaseFirestore.getInstance() }
    private val itemsRepo = ItemsRepository()

    private lateinit var tvTitle: TextView
    private lateinit var progress: ProgressBar
    private lateinit var tvEmpty: TextView
    private lateinit var rv: RecyclerView

    private lateinit var btnShareOutfit: Button
    private lateinit var btnOpenSuggestions: Button

    private lateinit var adapter: com.example.mycloset.ui.item.ItemAdapter

    private var currentOutfitId: String = ""

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        tvTitle = view.findViewById(R.id.tvOutfitTitle)
        progress = view.findViewById(R.id.progressOutfitDetails)
        tvEmpty = view.findViewById(R.id.tvOutfitEmpty)
        rv = view.findViewById(R.id.rvOutfitItems)

        btnShareOutfit = view.findViewById(R.id.btnShareOutfit)
        btnOpenSuggestions = view.findViewById(R.id.btnOpenSuggestions)

        adapter = com.example.mycloset.ui.item.ItemAdapter(
            mutableListOf(),
            onClick = { },
            onDelete = { }
        )

        rv.layoutManager = LinearLayoutManager(requireContext())
        rv.adapter = adapter

        val outfitId = arguments?.getString("outfitId").orEmpty()
        if (outfitId.isBlank()) {
            Toast.makeText(requireContext(), "Missing outfitId", Toast.LENGTH_SHORT).show()
            return
        }
        currentOutfitId = outfitId

        // Share Outfit
        btnShareOutfit.setOnClickListener {
            val user = FirebaseAuth.getInstance().currentUser
            if (user == null) {
                Toast.makeText(requireContext(), "Please login first", Toast.LENGTH_SHORT).show()
                findNavController().navigate(R.id.action_global_login)
                return@setOnClickListener
            }

            val args = Bundle().apply {
                putString("resourceType", "OUTFIT")
                putString("resourceId", currentOutfitId)
            }
            findNavController().navigate(R.id.nav_share_access, args)
        }

        // ✅ Open suggestions inbox (owner)
        btnOpenSuggestions.setOnClickListener {
            val args = Bundle().apply {
                putString("outfitId", currentOutfitId)
            }
            findNavController().navigate(R.id.nav_suggestions_inbox, args)
        }

        loadOutfit(outfitId)
    }

    private fun loadOutfit(outfitId: String) {
        val userId = FirebaseAuth.getInstance().currentUser?.uid
        if (userId == null) {
            Toast.makeText(requireContext(), "Please login first", Toast.LENGTH_SHORT).show()
            return
        }

        lifecycleScope.launch {
            try {
                progress.visibility = View.VISIBLE

                val outfitDoc = db.collection("users")
                    .document(userId)
                    .collection("outfits")
                    .document(outfitId)
                    .get()
                    .await()

                val outfit = outfitDoc.toObject(Outfit::class.java)
                if (outfit == null) {
                    Toast.makeText(requireContext(), "Outfit not found", Toast.LENGTH_SHORT).show()
                    return@launch
                }

                tvTitle.text = outfit.name

                val items: List<Item> = outfit.itemIds.map { itemId ->
                    async {
                        val itemDoc = db.collection("users")
                            .document(userId)
                            .collection("items")
                            .document(itemId)
                            .get()
                            .await()
                        itemDoc.toObject(Item::class.java)?.copy(id = itemDoc.id)
                    }
                }.awaitAll().filterNotNull()

                adapter.setData(items)
                tvEmpty.visibility = if (items.isEmpty()) View.VISIBLE else View.GONE

            } catch (e: Exception) {
                Toast.makeText(requireContext(), "Error: ${e.message}", Toast.LENGTH_LONG).show()
            } finally {
                progress.visibility = View.GONE
            }
        }
    }
}
