package com.example.mycloset.data.remote.firebase

import com.example.mycloset.data.model.User
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.tasks.await

class FirebaseUsersDataSource(
    private val db: FirebaseFirestore = FirebaseFirestore.getInstance()
) {

    suspend fun getMyUserDoc(myUid: String): User? {
        val doc = db.collection("users").document(myUid).get().await()
        return doc.toObject(User::class.java)?.copy(userUid = doc.id)
    }

    suspend fun searchUsers(query: String, limit: Long = 20): List<User> {
        val q = query.trim()
        if (q.isBlank()) return emptyList()


        val byEmail = db.collection("users")
            .whereEqualTo("userEmail", q)
            .limit(limit)
            .get()
            .await()
            .documents
            .mapNotNull { it.toObject(User::class.java)?.copy(userUid = it.id) }

        if (byEmail.isNotEmpty()) return byEmail

        // startsWith על שם: צריך orderBy + startAt/endAt
        val byName = db.collection("users")
            .orderBy("userName")
            .startAt(q)
            .endAt(q + "\uf8ff")
            .limit(limit)
            .get()
            .await()
            .documents
            .mapNotNull { it.toObject(User::class.java)?.copy(userUid = it.id) }

        return byName
    }

    suspend fun addFriend(myUid: String, friendUid: String) {
        db.collection("users").document(myUid)
            .update("userFriendsUids", FieldValue.arrayUnion(friendUid))
            .await()
    }

    suspend fun removeFriend(myUid: String, friendUid: String) {
        db.collection("users").document(myUid)
            .update("userFriendsUids", FieldValue.arrayRemove(friendUid))
            .await()
    }

    suspend fun getFriends(myUid: String): List<User> {
        val me = getMyUserDoc(myUid) ?: return emptyList()
        if (me.userFriendsUids.isEmpty()) return emptyList()

        // Firestore whereIn מוגבל ל-10, לכן נחלק לקבוצות
        val chunks = me.userFriendsUids.chunked(10)
        val result = mutableListOf<User>()

        for (chunk in chunks) {
            val snap = db.collection("users")
                .whereIn("__name__", chunk)
                .get()
                .await()

            result += snap.documents.mapNotNull { it.toObject(User::class.java)?.copy(userUid = it.id) }
        }
        return result
    }
}
