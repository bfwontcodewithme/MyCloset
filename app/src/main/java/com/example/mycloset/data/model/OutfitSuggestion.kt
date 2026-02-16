package com.example.mycloset.data.model

import com.google.firebase.Timestamp

data class OutfitSuggestion(
    val suggestionId: String = "",
    val ownerUid: String = "",
    val outfitId: String = "",
    val suggesterUid: String = "",
    val suggestedItemIds: List<String> = emptyList(),
    val note: String = "",
    val status: String = "PENDING", // PENDING / ACCEPTED / REJECTED
    val createdAt: Timestamp? = null
)
