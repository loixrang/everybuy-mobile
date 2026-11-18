package com.everybuy.mobile.ui.stock

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
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import com.everybuy.mobile.core.util.formatNaira
import com.everybuy.mobile.data.ProductDto
import com.everybuy.mobile.data.SupplierDto
import com.everybuy.mobile.ui.components.SheetSelect
import com.everybuy.mobile.ui.components.SheetSelectOption
import java.math.BigDecimal

@OptIn(ExperimentalMaterial3Api::class)
@Composable fun ReceiveStockScreen(initialProductId: String?, onBack: () -> Unit, onSessionExpired: () -> Unit, viewModel: ReceiveStockViewModel = hiltViewModel()) {
    val state by viewModel.state.collectAsState()
    LaunchedEffect(initialProductId) { viewModel.load(initialProductId) }
    LaunchedEffect(state) { if (state is ReceiveStockState.SessionExpired) onSessionExpired() }
    Scaffold(topBar = { TopAppBar(title = { Text("Receive stock") }, navigationIcon = { IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Rounded.ArrowBack, contentDescription = "Back") } }) }) { insets ->
        when (val current = state) {
            ReceiveStockState.Loading -> Column(Modifier.fillMaxSize().padding(insets), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center) { ExpressiveLoadingIndicator(); Text("Loading products…", Modifier.padding(top = 12.dp)) }
            ReceiveStockState.SessionExpired -> Text("Your session has expired.", Modifier.padding(insets).padding(24.dp))
            is ReceiveStockState.Failed -> Column(Modifier.fillMaxSize().padding(insets).padding(24.dp), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center) {
                Text(current.message, color = MaterialTheme.colorScheme.error)
                TextButton(onClick = { viewModel.load(initialProductId) }) { Text("Retry") }
            }
            is ReceiveStockState.Success -> ReceiptSuccess(current, onBack) { viewModel.receiveAnother() }
            is ReceiveStockState.Ready -> ReceiveForm(current, viewModel, Modifier.fillMaxSize().padding(insets))
        }
    }
}

@Composable private fun ReceiveForm(state: ReceiveStockState.Ready, viewModel: ReceiveStockViewModel, modifier: Modifier) {
    val draft = state.draft
    val product = state.products.firstOrNull { it.id == draft.productId }
    val bags = draft.bags.toIntOrNull()?.coerceAtLeast(0) ?: 0
    val loose = draft.loosePacks.toIntOrNull()?.coerceAtLeast(0) ?: 0
    val totalPacks = product?.let { (bags.toLong() * it.packsPerBag + loose).coerceAtMost(Int.MAX_VALUE.toLong()).toInt() } ?: 0
    val costPreview = draft.unitCost.trim().toBigDecimalOrNull()?.multiply(BigDecimal(totalPacks))?.toPlainString()
    Box(modifier.fillMaxSize(), contentAlignment = Alignment.TopCenter) {
    Column(
        Modifier
            .fillMaxWidth()
            .widthIn(max = 840.dp)
            .verticalScroll(rememberScrollState())
            .padding(20.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        Text("Record a delivery and update the shop's pack balance.", style = MaterialTheme.typography.bodyLarge, color = MaterialTheme.colorScheme.onSurfaceVariant)
        state.error?.let { Text(it, color = MaterialTheme.colorScheme.error) }
        state.suppliersWarning?.let { Text(it, color = MaterialTheme.colorScheme.tertiary) }
        SheetSelect(
            label = "Product",
            placeholder = "Choose product",
            options = state.products.map { item ->
                SheetSelectOption(item.id, item.id, "${item.name} · ${item.packName}")
            },
            selected = draft.productId.takeIf(String::isNotBlank),
            onSelect = { productId ->
                if (productId != null) {
                    viewModel.updateDraft { it.copy(productId = productId) }
                }
            },
            searchable = state.products.size >= 8,
            error = state.errors["product"],
        )
        product?.let {
            Text("${it.packsPerBag} packs per bag · current stock ${it.stockPacks} packs", style = MaterialTheme.typography.bodyMedium)
        }
        SheetSelect(
            label = "Supplier (optional)",
            placeholder = "No supplier",
            options = listOf(SheetSelectOption<Int>("none", null, "No supplier")) +
                state.suppliers.map { supplier -> SheetSelectOption(supplier.id, supplier.id, supplier.name) },
            selected = draft.supplierId,
            onSelect = { supplierId -> viewModel.updateDraft { it.copy(supplierId = supplierId) } },
            searchable = state.suppliers.size + 1 >= 8,
        )
        ReceiveQuantityField("Number of bags", draft.bags, state.errors["bags"]) { value -> viewModel.updateDraft { it.copy(bags = value) } }
        ReceiveQuantityField("Loose packs", draft.loosePacks, state.errors["loosePacks"]) { value -> viewModel.updateDraft { it.copy(loosePacks = value) } }
        Text("Total received: $totalPacks packs", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
        state.errors["quantity"]?.let { Text(it, color = MaterialTheme.colorScheme.error) }
        OutlinedTextField(draft.unitCost, { value -> viewModel.updateDraft { it.copy(unitCost = value) } }, Modifier.fillMaxWidth(), label = { Text("Cost per pack (₦)") }, singleLine = true, isError = state.errors.containsKey("unitCost"), supportingText = state.errors["unitCost"]?.let { message -> { Text(message) } }, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal))
        if (costPreview != null && totalPacks > 0) Text("Receipt cost: ${formatNaira(costPreview)}", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        state.errors["unitCost"]?.let { Text(it, color = MaterialTheme.colorScheme.error) }
        Button(onClick = { viewModel.submit() }, modifier = Modifier.fillMaxWidth(), enabled = !state.saving && product != null) {
            if (state.saving) ExpressiveLoadingIndicator(Modifier.height(18.dp)) else Text("Save receipt")
        }
    }
    }
}

@Composable private fun ReceiveQuantityField(label: String, value: String, error: String?, onValue: (String) -> Unit) {
    OutlinedTextField(value, onValue, Modifier.fillMaxWidth(), label = { Text(label) }, singleLine = true, isError = error != null, supportingText = if (error == null) null else { { Text(error) } }, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number))
}

@Composable private fun ReceiptSuccess(state: ReceiveStockState.Success, onBack: () -> Unit, onAgain: () -> Unit) {
    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
    Column(
        Modifier
            .fillMaxWidth()
            .widthIn(max = 840.dp)
            .padding(24.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer)) {
            Column(Modifier.padding(24.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                Text("Stock received", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.SemiBold)
                Text(state.productName, style = MaterialTheme.typography.titleLarge, modifier = Modifier.padding(top = 12.dp))
                Text("+${state.totalPacks} packs", style = MaterialTheme.typography.titleMedium, modifier = Modifier.padding(top = 8.dp))
                if (state.newStockPacks != null) {
                    Text("New balance: ${state.newStockPacks} packs", style = MaterialTheme.typography.bodyLarge, modifier = Modifier.padding(top = 4.dp))
                } else {
                    Text("The receipt is recorded. The updated balance is unavailable.", style = MaterialTheme.typography.bodyLarge, modifier = Modifier.padding(top = 4.dp))
                }
                if (state.replayed) Text("The server confirmed this receipt from your previous retry.", style = MaterialTheme.typography.bodySmall, modifier = Modifier.padding(top = 10.dp))
            }
        }
        Row(Modifier.fillMaxWidth().padding(top = 20.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            TextButton(onClick = onBack, modifier = Modifier.weight(1f)) { Text("Done") }
            Button(onClick = onAgain, modifier = Modifier.weight(1f)) { Text("Receive more") }
        }
    }
    }
}
