package com.example.mycloset.ui.auth

import android.app.Activity
import android.app.AlertDialog
import android.os.Bundle
import android.util.Patterns
import android.view.View
import android.widget.ProgressBar
import android.widget.TextView
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import com.example.mycloset.R
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.android.gms.common.api.ApiException
import com.google.android.material.button.MaterialButton
import com.google.android.material.textfield.TextInputEditText
import com.google.android.material.textfield.TextInputLayout
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.GoogleAuthProvider
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.messaging.FirebaseMessaging
import com.google.firebase.firestore.SetOptions


class LoginFragment : Fragment(R.layout.fragment_login) {

    private val auth by lazy { FirebaseAuth.getInstance() }
    private val db by lazy { FirebaseFirestore.getInstance() }

    private lateinit var tilEmail: TextInputLayout
    private lateinit var tilPassword: TextInputLayout
    private lateinit var etEmail: TextInputEditText
    private lateinit var etPassword: TextInputEditText
    private lateinit var btnLogin: MaterialButton
    private lateinit var btnGoogle: MaterialButton
    private lateinit var tvGoRegister: TextView
    private lateinit var tvForgotPassword: TextView
    private lateinit var progress: ProgressBar

    private val googleLauncher =
        registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->

            // המשתמש ביטל בחירת חשבון
            if (result.resultCode != Activity.RESULT_OK) {
                toast("Google sign-in canceled")
                return@registerForActivityResult
            }

