package com.example.mycloset.util

import com.example.mycloset.data.remote.firebase.FirebaseItemsDataSource
import com.example.mycloset.data.repository.ItemsRepository

object Injection {

    private var repository: ItemsRepository? = null

    /**
     * This is what the ViewModel (or anywhere else) can call.
     * If a test repository was set, it returns it.
     */
    fun provideItemsRepository(): ItemsRepository {
        return repository ?: ItemsRepository(FirebaseItemsDataSource())
    }

    /**
     * Tests can swap the repository here.
     */
    fun setRepository(repo: ItemsRepository) {
        repository = repo
    }

    /**
     * Clean up after tests.
     */
    fun reset() {
        repository = null
    }
}
