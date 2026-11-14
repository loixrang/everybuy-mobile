package com.everybuy.mobile.ui.reports

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.everybuy.mobile.data.DailyOverview
import com.everybuy.mobile.data.ReportsRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

sealed interface OverviewState {
    data object Loading : OverviewState
    data class Ready(val report: DailyOverview) : OverviewState
    data class Failed(val message: String, val forbidden: Boolean = false, val sessionExpired: Boolean = false) : OverviewState
}

@HiltViewModel class OverviewViewModel @Inject constructor(private val repository: ReportsRepository) : ViewModel() {
    private val _state = MutableStateFlow<OverviewState>(OverviewState.Loading)
    val state = _state.asStateFlow()
    init { refresh() }
    fun refresh() = viewModelScope.launch {
        _state.value = OverviewState.Loading
        _state.value = try { OverviewState.Ready(repository.dailyOverview()) }
        catch (e: retrofit2.HttpException) {
            if (e.code() == 401) OverviewState.Failed("Your session has expired.", forbidden = true, sessionExpired = true)
            else if (e.code() == 403) OverviewState.Failed("This account does not have staff access to reports.", forbidden = true)
            else OverviewState.Failed("The overview could not be loaded (${e.code()}).")
        }
        catch (e: java.io.IOException) { OverviewState.Failed("Couldn’t connect. Check your internet and try again.") }
        catch (e: kotlinx.coroutines.CancellationException) { throw e }
        catch (e: Exception) { OverviewState.Failed("The overview could not be loaded. Try again.") }
    }
}
