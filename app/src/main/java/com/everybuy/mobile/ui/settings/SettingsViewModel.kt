package com.everybuy.mobile.ui.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.everybuy.mobile.core.util.serverMessage
import com.everybuy.mobile.data.AdminRepository
import com.everybuy.mobile.data.ShopSettingsDto
import com.everybuy.mobile.data.UpdateShopSettingsInput
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

sealed interface SettingsState {
    data object Loading : SettingsState
    data class Ready(
        val shop: ShopSettingsDto? = null,
        val shopMissing: Boolean = false,
        val savingShop: Boolean = false,
        val shopError: String? = null,
        val shopSaved: Boolean = false,
        val savingProfile: Boolean = false,
        val profileError: String? = null,
        val profileSaved: Boolean = false,
    ) : SettingsState
    data object SessionExpired : SettingsState
    data class Failed(val message: String) : SettingsState
}

@HiltViewModel class SettingsViewModel @Inject constructor(private val repository: AdminRepository) : ViewModel() {
    private val _state = MutableStateFlow<SettingsState>(SettingsState.Loading)
    val state = _state.asStateFlow()

    init { refresh() }

    fun refresh() = viewModelScope.launch {
        _state.value = SettingsState.Loading
        _state.value = try {
            SettingsState.Ready(shop = repository.shop())
        } catch (e: CancellationException) { throw e }
        catch (e: retrofit2.HttpException) {
            when (e.code()) {
                401 -> SettingsState.SessionExpired
                403 -> SettingsState.Failed("This account does not have staff access to settings.")
                404 -> SettingsState.Ready(shopMissing = true)
                else -> SettingsState.Failed("Shop details could not be loaded (${e.code()}).")
            }
        }
        catch (_: java.io.IOException) { SettingsState.Failed("Couldn’t connect. Check your internet and retry.") }
        catch (_: Exception) { SettingsState.Failed("Shop details could not be loaded. Try again.") }
    }

    fun saveShop(id: String, input: UpdateShopSettingsInput) {
        val current = _state.value as? SettingsState.Ready ?: return
        val validation = validateShopSettings(input.name, input.currency)
        if (validation != null) { _state.value = current.copy(shopError = validation, shopSaved = false); return }
        if (current.savingShop) return
        _state.value = current.copy(savingShop = true, shopError = null, shopSaved = false)
        viewModelScope.launch {
            val next = try {
                val updated = repository.updateShop(id, input.copy(name = input.name.trim(), currency = input.currency.trim()))
                SettingsState.Ready(shop = updated, shopSaved = true)
            } catch (e: CancellationException) { throw e }
            catch (e: retrofit2.HttpException) {
                when (e.code()) {
                    401 -> SettingsState.SessionExpired
                    403 -> current.copy(savingShop = false, shopError = "Only an owner can change the shop details.")
                    else -> current.copy(savingShop = false, shopError = e.serverMessage() ?: "The shop details could not be saved (${e.code()}).")
                }
            }
            catch (_: java.io.IOException) { current.copy(savingShop = false, shopError = "Couldn’t connect. Check your internet and retry.") }
            catch (_: Exception) { current.copy(savingShop = false, shopError = "The shop details could not be saved. Try again.") }
            _state.value = next
        }
    }

    fun updateName(name: String) {
        val current = _state.value as? SettingsState.Ready ?: return
        val validation = validateProfileName(name)
        if (validation != null) { _state.value = current.copy(profileError = validation, profileSaved = false); return }
        if (current.savingProfile) return
        _state.value = current.copy(savingProfile = true, profileError = null, profileSaved = false)
        viewModelScope.launch {
            val next = try {
                repository.updateName(name.trim())
                current.copy(savingProfile = false, profileSaved = true)
            } catch (e: CancellationException) { throw e }
            catch (e: retrofit2.HttpException) {
                when (e.code()) {
                    401 -> SettingsState.SessionExpired
                    else -> current.copy(savingProfile = false, profileError = e.serverMessage() ?: "Your name could not be saved (${e.code()}).")
                }
            }
            catch (_: java.io.IOException) { current.copy(savingProfile = false, profileError = "Couldn’t connect. Check your internet and retry.") }
            catch (_: Exception) { current.copy(savingProfile = false, profileError = "Your name could not be saved. Try again.") }
            _state.value = next
        }
    }

    fun clearShopFeedback() = updateReady { it.copy(shopError = null, shopSaved = false) }
    fun clearProfileFeedback() = updateReady { it.copy(profileError = null, profileSaved = false) }

    private fun updateReady(transform: (SettingsState.Ready) -> SettingsState.Ready) {
        val current = _state.value as? SettingsState.Ready ?: return
        _state.value = transform(current)
    }
}
