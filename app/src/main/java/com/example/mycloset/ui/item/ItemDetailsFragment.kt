package com.example.mycloset.ui.item

import android.os.Bundle
import android.view.View
import android.widget.*
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import coil.load
import com.example.mycloset.R
import com.example.mycloset.data.model.Closet
import com.example.mycloset.data.model.Item
import com.example.mycloset.data.repository.ClosetsRepository
import com.example.mycloset.data.repository.ItemsRepository
import com.google.firebase.auth.FirebaseAuth
import kotlinx.coroutines.launch

class ItemDetailsFragment : Fragment(R.layout.fragment_item_details) {

    private val itemsRepo = ItemsRepository()
    private val closetsRepo = ClosetsRepository()

    private var userId: String? = null
    private var currentItem: Item? = null
    private var closets: List<Closet> = emptyList()

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        userId = FirebaseAuth.getInstance().currentUser?.uid
        if (userId == null) {
            Toast.makeText(requireContext(), "Please login first", Toast.LENGTH_SHORT).show()
            findNavController().navigate(R.id.action_global_login)
            return
        }

        val uid = userId!!
        val itemId = arguments?.getString("itemId").orEmpty()
        val fromClosetId = arguments?.getString("closetId").orEmpty()

        val img = view.findViewById<ImageView>(R.id.imgItemDetails)
        val progress = view.findViewById<ProgressBar>(R.id.progressDetails)

        val btnSave = view.findViewById<Button>(R.id.btnSaveDetails)
        val btnDelete = view.findViewById<Button>(R.id.btnDeleteDetails)

        val etName = view.findViewById<EditText>(R.id.etNameDetails)
        val etType = view.findViewById<EditText>(R.id.etTypeDetails)
        val etColor = view.findViewById<EditText>(R.id.etColorDetails)
        val etSeason = view.findViewById<EditText>(R.id.etSeasonDetails)
        val etTags = view.findViewById<EditText>(R.id.etTagsDetails)
        val spinnerClosets = view.findViewById<Spinner>(R.id.spClosets)

        if (itemId.isBlank()) {
            Toast.makeText(requireContext(), "Missing itemId", Toast.LENGTH_SHORT).show()
            findNavController().navigateUp()
            return
        }

        fun setLoading(loading: Boolean) {
            progress.visibility = if (loading) View.VISIBLE else View.GONE
            btnSave.isEnabled = !loading
            btnDelete.isEnabled = !loading

            etName.isEnabled = !loading
            etType.isEnabled = !loading
            etColor.isEnabled = !loading
            etSeason.isEnabled = !loading
            etTags.isEnabled = !loading
            spinnerClosets.isEnabled = !loading
        }

        fun fillUI(item: Item) {
            // תמונה
            img.load(item.imageUrl) {
                crossfade(true)
                placeholder(R.drawable.ic_launcher_foreground)
                error(R.drawable.ic_launcher_foreground)
            }

            etName.setText(item.name)
            etType.setText(item.type)
            etColor.setText(item.color)
            etSeason.setText(item.season)
            etTags.setText(item.tags.joinToString(", "))
        }

        setLoading(true)

        // Load item + closets
        lifecycleScope.launch {
            val item = runCatching { itemsRepo.getItemById(uid, itemId) }.getOrNull()
            if (item == null) {
                Toast.makeText(requireContext(), "Item not found", Toast.LENGTH_SHORT).show()
                findNavController().navigateUp()
                return@launch
            }

            currentItem = item
            fillUI(item)

            // Closets list for Move
            closets = runCatching { closetsRepo.getMyClosets(uid) }.getOrDefault(emptyList())

            val closetNames = if (closets.isEmpty()) {
                listOf("default")
            } else {
                closets.map { it.closetName }
            }

            spinnerClosets.adapter = ArrayAdapter(
                requireContext(),
                android.R.layout.simple_spinner_dropdown_item,
                closetNames
            )

            // select current closet
            val currentClosetId = item.closetId.ifBlank { fromClosetId }
            val selectedIndex = if (closets.isEmpty()) {
                0
            } else {
                val idx = closets.indexOfFirst { it.closetId == currentClosetId }
                if (idx >= 0) idx else 0
            }
            spinnerClosets.setSelection(selectedIndex)

            setLoading(false)
        }

        // SAVE (update item + move closet)
        btnSave.setOnClickListener {
            val item = currentItem ?: return@setOnClickListener

            val name = etName.text.toString().trim()
            val type = etType.text.toString().trim()
            val color = etColor.text.toString().trim()
            val season = etSeason.text.toString().trim()
            val tags = etTags.text.toString()
                .split(",")
                .map { it.trim() }
                .filter { it.isNotEmpty() }

            if (name.isBlank() || type.isBlank()) {
                Toast.makeText(requireContext(), "Name ו-Type mandatory", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            val newClosetId = if (closets.isEmpty()) {
                "default"
            } else {
                closets.getOrNull(spinnerClosets.selectedItemPosition)?.closetId ?: item.closetId
            }

            val updated = item.copy(
                name = name,
                type = type,
                color = color,
                season = season,
                tags = tags,
                closetId = newClosetId
            )

            lifecycleScope.launch {
                setLoading(true)
                runCatching { itemsRepo.updateItem(uid, updated) }
                    .onSuccess {
                        Toast.makeText(requireContext(), "Saved", Toast.LENGTH_SHORT).show()
                        findNavController().navigateUp()
                    }
                    .onFailure { e ->
                        Toast.makeText(requireContext(), "Save failed: ${e.message}", Toast.LENGTH_LONG).show()
                    }
                setLoading(false)
            }
        }

        // DELETE
        btnDelete.setOnClickListener {
            val item = currentItem ?: return@setOnClickListener

            AlertDialog.Builder(requireContext())
                .setTitle("Delete item?")
                .setMessage("Delete '${item.name}'?")
                .setPositiveButton("Delete") { _, _ ->
                    lifecycleScope.launch {
                        setLoading(true)
                        runCatching { itemsRepo.deleteItem(uid, item.id) }
                            .onSuccess {
                                Toast.makeText(requireContext(), "Deleted", Toast.LENGTH_SHORT).show()
                                findNavController().navigateUp()
                            }
                            .onFailure { e ->
                                Toast.makeText(requireContext(), "Delete failed: ${e.message}", Toast.LENGTH_LONG).show()
                            }
                        setLoading(false)
                    }
                }
                .setNegativeButton("Cancel", null)
                .show()
        }
    }
}
