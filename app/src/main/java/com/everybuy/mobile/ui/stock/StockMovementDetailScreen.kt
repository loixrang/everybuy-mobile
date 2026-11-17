package com.everybuy.mobile.ui.stock

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.everybuy.mobile.core.util.formatLagosDateTime
import com.everybuy.mobile.core.util.formatNaira
import com.everybuy.mobile.data.StockMovementDto

@OptIn(ExperimentalMaterial3Api::class)
@Composable fun StockMovementDetailScreen(
    id: String,
    onBack: () -> Unit,
    onProductSelected: (String) -> Unit,
    onSessionExpired: () -> Unit,
    viewModel: StockMovementDetailViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsState()
    LaunchedEffect(id) { viewModel.load(id) }
    LaunchedEffect(state) { if (state is StockMovementDetailState.SessionExpired) onSessionExpired() }
    Scaffold(topBar = {
        TopAppBar(title = { Text("Movement details") }, navigationIcon = { IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Rounded.ArrowBack, contentDescription = "Back") } })
    }) { insets ->
        when (val current = state) {
            StockMovementDetailState.Loading -> Column(Modifier.fillMaxSize().padding(insets), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center) {
                CircularProgressIndicator()
                Text("Loading movement…", Modifier.padding(top = 12.dp))
            }
            StockMovementDetailState.SessionExpired -> Text("Your session has expired.", Modifier.padding(insets).padding(24.dp))
            StockMovementDetailState.NotFound -> Column(Modifier.fillMaxSize().padding(insets).padding(24.dp), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center) {
                Text("Movement not found", style = MaterialTheme.typography.titleLarge)
                TextButton(onClick = onBack) { Text("Back to movements") }
            }
            is StockMovementDetailState.Failed -> Column(Modifier.fillMaxSize().padding(insets).padding(24.dp), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center) {
                Text(current.message, color = MaterialTheme.colorScheme.error)
                TextButton(onClick = { viewModel.load(id) }) { Text("Retry") }
            }
            is StockMovementDetailState.Ready -> MovementDetailContent(current.movement, current.product?.name, { onProductSelected(current.movement.productId) }, Modifier.fillMaxSize().padding(insets))
        }
    }
}

@Composable private fun MovementDetailContent(movement: StockMovementDto, productName: String?, onProduct: () -> Unit, modifier: Modifier = Modifier) {
    val positive = movement.changePacks > 0
    Column(modifier.verticalScroll(rememberScrollState()).padding(20.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
        Card(shape = RoundedCornerShape(24.dp), colors = CardDefaults.cardColors(containerColor = if (positive) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceContainerHigh)) {
            Column(Modifier.fillMaxWidth().padding(20.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(movementLabel(movement), style = MaterialTheme.typography.titleMedium)
                Text("${if (positive) "+" else ""}${movement.changePacks} packs", style = MaterialTheme.typography.displaySmall, fontWeight = FontWeight.Bold)
                Text(formatLagosDateTime(movement.createdAt), style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
        Card(shape = RoundedCornerShape(20.dp), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow)) {
            Column(Modifier.fillMaxWidth().padding(18.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text("Ledger entry", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                DetailRow("Product", productName ?: "Unknown product")
                if (productName != null) TextButton(onClick = onProduct, contentPadding = androidx.compose.foundation.layout.PaddingValues(0.dp)) { Text("Open product") }
                DetailRow("Movement type", movement.type)
                DetailRow("Change", "${if (positive) "+" else ""}${movement.changePacks} packs")
                movement.costPerPack?.let { DetailRow("Cost per pack", formatNaira(it)) }
                movement.note?.takeIf { it.isNotBlank() }?.let { DetailRow("Note", it) }
                movement.requestId?.let { DetailRow("Request ID", it) }
                movement.saleItemId?.let { DetailRow("Sale item", it) }
                movement.supplierId?.let { DetailRow("Supplier ID", it.toString()) }
                DetailRow("Recorded by", movement.createdBy.ifBlank { "Unknown" })
                DetailRow("Recorded at", formatLagosDateTime(movement.createdAt))
            }
        }
        Text("Stock balances come from the movement ledger. Staff cannot edit ledger entries directly.", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

private fun movementLabel(movement: StockMovementDto): String = when (movement.type) {
    "Sale" -> "Shop sale"
    "Receipt" -> "Stock receipt"
    "Opening" -> "Opening stock"
    "Adjustment" -> if (movement.note?.contains("stocktake", ignoreCase = true) == true) "Stocktake reconciliation" else "Stock adjustment"
    else -> movement.type
}

@Composable private fun DetailRow(label: String, value: String) {
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.Top) {
        Text(label, color = MaterialTheme.colorScheme.onSurfaceVariant, style = MaterialTheme.typography.bodyMedium, modifier = Modifier.weight(1f))
        Text(value, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Medium, modifier = Modifier.weight(1.4f), textAlign = androidx.compose.ui.text.style.TextAlign.End)
    }
}
