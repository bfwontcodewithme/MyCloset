package com.example.mycloset.data.repository

import com.example.mycloset.data.model.AccessGrant
import com.example.mycloset.data.remote.firebase.FirebaseGrantsDataSource

class GrantsRepository(
    private val ds: FirebaseGrantsDataSource = FirebaseGrantsDataSource()
) {
    suspend fun upsertGrant(grant: AccessGrant): String = ds.upsertGrant(grant)

    suspend fun hasGrant(
        ownerUid: String,
        granteeUid: String,
        resourceType: String,
        resourceId: String,
        permission: String
    ): Boolean = ds.hasGrant(ownerUid, granteeUid, resourceType, resourceId, permission)

    //  כבר יש לך
    suspend fun getSharedOutfitGrantsForMe(myUid: String): List<AccessGrant> =
        ds.getGrantsForMe(resourceType = "OUTFIT", myUid = myUid, permission = "SUGGEST_OUTFIT")

    //  חדש: שיתופי CLOSET (VIEW_ITEMS) — בשביל subset
    suspend fun getSharedClosetGrantsForMe(myUid: String): List<AccessGrant> =
        ds.getGrantsForMe(resourceType = "CLOSET", myUid = myUid, permission = "VIEW_ITEMS")

    suspend fun deleteGrant(
        ownerUid: String,
        granteeUid: String,
        resourceType: String,
        resourceId: String
    ) = ds.deleteGrant(ownerUid, granteeUid, resourceType, resourceId)
}
