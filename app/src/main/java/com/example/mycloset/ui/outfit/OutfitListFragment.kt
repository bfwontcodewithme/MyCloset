package com.example.mycloset.ui.outfit

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
import com.example.mycloset.data.repository.OutfitsRepository
import com.google.android.material.floatingactionbutton.FloatingActionButton
import com.google.firebase.auth.FirebaseAuth
import kotlinx.coroutines.launch

class OutfitListFragment : Fragment(R.layout.fragment_outfit_list) {

    private val repo = OutfitsRepository()
    private val adapter = OutfitsAdapter { outfit ->
        Toast.makeText(requireContext(), "Clicked: ${outfit.name}", Toast.LENGTH_SHORT).show()
        // בהמשך אפשר OutfitDetails
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val rv = view.findViewById<RecyclerView>(R.id.rvOutfits)
        val progress = view.findViewById<ProgressBar>(R.id.progressOutfits)
        val tvEmpty = view.findViewById<TextView>(R.id.tvEmptyOutfits)
        val fab = view.findViewById<FloatingActionButton>(R.id.fabCreateOutfit)

        rv.layoutManager = LinearLayoutManager(requireContext())
        rv.adapter = adapter

        fab.setOnClickListener {
            findNavController().navigate(R.id.action_outfitListFragment_to_createOutfitFragment)
        }

        val userId = FirebaseAuth.getInstance().currentUser?.uid
        if (userId == null) {
            Toast.makeText(requireContext(), "Please login first", Toast.LENGTH_SHORT).show()
            findNavController().navigate(R.id.action_global_loginFragment)
            return
        }

        lifecycleScope.launch {
            try {
                progress.visibility = View.VISIBLE
                val outfits = repo.getMyOutfits(userId)
                adapter.submitList(outfits)
                tvEmpty.visibility = if (outfits.isEmpty()) View.VISIBLE else View.GONE
            } catch (e: Exception) {
                Toast.makeText(requireContext(), "Error: ${e.message}", Toast.LENGTH_LONG).show()
            } finally {
                progress.visibility = View.GONE
            }
        }
    }
}