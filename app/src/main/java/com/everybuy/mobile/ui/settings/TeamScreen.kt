package com.everybuy.mobile.ui.settings

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.Refresh
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.everybuy.mobile.core.util.formatLagosDate
import com.everybuy.mobile.data.StaffInvitationDto
import com.everybuy.mobile.data.StaffMemberDto

@OptIn(ExperimentalMaterial3Api::class)
@Composable fun TeamScreen(
    currentUserId: String,
    onBack: () -> Unit,
    onSessionExpired: () -> Unit,
    viewModel: TeamViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsState()
    var showInvite by rememberSaveable { mutableStateOf(false) }
    var roleTarget by remember { mutableStateOf<StaffMemberDto?>(null) }
    var activeTarget by remember { mutableStateOf<StaffMemberDto?>(null) }
    var revokeTarget by remember { mutableStateOf<StaffInvitationDto?>(null) }

    LaunchedEffect(state) { if (state is TeamState.SessionExpired) onSessionExpired() }
    val ready = state as? TeamState.Ready
    LaunchedEffect(ready?.inviteSent) { if (ready?.inviteSent == true) showInvite = false }

    Scaffold(topBar = {
        TopAppBar(
            title = { Column { Text("Team"); Text("Roles, access and invitations", style = MaterialTheme.typography.labelMedium) } },
            navigationIcon = { IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Rounded.ArrowBack, contentDescription = "Back") } },
            actions = { IconButton(onClick = { viewModel.refresh() }) { Icon(Icons.Rounded.Refresh, contentDescription = "Refresh team") } },
        )
    }, floatingActionButton = {
        if (ready != null) {
            Button(onClick = { viewModel.clearFeedback(); showInvite = true }) {
                Icon(Icons.Rounded.Add, contentDescription = null, modifier = Modifier.padding(end = 6.dp))
                Text("Invite staff")
            }
        }
    }) { insets ->
        when (val current = state) {
            TeamState.Loading -> Column(Modifier.fillMaxSize().padding(insets), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center) {
                CircularProgressIndicator()
                Text("Loading team…", Modifier.padding(top = 12.dp))
            }
            TeamState.SessionExpired -> Text("Your session has expired.", Modifier.padding(insets).padding(24.dp))
            is TeamState.Forbidden -> Column(Modifier.fillMaxSize().padding(insets).padding(24.dp), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center) {
                Text(current.message, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                Text("Ask an owner to manage the team.", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.padding(top = 6.dp))
            }
            is TeamState.Failed -> Column(Modifier.fillMaxSize().padding(insets).padding(24.dp), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center) {
                Text(current.message, color = MaterialTheme.colorScheme.error)
                TextButton(onClick = { viewModel.refresh() }) { Text("Retry") }
            }
            is TeamState.Ready -> TeamContent(
                state = current,
                currentUserId = currentUserId,
                onInvite = { viewModel.clearFeedback(); showInvite = true },
                onRole = { roleTarget = it },
                onActive = { activeTarget = it },
                onRevoke = { revokeTarget = it },
                onDismissFeedback = { viewModel.clearFeedback() },
                modifier = Modifier.fillMaxSize().padding(insets),
            )
        }
    }

    if (showInvite && ready != null) {
        InviteDialog(
            inviting = ready.inviting,
            error = ready.actionError,
            onDismiss = { if (!ready.inviting) { showInvite = false; viewModel.clearFeedback() } },
            onConfirm = { name, email -> viewModel.invite(name, email) },
        )
    }
    roleTarget?.let { member ->
        ChoiceDialog(
            title = "Role for ${member.name}",
            message = "Owners can manage the team and settings. Staff can use the shop workflows.",
            options = listOf("Staff", "Owner"),
            current = member.role,
            onDismiss = { roleTarget = null },
            onSelect = { role -> roleTarget = null; viewModel.setRole(member, role) },
        )
    }
    activeTarget?.let { member ->
        val deactivating = member.active
        AlertDialog(
            onDismissRequest = { activeTarget = null },
            title = { Text(if (deactivating) "Deactivate ${member.name}?" else "Reactivate ${member.name}?") },
            text = { Text(if (deactivating) "They will no longer be able to use the shop. Their records are kept." else "They will be able to use the shop again.") },
            confirmButton = { Button(onClick = { activeTarget = null; viewModel.setActive(member, !member.active) }) { Text(if (deactivating) "Deactivate" else "Reactivate") } },
            dismissButton = { TextButton(onClick = { activeTarget = null }) { Text("Cancel") } },
        )
    }
    revokeTarget?.let { invitation ->
        AlertDialog(
            onDismissRequest = { revokeTarget = null },
            title = { Text("Revoke invitation?") },
            text = { Text("The invitation sent to ${invitation.email} will stop working.") },
            confirmButton = { Button(onClick = { revokeTarget = null; viewModel.revoke(invitation) }) { Text("Revoke") } },
            dismissButton = { TextButton(onClick = { revokeTarget = null }) { Text("Cancel") } },
        )
    }
}

