package com.example.mycloset.ui.outfit

import android.os.Bundle
import android.view.View
import android.widget.*
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.mycloset.R
import com.example.mycloset.data.model.Item
import com.example.mycloset.data.model.Outfit
import com.example.mycloset.data.repository.ItemsRepository
import com.example.mycloset.data.repository.OutfitsRepository
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.functions.ktx.functions
import com.google.firebase.ktx.Firebase
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import java.util.Locale

class CreateOutfitFragment : Fragment(R.layout.fragment_create_outfit) {

    private val itemsRepo = ItemsRepository()
    private val outfitsRepo = OutfitsRepository()
    private val pickAdapter = PickItemsAdapter()
    private val db by lazy { FirebaseFirestore.getInstance() }

    private var allItems: List<Item> = emptyList()

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val etName = view.findViewById<EditText>(R.id.etOutfitName)
        val spSeason = view.findViewById<Spinner>(R.id.spSeason)
        val etTag = view.findViewById<EditText>(R.id.etTag)

        val rv = view.findViewById<RecyclerView>(R.id.rvPickItems)
        val btnSuggestStats = view.findViewById<Button>(R.id.btnSuggestStats)
        val btnSuggestAI = view.findViewById<Button>(R.id.btnSuggestAI)
        val btnSave = view.findViewById<Button>(R.id.btnSaveOutfit)
        val progress = view.findViewById<ProgressBar>(R.id.progressSaveOutfit)

        rv.layoutManager = LinearLayoutManager(requireContext())
        rv.adapter = pickAdapter

        val userId = FirebaseAuth.getInstance().currentUser?.uid
        val userEmail = FirebaseAuth.getInstance().currentUser?.email

        if (userId == null) {
            Toast.makeText(requireContext(), "Please login first", Toast.LENGTH_SHORT).show()
            findNavController().navigate(R.id.action_global_login)
            return
        }

        // ✅ DEBUG: תראי בדיוק עם איזה משתמש את עובדת
        Toast.makeText(requireContext(), "UID=$userId\n$emailOrEmpty", Toast.LENGTH_LONG).show()

        // Spinner seasons
        val seasons = listOf("Any", "winter", "summer", "spring", "autumn")
        val seasonAdapter = ArrayAdapter(requireContext(), android.R.layout.simple_spinner_item, seasons)
        seasonAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        spSeason.adapter = seasonAdapter
        spSeason.setSelection(0)

        // Load items
        lifecycleScope.launch {
            try {
                progress.visibility = View.VISIBLE
                allItems = itemsRepo.getMyItems(userId)
                pickAdapter.submitList(allItems)
            } catch (e: Exception) {
                Toast.makeText(requireContext(), "Error loading items: ${e.message}", Toast.LENGTH_LONG).show()
            } finally {
                progress.visibility = View.GONE
            }
        }

        // Suggest by Stats
        btnSuggestStats.setOnClickListener {
            val seasonWanted = spSeason.selectedItem?.toString().orEmpty()
            val tagWanted = etTag.text.toString().trim().lowercase(Locale.getDefault())

            val suggestedIds = suggestByStats(allItems, seasonWanted, tagWanted, maxItems = 5)
            pickAdapter.setSelectedIds(suggestedIds)

            Toast.makeText(
                requireContext(),
                if (suggestedIds.isEmpty()) "No stats suggestions found"
                else "Stats suggested ${suggestedIds.size} items ✅",
                Toast.LENGTH_SHORT
            ).show()
        }

