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

        // ❌ אין Settings Fragment מחובר ל-navigation כרגע
        btnGoSettings.setOnClickListener {
            // TODO: לחבר Settings ל-navigation בעתיד
        }

        // ❌ אין Suggestions Inbox Fragment מחובר ל-navigation כרגע
        btnGoSuggestions.setOnClickListener {
            // TODO: לחבר Suggestions ל-navigation בעתיד
        }

        btnLogout.setOnClickListener {
            FirebaseAuth.getInstance().signOut()
            // ✅ זה ה-ID היחיד שקיים אצלך
            findNavController().navigate(R.id.action_global_login)
        }
    }
}
