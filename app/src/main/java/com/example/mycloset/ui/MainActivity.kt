package com.example.mycloset.ui

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.util.Log
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.app.AppCompatDelegate
import androidx.core.content.ContextCompat
import androidx.core.view.GravityCompat
import androidx.drawerlayout.widget.DrawerLayout
import androidx.navigation.NavController
import androidx.navigation.fragment.NavHostFragment
import androidx.navigation.ui.AppBarConfiguration
import androidx.navigation.ui.navigateUp
import androidx.navigation.ui.setupActionBarWithNavController
import androidx.navigation.ui.setupWithNavController
import com.example.mycloset.R
import com.example.mycloset.util.NotificationHelper
import com.google.android.material.appbar.MaterialToolbar
import com.google.android.material.navigation.NavigationView
import com.google.firebase.Firebase
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.auth
import com.google.firebase.firestore.firestore
import com.google.firebase.messaging.FirebaseMessaging
import kotlin.math.log


class MainActivity : AppCompatActivity() {

    private lateinit var drawerLayout: DrawerLayout
    private lateinit var navController: NavController
    private lateinit var appBarConfiguration: AppBarConfiguration
//    private lateinit var notificationHelper: NotificationHelper
    private lateinit var  firebaseMessaging: FirebaseMessaging
    private val TAG = "FCM_MainActivity"
    private val requestNotificationLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()) { isGranted: Boolean ->
            if (isGranted){
                // Permission Granted
            } else {
                Toast.makeText(this,
                    "Permission denied. You won't receive updates alerts.",
                    Toast.LENGTH_LONG
                ).show()
            }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        // ✅ Force LIGHT MODE (חשוב לפני super.onCreate)
        AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO)

        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        val toolbar = findViewById<MaterialToolbar>(R.id.mainToolbar)
        setSupportActionBar(toolbar)

        drawerLayout = findViewById(R.id.drawerLayout)
        val navView = findViewById<NavigationView>(R.id.navView)

        // ✅ הדרך הנכונה והיציבה להביא NavController
        val navHostFragment =
            supportFragmentManager.findFragmentById(R.id.nav_host_fragment) as NavHostFragment
        navController = navHostFragment.navController

        appBarConfiguration = AppBarConfiguration(
            setOf(
                R.id.nav_home,
                R.id.nav_closet_list,
                R.id.nav_outfit_list,
                R.id.nav_profile,
                R.id.nav_settings
            ),
            drawerLayout
        )

        setupActionBarWithNavController(navController, appBarConfiguration)
        navView.setupWithNavController(navController)

        navView.setNavigationItemSelectedListener { item ->
            when (item.itemId) {
                R.id.nav_logout -> {
                    drawerLayout.closeDrawer(GravityCompat.START)
                    FirebaseAuth.getInstance().signOut()
                    navController.navigate(R.id.action_global_login)
                    true
                }
                else -> {
                    val handled =
                        androidx.navigation.ui.NavigationUI.onNavDestinationSelected(item, navController)
                    if (handled) drawerLayout.closeDrawer(GravityCompat.START)
                    handled
                }
            }
        }

        // לא לאפשר Drawer ב-Login/Register
        navController.addOnDestinationChangedListener { _, destination, _ ->
            val showDrawer = destination.id != R.id.nav_login && destination.id != R.id.nav_register
            drawerLayout.setDrawerLockMode(
                if (showDrawer) DrawerLayout.LOCK_MODE_UNLOCKED else DrawerLayout.LOCK_MODE_LOCKED_CLOSED
            )


            if (destination.id == R.id.nav_login) {
                val currentUser = Firebase.auth.currentUser
                if (currentUser != null) {
                    // We are on Login screen but we have a user -> Jump to Home!
                    checkUserAndNavigate(currentUser.uid)
                }
            }
        }


        val helper = NotificationHelper(this)
        helper.createNotificationChannels()
        checkNotificationPermission()

    }

    private fun checkNotificationPermission() {
        // Only Android 13+ (API 33) needs this popup
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS)
                != PackageManager.PERMISSION_GRANTED) {

                // This starts the actual system popup
                requestNotificationLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
            }
        }
    }
    private fun refreshToken(uid: String){
        FirebaseMessaging.getInstance().token.addOnCompleteListener { task ->
            if(task.isSuccessful){
                val token = task.result
                Firebase.firestore.collection("users").document(uid)
                    .update("fcmToken", token).addOnSuccessListener { Log.d(TAG, "Token refreshed for logged user") }
            }
        }
    }
    override fun onSupportNavigateUp(): Boolean {
        return navController.navigateUp(appBarConfiguration) || super.onSupportNavigateUp()
    }

    private fun checkUserAndNavigate(uid: String){
        refreshToken(uid)
        Log.d(TAG, "User found: ${uid}. Checking role..")

        Firebase.firestore.collection("users").document(uid).get()
            .addOnSuccessListener { documentSnapshot ->
                val role = documentSnapshot.getString("role")

                drawerLayout.post {
                    if(navController.currentDestination?.id == R.id.nav_login) {
                        try {
                            if (role == "stylist") {
                                navController.navigate(R.id.action_global_stylist_home) {
                                    popUpTo(R.id.nav_login) { inclusive = true }
                                }
                            } else {
                                navController.navigate(R.id.action_global_home) {
                                    popUpTo(R.id.nav_login) { inclusive = true }
                                }
                            }
                        }catch (e: Exception){
                            Log.e(TAG, "Navigation Failed: ${e.message}")
                        }
                    }
                }
            }
            .addOnFailureListener {
                Log.e(TAG, "Error fetching user role", it)
            }
    }

}
