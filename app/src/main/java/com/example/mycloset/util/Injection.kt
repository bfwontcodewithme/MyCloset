package com.example.mycloset.util

import com.example.mycloset.data.repository.IItemRepository
import com.example.mycloset.data.repository.ItemsRepository
import com.example.mycloset.data.remote.firebase.FirebaseItemsDataSource

object Injection {
    private var repository: IItemRepository? = null

    /**
     * This is what the ViewModel calls.
     * It is just standard Kotlin code—no dependencies needed.
     */
    fun provideItemRepository(): IItemRepository {
        // Returns the fake if one is set; otherwise, creates the real one
        return repository ?: ItemsRepository(FirebaseItemsDataSource())
    }

    /**
     * This is what the UI Test calls to "swap" the data.
     */
    fun setRepository(repo: IItemRepository) {
        repository = repo
    }

    /**
     * Call this in @After to clean up for the next test.
     */
    fun reset() {
        repository = null
    }
}