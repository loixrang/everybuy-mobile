package com.everybuy.mobile.ui.auth

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.everybuy.mobile.core.util.serverMessage
import com.everybuy.mobile.data.AuthRepository
import com.everybuy.mobile.data.InvitePreviewDto
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

const val PASSWORD_MIN = 12
const val PASSWORD_MAX = 128

private val emailPattern = Regex("""^[^\s@]+@[^\s@]+\.[^\s@]+$""")

data class ForgotPasswordState(
    val email: String = "",
    val sending: Boolean = false,
    val sentTo: String? = null,
    val error: String? = null,
)

@HiltViewModel class ForgotPasswordViewModel @Inject constructor(private val repository: AuthRepository) : ViewModel() {
    private val _state = MutableStateFlow(ForgotPasswordState())
    val state = _state.asStateFlow()

    fun setEmail(value: String) { _state.value = _state.value.copy(email = value, error = null, sentTo = null) }

    fun submit() {
        val current = _state.value
        if (current.sending) return
        val email = current.email.trim()
        if (!emailPattern.matches(email)) { _state.value = current.copy(error = "Enter a valid email address."); return }
        _state.value = current.copy(sending = true, error = null)
        viewModelScope.launch {
            _state.value = try {
                repository.requestPasswordReset(email, RESET_PASSWORD_REDIRECT)
                current.copy(sending = false, sentTo = email)
            } catch (e: CancellationException) { throw e }
            catch (e: retrofit2.HttpException) {
                current.copy(
                    sending = false,
                    error = when (e.code()) {
                        429 -> "Too many reset requests. Wait a moment and try again."
                        else -> if (e.code() >= 500) "We could not send the email. Try again shortly." else e.serverMessage() ?: "Unable to request a password reset."
                    },
                )
            }
            catch (_: java.io.IOException) { current.copy(sending = false, error = "Unable to connect. Check your connection and try again.") }
            catch (_: Exception) { current.copy(sending = false, error = "Unable to request a password reset. Try again.") }
        }
    }

    fun reset() { _state.value = ForgotPasswordState() }
}

data class ResetPasswordState(
    val password: String = "",
    val confirmation: String = "",
    val saving: Boolean = false,
    val done: Boolean = false,
    val error: String? = null,
)

@HiltViewModel class ResetPasswordViewModel @Inject constructor(private val repository: AuthRepository) : ViewModel() {
    private val _state = MutableStateFlow(ResetPasswordState())
    val state = _state.asStateFlow()

    fun setPassword(value: String) { _state.value = _state.value.copy(password = value, error = null) }
    fun setConfirmation(value: String) { _state.value = _state.value.copy(confirmation = value, error = null) }

    fun submit(token: String) {
        val current = _state.value
        if (current.saving || current.done) return
        val validation = validateNewPassword(current.password, current.confirmation)
        if (validation != null) { _state.value = current.copy(error = validation); return }
        _state.value = current.copy(saving = true, error = null)
        viewModelScope.launch {
            _state.value = try {
                repository.resetPassword(token, current.password)
                current.copy(saving = false, done = true, password = "", confirmation = "")
            } catch (e: CancellationException) { throw e }
            catch (e: retrofit2.HttpException) {
                current.copy(
                    saving = false,
                    error = when (e.code()) {
                        400 -> "This reset link is invalid or has expired. Request a new one."
                        429 -> "Too many attempts. Wait a moment and try again."
                        else -> if (e.code() >= 500) "We could not reset your password. Try again shortly." else e.serverMessage() ?: "Unable to reset the password."
                    },
                )
            }
            catch (_: java.io.IOException) { current.copy(saving = false, error = "Unable to connect. Check your connection and try again.") }
            catch (_: Exception) { current.copy(saving = false, error = "Unable to reset the password. Try again.") }
        }
    }
}

sealed interface InviteAcceptState {
    data object Loading : InviteAcceptState
    data class Ready(
        val preview: InvitePreviewDto,
        val password: String = "",
        val confirmation: String = "",
        val accepting: Boolean = false,
        val error: String? = null,
    ) : InviteAcceptState
    data class Invalid(val message: String) : InviteAcceptState
    data class Failed(val message: String) : InviteAcceptState
    data class Done(val email: String) : InviteAcceptState
}

@HiltViewModel class InviteAcceptViewModel @Inject constructor(private val repository: AuthRepository) : ViewModel() {
    private val _state = MutableStateFlow<InviteAcceptState>(InviteAcceptState.Loading)
    val state = _state.asStateFlow()
    private var loadedToken: String? = null

    fun load(token: String) {
        if (loadedToken == token && _state.value !is InviteAcceptState.Loading) return
        loadedToken = token
        _state.value = InviteAcceptState.Loading
        viewModelScope.launch {
            _state.value = try {
                InviteAcceptState.Ready(repository.previewInvitation(token))
            } catch (e: CancellationException) { throw e }
            catch (e: retrofit2.HttpException) {
                if (e.code() == 400) InviteAcceptState.Invalid("This invitation is no longer available. It may have expired, already been used, or been replaced. Ask an owner for a new invitation.")
                else InviteAcceptState.Failed(e.serverMessage() ?: "The invitation could not be loaded (${e.code()}).")
            }
            catch (_: java.io.IOException) { InviteAcceptState.Failed("We could not reach Everybuy. Check your connection and try again.") }
            catch (_: Exception) { InviteAcceptState.Failed("The invitation could not be loaded. Try again.") }
        }
    }

    fun retry() {
        val token = loadedToken ?: return
        loadedToken = null
        load(token)
    }

    fun setPassword(value: String) = updateReady { it.copy(password = value, error = null) }
    fun setConfirmation(value: String) = updateReady { it.copy(confirmation = value, error = null) }

    fun accept(token: String) {
        val current = _state.value as? InviteAcceptState.Ready ?: return
        if (current.accepting) return
        val validation = validateNewPassword(current.password, current.confirmation)
        if (validation != null) { _state.value = current.copy(error = validation); return }
        _state.value = current.copy(accepting = true, error = null)
        viewModelScope.launch {
            _state.value = try {
                repository.acceptInvitation(token, current.password)
                InviteAcceptState.Done(current.preview.email)
            } catch (e: CancellationException) { throw e }
            catch (e: retrofit2.HttpException) {
                when (e.code()) {
                    400 -> InviteAcceptState.Invalid("This invitation is no longer available. It may have expired, already been used, or been replaced. Ask an owner for a new invitation.")
                    409 -> current.copy(accepting = false, error = e.serverMessage() ?: "An account already exists for this email. Ask an owner for help.")
                    else -> current.copy(accepting = false, error = e.serverMessage() ?: "Account setup failed. Please retry.")
                }
            }
            catch (_: java.io.IOException) { current.copy(accepting = false, error = "We could not reach Everybuy. Check your connection and try again.") }
            catch (_: Exception) { current.copy(accepting = false, error = "Account setup failed. Please retry.") }
        }
    }

    private fun updateReady(transform: (InviteAcceptState.Ready) -> InviteAcceptState.Ready) {
        val current = _state.value as? InviteAcceptState.Ready ?: return
        _state.value = transform(current)
    }
}

fun validateNewPassword(password: String, confirmation: String): String? {
    if (password.length < PASSWORD_MIN || password.length > PASSWORD_MAX) {
        return "Choose a password between $PASSWORD_MIN and $PASSWORD_MAX characters."
    }
    if (password != confirmation) return "The passwords do not match."
    return null
}
