package com.example.mycloset.ui.closet

import android.os.Bundle
import android.view.View
import android.widget.EditText
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.mycloset.R
import com.example.mycloset.data.repository.ClosetsRepository
import com.google.android.material.floatingactionbutton.FloatingActionButton
import com.google.firebase.auth.FirebaseAuth
import kotlinx.coroutines.launch

class ClosetListFragment : Fragment(R.layout.fragment_closet_list) {

    private val repo = ClosetsRepository()
    private val adapter = ClosetAdapter()

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val rv = view.findViewById<RecyclerView>(R.id.rvClosets)
        rv.layoutManager = LinearLayoutManager(requireContext())
        rv.adapter = adapter

        val userId = FirebaseAuth.getInstance().currentUser?.uid
        if (userId == null) {
            Toast.makeText(requireContext(), "Please login first", Toast.LENGTH_SHORT).show()
            return
        }

        fun load() {
            lifecycleScope.launch {
                val closets = repo.getMyClosets(userId)
                adapter.submitList(closets)
            }
        }

        load()

        view.findViewById<FloatingActionButton>(R.id.fabAddCloset).setOnClickListener {
            val input = EditText(requireContext())
            input.hint = "Closet name"

            AlertDialog.Builder(requireContext())
                .setTitle("Add Closet")
                .setView(input)
                .setPositiveButton("Add") { _, _ ->
                    val name = input.text.toString().trim().ifEmpty { "My Closet" }
                    lifecycleScope.launch {
                        repo.addCloset(userId, name)
                        load()
                    }
                }
                .setNegativeButton("Cancel", null)
                .show()
        }
    }
}