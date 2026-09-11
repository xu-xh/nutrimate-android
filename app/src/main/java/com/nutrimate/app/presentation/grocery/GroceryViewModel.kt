package com.nutrimate.app.presentation.grocery

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.nutrimate.app.domain.model.GroceryItem
import com.nutrimate.app.domain.repository.GroceryRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class GroceryUiState(
    val items: List<GroceryItem> = emptyList(),
    val empty: Boolean = true
)

@HiltViewModel
class GroceryViewModel @Inject constructor(
    private val groceryRepository: GroceryRepository
) : ViewModel() {

    private val _state = MutableStateFlow(GroceryUiState())
    val state: StateFlow<GroceryUiState> = _state

    init {
        viewModelScope.launch {
            groceryRepository.observeAll().collect { items ->
                _state.value = GroceryUiState(items = items, empty = items.isEmpty())
            }
        }
    }

    fun toggle(item: GroceryItem) {
        viewModelScope.launch {
            groceryRepository.setChecked(item.id, !item.checked)
        }
    }

    fun remove(item: GroceryItem) {
        viewModelScope.launch {
            groceryRepository.delete(item.id)
        }
    }

    fun clearAll() {
        viewModelScope.launch {
            groceryRepository.clearAll()
        }
    }
}