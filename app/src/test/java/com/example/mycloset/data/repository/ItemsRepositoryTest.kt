package com.example.mycloset.data.repository

import android.net.Uri
import com.example.mycloset.data.model.Item
import com.example.mycloset.data.remote.firebase.FirebaseItemsDataSource
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Test
import org.mockito.kotlin.mock
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever

class ItemsRepositoryTest {

    private val userId = "user123"

    @Test
    fun uploadImage_returnsUrlFromDataSource() = runTest {
        // Setup
        val dataSource = mock<FirebaseItemsDataSource>()
        val repo = ItemsRepository(dataSource)
        val fakeUri = mock<Uri>()
        val expectedUrl = "https://fake.storage/image.jpg"

        whenever(dataSource.uploadItemImage(userId, fakeUri)).thenReturn(expectedUrl)

        // Call
        val actualUrl = repo.uploadImage(userId, fakeUri)

        // Assertions
        assertEquals(expectedUrl, actualUrl)
        verify(dataSource).uploadItemImage(userId, fakeUri)
    }

    @Test
    fun addItem_returnsIdFromDataSource() = runTest {
        // Setup
        val dataSource = mock<FirebaseItemsDataSource>()
        val repo = ItemsRepository(dataSource)

        val item = Item(
            ownerUid = userId,
            closetId = "default",
            name = "Pink sweater",
            type = "shirt"
        )

        val expectedId = "doc_123"
        whenever(dataSource.addItem(userId, item)).thenReturn(expectedId)

        // Call
        val actualId = repo.addItem(userId, item)

        // Assertions
        assertEquals(expectedId, actualId)
        verify(dataSource).addItem(userId, item)
    }

    @Test
    fun getMyItems_returnsItemsFromDataSource() = runTest {
        // Setup
        val dataSource = mock<FirebaseItemsDataSource>()
        val repo = ItemsRepository(dataSource)

        val expected = listOf(
            Item(id = "1", ownerUid = userId, closetId = "default", name = "Jeans", type = "pants"),
            Item(id = "2", ownerUid = userId, closetId = "default", name = "Jacket", type = "coat")
        )

        whenever(dataSource.getItemsByOwner(userId)).thenReturn(expected)

        // Call
        val actual = repo.getMyItems(userId)

        // Assertions
        assertEquals(expected, actual)
        verify(dataSource).getItemsByOwner(userId)
    }
}
