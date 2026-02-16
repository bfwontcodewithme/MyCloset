package com.example.mycloset.data.repository

import com.example.mycloset.data.model.User
import com.example.mycloset.data.remote.firebase.FirebaseUsersDataSource

class UsersRepository(
    private val ds: FirebaseUsersDataSource = FirebaseUsersDataSource()
) {
    suspend fun getMyUserDoc(myUid: String): User? = ds.getMyUserDoc(myUid)
    suspend fun searchUsers(query: String): List<User> = ds.searchUsers(query)
    suspend fun addFriend(myUid: String, friendUid: String) = ds.addFriend(myUid, friendUid)
    suspend fun removeFriend(myUid: String, friendUid: String) = ds.removeFriend(myUid, friendUid)
    suspend fun getFriends(myUid: String): List<User> = ds.getFriends(myUid)
}
