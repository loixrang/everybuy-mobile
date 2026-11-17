package com.everybuy.mobile.ui.sales

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.Refresh
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
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
import com.everybuy.mobile.core.util.formatLagosDateTime
import com.everybuy.mobile.core.util.formatNaira
import com.everybuy.mobile.data.SalesListRowDto
import com.everybuy.mobile.data.SalesSummaryDto

private val statusFilters = listOf("All" to null, "Paid" to "PAID", "Part paid" to "PARTIAL", "Unpaid" to "UNPAID")

@OptIn(ExperimentalMaterial3Api::class)
@Composable fun SalesScreen(
    onOpenSale: (String) -> Unit,
    onRecordSale: () -> Unit,
    onSessionExpired: () -> Unit,
    viewModel: SalesViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsState()
    var query by rememberSaveable { mutableStateOf("") }
    var status by rememberSaveable { mutableStateOf<String?>(null) }
    var from by rememberSaveable { mutableStateOf("") }
    var to by rememberSaveable { mutableStateOf("") }
    var dateError by remember { mutableStateOf<String?>(null) }
    LaunchedEffect(state) { if (state is SalesState.SessionExpired) onSessionExpired() }
    Scaffold(topBar = {
        TopAppBar(title = { Column { Text("Sales"); Text("Shop sales, payments and balances", style = MaterialTheme.typography.labelMedium) } }, actions = {
            IconButton(onClick = { viewModel.refresh() }) { Icon(Icons.Rounded.Refresh, contentDescription = "Refresh sales") }
        })
    }, floatingActionButton = {
        Button(onClick = onRecordSale) {
            Icon(Icons.Rounded.Add, contentDescription = null, modifier = Modifier.padding(end = 6.dp))
            Text("Record sale")
        }
    }) { insets ->
        when (val current = state) {
            SalesState.Loading -> Column(Modifier.fillMaxSize().padding(insets), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center) {
                CircularProgressIndicator()
                Text("Loading sales…", Modifier.padding(top = 12.dp))
            }
            SalesState.SessionExpired -> Text("Your session has expired.", Modifier.padding(insets).padding(24.dp))
            is SalesState.Failed -> Column(Modifier.fillMaxSize().padding(insets).padding(24.dp), verticalArrangement = Arrangement.Center, horizontalAlignment = Alignment.CenterHorizontally) {
                Text(current.message, color = MaterialTheme.colorScheme.error)
                TextButton(onClick = { viewModel.refresh() }) { Text("Retry") }
            }
            is SalesState.Ready -> Column(Modifier.fillMaxSize().padding(insets).padding(horizontal = 16.dp)) {
                SummaryCards(current.summary, current.summaryError)
                OutlinedTextField(query, { query = it }, Modifier.fillMaxWidth().padding(top = 8.dp), label = { Text("Search sale number or customer") }, singleLine = true)
                LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.padding(vertical = 8.dp)) {
                    items(statusFilters, key = { it.first }) { (label, value) ->
                        FilterChip(selected = status == value, onClick = { status = value }, label = { Text(label) })
                    }
                }
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                    OutlinedTextField(from, { from = it }, Modifier.weight(1f), label = { Text("From") }, placeholder = { Text("YYYY-MM-DD") }, singleLine = true, isError = dateError != null)
                    OutlinedTextField(to, { to = it }, Modifier.weight(1f), label = { Text("To") }, placeholder = { Text("YYYY-MM-DD") }, singleLine = true, isError = dateError != null)
                }
                dateError?.let { Text(it, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall) }
                Row(Modifier.fillMaxWidth().padding(vertical = 4.dp), horizontalArrangement = Arrangement.End, verticalAlignment = Alignment.CenterVertically) {
                    TextButton(onClick = {
                        query = ""; status = null; from = ""; to = ""; dateError = null
                        viewModel.applyFilters(SalesFilters())
                    }) { Text("Clear") }
                    Button(onClick = {
                        dateError = validateDateFilters(from, to)
                        if (dateError == null) viewModel.applyFilters(SalesFilters(query = query.trim(), paymentStatus = status, from = from.trim().ifBlank { null }, to = to.trim().ifBlank { null }))
                    }) { Text("Apply filters") }
                }
                Text("${current.total} sale${if (current.total == 1) "" else "s"} found", style = MaterialTheme.typography.labelLarge, modifier = Modifier.padding(vertical = 6.dp))
                if (current.rows.isEmpty()) {
                    Column(Modifier.fillMaxSize().padding(24.dp), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center) {
                        Text("No sales found", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                        Text("Record a sale or change the filters.", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.padding(top = 6.dp))
                    }
                } else {
                    LazyColumn(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        items(current.rows, key = { it.id }) { row -> SaleRowCard(row, { onOpenSale(row.id) }) }
                    }
                    Row(Modifier.fillMaxWidth().padding(vertical = 10.dp), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                        TextButton(onClick = { viewModel.loadPage(current.page - 1) }, enabled = current.page > 1) { Text("Previous") }
                        Text("Page ${current.page} of ${current.totalPages}", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        TextButton(onClick = { viewModel.loadPage(current.page + 1) }, enabled = current.page < current.totalPages) { Text("Next") }
                    }
                }
            }
        }
    }
}

