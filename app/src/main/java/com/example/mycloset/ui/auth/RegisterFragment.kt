package com.example.mycloset.ui.auth

import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import com.example.mycloset.R
import com.google.firebase.auth.FirebaseAuth

class RegisterFragment : Fragment(R.layout.fragment_register) {

    private val auth: FirebaseAuth by lazy { FirebaseAuth.getInstance() }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val etEmail = view.findViewById<EditText>(R.id.etEmail)
        val etPassword = view.findViewById<EditText>(R.id.etPassword)
        val btnRegister = view.findViewById<Button>(R.id.btnRegister)
        val tvGoLogin = view.findViewById<TextView>(R.id.tvGoLogin)

        tvGoLogin.setOnClickListener {
            findNavController().navigateUp()
        }

        btnRegister.setOnClickListener {
            val email = etEmail.text.toString().trim()
            val pass = etPassword.text.toString().trim()

            if (email.isEmpty() || pass.isEmpty()) {
                Toast.makeText(requireContext(), "Enter email + password", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            auth.createUserWithEmailAndPassword(email, pass)
                .addOnSuccessListener {
                    val uid = auth.currentUser?.uid
                    Log.d("AUTH_TEST", "Register OK uid=$uid")
                    findNavController().navigate(R.id.action_nav_register_to_nav_home)
                }
                .addOnFailureListener { e ->
                    Log.e("AUTH_TEST", "Register FAILED", e)
                    Toast.makeText(requireContext(), "Register failed: ${e.message}", Toast.LENGTH_LONG).show()
                }
        }
    }
}