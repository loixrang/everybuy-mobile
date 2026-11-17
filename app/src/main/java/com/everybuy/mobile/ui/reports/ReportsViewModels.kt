package com.everybuy.mobile.ui.reports

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.everybuy.mobile.data.MonthlyReportDto
import com.everybuy.mobile.data.ProfitReportDto
import com.everybuy.mobile.data.ReportsRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

sealed interface MonthlyReportState {
    data object Loading : MonthlyReportState
    data class Ready(val report: MonthlyReportDto) : MonthlyReportState
    data object SessionExpired : MonthlyReportState
    data class Failed(val message: String) : MonthlyReportState
}

sealed interface ProfitReportState {
    data object Loading : ProfitReportState
    data class Ready(val report: ProfitReportDto) : ProfitReportState
    data object SessionExpired : ProfitReportState
    data class Failed(val message: String) : ProfitReportState
}

@HiltViewModel class MonthlyReportViewModel @Inject constructor(private val repository: ReportsRepository) : ViewModel() {
    private val _state = MutableStateFlow<MonthlyReportState>(MonthlyReportState.Loading)
    val state = _state.asStateFlow()

    fun load(month: String) = viewModelScope.launch {
        _state.value = MonthlyReportState.Loading
        if (!isReportMonth(month)) {
            _state.value = MonthlyReportState.Failed("Choose a valid month in YYYY-MM format.")
            return@launch
        }
        try {
            _state.value = MonthlyReportState.Ready(repository.monthly(month.trim()))
        } catch (e: CancellationException) { throw e }
        catch (e: retrofit2.HttpException) {
            _state.value = when (e.code()) {
                400 -> MonthlyReportState.Failed("The month was rejected. Choose a valid month and retry.")
                401 -> MonthlyReportState.SessionExpired
                403 -> MonthlyReportState.Failed("This account does not have staff access to reports.")
                else -> MonthlyReportState.Failed("The monthly report could not be loaded (${e.code()}).")
            }
        }
        catch (e: java.io.IOException) { _state.value = MonthlyReportState.Failed("Couldn’t connect. Check your internet and retry.") }
        catch (_: Exception) { _state.value = MonthlyReportState.Failed("The monthly report could not be loaded. Try again.") }
    }
}

@HiltViewModel class ProfitReportViewModel @Inject constructor(private val repository: ReportsRepository) : ViewModel() {
    private val _state = MutableStateFlow<ProfitReportState>(ProfitReportState.Loading)
    val state = _state.asStateFlow()

    fun load(month: String) = viewModelScope.launch {
        _state.value = ProfitReportState.Loading
        if (!isReportMonth(month)) {
            _state.value = ProfitReportState.Failed("Choose a valid month in YYYY-MM format.")
            return@launch
        }
        try {
            _state.value = ProfitReportState.Ready(repository.profit(month.trim()))
        } catch (e: CancellationException) { throw e }
        catch (e: retrofit2.HttpException) {
            _state.value = when (e.code()) {
                400 -> ProfitReportState.Failed("The month was rejected. Choose a valid month and retry.")
                401 -> ProfitReportState.SessionExpired
                403 -> ProfitReportState.Failed("This account does not have staff access to reports.")
                else -> ProfitReportState.Failed("The profit report could not be loaded (${e.code()}).")
            }
        }
        catch (e: java.io.IOException) { _state.value = ProfitReportState.Failed("Couldn’t connect. Check your internet and retry.") }
        catch (_: Exception) { _state.value = ProfitReportState.Failed("The profit report could not be loaded. Try again.") }
    }
}
