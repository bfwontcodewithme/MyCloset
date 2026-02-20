package com.example.mycloset.ui.closet

import android.os.Bundle
import android.view.View
import android.widget.TextView
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.mycloset.R
import com.example.mycloset.data.model.Closet
import com.example.mycloset.data.repository.ClosetsRepository
import com.google.firebase.auth.FirebaseAuth
import kotlinx.coroutines.launch

class PickClosetForItemFragment : Fragment(R.layout.fragment_pick_closet_for_item) {

    private val repo = ClosetsRepository()
    private lateinit var adapter: ClosetAdapter
    private var userId: String? = null
    private var emptyTv: TextView? = null

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        requireActivity().title = "Choose Closet"

        emptyTv = view.findViewById(R.id.tvEmptyPickCloset)

        userId = FirebaseAuth.getInstance().currentUser?.uid
        if (userId == null) {
            Toast.makeText(requireContext(), "Please login first", Toast.LENGTH_SHORT).show()
            return
        }

        // ✅ כאן אין Rename/Delete — רק בחירה
        adapter = ClosetAdapter(
            onClick = { openAddItem(it) },
            onRename = { }, // disabled
            onDelete = { }  // disabled
        )

        val rv = view.findViewById<RecyclerView>(R.id.rvPickCloset)
        rv.layoutManager = LinearLayoutManager(requireContext())
        rv.adapter = adapter

        load()
    }

    private fun setEmptyState(isEmpty: Boolean) {
        emptyTv?.visibility = if (isEmpty) View.VISIBLE else View.GONE
    }

    private fun load() {
        val uid = userId ?: return
        lifecycleScope.launch {
            runCatching { repo.getMyClosets(uid) }
                .onSuccess { closets ->
                    adapter.submitList(closets)
                    setEmptyState(closets.isEmpty())
                }
                .onFailure { e ->
                    Toast.makeText(requireContext(), "Failed to load closets: ${e.message}", Toast.LENGTH_LONG).show()
                }
        }
    }

    private fun openAddItem(closet: Closet) {
        if (closet.closetId.isBlank()) {
            Toast.makeText(requireContext(), "Closet ID missing (try reloading)", Toast.LENGTH_SHORT).show()
            load()
            return
        }

        val args = Bundle().apply {
            putString("closetId", closet.closetId)
            putString("closetName", closet.closetName)
        }

        findNavController().navigate(R.id.nav_add_item, args)

    }
}
