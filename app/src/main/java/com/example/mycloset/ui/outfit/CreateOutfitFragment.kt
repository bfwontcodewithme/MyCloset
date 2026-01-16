package com.example.mycloset.ui.outfit

import android.os.Bundle
import android.view.View
import android.widget.Button
import android.widget.EditText
import android.widget.ProgressBar
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.mycloset.R
import com.example.mycloset.data.model.Outfit
import com.example.mycloset.data.repository.ItemsRepository
import com.example.mycloset.data.repository.OutfitsRepository
import com.google.firebase.auth.FirebaseAuth
import kotlinx.coroutines.launch

class CreateOutfitFragment : Fragment(R.layout.fragment_create_outfit) {

    private val itemsRepo = ItemsRepository()
    private val outfitsRepo = OutfitsRepository()
    private val pickAdapter = PickItemsAdapter { /* optional toast */ }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val etName = view.findViewById<EditText>(R.id.etOutfitName)
        val rv = view.findViewById<RecyclerView>(R.id.rvPickItems)
        val btnSave = view.findViewById<Button>(R.id.btnSaveOutfit)
        val progress = view.findViewById<ProgressBar>(R.id.progressSaveOutfit)

        rv.layoutManager = LinearLayoutManager(requireContext())
        rv.adapter = pickAdapter

        val userId = FirebaseAuth.getInstance().currentUser?.uid
        if (userId == null) {
            Toast.makeText(requireContext(), "Please login first", Toast.LENGTH_SHORT).show()
            findNavController().navigate(R.id.action_global_login)

            return
        }

        // טוענים את הפריטים של המשתמש כדי לבחור מהם
        lifecycleScope.launch {
            try {
                progress.visibility = View.VISIBLE
                val items = itemsRepo.getMyItems(userId)
                pickAdapter.submitList(items)
            } catch (e: Exception) {
                Toast.makeText(requireContext(), "Error loading items: ${e.message}", Toast.LENGTH_LONG).show()
            } finally {
                progress.visibility = View.GONE
            }
        }

        btnSave.setOnClickListener {
            val outfitName = etName.text.toString().trim()
            val selected = pickAdapter.getSelectedIds()

            if (outfitName.isEmpty()) {
                Toast.makeText(requireContext(), "Name חובה", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            if (selected.isEmpty()) {
                Toast.makeText(requireContext(), "בחרי לפחות פריט אחד", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            lifecycleScope.launch {
                try {
                    progress.visibility = View.VISIBLE
                    btnSave.isEnabled = false

                    val outfit = Outfit(
                        ownerUid = userId,
                        name = outfitName,
                        itemIds = selected
                    )
                    outfitsRepo.addOutfit(userId, outfit)

                    Toast.makeText(requireContext(), "Outfit נשמר ✅", Toast.LENGTH_SHORT).show()
                    findNavController().popBackStack() // חוזר ל-Outfits list
                } catch (e: Exception) {
                    Toast.makeText(requireContext(), "Save error: ${e.message}", Toast.LENGTH_LONG).show()
                } finally {
                    progress.visibility = View.GONE
                    btnSave.isEnabled = true
                }
            }
        }
    }
}