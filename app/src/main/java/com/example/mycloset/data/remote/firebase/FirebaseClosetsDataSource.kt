package com.example.mycloset.data.remote.firebase

import com.example.mycloset.data.model.Closet
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.tasks.await

class FirebaseClosetsDataSource(
    private val db: FirebaseFirestore = FirebaseFirestore.getInstance()
) {

    suspend fun getClosets(userId: String): List<Closet> {
        val snap = db.collection("users")
            .document(userId)
            .collection("closets")
            .orderBy("createdAt")
            .get()
            .await()

        return snap.documents.mapNotNull { doc ->
            val c = doc.toObject(Closet::class.java) ?: return@mapNotNull null
            c.copy(
                closetId = doc.id, // ✅ תמיד doc.id
                ownerUid = if (c.ownerUid.isBlank()) userId else c.ownerUid
            )
        }
    }

    suspend fun addCloset(userId: String, name: String): String {
        val doc = db.collection("users")
            .document(userId)
            .collection("closets")
            .document()

        val closet = Closet(
            closetId = doc.id,
            ownerUid = userId,
            closetName = name,
            createdAt = System.currentTimeMillis()
        )

        doc.set(closet).await()
        return doc.id
    }

    suspend fun renameCloset(userId: String, closetId: String, newName: String) {
        db.collection("users")
            .document(userId)
            .collection("closets")
            .document(closetId)
            .update("closetName", newName)
            .await()
    }

    /**
     * ✅ מוחק ארון + מוחק את כל הפריטים ששייכים אליו (users/{uid}/items where closetId==X)
     * עובד בבאצ'ים של עד 450 למחיקה (בטוח מתחת ל-500)
     */
    suspend fun deleteClosetAndItsItems(userId: String, closetId: String) {
        // 1) מחיקת items של הארון (batch)
        deleteItemsByCloset(userId, closetId)

        // 2) מחיקת הארון עצמו
        db.collection("users")
            .document(userId)
            .collection("closets")
            .document(closetId)
            .delete()
            .await()
    }

    private suspend fun deleteItemsByCloset(userId: String, closetId: String) {
        val itemsCol = db.collection("users")
            .document(userId)
            .collection("items")

        while (true) {
            // לוקחים "עמוד" של מסמכים למחיקה
            val snap = itemsCol
                .whereEqualTo("closetId", closetId)
                .limit(450)
                .get()
                .await()

            if (snap.isEmpty) break

            val batch = db.batch()
            snap.documents.forEach { doc ->
                batch.delete(doc.reference)
            }
            batch.commit().await()
        }
    }

    // אם אי פעם תרצי למחוק רק closet בלי items:
    suspend fun deleteClosetOnly(userId: String, closetId: String) {
        db.collection("users")
            .document(userId)
            .collection("closets")
            .document(closetId)
            .delete()
            .await()
    }
}
