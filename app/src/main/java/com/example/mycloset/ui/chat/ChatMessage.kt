package com.example.mycloset.ui.chat

import com.google.firebase.Timestamp

data class ChatMessage(
    val senderId: String = "",
    val senderRole: String = "",   // "REGULAR" / "STYLIST"
    val text: String = "",
    val createdAt: Timestamp? = null
)
