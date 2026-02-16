package com.example.mycloset.ui.requests

import com.google.firebase.Timestamp

data class MyRequestUI(
    val docId: String = "",
    val stylistId: String = "",
    val stylistEmail: String = "",
    val status: String = "OPEN",
    val note: String = "",
    val createdAt: Timestamp? = null,

    //  chat summary
    val lastMessage: String = "",
    val lastMessageAt: Timestamp? = null,
    val lastSenderId: String = "",

    // unread counters
    val unreadForUser: Long = 0,
    val unreadForStylist: Long = 0
)
