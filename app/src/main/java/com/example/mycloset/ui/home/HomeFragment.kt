package com.example.mycloset.ui.home

import android.os.Bundle
import android.view.View
import android.widget.Button
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import com.example.mycloset.R
import com.google.firebase.auth.FirebaseAuth

class HomeFragment : Fragment(R.layout.fragment_home) {

    private val auth by lazy { FirebaseAuth.getInstance() }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val btnGoClosets = view.findViewById<Button>(R.id.btnGoClosets)
        val btnGoOutfits = view.findViewById<Button>(R.id.btnGoOutfits)
        val btnAddItem = view.findViewById<Button>(R.id.btnAddItem)
        val btnMyItems = view.findViewById<Button>(R.id.btnMyItems)
        val btnRequestStylist = view.findViewById<Button>(R.id.btnRequestStylist)

        // ✅ חדש
        val btnMyRequests = view.findViewById<Button>(R.id.btnMyRequests)

        val btnLogout = view.findViewById<Button>(R.id.btnLogout)

        btnRequestStylist.setOnClickListener {
            findNavController().navigate(R.id.action_nav_home_to_nav_stylist_list)
        }

        btnGoClosets.setOnClickListener {
            findNavController().navigate(R.id.action_nav_home_to_nav_closet)
        }

        btnAddItem.setOnClickListener {
            findNavController().navigate(R.id.action_nav_home_to_nav_add_item)
        }

        btnGoOutfits.setOnClickListener {
            findNavController().navigate(R.id.action_nav_home_to_nav_create_outfit)
        }

        btnMyItems.setOnClickListener {
            findNavController().navigate(R.id.action_nav_home_to_nav_closet)
        }

        // ✅ My Requests -> המסך של הבקשות
        btnMyRequests.setOnClickListener {
            findNavController().navigate(R.id.nav_my_requests)
            // אם תרצי בצורה “יותר נקייה” עם action, נגיד לי ונעשה action ב־nav.
        }

        btnLogout.setOnClickListener {
            auth.signOut()
            findNavController().navigate(R.id.action_global_login)
        }
    }
}
