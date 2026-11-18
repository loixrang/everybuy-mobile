package com.everybuy.mobile.ui.inventory

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import com.everybuy.mobile.ui.components.ExpressiveLoadingIndicator
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
import com.everybuy.mobile.data.ProductDto
import com.everybuy.mobile.data.StockMovementDto

@OptIn(ExperimentalMaterial3Api::class)
@Composable fun ProductDetailScreen(id: String, isOwner: Boolean, onBack: () -> Unit, onEdit: () -> Unit, onReceiveStock: () -> Unit, onViewMovements: () -> Unit, onSessionExpired: () -> Unit, viewModel: ProductDetailViewModel = hiltViewModel()) {
    val state by viewModel.state.collectAsState()
    LaunchedEffect(id) { viewModel.load(id) }
    LaunchedEffect(state) { if (state is ProductDetailState.SessionExpired) onSessionExpired() }
    Scaffold(topBar = { TopAppBar(title = { Text("Product details") }, navigationIcon = { IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Rounded.ArrowBack, contentDescription = "Back") } }, actions = { if (isOwner) TextButton(onClick = onEdit) { Text("Edit") } }) }) { insets ->
        when (val current = state) {
            ProductDetailState.Loading -> Column(Modifier.fillMaxSize().padding(insets), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center) { ExpressiveLoadingIndicator(); Text("Loading product…", Modifier.padding(top = 12.dp)) }
            ProductDetailState.NotFound -> Column(Modifier.fillMaxSize().padding(insets).padding(24.dp), verticalArrangement = Arrangement.Center, horizontalAlignment = Alignment.CenterHorizontally) { Text("Product not found", style = MaterialTheme.typography.titleLarge); TextButton(onClick = onBack) { Text("Back to inventory") } }
            ProductDetailState.SessionExpired -> Text("Your session has expired.", Modifier.padding(insets).padding(24.dp))
            is ProductDetailState.Failed -> Column(Modifier.fillMaxSize().padding(insets).padding(24.dp), verticalArrangement = Arrangement.Center, horizontalAlignment = Alignment.CenterHorizontally) {
                Text(current.message, color = MaterialTheme.colorScheme.error)
                TextButton(onClick = { viewModel.load(id) }) { Text("Retry") }
            }
            is ProductDetailState.Ready -> ProductDetailsContent(current.details.product, current.details.categoryName, current.details.brandName, current.recentMovements, current.movementsUnavailable, onReceiveStock, onViewMovements, Modifier.fillMaxSize().padding(insets))
        }
    }
}

