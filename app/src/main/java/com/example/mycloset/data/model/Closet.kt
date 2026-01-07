package com.example.mycloset.data.model

import com.google.firebase.firestore.Exclude

data class Closet(
    val closetId: String = "",
    val ownerUid: String = "",
    val closetName: String = "My Closet",
    val visibility: String = Visibility.PRIVATE.name,
    val createdAt: Long = System.currentTimeMillis()
)