package com.example.mycloset.ui.item

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.mycloset.data.model.Item
import com.example.mycloset.util.Injection // switch for test
import kotlinx.coroutines.launch

class ItemViewModel : ViewModel() {

    private val repository = Injection.provideItemRepository()

    // fragment will "watch" (observe) this.
    private val _items = MutableLiveData<List<Item>>()
    val items: LiveData<List<Item>> = _items

    // starts the data fetch.
    fun loadItems(userId: String) {
        viewModelScope.launch {
            try {
                // getting items from the repository
                val result = repository.getMyItems(userId)

                // Update the LiveData with the result
                // This triggers the Fragment to call adapter.setData()
                _items.value = result
            } catch (e: Exception) {
                // Handle errors here if needed
            }
        }
    }
}