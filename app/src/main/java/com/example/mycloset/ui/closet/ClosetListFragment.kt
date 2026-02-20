package com.example.mycloset.ui.closet

import android.os.Bundle
import android.view.View
import android.widget.EditText
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.mycloset.R
import com.example.mycloset.data.model.Closet
import com.example.mycloset.data.repository.ClosetsRepository
import com.google.android.material.floatingactionbutton.FloatingActionButton
import com.google.firebase.auth.FirebaseAuth
import kotlinx.coroutines.launch

class ClosetListFragment : Fragment(R.layout.fragment_closet_list) {

    private val repo = ClosetsRepository()
    private lateinit var adapter: ClosetAdapter
    private var userId: String? = null

    private var emptyTv: TextView? = null
    private var pickMode: Boolean = false

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        emptyTv = view.findViewById(R.id.tvEmptyClosets)
        pickMode = arguments?.getBoolean("pickMode", false) ?: false

        requireActivity().title = if (pickMode) "Choose Closet" else "My Closets"

        userId = FirebaseAuth.getInstance().currentUser?.uid
        if (userId == null) {
            Toast.makeText(requireContext(), "Please login first", Toast.LENGTH_SHORT).show()
            return
        }

        val fabAddCloset = view.findViewById<FloatingActionButton>(R.id.fabAddCloset)
        fabAddCloset.visibility = if (pickMode) View.GONE else View.VISIBLE

        adapter = if (pickMode) {
            // ✅ pick mode: only click, no rename/delete
            ClosetAdapter(
                showMenu = false,
                onClick = { pickClosetAndGoAddItem(it) },
                onRename = { },
                onDelete = { }
            )
        } else {
            // normal mode
            ClosetAdapter(
                showMenu = true,
                onClick = { openCloset(it) },
                onRename = { showRenameDialog(it) },
                onDelete = { confirmDelete(it) }
            )
        }

        val rv = view.findViewById<RecyclerView>(R.id.rvClosets)
        rv.layoutManager = LinearLayoutManager(requireContext())
        rv.adapter = adapter

        load()

        fabAddCloset.setOnClickListener {
            showAddDialog()
        }
    }

    private fun setEmptyState(isEmpty: Boolean) {
        emptyTv?.visibility = if (isEmpty) View.VISIBLE else View.GONE
        if (isEmpty) {
            emptyTv?.text = if (pickMode) "No closets yet" else "No closets yet"
        }
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

    private fun showAddDialog() {
        val uid = userId ?: return
        val input = EditText(requireContext()).apply { hint = "Closet name" }

        AlertDialog.Builder(requireContext())
            .setTitle("Add Closet")
            .setView(input)
            .setPositiveButton("Add") { _, _ ->
                val name = input.text.toString().trim().ifEmpty { "My Closet" }
                lifecycleScope.launch {
                    runCatching { repo.addCloset(uid, name) }
                        .onSuccess { load() }
                        .onFailure { e ->
                            Toast.makeText(requireContext(), "Failed to add closet: ${e.message}", Toast.LENGTH_LONG).show()
                        }
                }
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun showRenameDialog(closet: Closet) {
        val uid = userId ?: return
        val input = EditText(requireContext()).apply { setText(closet.closetName) }

        AlertDialog.Builder(requireContext())
            .setTitle("Rename Closet")
            .setView(input)
            .setPositiveButton("Save") { _, _ ->
                val newName = input.text.toString().trim().ifEmpty { closet.closetName }
                lifecycleScope.launch {
                    runCatching { repo.renameCloset(uid, closet.closetId, newName) }
                        .onSuccess { load() }
                        .onFailure { e ->
                            Toast.makeText(requireContext(), "Failed to rename closet: ${e.message}", Toast.LENGTH_LONG).show()
                        }
                }
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun confirmDelete(closet: Closet) {
        val uid = userId ?: return

        AlertDialog.Builder(requireContext())
            .setTitle("Delete closet?")
            .setMessage("Are you sure you want to delete '${closet.closetName}'?")
            .setPositiveButton("Delete") { _, _ ->
                lifecycleScope.launch {
                    runCatching { repo.deleteCloset(uid, closet.closetId) }
                        .onSuccess { load() }
                        .onFailure { e ->
                            Toast.makeText(requireContext(), "Failed to delete closet: ${e.message}", Toast.LENGTH_LONG).show()
                        }
                }
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun openCloset(closet: Closet) {
        if (closet.closetId.isBlank()) {
            Toast.makeText(requireContext(), "Closet ID missing (try reloading)", Toast.LENGTH_SHORT).show()
            load()
            return
        }

        val args = Bundle().apply {
            putString("closetId", closet.closetId)
            putString("closetName", closet.closetName)
        }
        findNavController().navigate(R.id.action_nav_closet_list_to_nav_closet, args)
    }

    private fun pickClosetAndGoAddItem(closet: Closet) {
        if (closet.closetId.isBlank()) {
            Toast.makeText(requireContext(), "Closet ID missing (try reloading)", Toast.LENGTH_SHORT).show()
            load()
            return
        }

        val args = Bundle().apply {
            putString("closetId", closet.closetId)
        }
        findNavController().navigate(R.id.nav_add_item, args)
    }
}
