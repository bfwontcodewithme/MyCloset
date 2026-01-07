package com.example.mycloset.data.repository

import com.example.mycloset.data.model.Outfit
import com.example.mycloset.data.remote.firebase.FirebaseOutfitsDataSource


class OutfitsRepository(
    private val ds: FirebaseOutfitsDataSource = FirebaseOutfitsDataSource()
) {
    suspend fun addOutfit(userId: String, outfit: Outfit): String = ds.addOutfit(userId, outfit)
    suspend fun getMyOutfits(userId: String): List<Outfit> = ds.getMyOutfits(userId)
}