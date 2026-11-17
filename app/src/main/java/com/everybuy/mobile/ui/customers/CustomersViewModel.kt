package com.everybuy.mobile.ui.customers

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.everybuy.mobile.data.CustomerListItemDto
import com.everybuy.mobile.data.CustomersRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.math.BigDecimal
import javax.inject.Inject

sealed interface CustomersState {
    data object Loading : CustomersState
    data class Ready(
        val customers: List<CustomerListItemDto>,
        val query: String = "",
        val status: String = STATUS_ALL,
    ) : CustomersState {
        val filtered: List<CustomerListItemDto>
            get() {
                val search = query.trim()
                return customers
                    .filter { customer ->
                        when (status) {
                            STATUS_ACTIVE -> customer.active
                            STATUS_INACTIVE -> !customer.active
                            else -> true
                        }
                    }
                    .filter { customer ->
                        search.isEmpty() || "${customer.name} ${customer.phone.orEmpty()}".contains(search, ignoreCase = true)
                    }
                    .sortedWith(compareByDescending<CustomerListItemDto> { it.active }.thenBy { it.name.lowercase() })
            }

        val activeCount: Int get() = customers.count { it.active }

        val totalOutstanding: BigDecimal
            get() = customers.fold(BigDecimal.ZERO) { total, customer ->
                total + (customer.balance.toBigDecimalOrNull() ?: BigDecimal.ZERO)
            }
    }
    data object SessionExpired : CustomersState
    data class Failed(val message: String) : CustomersState
}

const val STATUS_ALL = "ALL"
const val STATUS_ACTIVE = "ACTIVE"
const val STATUS_INACTIVE = "INACTIVE"

@HiltViewModel class CustomersViewModel @Inject constructor(private val repository: CustomersRepository) : ViewModel() {
    private val _state = MutableStateFlow<CustomersState>(CustomersState.Loading)
    val state = _state.asStateFlow()

    init { refresh() }

    fun refresh() = viewModelScope.launch {
        _state.value = CustomersState.Loading
        try {
            _state.value = CustomersState.Ready(repository.list())
        } catch (e: CancellationException) { throw e }
        catch (e: retrofit2.HttpException) {
            _state.value = when (e.code()) {
                401 -> CustomersState.SessionExpired
                403 -> CustomersState.Failed("This account does not have staff access to customers.")
                else -> CustomersState.Failed("Customers could not be loaded (${e.code()}).")
            }
        }
        catch (e: java.io.IOException) { _state.value = CustomersState.Failed("Couldn’t connect. Check your internet and retry.") }
        catch (_: Exception) { _state.value = CustomersState.Failed("Customers could not be loaded. Try again.") }
    }

    fun setQuery(value: String) = updateReady { it.copy(query = value) }

    fun setStatus(value: String) = updateReady { it.copy(status = value) }

    fun clearFilters() = updateReady { it.copy(query = "", status = STATUS_ALL) }

    private fun updateReady(transform: (CustomersState.Ready) -> CustomersState.Ready) {
        val current = _state.value as? CustomersState.Ready ?: return
        _state.value = transform(current)
    }
}
