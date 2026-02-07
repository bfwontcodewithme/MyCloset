package com.example.mycloset

import com.example.mycloset.data.model.Item
import com.example.mycloset.data.repository.IItemRepository

class FakeItemRepository : IItemRepository {
    var itemsToReturn = listOf<Item>()
    override suspend fun getMyItems(userId: String): List<Item> {
        return itemsToReturn
    }
}