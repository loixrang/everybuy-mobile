package com.everybuy.mobile.ui.auth

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.everybuy.mobile.data.AuthRepository
import com.everybuy.mobile.data.AuthState
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel class AuthViewModel @Inject constructor(private val repository: AuthRepository, private val preferences: ThemePreferences) : ViewModel() {
    private val _state = MutableStateFlow<AuthState>(AuthState.Loading)
    val state = _state.asStateFlow()
    val theme = preferences.theme
    init { restore() }
    fun restore() = viewModelScope.launch { _state.value = repository.restore() }
    fun signIn(email: String, password: String) = viewModelScope.launch { _state.value = AuthState.Loading; _state.value = repository.signIn(email, password) }
    fun verifyTotp(code: String) = viewModelScope.launch { _state.value = repository.verifyTotp(code) }
    fun signOut() = viewModelScope.launch { repository.signOut(); _state.value = AuthState.SignedOut }
    fun setTheme(theme: AppTheme) = viewModelScope.launch { preferences.set(theme) }
}