@Composable private fun TeamContent(
    state: TeamState.Ready,
    currentUserId: String,
    onInvite: () -> Unit,
    onRole: (StaffMemberDto) -> Unit,
    onActive: (StaffMemberDto) -> Unit,
    onRevoke: (StaffInvitationDto) -> Unit,
    onDismissFeedback: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Box(modifier.fillMaxSize(), contentAlignment = Alignment.TopCenter) {
    LazyColumn(
        modifier = Modifier
            .fillMaxWidth()
            .fillMaxHeight()
            .widthIn(max = 840.dp)
            .padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        item {
            Text(
                "${state.members.count { it.active }} active of ${state.members.size} shop member${if (state.members.size == 1) "" else "s"}",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(top = 10.dp),
            )
        }
        if (state.notice != null) item { FeedbackCard(state.notice, false, onDismissFeedback) }
        if (state.actionError != null) item { FeedbackCard(state.actionError, true, onDismissFeedback) }
        if (state.members.isEmpty()) {
            item {
                SectionCard(title = "No shop members yet", subtitle = "Invite a staff member to get started.") {
                    TextButton(onClick = onInvite) { Text("Invite staff") }
                }
            }
        } else {
            items(state.members, key = { it.staffId }) { member ->
                MemberCard(
                    member = member,
                    isSelf = member.userId == currentUserId,
                    busy = state.busyStaffId == member.staffId,
                    onRole = { onRole(member) },
                    onActive = { onActive(member) },
                )
            }
        }
        item {
            Row(Modifier.fillMaxWidth().padding(top = 8.dp), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                Text("Pending invitations", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                Text("Expire after 24 hours", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
        if (state.pendingInvites.isEmpty()) {
            item { Text("No pending invitations.", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant) }
        } else {
            items(state.pendingInvites, key = { it.id }) { invitation ->
                InvitationCard(invitation, revoking = state.revokingId == invitation.id) { onRevoke(invitation) }
            }
        }
        item { Text("", modifier = Modifier.padding(bottom = 12.dp)) }
    }
    }
}

@Composable private fun MemberCard(member: StaffMemberDto, isSelf: Boolean, busy: Boolean, onRole: () -> Unit, onActive: () -> Unit) {
    Card(Modifier.fillMaxWidth(), shape = RoundedCornerShape(20.dp), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow)) {
        Column(Modifier.fillMaxWidth().padding(16.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.Top) {
                Column(Modifier.weight(1f)) {
                    Text(
                        member.name.ifBlank { member.email } + if (isSelf) " · You" else "",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold,
                    )
                    Text(member.email, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                StatusPill(member.active)
            }
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                RolePill(member.role)
                if (member.createdAt.isNotBlank()) Text("Joined ${formatLagosDate(member.createdAt)}", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            if (!isSelf) {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.padding(top = 2.dp)) {
                    TextButton(onClick = onRole, enabled = !busy) { Text(if (busy) "Saving…" else "Change role") }
                    TextButton(onClick = onActive, enabled = !busy) { Text(if (member.active) "Deactivate" else "Reactivate") }
                }
            } else {
                Text("You cannot change your own role or access here.", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
    }
}

@Composable private fun RolePill(role: String) {
    val owner = role == "Owner"
    Surface(
        color = if (owner) MaterialTheme.colorScheme.tertiaryContainer else MaterialTheme.colorScheme.surfaceVariant,
        contentColor = if (owner) MaterialTheme.colorScheme.onTertiaryContainer else MaterialTheme.colorScheme.onSurfaceVariant,
        shape = RoundedCornerShape(8.dp),
    ) {
        Text(if (owner) "Owner" else "Staff", style = MaterialTheme.typography.labelMedium, modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp))
    }
}

@Composable private fun InvitationCard(invitation: StaffInvitationDto, revoking: Boolean, onRevoke: () -> Unit) {
    Card(Modifier.fillMaxWidth(), shape = RoundedCornerShape(20.dp), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow)) {
        Row(Modifier.fillMaxWidth().padding(16.dp), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Text(invitation.name.ifBlank { invitation.email }, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
                Text(invitation.email, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                if (invitation.expiresAt.isNotBlank()) Text("Expires ${formatLagosDate(invitation.expiresAt)}", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            TextButton(onClick = onRevoke, enabled = !revoking) { Text(if (revoking) "Revoking…" else "Revoke") }
        }
    }
}

@Composable private fun ChoiceDialog(title: String, message: String, options: List<String>, current: String, onDismiss: () -> Unit, onSelect: (String) -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(message, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                options.forEach { option ->
                    Button(onClick = { onSelect(option) }, enabled = option != current, modifier = Modifier.fillMaxWidth()) {
                        Text(if (option == current) "$option (current)" else option)
                    }
                }
            }
        },
        confirmButton = { TextButton(onClick = onDismiss) { Text("Cancel") } },
    )
}

@Composable private fun InviteDialog(inviting: Boolean, error: String?, onDismiss: () -> Unit, onConfirm: (String, String) -> Unit) {
    var name by rememberSaveable { mutableStateOf("") }
    var email by rememberSaveable { mutableStateOf("") }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Invite staff") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("The backend emails a one-time link that expires after 24 hours.", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                OutlinedTextField(name, { if (it.length <= PROFILE_NAME_MAX) name = it }, Modifier.fillMaxWidth(), label = { Text("Name") }, singleLine = true)
                OutlinedTextField(email, { email = it.trim() }, Modifier.fillMaxWidth(), label = { Text("Email") }, singleLine = true)
                if (error != null) Text(error, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall)
            }
        },
        confirmButton = {
            Button(onClick = { onConfirm(name, email) }, enabled = !inviting && name.isNotBlank() && email.isNotBlank()) {
                if (inviting) CircularProgressIndicator(Modifier.padding(2.dp)) else Text("Send invite")
            }
        },
        dismissButton = { TextButton(onClick = onDismiss, enabled = !inviting) { Text("Cancel") } },
    )
}
