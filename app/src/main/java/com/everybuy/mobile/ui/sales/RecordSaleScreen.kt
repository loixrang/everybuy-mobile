package com.everybuy.mobile.ui.sales

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
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
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.everybuy.mobile.core.util.formatNaira
import com.everybuy.mobile.data.CreateSaleResultDto
import com.everybuy.mobile.data.ProductDto
import java.math.BigDecimal

private val paymentMethods = listOf("cash" to "Cash", "transfer" to "Transfer", "pos" to "POS")

@OptIn(ExperimentalMaterial3Api::class)
@Composable fun RecordSaleScreen(
    onBack: () -> Unit,
    onViewMovements: () -> Unit,
    onSessionExpired: () -> Unit,
    viewModel: RecordSaleViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsState()
    LaunchedEffect(state) { if (state is RecordSaleState.SessionExpired) onSessionExpired() }
    Scaffold(topBar = {
        TopAppBar(title = { Text("Record sale") }, navigationIcon = { IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Rounded.ArrowBack, contentDescription = "Back") } })
    }) { insets ->
        when (val current = state) {
            RecordSaleState.Loading -> Column(Modifier.fillMaxSize().padding(insets), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center) {
                CircularProgressIndicator()
                Text("Loading products and customers…", Modifier.padding(top = 12.dp))
            }
            RecordSaleState.SessionExpired -> Text("Your session has expired.", Modifier.padding(insets).padding(24.dp))
            is RecordSaleState.Failed -> Column(Modifier.fillMaxSize().padding(insets).padding(24.dp), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center) {
                Text(current.message, color = MaterialTheme.colorScheme.error)
                TextButton(onClick = { viewModel.load() }) { Text("Retry") }
            }
            is RecordSaleState.Ready -> if (current.success != null) {
                SaleSuccess(current.success, onBack, onViewMovements, { viewModel.recordAnother() }, Modifier.fillMaxSize().padding(insets))
            } else {
                RecordSaleForm(current, viewModel, onBack, Modifier.fillMaxSize().padding(insets))
            }
        }
    }
}

