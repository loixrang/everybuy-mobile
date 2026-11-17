package com.everybuy.mobile.ui.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.everybuy.mobile.core.util.serverMessage
import com.everybuy.mobile.data.AdminRepository
import com.everybuy.mobile.data.StaffInvitationDto
import com.everybuy.mobile.data.StaffMemberDto
import com.everybuy.mobile.data.UpdateStaffInput
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

sealed interface TeamState {
    data object Loading : TeamState
    data class Ready(
        val members: List<StaffMemberDto> = emptyList(),
        val invitations: List<StaffInvitationDto> = emptyList(),
        val busyStaffId: String? = null,
        val revokingId: String? = null,
        val inviting: Boolean = false,
        val inviteSent: Boolean = false,
        val actionError: String? = null,
        val notice: String? = null,
    ) : TeamState {
        val pendingInvites: List<StaffInvitationDto> get() = pendingInvitations(invitations)
    }
    data class Forbidden(val message: String) : TeamState
    data object SessionExpired : TeamState
    data class Failed(val message: String) : TeamState
}

@HiltViewModel class TeamViewModel @Inject constructor(private val repository: AdminRepository) : ViewModel() {
    private val _state = MutableStateFlow<TeamState>(TeamState.Loading)
    val state = _state.asStateFlow()

    init { refresh() }

    fun refresh() = viewModelScope.launch {
        _state.value = TeamState.Loading
        _state.value = try {
            val members = repository.staff()
            val invitations = try { repository.invitations() } catch (e: retrofit2.HttpException) {
                if (e.code() == 404) emptyList() else throw e
            }
            TeamState.Ready(
                members = members.sortedWith(compareByDescending<StaffMemberDto> { it.role == "Owner" }.thenBy { it.name.lowercase() }),
                invitations = invitations,
            )
        } catch (e: CancellationException) { throw e }
        catch (e: retrofit2.HttpException) {
            when (e.code()) {
                401 -> TeamState.SessionExpired
                403 -> TeamState.Forbidden("Only an owner can manage the team.")
                else -> TeamState.Failed("The team could not be loaded (${e.code()}).")
            }
        }
        catch (_: java.io.IOException) { TeamState.Failed("Couldn’t connect. Check your internet and retry.") }
        catch (_: Exception) { TeamState.Failed("The team could not be loaded. Try again.") }
    }

    fun setRole(member: StaffMemberDto, role: String) {
        if (role == member.role) return
        mutate(member, { repository.updateStaff(member.staffId, UpdateStaffInput(role = role)) }) {
            "${member.name} is now ${if (role == "Owner") "an owner" else "staff"}."
        }
    }

    fun setActive(member: StaffMemberDto, active: Boolean) {
        mutate(member, { repository.updateStaff(member.staffId, UpdateStaffInput(active = active)) }) {
            "${member.name} ${if (active) "reactivated" else "deactivated"}."
        }
    }

    fun invite(name: String, email: String) {
        val current = _state.value as? TeamState.Ready ?: return
        val validation = validateStaffInvite(name, email)
        if (validation != null) { _state.value = current.copy(actionError = validation, notice = null); return }
        if (current.inviting) return
        _state.value = current.copy(inviting = true, inviteSent = false, actionError = null, notice = null)
        viewModelScope.launch {
            val next = try {
                repository.inviteStaff(name.trim(), email.trim())
                val invitations = try { repository.invitations() } catch (_: Exception) { current.invitations }
                current.copy(inviting = false, inviteSent = true, invitations = invitations, notice = "Invitation sent to ${email.trim()}.")
            } catch (e: CancellationException) { throw e }
            catch (e: retrofit2.HttpException) {
                when (e.code()) {
                    401 -> TeamState.SessionExpired
                    403 -> TeamState.Forbidden("Only an owner can invite staff.")
                    409 -> current.copy(inviting = false, actionError = e.serverMessage() ?: "A user or invitation already exists for that email.")
                    else -> current.copy(inviting = false, actionError = e.serverMessage() ?: "The invitation could not be sent (${e.code()}).")
                }
            }
            catch (_: java.io.IOException) { current.copy(inviting = false, actionError = "Couldn’t connect. Check your internet and retry.") }
            catch (_: Exception) { current.copy(inviting = false, actionError = "The invitation could not be sent. Try again.") }
            _state.value = next
        }
    }

    fun revoke(invitation: StaffInvitationDto) {
        val current = _state.value as? TeamState.Ready ?: return
        if (current.revokingId != null) return
        _state.value = current.copy(revokingId = invitation.id, actionError = null, notice = null)
        viewModelScope.launch {
            val next = try {
                repository.revokeInvitation(invitation.id)
                current.copy(revokingId = null, invitations = current.invitations.filterNot { it.id == invitation.id }, notice = "Invitation for ${invitation.email} revoked.")
            } catch (e: CancellationException) { throw e }
            catch (e: retrofit2.HttpException) {
                when (e.code()) {
                    401 -> TeamState.SessionExpired
                    403 -> TeamState.Forbidden("Only an owner can revoke invitations.")
                    else -> current.copy(revokingId = null, actionError = e.serverMessage() ?: "The invitation could not be revoked (${e.code()}).")
                }
            }
            catch (_: java.io.IOException) { current.copy(revokingId = null, actionError = "Couldn’t connect. Check your internet and retry.") }
            catch (_: Exception) { current.copy(revokingId = null, actionError = "The invitation could not be revoked. Try again.") }
            _state.value = next
        }
    }

    fun clearFeedback() = updateReady { it.copy(actionError = null, notice = null, inviteSent = false) }

    /** Applies one member update and reports the outcome through the same state. */
    private fun mutate(member: StaffMemberDto, call: suspend () -> StaffMemberDto?, notice: (StaffMemberDto) -> String) {
        val current = _state.value as? TeamState.Ready ?: return
        if (current.busyStaffId != null) return
        _state.value = current.copy(busyStaffId = member.staffId, actionError = null, notice = null)
        viewModelScope.launch {
            val next = try {
                val updated = call()
                if (updated == null) current.copy(busyStaffId = null, actionError = "The change was not returned. Refresh to check the team.")
                else current.copy(
                    busyStaffId = null,
                    members = current.members.map { if (it.staffId == member.staffId) updated else it },
                    notice = notice(updated),
                )
            } catch (e: CancellationException) { throw e }
            catch (e: retrofit2.HttpException) {
                when (e.code()) {
                    401 -> TeamState.SessionExpired
                    403 -> TeamState.Forbidden("Only an owner can manage the team.")
                    409 -> current.copy(busyStaffId = null, actionError = e.serverMessage() ?: "That change is not allowed.")
                    else -> current.copy(busyStaffId = null, actionError = e.serverMessage() ?: "The change could not be saved (${e.code()}).")
                }
            }
            catch (_: java.io.IOException) { current.copy(busyStaffId = null, actionError = "Couldn’t connect. Check your internet and retry.") }
            catch (_: Exception) { current.copy(busyStaffId = null, actionError = "The change could not be saved. Try again.") }
            _state.value = next
        }
    }

    private fun updateReady(transform: (TeamState.Ready) -> TeamState.Ready) {
        val current = _state.value as? TeamState.Ready ?: return
        _state.value = transform(current)
    }
}
