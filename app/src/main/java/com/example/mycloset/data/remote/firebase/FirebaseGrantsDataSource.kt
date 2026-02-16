package com.example.mycloset.data.remote.firebase

import com.example.mycloset.data.model.AccessGrant
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.tasks.await

class FirebaseGrantsDataSource(
    private val db: FirebaseFirestore = FirebaseFirestore.getInstance()
) {

    private fun grantId(ownerUid: String, granteeUid: String, resourceType: String, resourceId: String): String {
        return "${ownerUid}__${granteeUid}__${resourceType}__${resourceId}"
    }

    /**
     * Upsert: אם כבר קיים grant → update (permissions / granteeEmail / itemIds)
     * אם לא קיים → create חדש
     */
    suspend fun upsertGrant(grant: AccessGrant): String {
        val id = grantId(grant.ownerUid, grant.granteeUid, grant.resourceType, grant.resourceId)
        val doc = db.collection("access_grants").document(id)

        val existing = doc.get().await()

        if (existing.exists()) {
            doc.update(
                mapOf(
                    "permissions" to grant.permissions,
                    "granteeEmail" to grant.granteeEmail,
                    "itemIds" to grant.itemIds
                )
            ).await()
        } else {
            // אין grantId במודל שלך → שומרים כמו שהוא
            val data = hashMapOf(
                "ownerUid" to grant.ownerUid,
                "granteeUid" to grant.granteeUid,
                "granteeEmail" to grant.granteeEmail,
                "resourceType" to grant.resourceType,
                "resourceId" to grant.resourceId,
                "permissions" to grant.permissions,
                "itemIds" to grant.itemIds,
                "createdAt" to grant.createdAt
            )
            doc.set(data).await()
        }

        return id
    }

    suspend fun hasGrant(
        ownerUid: String,
        granteeUid: String,
        resourceType: String,
        resourceId: String,
        permission: String
    ): Boolean {
        val id = grantId(ownerUid, granteeUid, resourceType, resourceId)
        val doc = db.collection("access_grants").document(id).get().await()
        val g = doc.toObject(AccessGrant::class.java) ?: return false
        return g.permissions.contains(permission)
    }

    suspend fun getGrantsForMe(
        resourceType: String,
        myUid: String,
        permission: String
    ): List<AccessGrant> {
        val snap = db.collection("access_grants")
            .whereEqualTo("granteeUid", myUid)
            .whereEqualTo("resourceType", resourceType)
            .whereArrayContains("permissions", permission)
            .get()
            .await()

        return snap.documents.mapNotNull { it.toObject(AccessGrant::class.java) }
    }

    suspend fun deleteGrant(
        ownerUid: String,
        granteeUid: String,
        resourceType: String,
        resourceId: String
    ) {
        val id = grantId(ownerUid, granteeUid, resourceType, resourceId)
        db.collection("access_grants")
            .document(id)
            .delete()
            .await()
    }
}
