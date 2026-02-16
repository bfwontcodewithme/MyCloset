package com.example.mycloset.data.model

import com.google.firebase.Timestamp

data class AccessGrant(
    val ownerUid: String = "",
    val granteeUid: String = "",
    val granteeEmail: String = "",
    val resourceType: String = "", // "CLOSET" / "OUTFIT"
    val resourceId: String = "",   // closetId / outfitId
    val permissions: List<String> = emptyList(), // VIEW_ITEMS / SUGGEST_OUTFIT

    //  בשביל CLOSET subset
    val itemIds: List<String> = emptyList(),

    val createdAt: Timestamp? = null
)
