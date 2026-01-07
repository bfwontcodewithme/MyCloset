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

    val type: String = "",
    val color: String = "",
    val season: String = "",
    val tags: List<String> = emptyList()
){
    @get:Exclude
    val visibilityEnum: Visibility
        get() = try{
            Visibility.valueOf(visibility)
        } catch (e: Exception){
            Visibility.PRIVATE
        }
}