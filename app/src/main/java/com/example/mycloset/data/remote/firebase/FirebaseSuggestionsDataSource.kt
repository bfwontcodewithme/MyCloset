package com.example.mycloset.data.remote.firebase

import com.example.mycloset.data.model.OutfitSuggestion
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow

class FirebaseSuggestionsDataSource(
    private val db: FirebaseFirestore = FirebaseFirestore.getInstance()
) {

    private val suggestionsCol = db.collection("outfit_suggestions")

    suspend fun createSuggestion(s: OutfitSuggestion): String {
        val doc = suggestionsCol.document()
        val withId = s.copy(suggestionId = doc.id)
        doc.set(withId).await()
        return doc.id
    }

    suspend fun getOwnerInbox(ownerUid: String): List<OutfitSuggestion> {
        val snap = suggestionsCol
            .whereEqualTo("ownerUid", ownerUid)
            .whereEqualTo("status", "PENDING")
            .get()
            .await()

        return snap.documents.mapNotNull { d ->
            d.toObject(OutfitSuggestion::class.java)?.copy(suggestionId = d.id)
        }.sortedByDescending { it.createdAt?.seconds ?: 0L }

    }


        //  get suggestion by id
    suspend fun getById(suggestionId: String): OutfitSuggestion? {
        val doc = suggestionsCol.document(suggestionId).get().await()
        val s = doc.toObject(OutfitSuggestion::class.java) ?: return null
        return s.copy(suggestionId = doc.id)
    }

    suspend fun updateStatus(suggestionId: String, status: String) {
        suggestionsCol
            .document(suggestionId)
            .update("status", status)
            .await()
    }
    fun listenToInbox(ownerUid: String): Flow<List<OutfitSuggestion>> = callbackFlow {
        val query = suggestionsCol
            .whereEqualTo("ownerUid", ownerUid)
            .whereEqualTo("status", "PENDING")

        val subscription = query.addSnapshotListener { snapshot, error ->
            if (error != null) {
                close(error)
                return@addSnapshotListener
            }

            val suggestions = snapshot?.documents?.mapNotNull { d ->
                d.toObject(OutfitSuggestion::class.java)?.copy(suggestionId = d.id)
            } ?: emptyList()

            trySend(suggestions)
        }

        // This keeps the stream open until the collector is destroyed
        awaitClose { subscription.remove() }
    }
}
