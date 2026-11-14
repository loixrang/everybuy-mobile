package com.everybuy.mobile.ui.inventory

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.everybuy.mobile.data.ProductInventory
import com.everybuy.mobile.data.ProductsRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

sealed interface InventoryState {
    data object Loading : InventoryState
    data class Ready(val inventory: ProductInventory) : InventoryState
    data class Failed(val message: String) : InventoryState
    data object Unauthorized : InventoryState
}

@HiltViewModel class InventoryViewModel @Inject constructor(private val repository: ProductsRepository) : ViewModel() {
    private val _state = MutableStateFlow<InventoryState>(InventoryState.Loading)
    val state = _state.asStateFlow()
    init { refresh() }
    fun refresh() = viewModelScope.launch {
        _state.value = InventoryState.Loading
        _state.value = try { InventoryState.Ready(repository.load()) }
        catch (e: retrofit2.HttpException) { if (e.code() == 401) InventoryState.Unauthorized else InventoryState.Failed("Inventory could not be loaded (${e.code()}).") }
        catch (e: java.io.IOException) { InventoryState.Failed("Couldn’t connect. Check your internet and retry.") }
        catch (e: kotlinx.coroutines.CancellationException) { throw e }
        catch (e: Exception) { InventoryState.Failed("Inventory could not be loaded. Try again.") }
    }
}
