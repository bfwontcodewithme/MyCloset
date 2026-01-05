package com.example.mycloset.data.model

data class Closet(
    val id: String = "",
    val name: String = "My Closet",
    val createdAt: Long = System.currentTimeMillis()
)