@Composable private fun ProductDetailsContent(product: ProductDto, category: String?, brand: String?, recentMovements: List<StockMovementDto>, movementsUnavailable: Boolean, onReceiveStock: () -> Unit, onViewMovements: () -> Unit, modifier: Modifier = Modifier) {
    Box(modifier.fillMaxSize(), contentAlignment = Alignment.TopCenter) {
    Column(
        Modifier
            .fillMaxWidth()
            .widthIn(max = 840.dp)
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 20.dp, vertical = 16.dp),
    ) {
        Text(product.name, style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.SemiBold)
        Text(listOfNotNull(brand, category, product.packName, product.size?.takeIf { it.isNotBlank() }).joinToString(" · "), style = MaterialTheme.typography.bodyLarge, color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.padding(top = 6.dp))
        Spacer(Modifier.height(18.dp))
        Card(Modifier.fillMaxWidth(), shape = RoundedCornerShape(28.dp), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer)) {
            Column(Modifier.padding(20.dp)) {
                Text("Available stock", style = MaterialTheme.typography.titleMedium)
                Text("${product.stockPacks} packs", style = MaterialTheme.typography.displaySmall, fontWeight = FontWeight.SemiBold, modifier = Modifier.padding(top = 4.dp))
                val bags = if (product.packsPerBag > 0) product.stockPacks / product.packsPerBag else 0
                val loose = if (product.packsPerBag > 0) product.stockPacks % product.packsPerBag else product.stockPacks
                Text("$bags ${if (bags == 1) "bag" else "bags"} + $loose ${if (loose == 1) "pack" else "packs"}", style = MaterialTheme.typography.bodyMedium)
                Text("${product.packsPerBag} packs per bag", style = MaterialTheme.typography.labelLarge, modifier = Modifier.padding(top = 4.dp))
            }
        }
        Spacer(Modifier.height(16.dp))
        PriceRow("Pack", product.retailPrice, product.minRetailPrice)
        Spacer(Modifier.height(10.dp))
        PriceRow("Bag", product.bagPrice, product.minBagPrice)
        if (product.halfBagPrice != null && product.minHalfBagPrice != null) {
            Spacer(Modifier.height(10.dp))
            PriceRow("Half bag", product.halfBagPrice, product.minHalfBagPrice)
        }
        Spacer(Modifier.height(16.dp))
        Card(Modifier.fillMaxWidth(), shape = RoundedCornerShape(24.dp), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow)) {
            Column(Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("Product setup", style = MaterialTheme.typography.titleMedium)
                DetailLine("Pieces per pack", product.piecesPerPack?.toString() ?: "Not set")
                DetailLine("Status", if (product.active) "Active" else "Inactive")
            }
        }
        if (product.active) {
            Spacer(Modifier.height(18.dp))
            Button(onClick = onReceiveStock, modifier = Modifier.fillMaxWidth()) { Text("Receive stock") }
        }
        Spacer(Modifier.height(20.dp))
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
            Text("Recent stock movements", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
            TextButton(onClick = onViewMovements) { Text("View all") }
        }
        when {
            recentMovements.isEmpty() && !movementsUnavailable -> Text(
                "No stock movements recorded for this product yet.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(vertical = 6.dp),
            )
            recentMovements.isEmpty() -> Text(
                "Recent movements could not be loaded. Open the history tab to retry.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.error,
                modifier = Modifier.padding(vertical = 6.dp),
            )
            else -> Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                recentMovements.forEach { movement -> ProductMovementRow(movement) }
            }
        }
        Spacer(Modifier.height(16.dp))
    }
    }
}

@Composable private fun ProductMovementRow(movement: StockMovementDto) {
    val positive = movement.changePacks > 0
    val title = when (movement.type) {
        "Sale" -> "Shop sale"
        "Receipt" -> "Receipt"
        "Opening" -> "Opening stock"
        "Adjustment" -> if (movement.note?.contains("stocktake", ignoreCase = true) == true) "Stocktake" else "Adjustment"
        else -> movement.type
    }
    Card(Modifier.fillMaxWidth(), shape = RoundedCornerShape(18.dp), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow)) {
        Row(Modifier.fillMaxWidth().padding(14.dp), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Text(title, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.SemiBold)
                Text(formatLagosDateTime(movement.createdAt), style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                movement.note?.takeIf { it.isNotBlank() }?.let { Text(it, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 2) }
            }
            Text(
                "${if (positive) "+" else ""}${movement.changePacks} packs",
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.SemiBold,
                color = if (positive) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface,
            )
        }
    }
}

@Composable private fun PriceRow(label: String, price: String, minimum: String) {
    Card(Modifier.fillMaxWidth(), shape = RoundedCornerShape(22.dp), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow)) {
        Row(Modifier.fillMaxWidth().padding(18.dp), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
            Column { Text("$label price", style = MaterialTheme.typography.titleMedium); Text("Minimum ${formatNaira(minimum)}", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant) }
            Text(formatNaira(price), style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.SemiBold)
        }
    }
}

@Composable private fun DetailLine(label: String, value: String) {
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) { Text(label, color = MaterialTheme.colorScheme.onSurfaceVariant); Text(value, fontWeight = FontWeight.Medium) }
}
