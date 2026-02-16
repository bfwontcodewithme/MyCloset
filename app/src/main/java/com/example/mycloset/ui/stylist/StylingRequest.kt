package com.example.mycloset.ui.stylist

import com.google.firebase.Timestamp

data class StylingRequest(
    val stylistId: String = "",
    val fromUserId: String = "",
    val fromEmail: String = "",
    val status: String = "OPEN",
    val note: String = "",
    val createdAt: Timestamp? = null,

    //  chat summary
    val lastMessage: String = "",
    val lastMessageAt: Timestamp? = null,
    val lastSenderId: String = "",

    //  unread counters
    val unreadForUser: Long = 0,
    val unreadForStylist: Long = 0
)
