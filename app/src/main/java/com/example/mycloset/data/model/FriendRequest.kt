package com.example.mycloset.data.model

import com.google.firebase.Timestamp

data class FriendRequest(
    val requestId: String = "",
    val fromUid: String = "",
    val fromEmail: String = "",
    val toUid: String = "",
    val toEmail: String = "",
    val status: String = "PENDING",
    val createdAt: Timestamp? = null
)
