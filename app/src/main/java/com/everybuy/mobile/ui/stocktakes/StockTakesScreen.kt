package com.everybuy.mobile.ui.stocktakes

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
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
import com.everybuy.mobile.core.util.formatLagosDateTime
import com.everybuy.mobile.data.StockTakeDto

@OptIn(ExperimentalMaterial3Api::class)
@Composable fun StockTakesScreen(
    onOpenStockTake: (String) -> Unit,
    onSessionExpired: () -> Unit,
    viewModel: StockTakesViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsState()
    var createOpen by rememberSaveable { mutableStateOf(false) }
    LaunchedEffect(state) { if (state is StockTakesState.SessionExpired) onSessionExpired() }
    LaunchedEffect((state as? StockTakesState.Ready)?.createdId) {
        val createdId = (state as? StockTakesState.Ready)?.createdId
        if (createdId != null) {
            createOpen = false
            viewModel.clearCreated()
            onOpenStockTake(createdId)
        }
    }
    Scaffold(topBar = {
        TopAppBar(
            title = { Column { Text("Stocktakes"); Text("Count physical stock and review differences", style = MaterialTheme.typography.labelMedium) } },
            actions = { IconButton(onClick = { viewModel.refresh() }) { Icon(Icons.Rounded.Refresh, contentDescription = "Refresh stocktakes") } },
        )
    }, floatingActionButton = {
        Button(onClick = { createOpen = true }) {
            Icon(Icons.Rounded.Add, contentDescription = null, modifier = Modifier.padding(end = 6.dp))
            Text("Start stocktake")
        }
    }) { insets ->
        when (val current = state) {
            StockTakesState.Loading -> Column(Modifier.fillMaxSize().padding(insets), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center) {
                CircularProgressIndicator()
                Text("Loading stocktakes…", Modifier.padding(top = 12.dp))
            }
            StockTakesState.SessionExpired -> Text("Your session has expired.", Modifier.padding(insets).padding(24.dp))
            is StockTakesState.Failed -> Column(Modifier.fillMaxSize().padding(insets).padding(24.dp), verticalArrangement = Arrangement.Center, horizontalAlignment = Alignment.CenterHorizontally) {
                Text(current.message, color = MaterialTheme.colorScheme.error)
                TextButton(onClick = { viewModel.refresh() }) { Text("Retry") }
            }
            is StockTakesState.Ready -> StockTakeList(current, onOpenStockTake, { viewModel.load(it) }, { createOpen = true }, Modifier.fillMaxSize().padding(insets))
        }
    }
    if (createOpen) {
        val current = state as? StockTakesState.Ready
        CreateStockTakeDialog(
            creating = current?.creating == true,
            error = current?.createError,
            onDismiss = { if (current?.creating != true) { createOpen = false; viewModel.clearCreateError() } },
            onNameChanged = { viewModel.clearCreateError() },
            onCreate = { viewModel.create(it) },
        )
    }
}

@Composable private fun StockTakeList(state: StockTakesState.Ready, onOpen: (String) -> Unit, onPage: (Int) -> Unit, onStart: () -> Unit, modifier: Modifier = Modifier) {
    val totalPages = maxOf(1, (state.total + STOCK_TAKE_PAGE_SIZE - 1) / STOCK_TAKE_PAGE_SIZE)
    Column(modifier.padding(horizontal = 16.dp)) {
        if (state.stockTakes.isEmpty()) {
            Column(Modifier.fillMaxSize().padding(24.dp), verticalArrangement = Arrangement.Center, horizontalAlignment = Alignment.CenterHorizontally) {
                Text("No stocktakes yet", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                Text(
                    "Start a stocktake to record what is physically on the shelf. Every active product is included with its current stock.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(top = 8.dp, bottom = 16.dp),
                )
                Button(onClick = onStart) { Text("Start stocktake") }
            }
            return
        }
        LazyColumn(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(10.dp), contentPadding = androidx.compose.foundation.layout.PaddingValues(vertical = 12.dp)) {
            items(state.stockTakes, key = { it.id }) { stockTake ->
                StockTakeCard(stockTake, { onOpen(stockTake.id) })
            }
        }
        Row(Modifier.fillMaxWidth().padding(vertical = 12.dp), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
            TextButton(onClick = { onPage(state.page - 1) }, enabled = state.page > 1) { Text("Previous") }
            Text("Page ${state.page} of $totalPages · ${state.total} total", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            TextButton(onClick = { onPage(state.page + 1) }, enabled = state.page < totalPages) { Text("Next") }
        }
    }
}

@Composable private fun StockTakeCard(stockTake: StockTakeDto, onOpen: () -> Unit) {
    Card(Modifier.fillMaxWidth().clickable(onClick = onOpen), shape = RoundedCornerShape(22.dp), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow)) {
        Column(Modifier.fillMaxWidth().padding(16.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.Top) {
                Column(Modifier.weight(1f)) {
                    Text(stockTake.name, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                    Text(stockTake.reference, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                StockTakeStatusBadge(stockTake.status, Modifier.padding(start = 8.dp))
            }
            Text(
                stockTake.createdAt?.let { "Started ${formatLagosDateTime(it)}" } ?: "Start date unavailable",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@Composable private fun CreateStockTakeDialog(
    creating: Boolean,
    error: String?,
    onDismiss: () -> Unit,
    onNameChanged: () -> Unit,
    onCreate: (String) -> Unit,
) {
    var name by rememberSaveable { mutableStateOf("") }
    var localError by remember { mutableStateOf<String?>(null) }
    AlertDialog(
        onDismissRequest = { if (!creating) onDismiss() },
        title = { Text("Start stocktake") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text("Every active product is added with its current stock. Counts do not change stock until an owner approves them.", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                OutlinedTextField(
                    value = name,
                    onValueChange = { value -> name = value.take(MAX_STOCK_TAKE_NAME_LENGTH); localError = null; onNameChanged() },
                    modifier = Modifier.fillMaxWidth(),
                    label = { Text("Stocktake name") },
                    placeholder = { Text("October 2026 stock count") },
                    singleLine = true,
                    isError = localError != null || error != null,
                    supportingText = { Text(localError ?: error ?: "${name.length}/$MAX_STOCK_TAKE_NAME_LENGTH") },
                )
            }
        },
        confirmButton = {
            Button(
                enabled = !creating,
                onClick = {
                    val message = validateStockTakeName(name)
                    if (message != null) localError = message else { localError = null; onCreate(name) }
                },
            ) { if (creating) CircularProgressIndicator(Modifier.padding(horizontal = 12.dp), strokeWidth = 2.dp) else Text("Start stocktake") }
        },
        dismissButton = { TextButton(onClick = onDismiss, enabled = !creating) { Text("Cancel") } },
    )
}
