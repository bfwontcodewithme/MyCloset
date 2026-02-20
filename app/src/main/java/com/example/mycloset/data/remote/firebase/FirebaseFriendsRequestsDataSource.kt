package com.example.mycloset.data.remote.firebase

import com.example.mycloset.data.model.FriendRequest
import com.google.firebase.Timestamp
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.tasks.await

class FirebaseFriendsRequestsDataSource(
    private val db: FirebaseFirestore = FirebaseFirestore.getInstance()
) {
    private val col = db.collection("friend_requests")

    //  id לפי כיוון (from -> to)
    private fun reqId(fromUid: String, toUid: String) = "${fromUid}__${toUid}"

    suspend fun sendRequest(fromUid: String, fromEmail: String, toUid: String, toEmail: String) {
        val id = reqId(fromUid, toUid)
        val doc = col.document(id)
        val reverseId = reqId(toUid, fromUid)
        val reverse = col.document(reverseId).get().await()
        if (reverse.exists()) {
            // כבר יש בקשה ממנו אליי -> לא שולחים חדשה
            return
        }

        val existing = doc.get().await()
        if (existing.exists()) {
            val status = existing.getString("status") ?: ""
            // אם כבר חברים/מחכה – לא עושים כלום
            if (status == "PENDING" || status == "ACCEPTED") return

            // אם היה DECLINED בעבר – “מחיים” מחדש
            doc.update(
                mapOf(
                    "fromUid" to fromUid,
                    "fromEmail" to fromEmail,
                    "toUid" to toUid,
                    "toEmail" to toEmail,
                    "status" to "PENDING",
                    "createdAt" to Timestamp.now()
                )
            ).await()
            return
        }

        val req = FriendRequest(
            requestId = id,
            fromUid = fromUid,
            fromEmail = fromEmail,
            toUid = toUid,
            toEmail = toEmail,
            status = "PENDING",
            createdAt = Timestamp.now()
        )

        doc.set(req).await()
    }

    suspend fun getIncomingRequests(myUid: String): List<FriendRequest> {
        val snap = col
            .whereEqualTo("toUid", myUid)
            .whereEqualTo("status", "PENDING")
            .get()
            .await()

        return snap.documents.mapNotNull { d ->
            d.toObject(FriendRequest::class.java)?.copy(requestId = d.id)
        }.sortedByDescending { it.createdAt?.seconds ?: 0L }
    }

    suspend fun acceptRequest(requestId: String) {
        col.document(requestId).update("status", "ACCEPTED").await()
    }

    suspend fun declineRequest(requestId: String) {
        col.document(requestId).update("status", "DECLINED").await()
    }

    suspend fun getById(requestId: String): FriendRequest? {
        val d = col.document(requestId).get().await()
        val r = d.toObject(FriendRequest::class.java) ?: return null
        return r.copy(requestId = d.id)
    }
}
