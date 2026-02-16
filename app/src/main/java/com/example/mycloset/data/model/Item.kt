package com.example.mycloset.data.model

data class Item(
    val id: String = "",
    val ownerUid: String = "",
    val closetId: String = "",
    val name: String = "",
    val type: String = "",
    val color: String = "",
    val season: String = "",
    val tags: List<String> = emptyList(),
    val imageUrl: String = "",
    val visibility: String = Visibility.PRIVATE.name,
    val createdAt: Long = System.currentTimeMillis(),

    // Stats fields
    val wearCount: Long = 0,
    val lastWornAt: Long = 0
)
