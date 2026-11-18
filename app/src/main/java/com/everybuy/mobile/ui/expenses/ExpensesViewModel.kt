package com.everybuy.mobile.ui.expenses

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.everybuy.mobile.core.search.DebouncedFuzzySearch
import com.everybuy.mobile.core.util.formatNaira
import com.everybuy.mobile.core.util.serverMessage
import com.everybuy.mobile.data.CreateExpenseInput
import com.everybuy.mobile.data.ExpenseDto
import com.everybuy.mobile.data.ExpensesRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.util.UUID
import javax.inject.Inject

sealed interface ExpensesState {
    data object Loading : ExpensesState
    data class Ready(
        val expenses: List<ExpenseDto>,
        val query: String = "",
        val searchResults: List<ExpenseDto> = expenses,
        val searching: Boolean = false,
        val submitting: Boolean = false,
        val formError: String? = null,
        val successMessage: String? = null,
    ) : ExpensesState {
        val filtered: List<ExpenseDto>
            get() = searchResults
    }
    data object SessionExpired : ExpensesState
    data class Failed(val message: String) : ExpensesState
}

@HiltViewModel class ExpensesViewModel @Inject constructor(private val repository: ExpensesRepository) : ViewModel() {
    private val _state = MutableStateFlow<ExpensesState>(ExpensesState.Loading)
    val state = _state.asStateFlow()
    private val fuzzySearch = DebouncedFuzzySearch<ExpenseDto>(viewModelScope)
    private var payloadKey: String? = null
    private var requestId: String? = null

    init { refresh() }

    fun refresh() = viewModelScope.launch {
        fuzzySearch.cancel()
        _state.value = ExpensesState.Loading
        try {
            _state.value = ExpensesState.Ready(repository.list())
        } catch (e: CancellationException) { throw e }
        catch (e: retrofit2.HttpException) {
            _state.value = when (e.code()) {
                401 -> ExpensesState.SessionExpired
                403 -> ExpensesState.Failed("This account does not have staff access to expenses.")
                else -> ExpensesState.Failed("Expenses could not be loaded (${e.code()}).")
            }
        }
        catch (e: java.io.IOException) { _state.value = ExpensesState.Failed("Couldn’t connect. Check your internet and retry.") }
        catch (_: Exception) { _state.value = ExpensesState.Failed("Expenses could not be loaded. Try again.") }
    }

    fun setQuery(value: String) {
        val current = _state.value as? ExpensesState.Ready ?: return
        _state.value = current.copy(query = value, searching = value.isNotBlank())
        searchExpenses(current.expenses, value)
    }

    fun record(title: String, category: String, amount: String, notes: String) = viewModelScope.launch {
        val current = _state.value as? ExpensesState.Ready ?: return@launch
        val validation = validateExpense(title, category, amount, notes)
        if (validation != null) {
            _state.value = current.copy(formError = validation)
            return@launch
        }
        val cleanTitle = title.trim()
        val cleanCategory = category.trim()
        val cleanAmount = amount.trim()
        val cleanNotes = notes.trim().ifBlank { null }
        val key = "$cleanTitle|$cleanCategory|$cleanAmount|${cleanNotes.orEmpty()}"
        if (payloadKey != key) {
            payloadKey = key
            requestId = UUID.randomUUID().toString()
        }
        val stableRequestId = requestId ?: UUID.randomUUID().toString().also { requestId = it; payloadKey = key }
        _state.value = current.copy(submitting = true, formError = null)
        try {
            repository.create(
                CreateExpenseInput(
                    requestId = stableRequestId,
                    title = cleanTitle,
                    category = cleanCategory,
                    amount = cleanAmount,
                    notes = cleanNotes,
                ),
            )
            payloadKey = null
            requestId = null
            val expenses = repository.list()
            val ready = ExpensesState.Ready(
                expenses = expenses,
                query = current.query,
                searching = current.query.isNotBlank(),
                successMessage = "${formatNaira(cleanAmount)} recorded for $cleanTitle.",
            )
            _state.value = ready
            searchExpenses(expenses, current.query)
        } catch (e: CancellationException) { throw e }
        catch (e: retrofit2.HttpException) {
            if (e.code() == 401) { _state.value = ExpensesState.SessionExpired; return@launch }
            val message = when (e.code()) {
                400 -> e.serverMessage() ?: "The expense details were rejected. Check the values and try again."
                403 -> "This account does not have staff access to record expenses."
                409 -> "This submission was already used for a different expense. Change the details and try again."
                429 -> "Too many requests. Wait a moment and try again."
                else -> e.serverMessage() ?: "The expense could not be recorded (${e.code()})."
            }
            _state.value = current.copy(submitting = false, formError = message)
        }
        catch (e: java.io.IOException) {
            _state.value = current.copy(
                submitting = false,
                formError = "Couldn’t connect. Retry this expense to avoid recording it twice.",
            )
        }
        catch (_: Exception) {
            _state.value = current.copy(submitting = false, formError = "The expense could not be recorded. Try again.")
        }
    }

    fun clearFormError() = updateReady { if (it.formError != null) it.copy(formError = null) else it }

    fun dismissSuccess() = updateReady { if (it.successMessage != null) it.copy(successMessage = null) else it }

    private fun updateReady(transform: (ExpensesState.Ready) -> ExpensesState.Ready) {
        val current = _state.value as? ExpensesState.Ready ?: return
        _state.value = transform(current)
    }

    private fun searchExpenses(expenses: List<ExpenseDto>, query: String) {
        fuzzySearch.search(
            items = expenses,
            query = query,
            searchableText = { expense ->
                listOf(
                    expense.id,
                    expense.requestId,
                    expense.title,
                    expense.category,
                    expense.amount,
                    expense.notes.orEmpty(),
                    expense.spentAt,
                    expense.recordedBy,
                    expense.createdAt,
                ).joinToString(" ")
            },
        ) { results ->
            val latest = _state.value as? ExpensesState.Ready ?: return@search
            if (latest.expenses === expenses && latest.query == query) {
                _state.value = latest.copy(searchResults = results, searching = false)
            }
        }
    }
}
