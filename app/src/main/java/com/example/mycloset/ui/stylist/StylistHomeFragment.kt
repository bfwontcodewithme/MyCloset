package com.example.mycloset.ui.stylist

import android.Manifest
import android.content.pm.PackageManager
import android.os.Bundle
import android.view.View
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.mycloset.R
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
import com.google.android.gms.tasks.CancellationTokenSource
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.GeoPoint
import com.google.firebase.firestore.ListenerRegistration
import com.google.firebase.firestore.Query
import com.google.firebase.firestore.SetOptions

class StylistHomeFragment : Fragment(R.layout.fragment_stylist_home) {

    private val auth by lazy { FirebaseAuth.getInstance() }
    private val db by lazy { FirebaseFirestore.getInstance() }
    private val fusedClient by lazy { LocationServices.getFusedLocationProviderClient(requireActivity()) }

    private lateinit var rv: RecyclerView
    private lateinit var tvEmpty: TextView
    private lateinit var adapter: StylingRequestsAdapter

    private lateinit var btnFilterAll: Button
    private lateinit var btnFilterOpen: Button
    private lateinit var btnFilterInProgress: Button
    private lateinit var btnFilterDone: Button

    private var requestsListener: ListenerRegistration? = null

    // נשמור את כל הבקשות שהגיעו מהשרת
    private var allItems: List<StylingRequestWithId> = emptyList()

    // פילטר נוכחי
    private var currentFilter: String? = null
    // null = ALL, אחרת "OPEN"/"IN_PROGRESS"/"DONE"

    private val permissionLauncher =
        registerForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) { perms ->
            val granted = perms[Manifest.permission.ACCESS_FINE_LOCATION] == true ||
                    perms[Manifest.permission.ACCESS_COARSE_LOCATION] == true

            if (granted) updateMyLocation()
            else showStatus("Location permission denied ❌")
        }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val btnLogout = view.findViewById<Button>(R.id.btnStylistLogout)
        val btnUpdateLocation = view.findViewById<Button>(R.id.btnUpdateLocation)

        // Filters
        btnFilterAll = view.findViewById(R.id.btnFilterAll)
        btnFilterOpen = view.findViewById(R.id.btnFilterOpen)
        btnFilterInProgress = view.findViewById(R.id.btnFilterInProgress)
        btnFilterDone = view.findViewById(R.id.btnFilterDone)

        rv = view.findViewById(R.id.rvStylingRequests)
        tvEmpty = view.findViewById(R.id.tvEmptyRequests)

        adapter = StylingRequestsAdapter(
            onAccept = { docId -> updateStatus(docId, "IN_PROGRESS") },
            onDone = { docId -> updateStatus(docId, "DONE") }
        )
        rv.layoutManager = LinearLayoutManager(requireContext())
        rv.adapter = adapter

        btnLogout.setOnClickListener {
            auth.signOut()
            findNavController().navigate(R.id.action_global_login)
        }

        btnUpdateLocation.setOnClickListener {
            if (!hasLocationPermission()) requestLocationPermission()
            else updateMyLocation()
        }

        // Filter clicks
        btnFilterAll.setOnClickListener {
            currentFilter = null
            applyFilterAndRender()
        }
        btnFilterOpen.setOnClickListener {
            currentFilter = "OPEN"
            applyFilterAndRender()
        }
        btnFilterInProgress.setOnClickListener {
            currentFilter = "IN_PROGRESS"
            applyFilterAndRender()
        }
        btnFilterDone.setOnClickListener {
            currentFilter = "DONE"
            applyFilterAndRender()
        }

        listenToMyRequestsRealtime()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        requestsListener?.remove()
        requestsListener = null
    }

    // ===== Requests Realtime =====
    private fun listenToMyRequestsRealtime() {
        val myUid = auth.currentUser?.uid ?: run {
            showStatus("Not logged in ❌")
            return
        }

        showStatus("Loading requests...")

        requestsListener?.remove()
        requestsListener =
            db.collection("styling_requests")
                .whereEqualTo("stylistId", myUid)
                .orderBy("createdAt", Query.Direction.DESCENDING)
                .addSnapshotListener { snap, err ->
                    if (err != null) {
                        rv.visibility = View.GONE
                        tvEmpty.visibility = View.VISIBLE
                        tvEmpty.text = "Failed to load requests: ${err.message}"
                        return@addSnapshotListener
                    }


                    val items = snap?.documents.orEmpty().map { doc ->
                        val req = doc.toObject(StylingRequest::class.java) ?: StylingRequest()
                        StylingRequestWithId(docId = doc.id, req = req)
                    }

// ✅ sort by status priority, then by date desc (already in query)
                    val priority = mapOf("OPEN" to 0, "IN_PROGRESS" to 1, "DONE" to 2)
                    allItems = items.sortedWith(compareBy(
                        { priority[it.req.status] ?: 9 }
                    ))


                    applyFilterAndRender()
                }
    }

    private fun applyFilterAndRender() {
        val filtered = if (currentFilter == null) {
            allItems
        } else {
            allItems.filter { it.req.status == currentFilter }
        }

        adapter.submitList(filtered)

        val isEmpty = filtered.isEmpty()
        rv.visibility = if (isEmpty) View.GONE else View.VISIBLE
        tvEmpty.visibility = if (isEmpty) View.VISIBLE else View.GONE

        if (isEmpty) {
            val label = when (currentFilter) {
                null -> "No styling requests yet"
                "OPEN" -> "No OPEN requests"
                "IN_PROGRESS" -> "No IN_PROGRESS requests"
                "DONE" -> "No DONE requests"
                else -> "No requests"
            }
            tvEmpty.text = label
        }
    }

    private fun updateStatus(docId: String, newStatus: String) {
        db.collection("styling_requests")
            .document(docId)
            .update("status", newStatus)
            .addOnSuccessListener { toast("Updated to $newStatus ✅") }
            .addOnFailureListener { e -> toast("Failed: ${e.message}") }
    }

    // ===== Update Location =====
    private fun updateMyLocation() {
        val myUid = auth.currentUser?.uid ?: run {
            showStatus("Not logged in ❌")
            return
        }

        showStatus("Updating location...")

        val token = CancellationTokenSource()
        fusedClient.getCurrentLocation(Priority.PRIORITY_HIGH_ACCURACY, token.token)
            .addOnSuccessListener { loc ->
                if (loc == null) {
                    showStatus("Couldn't get location (try again)")
                    return@addOnSuccessListener
                }

                val data = hashMapOf(
                    "location" to mapOf(
                        "geo" to GeoPoint(loc.latitude, loc.longitude),
                        "updatedAt" to FieldValue.serverTimestamp()
                    )
                )

                db.collection("users").document(myUid)
                    .set(data, SetOptions.merge())
                    .addOnSuccessListener {
                        showStatus("Location updated ✅")
                        toast("Location saved ✅")
                    }
                    .addOnFailureListener { e ->
                        showStatus("Failed saving location: ${e.message}")
                        toast("Failed saving location ❌")
                    }
            }
            .addOnFailureListener { e ->
                showStatus("Location error: ${e.message}")
            }
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

    private fun showStatus(msg: String) {
        tvEmpty.visibility = View.VISIBLE
        tvEmpty.text = msg
    }

    private fun toast(msg: String) {
        Toast.makeText(requireContext(), msg, Toast.LENGTH_SHORT).show()
    }
}
