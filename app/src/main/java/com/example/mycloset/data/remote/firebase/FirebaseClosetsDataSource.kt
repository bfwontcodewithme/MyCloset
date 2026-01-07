package com.example.mycloset.data.remote.firebase

import com.example.mycloset.data.model.Closet
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.tasks.await

class FirebaseClosetsDataSource(
    private val db: FirebaseFirestore = FirebaseFirestore.getInstance()
) {
    suspend fun getClosets(userId: String): List<Closet> {
        val snap = db.collection("users").document(userId).collection("closets").get().await()
        return snap.documents.mapNotNull { it.toObject(Closet::class.java) }
    }

    suspend fun addCloset(userId: String, name: String): String {
        val doc = db.collection("users").document(userId).collection("closets").document()
        val closet = Closet(closetId = doc.id, closetName = name)
        doc.set(closet).await()
        return doc.id
    }
}