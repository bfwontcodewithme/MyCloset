package com.example.mycloset.ui.stylist

import android.Manifest
import android.content.pm.PackageManager
import android.location.Location
import android.os.Bundle
import android.view.View
import android.widget.ArrayAdapter
import android.widget.ListView
import android.widget.ProgressBar
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import com.example.mycloset.R
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
import com.google.android.gms.tasks.CancellationTokenSource
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.GeoPoint
import kotlin.math.roundToInt

class StylistListFragment : Fragment(R.layout.fragment_stylist_list) {

    private val db by lazy { FirebaseFirestore.getInstance() }
    private val auth by lazy { FirebaseAuth.getInstance() }
    private val fusedClient by lazy { LocationServices.getFusedLocationProviderClient(requireActivity()) }

    private lateinit var listView: ListView
    private lateinit var progress: ProgressBar

    private val stylistUids = mutableListOf<String>()
    private val stylistTitles = mutableListOf<String>()

    private val permissionLauncher =
        registerForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) { perms ->
            val granted = perms[Manifest.permission.ACCESS_FINE_LOCATION] == true ||
                    perms[Manifest.permission.ACCESS_COARSE_LOCATION] == true

            if (granted) {
                loadUserLocationAndStylists()
            } else {
                toast("Location permission denied")
                loadStylistsWithoutDistance()
            }
        }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        listView = view.findViewById(R.id.listStylists)
        progress = view.findViewById(R.id.progressStylists)

        loadUserLocationAndStylists()
    }

    private fun loadUserLocationAndStylists() {
        if (!hasLocationPermission()) {
            requestLocationPermission()
            return
        }

        progress.visibility = View.VISIBLE

        val token = CancellationTokenSource()
        fusedClient.getCurrentLocation(Priority.PRIORITY_HIGH_ACCURACY, token.token)
            .addOnSuccessListener { loc ->
                if (loc == null) {
                    progress.visibility = View.GONE
                    toast("Couldn't get your location")
                    loadStylistsWithoutDistance()
                    return@addOnSuccessListener
                }
                loadStylistsWithDistance(loc.latitude, loc.longitude)
            }
            .addOnFailureListener {
                progress.visibility = View.GONE
                toast("Location error: ${it.message}")
                loadStylistsWithoutDistance()
            }
    }

    private fun loadStylistsWithDistance(lat: Double, lng: Double) {
        db.collection("users")
            .whereEqualTo("role", "STYLIST")
            .get()
            .addOnSuccessListener { snap ->
                progress.visibility = View.GONE
                stylistUids.clear()
                stylistTitles.clear()

                val results = mutableListOf<Triple<String, String, Float>>() // uid, title, distanceMeters

                for (doc in snap.documents) {
                    val uid = doc.id
                    val email = doc.getString("email") ?: "(no email)"

                    val locMap = doc.get("location") as? Map<*, *>
                    val geo = locMap?.get("geo") as? GeoPoint
                    if (geo == null) continue

                    val distance = distanceMeters(lat, lng, geo.latitude, geo.longitude)
                    val km = (distance / 1000f * 10).roundToInt() / 10.0
                    val title = "$email • ${km}km"

                    results.add(Triple(uid, title, distance))
                }

                results.sortBy { it.third }

                for (r in results) {
                    stylistUids.add(r.first)
                    stylistTitles.add(r.second)
                }

                if (stylistTitles.isEmpty()) {
                    toast("No stylists with location yet")
                }

                val adapter = ArrayAdapter(
                    requireContext(),
                    android.R.layout.simple_list_item_1,
                    stylistTitles
                )
                listView.adapter = adapter

                listView.setOnItemClickListener { _, _, position, _ ->
                    val chosenStylistId = stylistUids[position]
                    createStylingRequest(chosenStylistId)
                }
            }
            .addOnFailureListener { e ->
                progress.visibility = View.GONE
                toast("Failed: ${e.message}")
            }
    }

    private fun loadStylistsWithoutDistance() {
        progress.visibility = View.VISIBLE

        db.collection("users")
            .whereEqualTo("role", "STYLIST")
            .get()
            .addOnSuccessListener { snap ->
                progress.visibility = View.GONE
                stylistUids.clear()
                stylistTitles.clear()

                for (doc in snap.documents) {
                    val uid = doc.id
                    val email = doc.getString("email") ?: "(no email)"
                    stylistUids.add(uid)
                    stylistTitles.add(email)
                }

                val adapter = ArrayAdapter(
                    requireContext(),
                    android.R.layout.simple_list_item_1,
                    stylistTitles
                )
                listView.adapter = adapter

                listView.setOnItemClickListener { _, _, position, _ ->
                    val chosenStylistId = stylistUids[position]
                    createStylingRequest(chosenStylistId)
                }
            }
            .addOnFailureListener { e ->
                progress.visibility = View.GONE
                toast("Failed: ${e.message}")
            }
    }

    private fun createStylingRequest(stylistId: String) {
        val user = auth.currentUser ?: run {
            toast("Not logged in")
            return
        }

        val fromUserId = user.uid
        val fromEmail = user.email ?: ""

        // מונע כפילות: בקשה אחת לכל זוג User+Stylist
        val docId = "${fromUserId}_$stylistId"

        val data = hashMapOf(
            "stylistId" to stylistId,
            "fromUserId" to fromUserId,
            "fromEmail" to fromEmail,
            "status" to "OPEN",
            "note" to "",
            "createdAt" to FieldValue.serverTimestamp()
        )

        progress.visibility = View.VISIBLE

        db.collection("styling_requests")
            .document(docId)
            .set(data)
            .addOnSuccessListener {
                progress.visibility = View.GONE
                toast("Request sent ✅")
            }
            .addOnFailureListener { e ->
                progress.visibility = View.GONE
                toast("Failed to send request: ${e.message}")
            }
    }

    private fun distanceMeters(lat1: Double, lon1: Double, lat2: Double, lon2: Double): Float {
        val res = FloatArray(1)
        Location.distanceBetween(lat1, lon1, lat2, lon2, res)
        return res[0]
    }

    private fun hasLocationPermission(): Boolean {
        val fine = ContextCompat.checkSelfPermission(
            requireContext(), Manifest.permission.ACCESS_FINE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED

        val coarse = ContextCompat.checkSelfPermission(
            requireContext(), Manifest.permission.ACCESS_COARSE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED

        return fine || coarse
    }

    private fun requestLocationPermission() {
        permissionLauncher.launch(
            arrayOf(
                Manifest.permission.ACCESS_FINE_LOCATION,
                Manifest.permission.ACCESS_COARSE_LOCATION
            )
        )
    }

    private fun toast(msg: String) {
        Toast.makeText(requireContext(), msg, Toast.LENGTH_SHORT).show()
    }
}