        // Suggest by AI
        btnSuggestAI.setOnClickListener {
            val seasonWanted = spSeason.selectedItem?.toString().orEmpty()
            val tagWanted = etTag.text.toString().trim().lowercase(Locale.getDefault())

            lifecycleScope.launch {
                try {
                    progress.visibility = View.VISIBLE
                    btnSave.isEnabled = false
                    btnSuggestAI.isEnabled = false
                    btnSuggestStats.isEnabled = false

                    val functions = Firebase.functions
                    val payload = hashMapOf(
                        "season" to seasonWanted,
                        "tag" to tagWanted,
                        "maxItems" to 5,
                        "items" to allItems.map {
                            hashMapOf(
                                "id" to it.id,
                                "type" to it.type,
                                "color" to it.color,
                                "season" to it.season,
                                "tags" to it.tags,
                                "wearCount" to it.wearCount,
                                "lastWornAt" to it.lastWornAt
                            )
                        }
                    )

                    val res = functions.getHttpsCallable("aiSuggestOutfit").call(payload).await()
                    val data = res.getData() as Map<*, *>

                    val picked = (data["pickedItemIds"] as? List<*>)?.mapNotNull { it as? String }.orEmpty()
                    pickAdapter.setSelectedIds(picked.toSet())

                    Toast.makeText(
                        requireContext(),
                        if (picked.isEmpty()) "AI: no suggestion"
                        else "AI suggested ${picked.size} items ✅",
                        Toast.LENGTH_SHORT
                    ).show()

                } catch (e: Exception) {
                    Toast.makeText(requireContext(), "AI error: ${e.message}", Toast.LENGTH_LONG).show()
                } finally {
                    progress.visibility = View.GONE
                    btnSave.isEnabled = true
                    btnSuggestAI.isEnabled = true
                    btnSuggestStats.isEnabled = true
                }
            }
        }

        // ✅ Save Outfit (עם בדיקת אמת אחרי כתיבה)
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
                    btnSuggestAI.isEnabled = false
                    btnSuggestStats.isEnabled = false

                    val outfit = Outfit(
                        ownerUid = userId,
                        name = outfitName,
                        itemIds = selected
                    )

                    val newId = outfitsRepo.addOutfit(userId, outfit)

                    // ✅ אמת: לקרוא חזרה את המסמך עכשיו
                    val check = db.collection("users")
                        .document(userId)
                        .collection("outfits")
                        .document(newId)
                        .get()
                        .await()

                    if (check.exists()) {
                        Toast.makeText(requireContext(), "Saved ✅ id=$newId", Toast.LENGTH_LONG).show()
                        findNavController().popBackStack()
                    } else {
                        Toast.makeText(requireContext(), "Saved but not found 😵 id=$newId", Toast.LENGTH_LONG).show()
                    }

                } catch (e: Exception) {
                    Toast.makeText(requireContext(), "Save error: ${e.message}", Toast.LENGTH_LONG).show()
                } finally {
                    progress.visibility = View.GONE
                    btnSave.isEnabled = true
                    btnSuggestAI.isEnabled = true
                    btnSuggestStats.isEnabled = true
                }
            }
        }
    }

    private val emailOrEmpty: String
        get() = FirebaseAuth.getInstance().currentUser?.email ?: ""

    private fun suggestByStats(
        items: List<Item>,
        seasonWanted: String,
        tagWanted: String,
        maxItems: Int
    ): Set<String> {
        val now = System.currentTimeMillis()
        fun daysSince(ts: Long): Long {
            if (ts <= 0) return 9999
            return (now - ts) / (1000L * 60 * 60 * 24)
        }

        val scored = items.map { item ->
            var score = 0.0
            if (seasonWanted.equals("Any", true)) score += 1.0
            else if (item.season.equals(seasonWanted, true)) score += 5.0

            if (tagWanted.isNotBlank() && item.tags.any { it.equals(tagWanted, true) }) score += 4.0
            score += (daysSince(item.lastWornAt).coerceAtMost(60)).toDouble() / 10.0
            score += (10.0 / (1.0 + item.wearCount.toDouble())).coerceAtMost(5.0)

            item.id to score
        }.sortedByDescending { it.second }

        return scored.take(maxItems).map { it.first }.toSet()
    }
}