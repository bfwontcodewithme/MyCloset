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

    suspend fun deleteItem(userId: String, itemId: String) {
        dataSource.deleteItem(userId, itemId)
    }

    suspend fun updateItem(userId: String, item: Item) {
        dataSource.updateItem(userId, item)
    }

    suspend fun getItemById(userId: String, itemId: String): Item? {
        return dataSource.getItemById(userId, itemId)
    }

    suspend fun getMyItems(userId: String): List<Item> {
        return dataSource.getItemsByOwner(userId)
    }

    suspend fun getItemsByCloset(userId: String, closetId: String): List<Item> {
        return dataSource.getItemsByCloset(userId, closetId)
    }

    // ✅ NEW
    suspend fun markItemWorn(userId: String, itemId: String) {
        dataSource.markItemWorn(userId, itemId)
    }
}
