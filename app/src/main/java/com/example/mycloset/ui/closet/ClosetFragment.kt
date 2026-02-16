package com.example.mycloset.ui.closet

import android.os.Bundle
import android.view.View
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import com.example.mycloset.R
import com.google.firebase.auth.FirebaseAuth

class ClosetFragment : Fragment(R.layout.fragment_closet) {

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val closetId = arguments?.getString("closetId").orEmpty()
        val closetName = arguments?.getString("closetName").orEmpty()

        val tvTitle = view.findViewById<TextView>(R.id.tvClosetTitle)
        tvTitle.text = if (closetName.isNotBlank()) closetName else "Closet"

        if (closetId.isBlank()) {
            Toast.makeText(requireContext(), "Missing closetId", Toast.LENGTH_SHORT).show()
            findNavController().navigateUp()
            return
        }

        val btnViewItems = view.findViewById<Button>(R.id.btnViewItems)
        val btnAdd = view.findViewById<Button>(R.id.btnAddGarment)
        val btnOutfit = view.findViewById<Button>(R.id.btnCreateOutfit)

        // ✅ NEW
        val btnShareCloset = view.findViewById<Button>(R.id.btnShareCloset)

        btnViewItems.setOnClickListener {
            val args = Bundle().apply {
                putString("closetId", closetId)
                putString("closetName", closetName)
            }
            findNavController().navigate(R.id.action_nav_closet_to_nav_items_list, args)
        }

        btnAdd.setOnClickListener {
            val user = FirebaseAuth.getInstance().currentUser
            if (user == null) {
                Toast.makeText(requireContext(), "Please login first", Toast.LENGTH_SHORT).show()
                findNavController().navigate(R.id.action_global_login)
            } else {
                val b = Bundle().apply { putString("closetId", closetId) }
                findNavController().navigate(R.id.action_nav_closet_to_nav_add_item, b)
            }
        }

        btnOutfit.setOnClickListener {
            val user = FirebaseAuth.getInstance().currentUser
            if (user == null) {
                Toast.makeText(requireContext(), "Please login first", Toast.LENGTH_SHORT).show()
                findNavController().navigate(R.id.action_global_login)
            } else {
                findNavController().navigate(R.id.action_nav_closet_to_nav_create_outfit)
            }
        }

        // ✅ Share Closet Access
        btnShareCloset.setOnClickListener {
            val user = FirebaseAuth.getInstance().currentUser
            if (user == null) {
                Toast.makeText(requireContext(), "Please login first", Toast.LENGTH_SHORT).show()
                findNavController().navigate(R.id.action_global_login)
                return@setOnClickListener
            }

            val args = Bundle().apply {
                putString("resourceType", "CLOSET")
                putString("resourceId", closetId)
            }
            findNavController().navigate(R.id.nav_share_access, args)
        }
    }
}
