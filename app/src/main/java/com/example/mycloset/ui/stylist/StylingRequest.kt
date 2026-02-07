package com.example.mycloset.ui.stylist

import com.google.firebase.Timestamp

data class StylingRequest(
    val stylistId: String = "",
    val fromUserId: String = "",
    val fromEmail: String = "",
    val status: String = "OPEN",     // OPEN / IN_PROGRESS / DONE
    val note: String = "",
    val createdAt: Timestamp? = null
)
