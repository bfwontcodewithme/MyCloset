package com.example.mycloset.data.model

import com.google.firebase.Timestamp

data class OutfitSuggestion(
    val suggestionId: String = "",
    val ownerUid: String = "",

    //  what is being suggested for
    val targetType: String = "",   // "OUTFIT" | "CLOSET"
    val targetId: String = "",     // outfitId | closetId

    val suggesterUid: String = "",
    val suggestedItemIds: List<String> = emptyList(),
    val note: String = "",
    val status: String = "PENDING",
    val createdAt: Timestamp? = null,

    // ️ legacy (keep so older docs won't crash deserialization)
    val outfitId: String = ""
)