@Composable private fun SaleSuccess(result: CreateSaleResultDto, onBack: () -> Unit, onViewMovements: () -> Unit, onAnother: () -> Unit, modifier: Modifier = Modifier) {
    val paid = result.payment.firstOrNull()?.amount ?: "0"
    val balance = ((result.sale.itemsTotal.toBigDecimalOrNull() ?: BigDecimal.ZERO) - (paid.toBigDecimalOrNull() ?: BigDecimal.ZERO)).max(BigDecimal.ZERO)
    Box(modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
    Column(
        Modifier
            .fillMaxWidth()
            .widthIn(max = 840.dp)
            .padding(24.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Card(shape = RoundedCornerShape(24.dp), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer)) {
            Column(Modifier.padding(22.dp), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Text("Sale recorded", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
                Text(result.sale.saleNumber, style = MaterialTheme.typography.titleMedium)
                Text("Total ${formatNaira(result.sale.itemsTotal)}", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.SemiBold, modifier = Modifier.padding(top = 6.dp))
                Text("Amount paid ${formatNaira(paid)}", style = MaterialTheme.typography.bodyMedium)
                Text("Balance ${formatNaira(balance.toPlainString())}", style = MaterialTheme.typography.bodyMedium)
                if (result.replayed) Text("The server confirmed this sale from your previous retry.", style = MaterialTheme.typography.bodySmall, modifier = Modifier.padding(top = 6.dp))
            }
        }
        Row(Modifier.fillMaxWidth().padding(top = 20.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            TextButton(onClick = onBack, modifier = Modifier.weight(1f)) { Text("Done") }
            Button(onClick = onAnother, modifier = Modifier.weight(1f)) { Text("Record another") }
        }
        TextButton(onClick = onViewMovements, modifier = Modifier.padding(top = 4.dp)) { Text("View stock movements") }
    }
    }
}

@Composable private fun RecordSaleForm(state: RecordSaleState.Ready, viewModel: RecordSaleViewModel, onBack: () -> Unit, modifier: Modifier = Modifier) {
    var customerMenu by remember { mutableStateOf(false) }
    val productMap = remember(state.products) { state.products.associateBy { it.id } }
    val lines = remember(state.lines, productMap) { buildSaleLines(state.lines, productMap) }
    val total = remember(lines) { saleTotal(lines) }
    val amountPaid = state.amountPaid.trim().ifBlank { "0" }.toBigDecimalOrNull() ?: BigDecimal.ZERO
    val balance = (total - amountPaid).max(BigDecimal.ZERO)
    val nothingPaid = amountPaid.compareTo(BigDecimal.ZERO) == 0
    val searchResults = remember(state.products, state.search) {
        val query = state.search.trim()
        if (query.isBlank()) emptyList() else state.products.filter { it.active && it.stockPacks > 0 && "${it.name} ${it.size.orEmpty()}".contains(query, ignoreCase = true) }.take(8)
    }
    val selectedCustomer = state.customers.firstOrNull { it.id == state.customerId }

    Box(modifier.fillMaxSize(), contentAlignment = Alignment.TopCenter) {
    Column(
        Modifier
            .fillMaxWidth()
            .widthIn(max = 840.dp)
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Text("Sell packs or bags from the same stock balance. Prices and stock are verified by the server.", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        state.customerWarning?.let { Text(it, color = MaterialTheme.colorScheme.tertiary, style = MaterialTheme.typography.bodySmall) }
        state.error?.let { Text(it, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodyMedium) }

        OutlinedTextField(state.search, viewModel::updateSearch, Modifier.fillMaxWidth(), label = { Text("Search products to add") }, singleLine = true)
        if (searchResults.isNotEmpty()) {
            Card(shape = RoundedCornerShape(16.dp), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow)) {
                Column {
                    searchResults.forEach { product ->
                        DropdownMenuItem(
                            text = { Column { Text(product.name, fontWeight = FontWeight.Medium); Text("${product.packName} · ${product.stockPacks} packs available", style = MaterialTheme.typography.bodySmall) } },
                            onClick = { viewModel.addProduct(product.id) },
                        )
                    }
                }
            }
        } else if (state.search.isNotBlank()) {
            Text("No active product with stock matches that search.", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }

        Text("Items", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
        if (state.lines.isEmpty()) {
            Text("No items yet. Search for a product above.", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        state.lines.forEachIndexed { index, line ->
            val product = productMap[line.productId] ?: return@forEachIndexed
            SaleLineCard(index, line, product, viewModel)
        }

        Text("Customer and payment", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
        Box {
            OutlinedTextField(selectedCustomer?.name ?: "Walk-in customer", {}, Modifier.fillMaxWidth(), label = { Text("Customer") }, readOnly = true, singleLine = true, trailingIcon = { TextButton(onClick = { customerMenu = true }) { Text("Change") } })
            DropdownMenu(expanded = customerMenu, onDismissRequest = { customerMenu = false }) {
                DropdownMenuItem(text = { Text("Walk-in customer") }, onClick = { viewModel.setCustomer(""); customerMenu = false })
                state.customers.forEach { customer ->
                    DropdownMenuItem(text = { Column { Text(customer.name); Text("Balance ${formatNaira(customer.balance)}", style = MaterialTheme.typography.bodySmall) } }, onClick = { viewModel.setCustomer(customer.id); customerMenu = false })
                }
            }
        }
        if (state.customers.isEmpty()) Text("No customers available. A customer is required only when the sale has an unpaid balance.", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        OutlinedTextField(state.amountPaid, viewModel::setAmountPaid, Modifier.fillMaxWidth(), label = { Text("Amount paid (₦)") }, placeholder = { Text("0") }, singleLine = true, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal), supportingText = { Text(if (nothingPaid) "Nothing paid is recorded as a credit sale." else "Payment for this sale") })
        LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            items(paymentMethods, key = { it.first }) { (value, label) ->
                FilterChip(selected = state.method == value, onClick = { viewModel.setMethod(value) }, enabled = !nothingPaid, label = { Text(label) })
            }
        }
        OutlinedTextField(state.reference, viewModel::setReference, Modifier.fillMaxWidth(), label = { Text("Payment reference (optional)") }, singleLine = true, enabled = !nothingPaid)
        Card(shape = RoundedCornerShape(20.dp), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow)) {
            Column(Modifier.fillMaxWidth().padding(16.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) { Text("Sale total"); Text(formatNaira(total.toPlainString()), fontWeight = FontWeight.Bold) }
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) { Text("Amount paid"); Text(formatNaira(amountPaid.toPlainString())) }
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) { Text("Balance owed"); Text(formatNaira(balance.toPlainString()), color = if (balance > BigDecimal.ZERO) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurface, fontWeight = FontWeight.SemiBold) }
            }
        }
        Button(onClick = { viewModel.submit() }, modifier = Modifier.fillMaxWidth(), enabled = !state.submitting && state.lines.isNotEmpty()) {
            if (state.submitting) CircularProgressIndicator(Modifier.height(18.dp)) else Text("Record sale")
        }
        TextButton(onClick = onBack, modifier = Modifier.align(Alignment.CenterHorizontally)) { Text("Cancel") }
    }
    }
}

@Composable private fun SaleLineCard(index: Int, line: SaleLineDraft, product: ProductDto, viewModel: RecordSaleViewModel) {
    val lineTotal = (line.unitPrice.toBigDecimalOrNull() ?: BigDecimal.ZERO) * BigDecimal(line.quantity)
    val units = remember(product) { availableSaleUnits(product) }
    Card(shape = RoundedCornerShape(20.dp), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow)) {
        Column(Modifier.fillMaxWidth().padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.Top) {
                Column(Modifier.weight(1f)) {
                    Text(product.name, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                    Text("${product.stockPacks} packs available · ${formatNaira(lineTotal.toPlainString())}", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                TextButton(onClick = { viewModel.removeLine(index) }) { Text("Remove") }
            }
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                units.forEach { unit ->
                    FilterChip(selected = line.unit == unit, onClick = { viewModel.updateUnit(index, unit) }, label = { Text(unit) })
                }
            }
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(
                    value = line.quantity.toString(),
                    onValueChange = { viewModel.updateQuantity(index, it.filter(Char::isDigit)) },
                    modifier = Modifier.weight(1f),
                    label = { Text("Quantity") },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                )
                OutlinedTextField(
                    value = line.unitPrice,
                    onValueChange = { viewModel.updateUnitPrice(index, it) },
                    modifier = Modifier.weight(1.2f),
                    label = { Text("Unit price (₦)") },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    supportingText = { Text("Minimum ${formatNaira(saleUnitMinimumPrice(product, line.unit))}") },
                )
            }
        }
    }
}
