package com.example.mycloset.ui.profile

import android.os.Bundle
import android.view.View
import android.widget.Button
import android.widget.TextView
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import com.example.mycloset.R
import com.google.firebase.auth.FirebaseAuth

class ProfileFragment : Fragment(R.layout.fragment_profile) {

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val tvUserEmail = view.findViewById<TextView>(R.id.tvUserEmail)
        val btnGoSettings = view.findViewById<Button>(R.id.btnGoSettings)
        val btnGoSuggestions = view.findViewById<Button>(R.id.btnGoSuggestions)
        val btnLogout = view.findViewById<Button>(R.id.btnLogoutProfile)

        val user = FirebaseAuth.getInstance().currentUser
        tvUserEmail.text = "Email: ${user?.email ?: "Unknown"}"

        btnGoSettings.setOnClickListener {
            findNavController().navigate(R.id.action_homeFragment_to_settingsFragment) // אם לא נוח, אפשר ישירות R.id.settingsFragment
            // אופציה בטוחה יותר:
            // findNavController().navigate(R.id.settingsFragment)
        }

        btnGoSuggestions.setOnClickListener {
            findNavController().navigate(R.id.suggestionsInboxFragment)
        }

        btnLogout.setOnClickListener {
            FirebaseAuth.getInstance().signOut()
            findNavController().navigate(R.id.action_global_loginFragment)
        }
    }
}