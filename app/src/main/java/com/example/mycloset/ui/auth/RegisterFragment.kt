package com.example.mycloset.ui.auth

import android.os.Bundle
import android.util.Patterns
import android.view.View
import android.widget.ProgressBar
import android.widget.RadioButton
import android.widget.RadioGroup
import android.widget.TextView
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import com.example.mycloset.R
import com.google.android.material.button.MaterialButton
import com.google.android.material.textfield.TextInputEditText
import com.google.android.material.textfield.TextInputLayout
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore

class RegisterFragment : Fragment(R.layout.fragment_register) {

    private val auth by lazy { FirebaseAuth.getInstance() }
    private val db by lazy { FirebaseFirestore.getInstance() }

    private lateinit var tilEmail: TextInputLayout
    private lateinit var tilPassword: TextInputLayout
    private lateinit var tilConfirm: TextInputLayout
    private lateinit var etEmail: TextInputEditText
    private lateinit var etPassword: TextInputEditText
    private lateinit var etConfirm: TextInputEditText
    private lateinit var rgRole: RadioGroup
    private lateinit var rbUser: RadioButton
    private lateinit var rbStylist: RadioButton
    private lateinit var btnRegister: MaterialButton
    private lateinit var tvGoLogin: TextView
    private lateinit var progress: ProgressBar

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        tilEmail = view.findViewById(R.id.tilEmail)
        tilPassword = view.findViewById(R.id.tilPassword)
        tilConfirm = view.findViewById(R.id.tilConfirmPassword)

        etEmail = view.findViewById(R.id.etEmail)
        etPassword = view.findViewById(R.id.etPassword)
        etConfirm = view.findViewById(R.id.etConfirmPassword)

        rgRole = view.findViewById(R.id.rgRole)
        rbUser = view.findViewById(R.id.rbUser)
        rbStylist = view.findViewById(R.id.rbStylist)

        btnRegister = view.findViewById(R.id.btnRegister)
        tvGoLogin = view.findViewById(R.id.tvGoLogin)
        progress = view.findViewById(R.id.progressRegister)

        tvGoLogin.setOnClickListener { findNavController().navigateUp() }
        btnRegister.setOnClickListener { doRegister() }
    }

    private fun doRegister() {
        clearErrors()

        val email = etEmail.text?.toString()?.trim().orEmpty()
        val pass = etPassword.text?.toString()?.trim().orEmpty()
        val confirm = etConfirm.text?.toString()?.trim().orEmpty()

        var ok = true
        if (email.isBlank() || !Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            tilEmail.error = "Please enter a valid email"
            ok = false
        }
        if (pass.length < 6) {
            tilPassword.error = "Password must be at least 6 characters"
            ok = false
        }
        if (confirm != pass) {
            tilConfirm.error = "Passwords do not match"
            ok = false
        }
        if (!ok) return

        val role = if (rbStylist.isChecked) "STYLIST" else "USER"
        val defaultName = email.substringBefore("@")

        setLoading(true)
        auth.createUserWithEmailAndPassword(email, pass)
            .addOnSuccessListener {
                val uid = auth.currentUser?.uid ?: run {
                    setLoading(false)
                    return@addOnSuccessListener
                }

                val userDoc = hashMapOf(
                    "userEmail" to email,
                    "userName" to defaultName,
                    "role" to role,
                    "createdAt" to FieldValue.serverTimestamp(),
                    "userFriendsUids" to emptyList<String>(),
                    "fcmToken" to "",
                    "profileImageUrl" to ""
                )

                db.collection("users").document(uid).set(userDoc)
                    .addOnSuccessListener {
                        setLoading(false)
                        if (role == "STYLIST") {
                            findNavController().navigate(R.id.action_global_stylist_home)
                        } else {
                            findNavController().navigate(R.id.action_global_home)
                        }
                    }
                    .addOnFailureListener { e ->
                        setLoading(false)
                        toast("Failed to save user: ${e.message}")
                    }
            }
            .addOnFailureListener { e ->
                setLoading(false)
                toast("Register failed: ${e.message}")
            }
    }

    private fun setLoading(loading: Boolean) {
        progress.visibility = if (loading) View.VISIBLE else View.GONE
        btnRegister.isEnabled = !loading
        tvGoLogin.isEnabled = !loading
    }

    private fun clearErrors() {
        tilEmail.error = null
        tilPassword.error = null
        tilConfirm.error = null
    }

    private fun toast(msg: String) {
        Toast.makeText(requireContext(), msg, Toast.LENGTH_LONG).show()
    }
}
