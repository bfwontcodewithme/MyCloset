package com.example.mycloset

import com.example.mycloset.data.model.Item

class FakeItemRepository : IItemRepository {
    var itemsToReturn = listOf<Item>()
    override suspend fun getMyItems(userId: String): List<Item> {
        return itemsToReturn
    }
}