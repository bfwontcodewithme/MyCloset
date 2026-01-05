package com.example.mycloset.data.model

data class ClosetItem(
    val id: String = "",
    val ownerUid: String = "",
    val closetID: String = "",
    val name: String = "",
    val type: String = "",
    val color: String = "",
    val season: String = "",
    val tags: List<String> = emptyList(),
    val imageUri: String = "",
    val createdAt: Long = System.currentTimeMillis()
)