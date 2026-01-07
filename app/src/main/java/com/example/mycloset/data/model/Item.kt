package com.example.mycloset.data.model

import com.google.firebase.firestore.Exclude

data class Item(
    val itemId: String = "",
    //  val itemName: String = "",
    val ownerUid: String = "",
    val parentClosetId: String = "",
    val itemImageUri: String = "",
    val createdAt: Long = System.currentTimeMillis(),
    val visibility: String = Visibility.PRIVATE.name,
    val category: String = Category.TOPS.name,
    val subCategory: String = "T-Shirt",
    val season: String = Season.ALL_YEAR.name,
    val color: String = Color.BLACK.name,
    val tags: List<String> = emptyList()
)