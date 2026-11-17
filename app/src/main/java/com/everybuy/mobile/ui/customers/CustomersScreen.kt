package com.everybuy.mobile.ui.customers

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
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.everybuy.mobile.core.util.formatNaira
import com.everybuy.mobile.data.CustomerListItemDto
import java.math.BigDecimal

private val statusFilters = listOf("All customers" to STATUS_ALL, "Active" to STATUS_ACTIVE, "Inactive" to STATUS_INACTIVE)

@OptIn(ExperimentalMaterial3Api::class)
@Composable fun CustomersScreen(
    onOpenCustomer: (String) -> Unit,
    onAddCustomer: () -> Unit,
    onSessionExpired: () -> Unit,
    viewModel: CustomersViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsState()
    LaunchedEffect(state) { if (state is CustomersState.SessionExpired) onSessionExpired() }
    Scaffold(topBar = {
        TopAppBar(
            title = { Column { Text("Customers"); Text("Statements, credit and repayments", style = MaterialTheme.typography.labelMedium) } },
            actions = { IconButton(onClick = { viewModel.refresh() }) { Icon(Icons.Rounded.Refresh, contentDescription = "Refresh customers") } },
        )
    }, floatingActionButton = {
        Button(onClick = onAddCustomer) {
            Icon(Icons.Rounded.Add, contentDescription = null, modifier = Modifier.padding(end = 6.dp))
            Text("Add customer")
        }
    }) { insets ->
        when (val current = state) {
            CustomersState.Loading -> Column(Modifier.fillMaxSize().padding(insets), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center) {
                CircularProgressIndicator()
                Text("Loading customers…", Modifier.padding(top = 12.dp))
            }
            CustomersState.SessionExpired -> Text("Your session has expired.", Modifier.padding(insets).padding(24.dp))
            is CustomersState.Failed -> Column(Modifier.fillMaxSize().padding(insets).padding(24.dp), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center) {
                Text(current.message, color = MaterialTheme.colorScheme.error)
                TextButton(onClick = { viewModel.refresh() }) { Text("Retry") }
            }
            is CustomersState.Ready -> CustomersContent(current, viewModel, onOpenCustomer, Modifier.fillMaxSize().padding(insets))
        }
    }
}

@Composable private fun CustomersContent(
    state: CustomersState.Ready,
    viewModel: CustomersViewModel,
    onOpenCustomer: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    val filtered = state.filtered
    Column(modifier.padding(horizontal = 16.dp)) {
        SummaryRow(state.activeCount, state.customers.size, state.totalOutstanding)
        OutlinedTextField(
            state.query,
            viewModel::setQuery,
            Modifier.fillMaxWidth().padding(top = 10.dp),
            label = { Text("Search name or phone") },
            singleLine = true,
        )
        LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.padding(vertical = 8.dp)) {
            items(statusFilters, key = { it.second }) { (label, value) ->
                FilterChip(selected = state.status == value, onClick = { viewModel.setStatus(value) }, label = { Text(label) })
            }
            if (state.query.isNotBlank() || state.status != STATUS_ALL) {
                item { TextButton(onClick = { viewModel.clearFilters() }) { Text("Clear filters") } }
            }
        }
        if (filtered.isEmpty()) {
            Column(Modifier.fillMaxSize().padding(24.dp), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center) {
                Text(if (state.customers.isEmpty()) "No customers yet" else "No matching customers", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                Text(
                    if (state.customers.isEmpty()) "Add a customer to record credit sales and later repayments." else "Try a different name or phone number, or clear the status filter.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(top = 6.dp),
                )
            }
        } else {
            LazyColumn(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                items(filtered, key = { it.id }) { customer ->
                    CustomerRowCard(customer) { onOpenCustomer(customer.id) }
                }
            }
            Text(
                "${filtered.size} customer${if (filtered.size == 1) "" else "s"} shown · Total outstanding ${formatNaira(state.totalOutstanding.toPlainString())}",
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(vertical = 10.dp),
            )
        }
    }
}

@Composable private fun SummaryRow(activeCount: Int, totalCount: Int, totalOutstanding: BigDecimal) {
    Row(Modifier.fillMaxWidth().padding(top = 8.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        SummaryCard("Active", "$activeCount of $totalCount", Modifier.weight(1f))
        SummaryCard("Owed by customers", formatNaira(totalOutstanding.toPlainString()), Modifier.weight(1.4f))
    }
}

@Composable private fun SummaryCard(label: String, value: String, modifier: Modifier = Modifier) {
    Card(modifier, shape = RoundedCornerShape(20.dp), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow)) {
        Column(Modifier.fillMaxWidth().padding(14.dp)) {
            Text(label, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Text(value, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, modifier = Modifier.padding(top = 2.dp))
        }
    }
}

@Composable private fun CustomerRowCard(customer: CustomerListItemDto, onOpen: () -> Unit) {
    Card(Modifier.fillMaxWidth().clickable(onClick = onOpen), shape = RoundedCornerShape(20.dp), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow)) {
        Column(Modifier.fillMaxWidth().padding(16.dp), verticalArrangement = Arrangement.spacedBy(5.dp)) {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.Top) {
                Column(Modifier.weight(1f)) {
                    Text(customer.name, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                    Text(customer.phone?.takeIf { it.isNotBlank() } ?: "No phone number", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                CustomerStatusBadge(customer.active)
            }
            Row(Modifier.fillMaxWidth().padding(top = 2.dp), horizontalArrangement = Arrangement.SpaceBetween) {
                val balance = customer.balance.toBigDecimalOrNull() ?: BigDecimal.ZERO
                Text(
                    if (balance > BigDecimal.ZERO) "Owes ${formatNaira(customer.balance)}" else "No balance",
                    style = MaterialTheme.typography.bodyMedium,
                    color = if (balance > BigDecimal.ZERO) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurfaceVariant,
                    fontWeight = if (balance > BigDecimal.ZERO) FontWeight.SemiBold else FontWeight.Normal,
                )
                Text("Statement", style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.primary)
            }
        }
    }
}

@Composable fun CustomerStatusBadge(active: Boolean) {
    val container = if (active) MaterialTheme.colorScheme.secondaryContainer else MaterialTheme.colorScheme.surfaceVariant
    val content = if (active) MaterialTheme.colorScheme.onSecondaryContainer else MaterialTheme.colorScheme.onSurfaceVariant
    Surface(color = container, contentColor = content, shape = RoundedCornerShape(8.dp)) {
        Text(if (active) "Active" else "Inactive", style = MaterialTheme.typography.labelMedium, modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp))
    }
}
