package com.example.mycloset.ui.item

import android.os.Bundle
import android.view.View
import android.widget.ProgressBar
import android.widget.TextView
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.mycloset.R
import com.example.mycloset.data.repository.ItemsRepository
import com.google.android.material.floatingactionbutton.FloatingActionButton
import com.google.firebase.auth.FirebaseAuth
import kotlinx.coroutines.launch

class ItemListFragment : Fragment(R.layout.fragment_items_list) {

    val viewModel: ItemViewModel by viewModels()
    private val adapter = ItemAdapter(mutableListOf()) { item ->
        Toast.makeText(requireContext(), item.name, Toast.LENGTH_SHORT).show()
    }


    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val rv = view.findViewById<RecyclerView>(R.id.rvItems)
        val progress = view.findViewById<ProgressBar>(R.id.progressItems)
        val tvEmpty = view.findViewById<TextView>(R.id.tvEmpty)
        val fab = view.findViewById<FloatingActionButton>(R.id.fabAddItem)

        rv.layoutManager = LinearLayoutManager(requireContext())
        rv.adapter = adapter

        viewModel.items.observe(viewLifecycleOwner) { itemList ->

            adapter.setData(itemList)
            progress.visibility = View.GONE
            if (itemList.isEmpty()) {
                tvEmpty.visibility = View.VISIBLE
                tvEmpty.text = "Your closet is empty"
            } else {
                tvEmpty.visibility = View.GONE
            }
        }

        //
        val userId = FirebaseAuth.getInstance().currentUser?.uid
        if (userId == null) {
            progress.visibility = View.GONE
            tvEmpty.visibility = View.VISIBLE
            tvEmpty.text = "Please login first"

        } else {
            progress.visibility = View.VISIBLE
            tvEmpty.visibility = View.VISIBLE
            viewModel.loadItems(userId)
        }
        fab.setOnClickListener {
            // ✅ לפי navigation שלך
            findNavController().navigate(R.id.nav_add_item)
        }

    }
}
