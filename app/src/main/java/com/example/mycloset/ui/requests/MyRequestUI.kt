package com.example.mycloset.ui.requests

import com.google.firebase.Timestamp

data class MyRequestUI(
    val docId: String = "",
    val stylistId: String = "",
    val stylistEmail: String = "",
    val status: String = "OPEN",
    val note: String = "",
    val createdAt: Timestamp? = null
)
