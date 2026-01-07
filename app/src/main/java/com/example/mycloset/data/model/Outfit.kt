package com.example.mycloset.data.model

import com.google.firebase.firestore.Exclude

data class Outfit(
    val outfitId: String = "",
    val ownerUid: String = "",
    val name: String = "",
    val visibility: String = Visibility.PRIVATE.name,
    val itemIds: List<String> = emptyList(),
    val createdAt: Long = System.currentTimeMillis()
)