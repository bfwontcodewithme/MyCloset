// ui/home/HomeFragment.kt
package com.example.mycloset.ui.home

import android.os.Bundle
import android.view.View
import android.widget.Button
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import com.example.mycloset.R
import com.google.firebase.auth.FirebaseAuth

class MainHomeFragment : Fragment(R.layout.fragment_home) {

    private val auth by lazy { FirebaseAuth.getInstance() }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val btnGoClosets = view.findViewById<Button>(R.id.btnGoClosets)
        val btnGoOutfits = view.findViewById<Button>(R.id.btnGoOutfits)
        val btnAddItem = view.findViewById<Button>(R.id.btnAddItem)
        val btnMyItems = view.findViewById<Button>(R.id.btnMyItems)
        val btnRequestStylist = view.findViewById<Button>(R.id.btnRequestStylist)
        val btnMyRequests = view.findViewById<Button>(R.id.btnMyRequests)
        val btnLogout = view.findViewById<Button>(R.id.btnLogout)

        btnGoClosets.setOnClickListener {
            findNavController().navigate(R.id.action_nav_home_to_nav_closet)
        }

        // ✅ Outfits הולך לרשימת Outfits (לא ל-Create)
        btnGoOutfits.setOnClickListener {
            findNavController().navigate(R.id.action_nav_home_to_nav_outfit_list)
        }

        btnAddItem.setOnClickListener {
            findNavController().navigate(R.id.action_nav_home_to_nav_add_item)
        }


        btnMyItems.setOnClickListener {
            findNavController().navigate(R.id.action_nav_home_to_nav_closet)
        }

        btnRequestStylist.setOnClickListener {
            findNavController().navigate(R.id.action_nav_home_to_nav_stylist_list)
        }

        btnMyRequests.setOnClickListener {
            findNavController().navigate(R.id.nav_my_requests)
        }

        btnLogout.setOnClickListener {
            auth.signOut()
            findNavController().navigate(R.id.action_global_login)
        }
    }
}
