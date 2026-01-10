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
import com.google.firebase.auth.FirebaseAuth
import com.example.mycloset.R

class LoginFragment : Fragment() {
    private val auth: FirebaseAuth by lazy{ FirebaseAuth.getInstance() }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val etEmail = view.findViewById<EditText>(R.id.etEmail)
        val etPassword = view.findViewById<EditText>(R.id.etPassword)
        val btnLogin = view.findViewById<Button>(R.id.btnLogin)
        val tvGoRegister = view.findViewById<TextView>(R.id.tvGoRegister)

        tvGoRegister.setOnClickListener {
            findNavController().navigate(R.id.action_nav_login_to_nav_register)
        }

        btnLogin.setOnClickListener {
            val email = etEmail.text.toString().trim()
            val pass = etPassword.text.toString().trim()

            if(email.isEmpty() || pass.isEmpty()){
                Toast.makeText(requireContext(), "Enter email and password", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
             auth.signInWithEmailAndPassword(email,pass)
                 .addOnSuccessListener {
                 val uid = auth.currentUser?.uid
                 Log.d("AUTH_TEST", "Login OK uid=$uid")
                 findNavController().navigate(R.id.action_nav_login_to_nav_home)
                }
                 .addOnFailureListener { e ->
                     Log.e("AUTH_TEST", "Login Failed", e)
                     Toast.makeText(requireContext(), "Login failed: ${e.message}", Toast.LENGTH_LONG).show()
                 }
        }

    }
}