package com.example.mycloset.data.remote.firebase

import android.net.Uri
import com.example.mycloset.data.model.Item
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.storage.FirebaseStorage
import kotlinx.coroutines.tasks.await

class FirebaseItemsDataSource(
    private val db: FirebaseFirestore = FirebaseFirestore.getInstance(),
    private val storage: FirebaseStorage = FirebaseStorage.getInstance()
) {

    suspend fun uploadItemImage(userId: String, localUri: Uri): String {
        val ref = storage.reference
            .child("users/$userId/items/${System.currentTimeMillis()}.jpg")

        ref.putFile(localUri).await()
        return ref.downloadUrl.await().toString()
    }

    suspend fun addItem(userId: String, item: Item): String {
        val doc = db.collection("users")
            .document(userId)
            .collection("items")
            .document()

        val itemWithId = item.copy(id = doc.id)
        doc.set(itemWithId).await()
        return doc.id
    }

    suspend fun deleteItem(userId: String, itemId: String) {
        db.collection("users")
            .document(userId)
            .collection("items")
            .document(itemId)
            .delete()
            .await()
    }

    suspend fun updateItem(userId: String, item: Item) {
        db.collection("users")
            .document(userId)
            .collection("items")
            .document(item.id)
            .set(item)
            .await()
    }

    suspend fun getItemById(userId: String, itemId: String): Item? {
        val doc = db.collection("users")
            .document(userId)
            .collection("items")
            .document(itemId)
            .get()
            .await()

        return doc.toObject(Item::class.java)?.copy(id = doc.id)
    }

    suspend fun getItemsByOwner(userId: String): List<Item> {
        val snapshot = db.collection("users")
            .document(userId)
            .collection("items")
            .get()
            .await()

        return snapshot.documents.mapNotNull { it.toObject(Item::class.java)?.copy(id = it.id) }
    }

    suspend fun getItemsByCloset(userId: String, closetId: String): List<Item> {
        val snapshot = db.collection("users")
            .document(userId)
            .collection("items")
            .whereEqualTo("closetId", closetId)
            .get()
            .await()

        return snapshot.documents.mapNotNull { it.toObject(Item::class.java)?.copy(id = it.id) }
    }

    // ✅ NEW: update stats when item was worn
    suspend fun markItemWorn(userId: String, itemId: String) {
        val ref = db.collection("users")
            .document(userId)
            .collection("items")
            .document(itemId)

        db.runTransaction { tx ->
            val snap = tx.get(ref)
            val wearCount = (snap.getLong("wearCount") ?: 0L) + 1L
            tx.update(
                ref,
                mapOf(
                    "wearCount" to wearCount,
                    "lastWornAt" to System.currentTimeMillis()
                )
            )
        }.await()
    }
}
