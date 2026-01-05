package com.example.mycloset.data.model

data class Outfit(
    val id: String = "",
    val ownerUid: String = "",
    val name: String = "",
    val itemIds: List<String> = emptyList(),
    val createdAt: Long = System.currentTimeMillis()
)