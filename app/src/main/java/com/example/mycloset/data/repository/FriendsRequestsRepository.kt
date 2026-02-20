package com.example.mycloset.data.repository

import com.example.mycloset.data.model.FriendRequest
import com.example.mycloset.data.remote.firebase.FirebaseFriendsRequestsDataSource

class FriendsRequestsRepository(
    private val ds: FirebaseFriendsRequestsDataSource = FirebaseFriendsRequestsDataSource()
) {
    suspend fun sendRequest(fromUid: String, fromEmail: String, toUid: String, toEmail: String) =
        ds.sendRequest(fromUid, fromEmail, toUid, toEmail)

    suspend fun getIncoming(myUid: String): List<FriendRequest> =
        ds.getIncomingRequests(myUid)

    suspend fun accept(requestId: String) = ds.acceptRequest(requestId)
    suspend fun decline(requestId: String) = ds.declineRequest(requestId)

    suspend fun getById(requestId: String): FriendRequest? = ds.getById(requestId)
}
