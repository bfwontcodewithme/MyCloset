package com.example.mycloset.ui.share

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
import com.example.mycloset.data.repository.GrantsRepository
import com.example.mycloset.ui.outfit.PickItemsAdapter
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await

class PickItemsToShareFragment : Fragment(R.layout.fragment_pick_items_to_share) {

    private val db by lazy { FirebaseFirestore.getInstance() }
    private val grantsRepo = GrantsRepository()

    private lateinit var rv: RecyclerView
    private lateinit var progress: ProgressBar
    private lateinit var btnConfirm: Button
    private lateinit var tvSub: TextView

    private lateinit var adapter: PickItemsAdapter

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        rv = view.findViewById(R.id.rvPickItems)
        progress = view.findViewById(R.id.progressPickItems)
        btnConfirm = view.findViewById(R.id.btnConfirmShareItems)
        tvSub = view.findViewById(R.id.tvPickItemsSub)

        val closetId = arguments?.getString("closetId").orEmpty()
        val friendUid = arguments?.getString("friendUid").orEmpty()
        val friendLabel = arguments?.getString("friendLabel").orEmpty()

        val ownerUid = FirebaseAuth.getInstance().currentUser?.uid.orEmpty()
        if (ownerUid.isBlank() || closetId.isBlank() || friendUid.isBlank()) {
            Toast.makeText(requireContext(), "Missing data", Toast.LENGTH_SHORT).show()
            findNavController().popBackStack()
            return
        }

        tvSub.text = "Choose which items to share with: ${if (friendLabel.isBlank()) friendUid else friendLabel}"

        adapter = PickItemsAdapter(
            onSelectionChanged = {
                val c = adapter.getSelectedIds().size
                btnConfirm.text = if (c > 0) "Share selected items ($c)" else "Share selected items"
                btnConfirm.isEnabled = c > 0 && progress.visibility != View.VISIBLE
            }
        )

        rv.layoutManager = LinearLayoutManager(requireContext())
        rv.adapter = adapter

        btnConfirm.isEnabled = false
        btnConfirm.setOnClickListener {
            val selected = adapter.getSelectedIds()
            if (selected.isEmpty()) return@setOnClickListener

            lifecycleScope.launch {
                try {
                    setLoading(true)

                    val grant = AccessGrant(
                        ownerUid = ownerUid,
                        granteeUid = friendUid,
                        granteeEmail = friendLabel,
                        resourceType = "CLOSET",
                        resourceId = closetId,
                        permissions = listOf("VIEW_ITEMS"),
                        itemIds = selected
                    )

                    grantsRepo.upsertGrant(grant)

                    Toast.makeText(requireContext(), "Shared subset ✅", Toast.LENGTH_SHORT).show()
                    findNavController().popBackStack()

                } catch (e: Exception) {
                    Toast.makeText(requireContext(), "Error: ${e.message}", Toast.LENGTH_LONG).show()
                } finally {
                    setLoading(false)
                }
            }
        }

        loadItems(ownerUid, closetId)
    }

    private fun setLoading(loading: Boolean) {
        progress.visibility = if (loading) View.VISIBLE else View.GONE
        rv.isEnabled = !loading
        btnConfirm.isEnabled = !loading && adapter.getSelectedIds().isNotEmpty()
    }

    private fun loadItems(ownerUid: String, closetId: String) {
        lifecycleScope.launch {
            try {
                setLoading(true)

                val snap = db.collection("users")
                    .document(ownerUid)
                    .collection("items")
                    .whereEqualTo("closetId", closetId)
                    .get()
                    .await()

                val items: List<Item> = snap.documents.mapNotNull { d ->
                    d.toObject(Item::class.java)?.copy(id = d.id)
                }

                adapter.submitList(items)

                if (items.isEmpty()) {
                    Toast.makeText(requireContext(), "No items in this closet", Toast.LENGTH_SHORT).show()
                }

            } catch (e: Exception) {
                Toast.makeText(requireContext(), "Error: ${e.message}", Toast.LENGTH_LONG).show()
            } finally {
                setLoading(false)
            }
        }
    }
}
