package com.everybuy.mobile.ui.audit

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.everybuy.mobile.core.search.DebouncedFuzzySearch
import com.everybuy.mobile.core.util.formatNaira
import com.everybuy.mobile.data.AuditEntryDto
import com.everybuy.mobile.data.AuditRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.serialization.json.JsonPrimitive
import javax.inject.Inject

sealed interface AuditState {
    data object Loading : AuditState
    data class Ready(
        val entries: List<AuditEntryDto>,
        val query: String = "",
        val searchResults: List<AuditEntryDto> = entries,
        val searching: Boolean = false,
    ) : AuditState {
        val filtered: List<AuditEntryDto> get() = searchResults
    }
    data object SessionExpired : AuditState
    data class Failed(val message: String) : AuditState
}

fun auditDetailSummary(entry: AuditEntryDto): String {
    val detail = entry.detail ?: return "—"
    val title = (detail["title"] as? JsonPrimitive)?.takeIf { it.isString }?.content
    val amount = (detail["amount"] as? JsonPrimitive)?.takeIf { it.isString }?.content
    return when {
        title != null && amount != null -> "$title · ${formatNaira(amount)}"
        title != null -> title
        amount != null -> formatNaira(amount)
        detail.keys.isNotEmpty() -> detail.keys.joinToString(", ")
        else -> "—"
    }
}

@HiltViewModel class AuditViewModel @Inject constructor(private val repository: AuditRepository) : ViewModel() {
    private val _state = MutableStateFlow<AuditState>(AuditState.Loading)
    val state = _state.asStateFlow()
    private val fuzzySearch = DebouncedFuzzySearch<AuditEntryDto>(viewModelScope)

    init { refresh() }

    fun refresh() = viewModelScope.launch {
        fuzzySearch.cancel()
        _state.value = AuditState.Loading
        try {
            _state.value = AuditState.Ready(repository.list())
        } catch (e: CancellationException) { throw e }
        catch (e: retrofit2.HttpException) {
            _state.value = when (e.code()) {
                401 -> AuditState.SessionExpired
                403 -> AuditState.Failed("This account does not have staff access to the audit log.")
                else -> AuditState.Failed("The audit log could not be loaded (${e.code()}).")
            }
        }
        catch (e: java.io.IOException) { _state.value = AuditState.Failed("Couldn’t connect. Check your internet and retry.") }
        catch (_: Exception) { _state.value = AuditState.Failed("The audit log could not be loaded. Try again.") }
    }

    fun setQuery(value: String) {
        val current = _state.value as? AuditState.Ready ?: return
        _state.value = current.copy(query = value, searching = value.isNotBlank())
        fuzzySearch.search(
            items = current.entries,
            query = value,
            searchableText = { entry ->
                listOf(
                    entry.actor,
                    entry.action,
                    entry.entity,
                    entry.entityId,
                    entry.at,
                    entry.detail?.entries?.joinToString(" ") { (key, detailValue) -> "$key $detailValue" }.orEmpty(),
                ).joinToString(" ")
            },
        ) { results ->
            val latest = _state.value as? AuditState.Ready ?: return@search
            if (latest.entries === current.entries && latest.query == value) {
                _state.value = latest.copy(searchResults = results, searching = false)
            }
        }
    }
}
