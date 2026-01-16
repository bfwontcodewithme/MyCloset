package com.example.mycloset.data.model

data class Item(
    val id: String = "",
    val ownerUid: String = "",
    val closetId: String = "",   // ✅ היה closetid
    val name: String = "",
    val type: String = "",
    val color: String = "",
    val season: String = "",
    val tags: List<String> = emptyList(),
    val imageUrl: String = "",   // ✅ היה imageurl
    val visibility: String = Visibility.PRIVATE.name,
    val createdAt: Long = System.currentTimeMillis()
)
