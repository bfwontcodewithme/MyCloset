package com.example.mycloset.data.repository

import android.net.Uri
import com.example.mycloset.data.model.Item
import com.example.mycloset.data.remote.firebase.FirebaseItemsDataSource

class ItemsRepository(
    private val dataSource: FirebaseItemsDataSource = FirebaseItemsDataSource()
) {
    suspend fun uploadImage(userId: String, uri: Uri): String {
        return dataSource.uploadItemImage(userId, uri)
    }

    suspend fun addItem(userId: String, item: Item): String {
        return dataSource.addItem(userId, item)
    }


    suspend fun getMyItems(userId: String): List<Item> {
        return dataSource.getItemsByOwner(userId)
    }
}