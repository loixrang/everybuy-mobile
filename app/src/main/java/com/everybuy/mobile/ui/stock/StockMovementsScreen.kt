package com.everybuy.mobile.ui.stock

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
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
import com.everybuy.mobile.data.StockMovementDto

private val movementTypes = listOf("All" to "ALL", "Opening stock" to "Opening", "Receipts" to "Receipt", "Sales" to "Sale", "Adjustments" to "Adjustment")

@OptIn(ExperimentalMaterial3Api::class)
@Composable fun StockMovementsScreen(onSessionExpired: () -> Unit, onProductSelected: (String) -> Unit, viewModel: StockMovementsViewModel = hiltViewModel()) {
    val state by viewModel.state.collectAsState()
    var query by rememberSaveable { mutableStateOf("") }
    var type by rememberSaveable { mutableStateOf("ALL") }
    var productFilter by rememberSaveable { mutableStateOf("ALL") }
    var productMenu by remember { mutableStateOf(false) }
    LaunchedEffect(state) { if (state is StockMovementsState.SessionExpired) onSessionExpired() }
    Scaffold(topBar = { TopAppBar(title = { Text("Stock movements") }) }) { insets ->
        when (val current = state) {
            StockMovementsState.Loading -> Column(Modifier.fillMaxSize().padding(insets), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center) { CircularProgressIndicator(); Text("Loading stock ledger…", Modifier.padding(top = 12.dp)) }
            StockMovementsState.SessionExpired -> Text("Your session has expired.", Modifier.padding(insets).padding(24.dp))
            is StockMovementsState.Failed -> Column(Modifier.fillMaxSize().padding(insets).padding(24.dp), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center) { Text(current.message, color = MaterialTheme.colorScheme.error); TextButton(onClick = { viewModel.refresh() }) { Text("Retry") } }
            is StockMovementsState.Ready -> {
                val products = current.products.associateBy { it.id }
                val filtered = current.movements.filter { movement ->
                    (type == "ALL" || movement.type == type) && (productFilter == "ALL" || movement.productId == productFilter) &&
                        "${products[movement.productId]?.name.orEmpty()} ${products[movement.productId]?.size.orEmpty()} ${movement.type} ${movement.note.orEmpty()} ${movement.requestId.orEmpty()} ${movement.saleItemId.orEmpty()}".contains(query.trim(), ignoreCase = true)
                }
                Column(Modifier.fillMaxSize().padding(insets).padding(horizontal = 16.dp)) {
                    OutlinedTextField(query, { query = it }, Modifier.fillMaxWidth().padding(top = 8.dp), label = { Text("Search movements") }, singleLine = true)
                    LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.padding(vertical = 8.dp)) {
                        items(movementTypes, key = { it.second }) { (label, value) -> FilterChip(selected = type == value, onClick = { type = value }, label = { Text(label) }) }
                    }
                    androidx.compose.material3.Box {
                        TextButton(onClick = { productMenu = true }) { Text(products[productFilter]?.name ?: "All products") }
                        DropdownMenu(expanded = productMenu, onDismissRequest = { productMenu = false }) {
                            DropdownMenuItem(text = { Text("All products") }, onClick = { productFilter = "ALL"; productMenu = false })
                            current.products.forEach { product -> DropdownMenuItem(text = { Text(product.name) }, onClick = { productFilter = product.id; productMenu = false }) }
                        }
                    }
                    Text("${filtered.size} movements loaded", style = MaterialTheme.typography.labelLarge, modifier = Modifier.padding(vertical = 8.dp))
                    if (filtered.isEmpty()) Text(if (query.isBlank() && type == "ALL" && productFilter == "ALL") "No stock movements yet." else "No movements match these filters.", modifier = Modifier.padding(16.dp), style = MaterialTheme.typography.bodyLarge)
                    else LazyColumn(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        items(filtered, key = { it.id }) { movement -> MovementCard(movement, products[movement.productId]?.name ?: "Unknown product", { onProductSelected(movement.productId) }) }
                        if (current.movements.size < current.total) item {
                            Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.fillMaxWidth().padding(vertical = 12.dp)) {
                                current.moreError?.let { Text(it, color = MaterialTheme.colorScheme.error) }
                                Button(onClick = { viewModel.loadMore() }, enabled = !current.loadingMore) { if (current.loadingMore) CircularProgressIndicator(Modifier.height(16.dp)) else Text("Load more") }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable private fun MovementCard(item: StockMovementDto, productName: String, onProduct: () -> Unit) {
    val positive = item.changePacks > 0
    val title = when (item.type) { "Sale" -> "Shop sale"; "Receipt" -> "Receipt"; "Opening" -> "Opening stock"; "Adjustment" -> if (item.note?.contains("stocktake", ignoreCase = true) == true) "Stocktake" else "Adjustment"; else -> item.type }
    Card(Modifier.fillMaxWidth(), shape = RoundedCornerShape(22.dp), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow)) {
        Column(Modifier.fillMaxWidth().padding(16.dp), verticalArrangement = Arrangement.spacedBy(5.dp)) {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                Text(formatLagosDateTime(item.createdAt), style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Text("${if (positive) "+" else ""}${item.changePacks} packs", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = if (positive) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface)
            }
            TextButton(onClick = onProduct, modifier = Modifier.align(Alignment.Start)) { Text(productName) }
            Text(title, style = MaterialTheme.typography.bodyMedium)
            val ref = item.note?.takeIf { it.startsWith("Ref:") || it.startsWith("ST-") || it.startsWith("RC-") || it.startsWith("SL-") }?.removePrefix("Ref: ")
                ?: if (item.type == "Sale" && item.saleItemId != null) "SL-${item.saleItemId.take(6).uppercase()}" else item.requestId?.let { "RC-${it.take(6).uppercase()}" }
            if (!ref.isNullOrBlank()) Text(ref, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}
