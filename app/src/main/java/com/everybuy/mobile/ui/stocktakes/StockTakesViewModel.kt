package com.everybuy.mobile.ui.stocktakes

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.everybuy.mobile.core.util.serverMessage
import com.everybuy.mobile.data.StockTakeDto
import com.everybuy.mobile.data.StockTakesRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

sealed interface StockTakesState {
    data object Loading : StockTakesState
    data class Ready(
        val stockTakes: List<StockTakeDto>,
        val page: Int,
        val total: Int,
        val creating: Boolean = false,
        val createError: String? = null,
        val createdId: String? = null,
    ) : StockTakesState
    data object SessionExpired : StockTakesState
    data class Failed(val message: String) : StockTakesState
}

@HiltViewModel class StockTakesViewModel @Inject constructor(private val repository: StockTakesRepository) : ViewModel() {
    private val _state = MutableStateFlow<StockTakesState>(StockTakesState.Loading)
    val state = _state.asStateFlow()
    private var currentPage = 1

    init { load(1) }

    fun load(page: Int) = viewModelScope.launch {
        val target = page.coerceAtLeast(1)
        _state.value = StockTakesState.Loading
        try {
            val result = repository.list(target, STOCK_TAKE_PAGE_SIZE)
            currentPage = result.page
            _state.value = StockTakesState.Ready(
                stockTakes = result.data,
                page = result.page,
                total = result.total,
            )
        } catch (e: CancellationException) { throw e }
        catch (e: retrofit2.HttpException) {
            _state.value = when (e.code()) {
                401 -> StockTakesState.SessionExpired
                403 -> StockTakesState.Ready(emptyList(), target, 0)
                else -> StockTakesState.Failed("Stocktakes could not be loaded (${e.code()}).")
            }
        }
        catch (e: java.io.IOException) { _state.value = StockTakesState.Failed("Couldn’t connect. Check your internet and retry.") }
        catch (_: Exception) { _state.value = StockTakesState.Failed("Stocktakes could not be loaded. Try again.") }
    }

    fun refresh() = load(currentPage)

    fun create(name: String) = viewModelScope.launch {
        val current = _state.value as? StockTakesState.Ready ?: return@launch
        validateStockTakeName(name)?.let { message ->
            _state.value = current.copy(createError = message)
            return@launch
        }
        _state.value = current.copy(creating = true, createError = null)
        try {
            val created = repository.create(name)
            _state.value = current.copy(creating = false, createdId = created.id)
        } catch (e: CancellationException) { throw e }
        catch (e: retrofit2.HttpException) {
            if (e.code() == 401) { _state.value = StockTakesState.SessionExpired; return@launch }
            val message = when (e.code()) {
                400 -> e.serverMessage()?.let { "$it Add an active product before starting a stocktake." }
                    ?: "A stocktake needs at least one active product."
                403 -> "This account cannot start a stocktake."
                else -> "The stocktake could not be started (${e.code()})."
            }
            _state.value = current.copy(creating = false, createError = message)
        }
        catch (e: java.io.IOException) { _state.value = current.copy(creating = false, createError = "Couldn’t connect. Check your internet and retry.") }
        catch (_: Exception) { _state.value = current.copy(creating = false, createError = "The stocktake could not be started. Try again.") }
    }

    fun clearCreateError() {
        val current = _state.value as? StockTakesState.Ready ?: return
        if (current.createError != null) _state.value = current.copy(createError = null)
    }

    fun clearCreated() {
        val current = _state.value as? StockTakesState.Ready ?: return
        if (current.createdId != null) _state.value = current.copy(createdId = null)
    }
}