private fun validateDateFilters(from: String, to: String): String? {
    val pattern = Regex("""^\d{4}-\d{2}-\d{2}$""")
    if (from.isNotBlank() && !pattern.matches(from.trim())) return "Use YYYY-MM-DD for the start date."
    if (to.isNotBlank() && !pattern.matches(to.trim())) return "Use YYYY-MM-DD for the end date."
    if (from.isNotBlank() && to.isNotBlank() && from.trim() > to.trim()) return "The end date must be on or after the start date."
    return null
}

@Composable private fun SummaryCards(summary: SalesSummaryDto?, error: String?) {
    if (summary == null) {
        Text(error ?: "Sales totals are unavailable.", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.padding(top = 8.dp))
        return
    }
    Column(verticalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.padding(top = 8.dp)) {
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            SummaryCard("Sales", formatNaira(summary.salesTotal), Modifier.weight(1f))
            SummaryCard("Received", formatNaira(summary.amountPaid), Modifier.weight(1f))
        }
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            SummaryCard("Outstanding", formatNaira(summary.outstanding), Modifier.weight(1f))
            SummaryCard("Count", summary.salesCount.toString(), Modifier.weight(1f))
        }
    }
}

@Composable private fun SummaryCard(label: String, value: String, modifier: Modifier = Modifier) {
    Card(modifier, shape = RoundedCornerShape(20.dp), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow)) {
        Column(Modifier.fillMaxWidth().padding(14.dp)) {
            Text(label, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Text(value, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold, modifier = Modifier.padding(top = 2.dp))
        }
    }
}

@Composable private fun SaleRowCard(row: SalesListRowDto, onOpen: () -> Unit) {
    Card(Modifier.fillMaxWidth().clickable(onClick = onOpen), shape = RoundedCornerShape(20.dp), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow)) {
        Column(Modifier.fillMaxWidth().padding(16.dp), verticalArrangement = Arrangement.spacedBy(5.dp)) {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.Top) {
                Column(Modifier.weight(1f)) {
                    Text(row.reference.ifBlank { "Sale" }, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                    Text(row.customer?.name ?: "Walk-in customer", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                PaymentStatusBadge(row.status)
            }
            Text(formatLagosDateTime(row.createdAt), style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text("Total ${formatNaira(row.itemsTotal)}", style = MaterialTheme.typography.bodyMedium)
                Text("Balance ${formatNaira(row.balance)}", style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Medium)
            }
        }
    }
}

@Composable fun PaymentStatusBadge(status: String) {
    val normalised = normalisedPaymentStatus(status)
    val container = when (normalised) {
        "PAID" -> MaterialTheme.colorScheme.primaryContainer
        "PART_PAID" -> MaterialTheme.colorScheme.tertiaryContainer
        else -> MaterialTheme.colorScheme.errorContainer
    }
    val content = when (normalised) {
        "PAID" -> MaterialTheme.colorScheme.onPrimaryContainer
        "PART_PAID" -> MaterialTheme.colorScheme.onTertiaryContainer
        else -> MaterialTheme.colorScheme.onErrorContainer
    }
    Surface(color = container, contentColor = content, shape = RoundedCornerShape(8.dp)) {
        Text(paymentStatusLabel(normalised), style = MaterialTheme.typography.labelMedium, modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp))
    }
}
