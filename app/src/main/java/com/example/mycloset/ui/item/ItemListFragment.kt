package com.example.mycloset.ui.item

import android.os.Bundle
import android.view.View
import android.widget.ProgressBar
import android.widget.TextView
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.mycloset.R
import com.example.mycloset.data.repository.ItemsRepository
import com.google.android.material.floatingactionbutton.FloatingActionButton
import com.google.firebase.auth.FirebaseAuth
import kotlinx.coroutines.launch

class ItemsListFragment : Fragment(R.layout.fragment_items_list) {

    private val repo = ItemsRepository()

    private val adapter = ItemsAdapter { item ->
        Toast.makeText(requireContext(), "Clicked: ${item.name}", Toast.LENGTH_SHORT).show()
        // אפשר בהמשך: ניווט ל-ItemDetails
    }

    private lateinit var rv: RecyclerView
    private lateinit var progress: ProgressBar
    private lateinit var tvEmpty: TextView

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        rv = view.findViewById(R.id.rvItems)
        progress = view.findViewById(R.id.progressItems)
        tvEmpty = view.findViewById(R.id.tvEmpty)

        rv.layoutManager = LinearLayoutManager(requireContext())
        rv.adapter = adapter

        val fab = view.findViewById<FloatingActionButton>(R.id.fabAddItem)
        fab.setOnClickListener {
            findNavController().navigate(R.id.action_itemsListFragment_to_addItemFragment)
        }

        loadItems()
    }

    override fun onResume() {
        super.onResume()
        // ✅ אם חזרת מ-AddItem, זה ירענן ויראה את הפריט החדש
        loadItems()
    }

    private fun loadItems() {
        val userId = FirebaseAuth.getInstance().currentUser?.uid
        if (userId == null) {
            tvEmpty.visibility = View.VISIBLE
            tvEmpty.text = "Please login first"
            return
        }

        lifecycleScope.launch {
            try {
                progress.visibility = View.VISIBLE
                val items = repo.getMyItems(userId)
                adapter.submitList(items)
                tvEmpty.visibility = if (items.isEmpty()) View.VISIBLE else View.GONE
            } catch (e: Exception) {
                Toast.makeText(requireContext(), "Error: ${e.message}", Toast.LENGTH_LONG).show()
            } finally {
                progress.visibility = View.GONE
            }
        }
    }
}