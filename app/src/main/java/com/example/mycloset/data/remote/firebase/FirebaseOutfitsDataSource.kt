package com.example.mycloset.data.remote.firebase

import com.example.mycloset.data.model.Outfit
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.tasks.await

class FirebaseOutfitsDataSource(
    private val db: FirebaseFirestore = FirebaseFirestore.getInstance()
) {
    suspend fun addOutfit(userId: String, outfit: Outfit): String {
        val doc = db.collection("users")
            .document(userId)
            .collection("outfits")
            .document()

        val withId = outfit.copy(outfitId = doc.id)
        doc.set(withId).await()
        return doc.id
    }

    suspend fun getMyOutfits(userId: String): List<Outfit> {
        val snap = db.collection("users")
            .document(userId)
            .collection("outfits")
            .get()
            .await()

        return snap.documents.mapNotNull { it.toObject(Outfit::class.java) }
    }
}