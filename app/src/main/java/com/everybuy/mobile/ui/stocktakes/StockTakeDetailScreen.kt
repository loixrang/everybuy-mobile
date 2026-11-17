package com.everybuy.mobile.ui.stocktakes

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.everybuy.mobile.core.util.formatLagosDateTime
import com.everybuy.mobile.data.StockTakeDetailDto
import com.everybuy.mobile.data.StockTakeItemDto

@OptIn(ExperimentalMaterial3Api::class)
@Composable fun StockTakeDetailScreen(
    id: String,
    isOwner: Boolean,
    currentUserId: String?,
    onBack: () -> Unit,
    onEditProduct: (String) -> Unit,
    onSessionExpired: () -> Unit,
    viewModel: StockTakeDetailViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsState()
    var pendingAction by rememberSaveable { mutableStateOf<String?>(null) }
    LaunchedEffect(id) { viewModel.load(id) }
    LaunchedEffect(state) { if (state is StockTakeDetailState.SessionExpired) onSessionExpired() }
    Scaffold(topBar = {
        TopAppBar(title = { Text("Stocktake") }, navigationIcon = { IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Rounded.ArrowBack, contentDescription = "Back") } })
    }) { insets ->
        when (val current = state) {
            StockTakeDetailState.Loading -> Column(Modifier.fillMaxSize().padding(insets), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center) {
                CircularProgressIndicator()
                Text("Loading stocktake…", Modifier.padding(top = 12.dp))
            }
            StockTakeDetailState.SessionExpired -> Text("Your session has expired.", Modifier.padding(insets).padding(24.dp))
            is StockTakeDetailState.Failed -> Column(Modifier.fillMaxSize().padding(insets).padding(24.dp), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center) {
                Text(current.message, color = MaterialTheme.colorScheme.error)
                Row(Modifier.padding(top = 12.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    TextButton(onClick = onBack) { Text("Back to stocktakes") }
                    Button(onClick = { viewModel.load(id) }) { Text("Retry") }
                }
            }
            is StockTakeDetailState.Ready -> StockTakeDetailContent(
                state = current,
                isOwner = isOwner,
                currentUserId = currentUserId,
                onCount = { viewModel.startCount(it) },
                onDismissCount = { viewModel.dismissCount() },
                onSaveCount = { itemId, bags, loose -> viewModel.recordCount(itemId, bags, loose) },
                onFinalize = { pendingAction = "finalize" },
                onCancel = { pendingAction = "cancel" },
                onEditProduct = onEditProduct,
                modifier = Modifier.fillMaxSize().padding(insets),
            )
        }
    }
    if (pendingAction != null) {
        val ready = state as? StockTakeDetailState.Ready
        val finalize = pendingAction == "finalize"
        AlertDialog(
            onDismissRequest = { if (ready?.actionBusy != true) pendingAction = null },
            title = { Text(if (finalize) "Approve stocktake" else "Cancel stocktake") },
            text = {
                Text(
                    if (finalize) "Stock balances are updated by each difference and recorded as stock adjustments. Sales or receipts made after a count are kept."
                    else "The recorded counts are kept for reference, but stock balances stay unchanged. This cannot be undone.",
                )
            },
            confirmButton = {
                Button(
                    enabled = ready?.actionBusy != true,
                    onClick = {
                        if (finalize) viewModel.finalize() else viewModel.cancel()
                        pendingAction = null
                    },
                ) {
                    if (ready?.actionBusy == true) CircularProgressIndicator(Modifier.height(18.dp)) else Text(if (finalize) "Approve stocktake" else "Cancel stocktake")
                }
            },
            dismissButton = { TextButton(onClick = { pendingAction = null }, enabled = ready?.actionBusy != true) { Text("Keep stocktake") } },
        )
    }
}

