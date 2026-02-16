package com.example.mycloset.ui.item

import android.os.Bundle
import android.view.View
import android.widget.ProgressBar
import android.widget.Spinner
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.widget.SearchView
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import android.widget.ArrayAdapter
import com.example.mycloset.R
import com.example.mycloset.data.model.Item
import com.google.android.material.floatingactionbutton.FloatingActionButton
import com.google.firebase.auth.FirebaseAuth
import java.util.Locale

class ItemListFragment : Fragment(R.layout.fragment_items_list) {

    private val viewModel: ItemViewModel by viewModels()
    private lateinit var adapter: ItemAdapter

    private var closetIdArg: String = ""
    private var closetNameArg: String = ""
    private var userId: String? = null

    // ✅ for filtering
    private var allItems: List<Item> = emptyList()
    private var currentQuery: String = ""
    private var selectedType: String = "All"
    private var selectedColor: String = "All"
    private var selectedSeason: String = "All"

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        closetIdArg = arguments?.getString("closetId").orEmpty()
        closetNameArg = arguments?.getString("closetName").orEmpty()

        val rv = view.findViewById<RecyclerView>(R.id.rvItems)
        val progress = view.findViewById<ProgressBar>(R.id.progressItems)
        val tvEmpty = view.findViewById<TextView>(R.id.tvEmpty)
        val fab = view.findViewById<FloatingActionButton>(R.id.fabAddItem)

        // ✅ new ui
        val sv = view.findViewById<SearchView>(R.id.svItems)
        val spType = view.findViewById<Spinner>(R.id.spType)
        val spColor = view.findViewById<Spinner>(R.id.spColor)
        val spSeason = view.findViewById<Spinner>(R.id.spSeason)

        if (closetIdArg.isBlank()) {
            tvEmpty.visibility = View.VISIBLE
            tvEmpty.text = "Missing closetId"
            return
        }

        if (closetNameArg.isNotBlank()) {
            requireActivity().title = closetNameArg
        }

        userId = FirebaseAuth.getInstance().currentUser?.uid
        if (userId == null) {
            progress.visibility = View.GONE
            tvEmpty.visibility = View.VISIBLE
            tvEmpty.text = "Please login first"
            return
        }

        adapter = ItemAdapter(
            items = mutableListOf(),
            onClick = { item ->
                val args = Bundle().apply {
                    putString("itemId", item.id)
                    putString("closetId", closetIdArg)
                    putString("closetName", closetNameArg)
                }
                findNavController().navigate(
                    R.id.action_nav_items_list_to_nav_item_details,
                    args
                )
            },
            onDelete = { item ->
                confirmDeleteItem(userId!!, closetIdArg, item)
            }
        )

        rv.layoutManager = LinearLayoutManager(requireContext())
        rv.adapter = adapter

        // ✅ Search listeners
        sv.setOnQueryTextListener(object : SearchView.OnQueryTextListener {
            override fun onQueryTextSubmit(query: String?): Boolean = true
            override fun onQueryTextChange(newText: String?): Boolean {
                currentQuery = newText.orEmpty()
                applyFiltersAndRender(tvEmpty)
                return true
            }
        })

        // Observe items
        viewModel.items.observe(viewLifecycleOwner) { itemList ->
            allItems = itemList

            setupSpinners(spType, spColor, spSeason, allItems) {
                // callback whenever selection changes
                selectedType = spType.selectedItem?.toString() ?: "All"
                selectedColor = spColor.selectedItem?.toString() ?: "All"
                selectedSeason = spSeason.selectedItem?.toString() ?: "All"
                applyFiltersAndRender(tvEmpty)
            }

            progress.visibility = View.GONE
            applyFiltersAndRender(tvEmpty)
        }

        progress.visibility = View.VISIBLE
        viewModel.loadItemsForCloset(userId!!, closetIdArg)

        fab.setOnClickListener {
            val args = Bundle().apply { putString("closetId", closetIdArg) }
            findNavController().navigate(R.id.nav_add_item, args)
        }
    }

    override fun onResume() {
        super.onResume()
        val uid = userId ?: return
        if (closetIdArg.isNotBlank()) {
            viewModel.loadItemsForCloset(uid, closetIdArg)
        }
    }

    private fun applyFiltersAndRender(tvEmpty: TextView) {
        val q = currentQuery.trim().lowercase(Locale.getDefault())

        val filtered = allItems.filter { item ->
            val matchesText =
                q.isBlank() ||
                        item.name.lowercase(Locale.getDefault()).contains(q) ||
                        item.type.lowercase(Locale.getDefault()).contains(q) ||
                        item.color.lowercase(Locale.getDefault()).contains(q) ||
                        item.season.lowercase(Locale.getDefault()).contains(q) ||
                        item.tags.any { it.lowercase(Locale.getDefault()).contains(q) }

            val matchesType = (selectedType == "All" || item.type == selectedType)
            val matchesColor = (selectedColor == "All" || item.color == selectedColor)
            val matchesSeason = (selectedSeason == "All" || item.season == selectedSeason)

            matchesText && matchesType && matchesColor && matchesSeason
        }

        adapter.setData(filtered)

        if (filtered.isEmpty()) {
            tvEmpty.visibility = View.VISIBLE
            tvEmpty.text = "No results"
        } else {
            tvEmpty.visibility = View.GONE
        }
    }

    private fun setupSpinners(
        spType: Spinner,
        spColor: Spinner,
        spSeason: Spinner,
        items: List<Item>,
        onChanged: () -> Unit
    ) {
        val types = listOf("All") + items.map { it.type }.filter { it.isNotBlank() }.distinct().sorted()
        val colors = listOf("All") + items.map { it.color }.filter { it.isNotBlank() }.distinct().sorted()
        val seasons = listOf("All") + items.map { it.season }.filter { it.isNotBlank() }.distinct().sorted()

        fun setSpinner(sp: Spinner, values: List<String>) {
            val ad = ArrayAdapter(requireContext(), android.R.layout.simple_spinner_item, values)
            ad.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
            sp.adapter = ad
        }

        setSpinner(spType, types)
        setSpinner(spColor, colors)
        setSpinner(spSeason, seasons)

        spType.setSelection(0)
        spColor.setSelection(0)
        spSeason.setSelection(0)

        spType.setOnItemSelectedListener(SimpleItemSelectedListener { onChanged() })
        spColor.setOnItemSelectedListener(SimpleItemSelectedListener { onChanged() })
        spSeason.setOnItemSelectedListener(SimpleItemSelectedListener { onChanged() })
    }

    private fun confirmDeleteItem(userId: String, closetId: String, item: Item) {
        AlertDialog.Builder(requireContext())
            .setTitle("Delete item?")
            .setMessage("Delete '${item.name}'?")
            .setPositiveButton("Delete") { _, _ ->
                viewModel.deleteItem(userId, item.id, closetId)
                Toast.makeText(requireContext(), "Deleted", Toast.LENGTH_SHORT).show()
            }
            .setNegativeButton("Cancel", null)
            .show()
    }
}
