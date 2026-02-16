package com.example.mycloset.ui.item

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.mycloset.data.model.Item
import com.example.mycloset.util.Injection
import kotlinx.coroutines.launch

class ItemViewModel : ViewModel() {

    private val repository = Injection.provideItemsRepository()

    private val _items = MutableLiveData<List<Item>>()
    val items: LiveData<List<Item>> = _items

    fun loadItemsForCloset(userId: String, closetId: String) {
        viewModelScope.launch {
            _items.value = runCatching {
                repository.getItemsByCloset(userId, closetId)
            }.getOrElse { emptyList() }
        }
    }

    fun deleteItem(userId: String, itemId: String, closetId: String) {
        viewModelScope.launch {
            runCatching { repository.deleteItem(userId, itemId) }
            loadItemsForCloset(userId, closetId)
        }
    }
}