@Composable private fun StockTakeDetailContent(
    state: StockTakeDetailState.Ready,
    isOwner: Boolean,
    currentUserId: String?,
    onCount: (String) -> Unit,
    onDismissCount: () -> Unit,
    onSaveCount: (String, String, String) -> Unit,
    onFinalize: () -> Unit,
    onCancel: () -> Unit,
    onEditProduct: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    val detail = state.detail
    val draft = isDraftStockTake(detail.status)
    val progress = stockTakeProgress(detail.countedProducts, detail.totalProducts)
    val allCounted = detail.totalProducts > 0 && detail.countedProducts >= detail.totalProducts
    val unvaluedIncreases = detail.items.filter {
        val counted = it.countedPacks
        val atCount = it.stockAtCountPacks
        counted != null && atCount != null && counted > atCount && it.costPerPack == null
    }
    val countingItem = detail.items.firstOrNull { it.id == state.countingItemId }

    LazyColumn(modifier.padding(horizontal = 16.dp), verticalArrangement = Arrangement.spacedBy(12.dp), contentPadding = androidx.compose.foundation.layout.PaddingValues(vertical = 12.dp)) {
        item {
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.Top) {
                    Column(Modifier.weight(1f)) {
                        Text(if (draft) "Count stock" else detail.name, style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
                        Text(detail.reference, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        if (!draft) Text(detail.createdAt?.let { "Started ${formatLagosDateTime(it)}" } ?: detail.name, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                    StockTakeStatusBadge(detail.status)
                }
                if (detail.name.isNotBlank() && draft) {
                    Text(detail.name, style = MaterialTheme.typography.bodyLarge, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
        }

        if (draft) {
            item {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                        Text("${detail.countedProducts} of ${detail.totalProducts} products counted", style = MaterialTheme.typography.titleSmall)
                        Text("$progress%", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                    LinearProgressIndicator(progress = { progress / 100f }, modifier = Modifier.fillMaxWidth().height(8.dp))
                    Text(if (isOwner) "Approving applies each difference to stock and writes it to the movement ledger." else "An owner approves the count. Saved counts never change stock on their own.", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
        }

        state.actionError?.let { message ->
            item { ErrorCard(message) }
        }

        if (isOwner && draft && unvaluedIncreases.isNotEmpty()) {
            item {
                Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.tertiaryContainer), shape = RoundedCornerShape(20.dp)) {
                    Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        Text("Value stock before approving", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
                        Text("These counts increase stock for products with no recorded acquisition cost. Enter the known opening cost in product setup; physical counts do not set a cost.", style = MaterialTheme.typography.bodySmall)
                        unvaluedIncreases.forEach { item ->
                            Text(item.productName, modifier = Modifier.clickable { onEditProduct(item.productId) }.padding(vertical = 2.dp), style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.SemiBold, color = MaterialTheme.colorScheme.primary)
                        }
                    }
                }
            }
        }

        if (detail.status == "Finalized") {
            item {
                Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer), shape = RoundedCornerShape(20.dp)) {
                    Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Text("Stocktake approved", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                        Text(detail.finalizedAt?.let { "Stock balances were updated on ${formatLagosDateTime(it)}. Each difference is recorded as an adjustment in the movement ledger." } ?: "Stock balances were updated and the differences were recorded as adjustments.", style = MaterialTheme.typography.bodySmall)
                    }
                }
            }
        }

        if (detail.status == "Cancelled") {
            item {
                Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant), shape = RoundedCornerShape(20.dp)) {
                    Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Text("Stocktake cancelled", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                        Text("No stock balances were changed.", style = MaterialTheme.typography.bodySmall)
                    }
                }
            }
        }

        if (isOwner && draft) {
            item {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        TextButton(onClick = onCancel, enabled = !state.actionBusy, modifier = Modifier.weight(1f)) { Text("Cancel stocktake") }
                        Button(
                            onClick = onFinalize,
                            enabled = !state.actionBusy && allCounted && unvaluedIncreases.isEmpty(),
                            modifier = Modifier.weight(1f),
                        ) { if (state.actionBusy) CircularProgressIndicator(Modifier.height(18.dp)) else Text("Approve stocktake") }
                    }
                    if (!allCounted) Text("Every product must be counted before approval.", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    if (unvaluedIncreases.isNotEmpty()) Text("Record the known acquisition cost for each stock increase before approval.", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
        }

        if (detail.items.isEmpty()) {
            item {
                Column(Modifier.fillMaxWidth().padding(24.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("No products in this stocktake", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                    Text("The stocktake was created when no active products existed. Add a product, then start a new stocktake.", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.padding(top = 6.dp))
                }
            }
        } else {
            items(detail.items, key = { it.id }) { item ->
                StockTakeItemCard(item, draft, currentUserId, onCount)
            }
        }
    }

    if (countingItem != null) {
        CountStockDialog(
            item = countingItem,
            saving = state.countSaving,
            error = state.countError,
            onDismiss = onDismissCount,
            onSave = { bags, loose -> onSaveCount(countingItem.id, bags, loose) },
        )
    }
}

@Composable private fun StockTakeItemCard(item: StockTakeItemDto, draft: Boolean, currentUserId: String?, onCount: (String) -> Unit) {
    val difference = differenceFromExpected(item)
    val differenceColor = when {
        difference == null -> MaterialTheme.colorScheme.onSurfaceVariant
        difference < 0 -> MaterialTheme.colorScheme.error
        difference > 0 -> MaterialTheme.colorScheme.tertiary
        else -> MaterialTheme.colorScheme.onSurfaceVariant
    }
    Card(shape = RoundedCornerShape(20.dp), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow)) {
        Column(Modifier.fillMaxWidth().padding(16.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.Top) {
                Column(Modifier.weight(1f)) {
                    Text(item.productName, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                    Text(listOfNotNull(item.packName.ifBlank { null }, item.size?.takeIf { it.isNotBlank() }, "${item.packsPerBag} packs per bag").joinToString(" · "), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                if (difference != null) Text(formatPackDifference(difference), style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.SemiBold, color = differenceColor)
            }
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(20.dp)) {
                Column { Text("Expected", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant); Text("${item.expectedPacks} packs", style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Medium) }
                Column { Text("Counted", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant); Text(item.countedPacks?.let { "$it packs" } ?: "Not counted", style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Medium) }
            }
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                Text(
                    item.countedAt?.let { "${countedByLabel(item.countedBy, currentUserId)} · ${formatLagosDateTime(it)}" } ?: "Not counted yet",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.weight(1f),
                )
                if (draft) TextButton(onClick = { onCount(item.id) }) { Text(if (item.countedPacks == null) "Count" else "Recount") }
            }
        }
    }
}

@Composable private fun CountStockDialog(item: StockTakeItemDto, saving: Boolean, error: String?, onDismiss: () -> Unit, onSave: (String, String) -> Unit) {
    val packsPerBag = item.packsPerBag.coerceAtLeast(1)
    val initial = item.countedPacks ?: 0
    var bags by remember(item.id) { mutableStateOf((initial / packsPerBag).toString()) }
    var loose by remember(item.id) { mutableStateOf((initial % packsPerBag).toString()) }
    var localError by remember(item.id) { mutableStateOf<String?>(null) }
    val preview = validateStockTakeCount(bags, loose, item.packsPerBag)
    AlertDialog(
        onDismissRequest = { if (!saving) onDismiss() },
        title = { Text("Count ${item.productName}") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text("Expected at start: ${item.expectedPacks} packs. ${item.packsPerBag} packs per bag.", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = bags,
                        onValueChange = { value -> bags = value.filter(Char::isDigit); localError = null },
                        modifier = Modifier.weight(1f),
                        label = { Text("Bags") },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    )
                    OutlinedTextField(
                        value = loose,
                        onValueChange = { value -> loose = value.filter(Char::isDigit); localError = null },
                        modifier = Modifier.weight(1f),
                        label = { Text("Loose packs") },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    )
                }
                Text(
                    preview.countedPacks?.let { "Physical count: $it packs" } ?: "Enter a whole number of bags and loose packs.",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold,
                )
                val message = localError ?: error ?: preview.message
                if (message != null) Text(message, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall)
            }
        },
        confirmButton = {
            Button(
                enabled = !saving,
                onClick = {
                    val message = validateStockTakeCount(bags, loose, item.packsPerBag).message
                    if (message != null) localError = message else { localError = null; onSave(bags, loose) }
                },
            ) { if (saving) CircularProgressIndicator(Modifier.height(18.dp)) else Text(if (item.countedPacks == null) "Save count" else "Update count") }
        },
        dismissButton = { TextButton(onClick = onDismiss, enabled = !saving) { Text("Cancel") } },
    )
}

@Composable private fun ErrorCard(message: String) {
    Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer), shape = RoundedCornerShape(16.dp)) {
        Text(message, Modifier.padding(14.dp), color = MaterialTheme.colorScheme.onErrorContainer, style = MaterialTheme.typography.bodySmall)
    }
}
