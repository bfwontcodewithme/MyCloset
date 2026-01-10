package com.example.mycloset.data.repository

import com.example.mycloset.data.model.Closet
import com.example.mycloset.data.remote.firebase.FirebaseClosetsDataSource


class ClosetsRepository(
    private val ds: FirebaseClosetsDataSource = FirebaseClosetsDataSource()
) {
    suspend fun getMyClosets(userId: String): List<Closet> = ds.getClosets(userId)
    suspend fun addCloset(userId: String, name: String): String = ds.addCloset(userId, name)
}