            try {
                val task = GoogleSignIn.getSignedInAccountFromIntent(result.data)
                val account = task.getResult(ApiException::class.java)

                val idToken = account.idToken
                if (idToken.isNullOrBlank()) {
                    toast("Google sign-in failed: idToken is null (check SHA-1 + google-services.json)")
                    return@registerForActivityResult
                }

                val credential = GoogleAuthProvider.getCredential(idToken, null)

                setLoading(true)
                auth.signInWithCredential(credential)
                    .addOnSuccessListener {
                        val user = auth.currentUser ?: run {
                            setLoading(false)
                            toast("Google sign-in failed: user is null")
                            return@addOnSuccessListener
                        }
                        ensureUserDocExists(uid = user.uid, email = user.email ?: "")
                    }
                    .addOnFailureListener { e ->
                        setLoading(false)
                        toast("Firebase auth failed: ${e.message}")
                    }

            } catch (e: ApiException) {
                // פה את מקבלת קוד מדויק כמו 10 / 12500
                // 10 = DEVELOPER_ERROR (SHA-1 / client mismatch)
                // 12500 = config/play-services issue
                toast("Google sign-in failed (ApiException ${e.statusCode}): ${e.message}")
            } catch (e: Exception) {
                toast("Google sign-in failed: ${e.javaClass.simpleName}: ${e.message}")
            }
        }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        tilEmail = view.findViewById(R.id.tilEmail)
        tilPassword = view.findViewById(R.id.tilPassword)
        etEmail = view.findViewById(R.id.etEmail)
        etPassword = view.findViewById(R.id.etPassword)
        btnLogin = view.findViewById(R.id.btnLogin)
        btnGoogle = view.findViewById(R.id.btnGoogle)
        tvGoRegister = view.findViewById(R.id.tvGoRegister)
        tvForgotPassword = view.findViewById(R.id.tvForgotPassword)
        progress = view.findViewById(R.id.progressLogin)

        tvGoRegister.setOnClickListener {
            findNavController().navigate(R.id.action_nav_login_to_nav_register)
        }

        tvForgotPassword.setOnClickListener { showResetPasswordDialog() }
        btnLogin.setOnClickListener { doEmailPasswordLogin() }
        btnGoogle.setOnClickListener { doGoogleLogin() }
    }

    private fun doEmailPasswordLogin() {
        clearErrors()

        val email = etEmail.text?.toString()?.trim().orEmpty()
        val pass = etPassword.text?.toString()?.trim().orEmpty()

        var ok = true
        if (email.isBlank() || !Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            tilEmail.error = "Please enter a valid email"
            ok = false
        }
        if (pass.length < 6) {
            tilPassword.error = "Password must be at least 6 characters"
            ok = false
        }
        if (!ok) return

        setLoading(true)
        auth.signInWithEmailAndPassword(email, pass)
            .addOnSuccessListener {
                val user = auth.currentUser ?: run { setLoading(false); return@addOnSuccessListener }
                ensureUserDocExists(uid = user.uid, email = user.email ?: email)
            }
            .addOnFailureListener { e ->
                setLoading(false)
                toast("Login failed: ${e.message}")
            }
    }

    private fun doGoogleLogin() {
        val webClientId = getString(R.string.default_web_client_id)

        if (webClientId.isBlank() || webClientId == "YOUR_WEB_CLIENT_ID") {
            toast("default_web_client_id missing. Check google-services.json location + Sync.")
            return
        }

        val gso = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
            .requestIdToken(webClientId)
            .requestEmail()
            .build()

        val client = GoogleSignIn.getClient(requireActivity(), gso)

        // מומלץ כדי לא “להיתקע” עם חשבון ישן
        client.signOut().addOnCompleteListener {
            googleLauncher.launch(client.signInIntent)
        }
    }

    private fun showResetPasswordDialog() {
        val input = android.widget.EditText(requireContext()).apply { hint = "Email" }

        AlertDialog.Builder(requireContext())
            .setTitle("Reset password")
            .setMessage("Enter your email and we'll send a reset link.")
            .setView(input)
            .setPositiveButton("Send") { _, _ ->
                val email = input.text.toString().trim()
                if (!Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
                    toast("Enter a valid email")
                    return@setPositiveButton
                }

                setLoading(true)
                auth.sendPasswordResetEmail(email)
                    .addOnSuccessListener { setLoading(false); toast("Reset email sent") }
                    .addOnFailureListener { e -> setLoading(false); toast("Failed: ${e.message}") }
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun ensureUserDocExists(uid: String, email: String) {
        val ref = db.collection("users").document(uid)

        ref.get()
            .addOnSuccessListener { snap ->
                if (snap.exists()) {
                    updateFcmToken()
                    routeByRole(uid)
                } else {
                    val data = hashMapOf(
                        "email" to email,
                        "role" to "USER",
                        "createdAt" to FieldValue.serverTimestamp()
                    )
                    ref.set(data)
                        .addOnSuccessListener {
                            updateFcmToken()
                            routeByRole(uid) }
                        .addOnFailureListener { e -> setLoading(false); toast("Failed saving user: ${e.message}") }
                }
            }
            .addOnFailureListener { e ->
                setLoading(false)
                toast("Failed reading user: ${e.message}")
            }
    }

    private fun routeByRole(uid: String) {
        db.collection("users").document(uid).get()
            .addOnSuccessListener { snap ->
                setLoading(false)

                val role = snap.getString("role") ?: "REGULAR"

                if (role.uppercase() == "STYLIST") {
                    findNavController().navigate(R.id.action_global_stylist_home)
                } else {
                    findNavController().navigate(R.id.action_global_home)
                }
            }
            .addOnFailureListener { e ->
                setLoading(false)
                toast("Failed reading role: ${e.message}")
                findNavController().navigate(R.id.action_global_home)
            }
    }

    private fun updateFcmToken() {
        val uid = auth.currentUser?.uid ?: return

        FirebaseMessaging.getInstance().token
            .addOnSuccessListener { token ->
                db.collection("users")
                    .document(uid)
                    .set(mapOf("fcmToken" to token), SetOptions.merge())
            }
    }



    private fun setLoading(loading: Boolean) {
        progress.visibility = if (loading) View.VISIBLE else View.GONE
        btnLogin.isEnabled = !loading
        btnGoogle.isEnabled = !loading
        tvGoRegister.isEnabled = !loading
        tvForgotPassword.isEnabled = !loading
    }

    private fun clearErrors() {
        tilEmail.error = null
        tilPassword.error = null
    }

    private fun toast(msg: String) {
        Toast.makeText(requireContext(), msg, Toast.LENGTH_LONG).show()
    }

}
