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
import androidx.compose.material3.PrimaryTabRow
import androidx.compose.material3.Tab
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

@OptIn(ExperimentalMaterial3Api::class)
@Composable fun CatalogScreen(
    onBack: () -> Unit,
    onSessionExpired: () -> Unit,
    viewModel: CatalogViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsState()
    var kind by rememberSaveable { mutableStateOf(CatalogKind.CATEGORIES) }
    var showAdd by rememberSaveable { mutableStateOf(false) }
    var toggleTarget by remember { mutableStateOf<Pair<CatalogKind, String>?>(null) }
    val ready = state as? CatalogState.Ready

    LaunchedEffect(state) { if (state is CatalogState.SessionExpired) onSessionExpired() }
    LaunchedEffect(ready?.created) { if (ready?.created == true) showAdd = false }

    val records = when (kind) {
        CatalogKind.CATEGORIES -> ready?.categories?.map { Triple(it.id, it.name, it.active) } ?: emptyList()
        CatalogKind.BRANDS -> ready?.brands?.map { Triple(it.id, it.name, it.active) } ?: emptyList()
    }

    Scaffold(topBar = {
        TopAppBar(
            title = { Column { Text("Categories & brands"); Text("Organise products used in your inventory", style = MaterialTheme.typography.labelMedium) } },
            navigationIcon = { IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Rounded.ArrowBack, contentDescription = "Back") } },
            actions = { IconButton(onClick = { viewModel.refresh() }) { Icon(Icons.Rounded.Refresh, contentDescription = "Refresh catalog") } },
        )
    }, floatingActionButton = {
        if (ready != null) {
            Button(onClick = { viewModel.clearFeedback(); showAdd = true }) {
                Icon(Icons.Rounded.Add, contentDescription = null, modifier = Modifier.padding(end = 6.dp))
                Text(if (kind == CatalogKind.CATEGORIES) "Add category" else "Add brand")
            }
        }
    }) { insets ->
        when (val current = state) {
            CatalogState.Loading -> Column(Modifier.fillMaxSize().padding(insets), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center) {
                CircularProgressIndicator()
                Text("Loading categories and brands…", Modifier.padding(top = 12.dp))
            }
            CatalogState.SessionExpired -> Text("Your session has expired.", Modifier.padding(insets).padding(24.dp))
            is CatalogState.Forbidden -> Column(Modifier.fillMaxSize().padding(insets).padding(24.dp), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center) {
                Text(current.message, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            }
            is CatalogState.Failed -> Column(Modifier.fillMaxSize().padding(insets).padding(24.dp), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center) {
                Text(current.message, color = MaterialTheme.colorScheme.error)
                TextButton(onClick = { viewModel.refresh() }) { Text("Retry") }
            }
            is CatalogState.Ready -> Box(
                Modifier.fillMaxSize().padding(insets),
                contentAlignment = Alignment.TopCenter,
            ) {
                Column(
                    Modifier
                        .fillMaxWidth()
                        .fillMaxHeight()
                        .widthIn(max = 840.dp),
                ) {
                PrimaryTabRow(selectedTabIndex = kind.ordinal, divider = {}) {
                    CatalogKind.entries.forEach { option ->
                        Tab(selected = kind == option, onClick = { kind = option; viewModel.clearFeedback() }, text = { Text(option.name.lowercase().replaceFirstChar { it.uppercase() }) })
                    }
                }
                Column(Modifier.weight(1f).padding(horizontal = 16.dp)) {
                    if (current.notice != null) FeedbackCard(current.notice, false) { viewModel.clearFeedback() }
                    if (current.actionError != null) FeedbackCard(current.actionError, true) { viewModel.clearFeedback() }
                    if (records.isEmpty()) {
                        Column(Modifier.fillMaxSize().padding(24.dp), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center) {
                            Text("No ${kind.label}s yet", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                            Text("Add the first ${kind.label} using the button below.", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.padding(top = 6.dp))
                        }
                    } else {
                        LazyColumn(Modifier.weight(1f).fillMaxWidth().padding(top = 8.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                            items(records, key = { it.first }) { record ->
                                CatalogCard(
                                    name = record.second,
                                    active = record.third,
                                    busy = current.busyId == record.first,
                                    onToggle = { toggleTarget = kind to record.first },
                                )
                            }
                            item { Text("", Modifier.padding(bottom = 12.dp)) }
                        }
                    }
                }
                }
            }
        }
    }

    if (showAdd && ready != null) {
        AddCatalogDialog(
            noun = kind.label,
            busy = ready.busy,
            error = ready.actionError,
            onDismiss = { if (!ready.busy) { showAdd = false; viewModel.clearFeedback() } },
            onConfirm = { name -> viewModel.add(kind, name) },
        )
    }
    toggleTarget?.let { (targetKind, id) ->
        val record = when (targetKind) {
            CatalogKind.CATEGORIES -> ready?.categories?.firstOrNull { it.id == id }?.let { it.name to it.active }
            CatalogKind.BRANDS -> ready?.brands?.firstOrNull { it.id == id }?.let { it.name to it.active }
        }
        if (record != null) {
            val (recordName, recordActive) = record
            val deactivating = recordActive
            AlertDialog(
                onDismissRequest = { toggleTarget = null },
                title = { Text(if (deactivating) "Deactivate ${targetKind.label}?" else "Reactivate ${targetKind.label}?") },
                text = {
                    Text(
                        if (deactivating) "“$recordName” will remain in your records and existing product history will be kept."
                        else "“$recordName” will be available again for product setup.",
                    )
                },
                confirmButton = {
                    Button(onClick = { toggleTarget = null; viewModel.toggle(targetKind, id, !recordActive) }) {
                        Text(if (deactivating) "Deactivate" else "Reactivate")
                    }
                },
                dismissButton = { TextButton(onClick = { toggleTarget = null }) { Text("Cancel") } },
            )
        }
    }
}

@Composable private fun CatalogCard(name: String, active: Boolean, busy: Boolean, onToggle: () -> Unit) {
    Card(Modifier.fillMaxWidth(), shape = RoundedCornerShape(20.dp), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow)) {
        Row(Modifier.fillMaxWidth().padding(16.dp), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Text(name, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold, color = if (active) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurfaceVariant)
                Row(Modifier.padding(top = 4.dp)) { StatusPill(active) }
            }
            TextButton(onClick = onToggle, enabled = !busy) { Text(if (busy) "Saving…" else if (active) "Deactivate" else "Reactivate") }
        }
    }
}

@Composable private fun AddCatalogDialog(noun: String, busy: Boolean, error: String?, onDismiss: () -> Unit, onConfirm: (String) -> Unit) {
    var name by rememberSaveable { mutableStateOf("") }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Add ${noun.lowercase()}") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(
                    name,
                    { if (it.length <= CATALOG_NAME_MAX) name = it },
                    Modifier.fillMaxWidth(),
                    label = { Text("${noun.replaceFirstChar { it.uppercase() }} name") },
                    singleLine = true,
                    placeholder = { Text(if (noun == "category") "e.g. Baby care" else "e.g. Molfix") },
                )
                if (error != null) Text(error, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall)
            }
        },
        confirmButton = {
            Button(onClick = { onConfirm(name) }, enabled = !busy && name.isNotBlank()) {
                if (busy) CircularProgressIndicator(Modifier.padding(2.dp)) else Text("Add ${noun.lowercase()}")
            }
        },
        dismissButton = { TextButton(onClick = onDismiss, enabled = !busy) { Text("Cancel") } },
    )
}
