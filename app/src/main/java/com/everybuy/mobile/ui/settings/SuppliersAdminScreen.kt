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
import com.everybuy.mobile.ui.components.GlassAlertDialog as AlertDialog
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
import com.everybuy.mobile.data.SupplierDto

@OptIn(ExperimentalMaterial3Api::class)
@Composable fun SuppliersAdminScreen(
    onBack: () -> Unit,
    onSessionExpired: () -> Unit,
    viewModel: SuppliersAdminViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsState()
    var showForm by rememberSaveable { mutableStateOf(false) }
    var editing by remember { mutableStateOf<SupplierDto?>(null) }
    var toggleTarget by remember { mutableStateOf<SupplierDto?>(null) }
    val ready = state as? SuppliersAdminState.Ready

    LaunchedEffect(state) { if (state is SuppliersAdminState.SessionExpired) onSessionExpired() }
    LaunchedEffect(ready?.saved) { if (ready?.saved == true) { showForm = false; editing = null } }

    Scaffold(topBar = {
        TopAppBar(
            title = { Column { Text("Suppliers"); Text("Contacts used when receiving stock", style = MaterialTheme.typography.labelMedium) } },
            navigationIcon = { IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Rounded.ArrowBack, contentDescription = "Back") } },
            actions = { IconButton(onClick = { viewModel.refresh() }) { Icon(Icons.Rounded.Refresh, contentDescription = "Refresh suppliers") } },
        )
    }, floatingActionButton = {
        if (ready != null) {
            Button(onClick = { viewModel.clearFeedback(); editing = null; showForm = true }) {
                Icon(Icons.Rounded.Add, contentDescription = null, modifier = Modifier.padding(end = 6.dp))
                Text("Add supplier")
            }
        }
    }) { insets ->
        when (val current = state) {
            SuppliersAdminState.Loading -> Column(Modifier.fillMaxSize().padding(insets), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center) {
                CircularProgressIndicator()
                Text("Loading suppliers…", Modifier.padding(top = 12.dp))
            }
            SuppliersAdminState.SessionExpired -> Text("Your session has expired.", Modifier.padding(insets).padding(24.dp))
            is SuppliersAdminState.Forbidden -> Column(Modifier.fillMaxSize().padding(insets).padding(24.dp), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center) {
                Text(current.message, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
            }
            is SuppliersAdminState.Failed -> Column(Modifier.fillMaxSize().padding(insets).padding(24.dp), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center) {
                Text(current.message, color = MaterialTheme.colorScheme.error)
                TextButton(onClick = { viewModel.refresh() }) { Text("Retry") }
            }
            is SuppliersAdminState.Ready -> Box(
                Modifier.fillMaxSize().padding(insets),
                contentAlignment = Alignment.TopCenter,
            ) {
                Column(
                    Modifier
                        .fillMaxWidth()
                        .fillMaxHeight()
                        .widthIn(max = 840.dp)
                        .padding(horizontal = 16.dp),
                ) {
                if (current.notice != null) FeedbackCard(current.notice, false) { viewModel.clearFeedback() }
                if (current.actionError != null) FeedbackCard(current.actionError, true) { viewModel.clearFeedback() }
                if (current.suppliers.isEmpty()) {
                    Column(Modifier.fillMaxSize().padding(24.dp), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center) {
                        Text("No suppliers yet", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                        Text("Add the first supplier so stock receipts can name who supplied them.", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.padding(top = 6.dp))
                    }
                } else {
                    LazyColumn(Modifier.weight(1f).padding(top = 8.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        items(current.suppliers, key = { it.id }) { supplier ->
                            SupplierAdminCard(
                                supplier = supplier,
                                busy = current.busyId == supplier.id,
                                onEdit = { viewModel.clearFeedback(); editing = supplier; showForm = true },
                                onToggle = { toggleTarget = supplier },
                            )
                        }
                        item { Text("", Modifier.padding(bottom = 12.dp)) }
                    }
                }
                }
            }
        }
    }

    if (showForm && ready != null) {
        SupplierFormDialog(
            supplier = editing,
            saving = ready.saving,
            error = ready.actionError,
            onDismiss = { if (!ready.saving) { showForm = false; editing = null; viewModel.clearFeedback() } },
            onConfirm = { name, phone -> viewModel.save(editing, name, phone) },
        )
    }
    toggleTarget?.let { supplier ->
        val deactivating = supplier.active
        AlertDialog(
            onDismissRequest = { toggleTarget = null },
            title = { Text(if (deactivating) "Deactivate supplier?" else "Reactivate supplier?") },
            text = {
                Text(
                    if (deactivating) "“${supplier.name}” will be unavailable for new stock receipts. Existing stock and history will remain."
                    else "“${supplier.name}” will become available for new stock receipts again.",
                )
            },
            confirmButton = {
                Button(onClick = { toggleTarget = null; viewModel.toggle(supplier) }) { Text(if (deactivating) "Deactivate" else "Reactivate") }
            },
            dismissButton = { TextButton(onClick = { toggleTarget = null }) { Text("Cancel") } },
        )
    }
}

@Composable private fun SupplierAdminCard(supplier: SupplierDto, busy: Boolean, onEdit: () -> Unit, onToggle: () -> Unit) {
    Card(Modifier.fillMaxWidth(), shape = RoundedCornerShape(20.dp), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow)) {
        Column(Modifier.fillMaxWidth().padding(16.dp), verticalArrangement = Arrangement.spacedBy(5.dp)) {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.Top) {
                Column(Modifier.weight(1f)) {
                    Text(supplier.name, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold, color = if (supplier.active) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurfaceVariant)
                    Text(supplier.phone?.takeIf { it.isNotBlank() } ?: "No phone added", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                StatusPill(supplier.active)
            }
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.padding(top = 2.dp)) {
                TextButton(onClick = onEdit, enabled = !busy) { Text("Edit") }
                TextButton(onClick = onToggle, enabled = !busy) { Text(if (busy) "Saving…" else if (supplier.active) "Deactivate" else "Reactivate") }
            }
        }
    }
}

@Composable private fun SupplierFormDialog(supplier: SupplierDto?, saving: Boolean, error: String?, onDismiss: () -> Unit, onConfirm: (String, String) -> Unit) {
    var name by rememberSaveable { mutableStateOf(supplier?.name ?: "") }
    var phone by rememberSaveable { mutableStateOf(supplier?.phone.orEmpty()) }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(if (supplier == null) "Add supplier" else "Edit supplier") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(name, { if (it.length <= SUPPLIER_NAME_MAX) name = it }, Modifier.fillMaxWidth(), label = { Text("Supplier name") }, singleLine = true)
                OutlinedTextField(phone, { if (it.length <= SUPPLIER_PHONE_MAX) phone = it }, Modifier.fillMaxWidth(), label = { Text("Phone (optional)") }, singleLine = true)
                if (error != null) Text(error, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall)
            }
        },
        confirmButton = {
            Button(onClick = { onConfirm(name, phone) }, enabled = !saving && name.isNotBlank()) {
                if (saving) CircularProgressIndicator(Modifier.padding(2.dp)) else Text(if (supplier == null) "Add supplier" else "Save supplier")
            }
        },
        dismissButton = { TextButton(onClick = onDismiss, enabled = !saving) { Text("Cancel") } },
    )
}
