package com.example.mycloset.data.repository

import com.example.mycloset.data.model.OutfitSuggestion
import com.example.mycloset.data.remote.firebase.FirebaseSuggestionsDataSource

class SuggestionsRepository(
    private val ds: FirebaseSuggestionsDataSource = FirebaseSuggestionsDataSource()
) {
    suspend fun createSuggestion(s: OutfitSuggestion): String =
        ds.createSuggestion(s)

    suspend fun getOwnerInbox(ownerUid: String): List<OutfitSuggestion> =
        ds.getOwnerInbox(ownerUid)

    suspend fun getById(suggestionId: String): OutfitSuggestion? =
        ds.getById(suggestionId)

    suspend fun updateStatus(suggestionId: String, status: String) =
        ds.updateStatus(suggestionId, status)
}
