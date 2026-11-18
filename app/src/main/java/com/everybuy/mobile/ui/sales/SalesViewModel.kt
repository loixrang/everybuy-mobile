package com.everybuy.mobile.ui.sales

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.everybuy.mobile.core.search.DebouncedFuzzySearch
import com.everybuy.mobile.data.SalesListRowDto
import com.everybuy.mobile.data.SalesRepository
import com.everybuy.mobile.data.SalesSummaryDto
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

const val SALES_PAGE_SIZE = 20

data class SalesFilters(
    val query: String = "",
    val paymentStatus: String? = null,
    val from: String? = null,
    val to: String? = null,
)

sealed interface SalesState {
    data object Loading : SalesState
    data class Ready(
        val rows: List<SalesListRowDto>,
        val summary: SalesSummaryDto?,
        val summaryError: String?,
        val page: Int,
        val total: Int,
        val totalPages: Int,
        val filters: SalesFilters,
        val searchResults: List<SalesListRowDto> = rows,
        val searching: Boolean = false,
    ) : SalesState
    data object SessionExpired : SalesState
    data class Failed(val message: String) : SalesState
}

@HiltViewModel class SalesViewModel @Inject constructor(private val repository: SalesRepository) : ViewModel() {
    private val _state = MutableStateFlow<SalesState>(SalesState.Loading)
    val state = _state.asStateFlow()
    private val fuzzySearch = DebouncedFuzzySearch<SalesListRowDto>(viewModelScope)
    private var currentFilters = SalesFilters()
    private var currentPage = 1

    init { load(1, currentFilters) }

    fun refresh() = load(currentPage, currentFilters)

    fun applyFilters(filters: SalesFilters) = load(1, filters)

    fun loadPage(page: Int) = load(page.coerceAtLeast(1), currentFilters)

    fun setQuery(value: String) {
        val current = _state.value as? SalesState.Ready ?: return
        currentFilters = currentFilters.copy(query = value)
        _state.value = current.copy(filters = current.filters.copy(query = value), searching = value.isNotBlank())
        searchRows(current.rows, value)
    }

    private fun load(page: Int, filters: SalesFilters) = viewModelScope.launch {
        fuzzySearch.cancel()
        _state.value = SalesState.Loading
        try {
            coroutineScope {
                val sales = async {
                    repository.list(
                        page = page,
                        limit = SALES_PAGE_SIZE,
                        from = filters.from,
                        to = filters.to,
                        query = null,
                        paymentStatus = filters.paymentStatus,
                    )
                }
                val summary = async { runCatching { repository.summary(filters.from, filters.to) } }
                val result = sales.await()
                val summaryResult = summary.await()
                currentFilters = filters
                currentPage = result.page
                val ready = SalesState.Ready(
                    rows = result.data,
                    summary = summaryResult.getOrNull(),
                    summaryError = summaryResult.exceptionOrNull()?.let { "Sales totals are unavailable." },
                    page = result.page,
                    total = result.total,
                    totalPages = maxOf(1, result.totalPages),
                    filters = filters,
                    searching = filters.query.isNotBlank(),
                )
                _state.value = ready
                searchRows(result.data, filters.query)
            }
        } catch (e: CancellationException) { throw e }
        catch (e: retrofit2.HttpException) {
            _state.value = when (e.code()) {
                401 -> SalesState.SessionExpired
                403 -> SalesState.Failed("This account does not have staff access to sales.")
                else -> SalesState.Failed("Sales could not be loaded (${e.code()}).")
            }
        }
        catch (e: java.io.IOException) { _state.value = SalesState.Failed("Couldn’t connect. Check your internet and retry.") }
        catch (_: Exception) { _state.value = SalesState.Failed("Sales could not be loaded. Try again.") }
    }

    private fun searchRows(rows: List<SalesListRowDto>, query: String) {
        fuzzySearch.search(
            items = rows,
            query = query,
            searchableText = { row ->
                listOf(
                    row.id,
                    row.reference,
                    row.createdAt,
                    row.customer?.id.orEmpty(),
                    row.customer?.name.orEmpty(),
                    row.itemsTotal,
                    row.amountPaid,
                    row.balance,
                    row.status,
                ).joinToString(" ")
            },
        ) { results ->
            val latest = _state.value as? SalesState.Ready ?: return@search
            if (latest.rows === rows && latest.filters.query == query) {
                _state.value = latest.copy(searchResults = results, searching = false)
            }
        }
    }
}
