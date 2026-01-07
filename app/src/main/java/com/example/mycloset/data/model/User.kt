package com.example.mycloset.data.model

data class User(
    val userUid: String = "",
    val userName:String = "",
    val userEmail: String = "",
    val userFriendsUids: List<String> = emptyList()
)