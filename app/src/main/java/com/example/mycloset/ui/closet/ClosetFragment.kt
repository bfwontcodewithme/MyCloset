package com.example.mycloset.ui.closet

import android.os.Bundle
import android.view.View
import android.widget.Button
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import com.example.mycloset.R
import com.google.firebase.auth.FirebaseAuth

class ClosetFragment : Fragment(R.layout.fragment_closet) {

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val btnAdd = view.findViewById<Button>(R.id.btnAddGarment)
        val btnOutfit = view.findViewById<Button>(R.id.btnCreateOutfit)

        btnAdd.setOnClickListener {
            val user = FirebaseAuth.getInstance().currentUser
            if (user == null) {
                Toast.makeText(requireContext(), "Please login first", Toast.LENGTH_SHORT).show()
                findNavController().navigate(R.id.action_global_login)
            } else {
                findNavController().navigate(R.id.action_nav_closet_to_nav_add_item)
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
    }
}
