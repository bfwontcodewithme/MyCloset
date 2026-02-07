package com.example.mycloset.data.repository

import com.example.mycloset.data.model.Item

interface IItemRepository {
    suspend fun getMyItems(userId: String): List<Item>
}