package com.everybuy.mobile.ui.reports

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
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
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.everybuy.mobile.core.util.formatLagosDateTime
import com.everybuy.mobile.core.util.formatNaira
import com.everybuy.mobile.data.AuthUser
import com.everybuy.mobile.data.OverviewRecentSale
import com.everybuy.mobile.ui.auth.AppTheme

@OptIn(ExperimentalMaterial3Api::class)
@Composable fun OverviewScreen(user: AuthUser, theme: AppTheme, onSignOut: () -> Unit, onSessionExpired: () -> Unit, setTheme: (AppTheme) -> Unit, viewModel: OverviewViewModel = hiltViewModel()) {
    val state by viewModel.state.collectAsState()
    var themeMenu by remember { mutableStateOf(false) }
    LaunchedEffect(state) { if ((state as? OverviewState.Failed)?.sessionExpired == true) onSessionExpired() }
    Scaffold(topBar = {
        TopAppBar(title = { Column { Text("Overview"); Text(user.name.ifBlank { user.email }, style = MaterialTheme.typography.labelMedium) } }, actions = {
            androidx.compose.material3.Box {
                TextButton(onClick = { themeMenu = true }) { Text("${theme.name.lowercase().replaceFirstChar { it.uppercase() }} theme") }
                DropdownMenu(expanded = themeMenu, onDismissRequest = { themeMenu = false }) {
                    AppTheme.entries.forEach { option -> DropdownMenuItem(text = { Text(option.name.lowercase().replaceFirstChar { it.uppercase() }) }, onClick = { themeMenu = false; setTheme(option) }) }
                }
            }
            TextButton(onClick = onSignOut) { Text("Sign out") }
        })
    }) { inset ->
        when (val current = state) {
            OverviewState.Loading -> Column(Modifier.fillMaxSize().padding(inset), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center) { CircularProgressIndicator(); Text("Loading today’s overview…", Modifier.padding(top = 12.dp)) }
            is OverviewState.Failed -> Column(Modifier.fillMaxSize().padding(inset).padding(24.dp), verticalArrangement = Arrangement.Center, horizontalAlignment = Alignment.CenterHorizontally) {
                Text(if (current.forbidden) "Reports unavailable" else "Couldn’t load overview", style = MaterialTheme.typography.titleLarge)
                Text(current.message, Modifier.padding(top = 8.dp), color = MaterialTheme.colorScheme.onSurfaceVariant)
                if (!current.forbidden) TextButton(onClick = { viewModel.refresh() }) { Text("Retry") }
            }
            is OverviewState.Ready -> LazyColumn(Modifier.fillMaxSize().padding(inset).padding(horizontal = 16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                item { Text("Today at a glance", style = MaterialTheme.typography.headlineSmall, modifier = Modifier.padding(top = 8.dp)) }
                item {
                    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                            SummaryCard("Sales today", formatNaira(current.report.salesToday.total), "${current.report.salesToday.count} sales", Modifier.weight(1f))
                            SummaryCard("Payments received", formatNaira(current.report.paymentsReceived), "Today", Modifier.weight(1f))
                        }
                        SummaryCard("Customer balances", formatNaira(current.report.customerBalances), "Outstanding", Modifier.fillMaxWidth())
                    }
                }
                item { Text("Low stock", style = MaterialTheme.typography.titleLarge, modifier = Modifier.padding(top = 8.dp)) }
                if (current.report.lowStock.isEmpty()) item { Text("No products are below the backend’s low-stock threshold.", color = MaterialTheme.colorScheme.onSurfaceVariant) }
                else items(current.report.lowStock, key = { it.productId }) { item ->
                    CompactCard(item.name, "${item.packsAvailable} packs available")
                }
                item { Text("Recent sales", style = MaterialTheme.typography.titleLarge, modifier = Modifier.padding(top = 8.dp)) }
                if (current.report.recentSales.isEmpty()) item { Text("No sales recorded today.", color = MaterialTheme.colorScheme.onSurfaceVariant) }
                else items(current.report.recentSales, key = { it.id }) { sale -> SaleCard(sale) }
                item { Spacer(Modifier.height(8.dp)) }
            }
        }
    }
}

@Composable private fun SummaryCard(title: String, value: String, supporting: String, modifier: Modifier = Modifier) {
    Card(modifier, shape = RoundedCornerShape(26.dp), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.secondaryContainer)) {
        Column(Modifier.fillMaxWidth().padding(16.dp)) {
            Text(title, style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.onSecondaryContainer)
            Text(value, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold, modifier = Modifier.padding(top = 8.dp))
            Text(supporting, style = MaterialTheme.typography.bodySmall, modifier = Modifier.padding(top = 4.dp))
        }
    }
}

@Composable private fun CompactCard(title: String, supporting: String) {
    Card(Modifier.fillMaxWidth(), shape = RoundedCornerShape(22.dp), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow)) {
        Row(Modifier.fillMaxWidth().padding(16.dp), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
            Text(title, style = MaterialTheme.typography.titleMedium, modifier = Modifier.weight(1f))
            Text(supporting, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

@Composable private fun SaleCard(sale: OverviewRecentSale) {
    Card(Modifier.fillMaxWidth(), shape = RoundedCornerShape(22.dp), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow)) {
        Row(Modifier.fillMaxWidth().padding(16.dp), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Text(sale.reference, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                Text(sale.customer?.name ?: "Walk-in customer", style = MaterialTheme.typography.bodyMedium)
                Text(formatLagosDateTime(sale.soldAt), style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            Column(horizontalAlignment = Alignment.End) {
                Text(formatNaira(sale.itemsTotal), style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                if (sale.balance != "0" && sale.balance != "0.00") Text("Due ${formatNaira(sale.balance)}", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.error)
            }
        }
    }
}
