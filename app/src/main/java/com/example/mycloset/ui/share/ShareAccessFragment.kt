package com.example.mycloset.ui.share

import android.os.Bundle
import android.view.View
import android.widget.*
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import com.example.mycloset.R
import com.example.mycloset.data.model.AccessGrant
import com.example.mycloset.data.repository.GrantsRepository
import com.google.firebase.auth.FirebaseAuth
import kotlinx.coroutines.launch

class ShareAccessFragment : Fragment(R.layout.fragment_share_access) {

    private val grantsRepo = GrantsRepository()

    private var pickedFriendUid: String? = null
    private var pickedFriendLabel: String? = null

    //  הרשימת פריטים לשיתוף subset
    private var pickedItemIds: ArrayList<String> = arrayListOf()

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val tvTitle = view.findViewById<TextView>(R.id.tvTitle)
        val btnChooseFriend = view.findViewById<Button>(R.id.btnChooseFriend)
        val tvChosenFriend = view.findViewById<TextView>(R.id.tvChosenFriend)

        val tvResource = view.findViewById<TextView>(R.id.tvResource)
        val cbViewItems = view.findViewById<CheckBox>(R.id.cbViewItems)
        val cbSuggestOutfit = view.findViewById<CheckBox>(R.id.cbSuggestOutfit)

        val btnShare = view.findViewById<Button>(R.id.btnShare)
        val progress = view.findViewById<ProgressBar>(R.id.progressShare)

        val ownerUid = FirebaseAuth.getInstance().currentUser?.uid
        if (ownerUid == null) {
            Toast.makeText(requireContext(), "Please login", Toast.LENGTH_SHORT).show()
            findNavController().navigate(R.id.action_global_login)
            return
        }

        val resourceType = arguments?.getString("resourceType").orEmpty() // "CLOSET" / "OUTFIT"
        val resourceId = arguments?.getString("resourceId").orEmpty()

        tvTitle.text = if (resourceType == "CLOSET") "Share Closet Access" else "Share Outfit Access"
        tvResource.text = "Resource: $resourceType / $resourceId"

        cbViewItems.isEnabled = resourceType == "CLOSET"
        cbSuggestOutfit.isEnabled = resourceType == "OUTFIT"

        val handle = findNavController().currentBackStackEntry?.savedStateHandle

        handle?.getLiveData<String>("pickedFriendUid")?.observe(viewLifecycleOwner) { uid ->
            pickedFriendUid = uid
            handle.remove<String>("pickedFriendUid")
        }

        handle?.getLiveData<String>("pickedFriendLabel")?.observe(viewLifecycleOwner) { label ->
            pickedFriendLabel = label
            tvChosenFriend.text = label
            handle.remove<String>("pickedFriendLabel")
        }

        //  מקבל את itemIds מהמסך PickItemsToShareFragment
        handle?.getLiveData<ArrayList<String>>("pickedItemIds")?.observe(viewLifecycleOwner) { ids ->
            pickedItemIds = ids ?: arrayListOf()
            Toast.makeText(requireContext(), "Selected ${pickedItemIds.size} items", Toast.LENGTH_SHORT).show()
            handle.remove<ArrayList<String>>("pickedItemIds")
        }

        btnShare.setOnClickListener {
            if (resourceType.isBlank() || resourceId.isBlank()) {
                Toast.makeText(requireContext(), "Missing resource", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            val friendUid = pickedFriendUid
            if (friendUid.isNullOrBlank()) {
                Toast.makeText(requireContext(), "Choose a friend first", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            val permissions = mutableListOf<String>()
            if (resourceType == "CLOSET" && cbViewItems.isChecked) permissions.add("VIEW_ITEMS")
            if (resourceType == "OUTFIT" && cbSuggestOutfit.isChecked) permissions.add("SUGGEST_OUTFIT")

            if (permissions.isEmpty()) {
                Toast.makeText(requireContext(), "Choose at least one permission", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            //  CLOSET subset: קודם בוחרים items
            if (resourceType == "CLOSET" && cbViewItems.isChecked && pickedItemIds.isEmpty()) {
                val args = Bundle().apply {
                    putString("closetId", resourceId)
                }
                findNavController().navigate(R.id.nav_pick_items_to_share, args)
                return@setOnClickListener
            }

            lifecycleScope.launch {
                try {
                    progress.visibility = View.VISIBLE
                    btnShare.isEnabled = false

                    val grant = AccessGrant(
                        ownerUid = ownerUid,
                        granteeUid = friendUid,
                        granteeEmail = pickedFriendLabel ?: "",
                        resourceType = resourceType,
                        resourceId = resourceId,
                        permissions = permissions,
                        itemIds = if (resourceType == "CLOSET") pickedItemIds else emptyList()
                    )

                    grantsRepo.upsertGrant(grant)

                    Toast.makeText(requireContext(), "Shared ", Toast.LENGTH_SHORT).show()
                    findNavController().popBackStack()

                } catch (e: Exception) {
                    Toast.makeText(requireContext(), "Error: ${e.message}", Toast.LENGTH_LONG).show()
                } finally {
                    progress.visibility = View.GONE
                    btnShare.isEnabled = true
                }
            }
        }
    }
}
