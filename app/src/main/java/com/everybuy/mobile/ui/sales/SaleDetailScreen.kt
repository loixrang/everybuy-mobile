package com.everybuy.mobile.ui.sales

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
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
import com.everybuy.mobile.data.CustomerDto
import com.everybuy.mobile.data.PaymentDto
import com.everybuy.mobile.data.SaleDetailDto
import com.everybuy.mobile.data.SaleItemDto

@OptIn(ExperimentalMaterial3Api::class)
@Composable fun SaleDetailScreen(
    id: String,
    onBack: () -> Unit,
    onCustomerSelected: (String) -> Unit = {},
    onSessionExpired: () -> Unit,
    viewModel: SaleDetailViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsState()
    LaunchedEffect(id) { viewModel.load(id) }
    LaunchedEffect(state) { if (state is SaleDetailState.SessionExpired) onSessionExpired() }
    Scaffold(topBar = {
        TopAppBar(title = { Text("Sale details") }, navigationIcon = { IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Rounded.ArrowBack, contentDescription = "Back") } })
    }) { insets ->
        when (val current = state) {
            SaleDetailState.Loading -> Column(Modifier.fillMaxSize().padding(insets), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center) {
                CircularProgressIndicator()
                Text("Loading sale…", Modifier.padding(top = 12.dp))
            }
            SaleDetailState.SessionExpired -> Text("Your session has expired.", Modifier.padding(insets).padding(24.dp))
            SaleDetailState.NotFound -> Column(Modifier.fillMaxSize().padding(insets).padding(24.dp), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center) {
                Text("Sale not found", style = MaterialTheme.typography.titleLarge)
                TextButton(onClick = onBack) { Text("Back to sales") }
            }
            is SaleDetailState.Failed -> Column(Modifier.fillMaxSize().padding(insets).padding(24.dp), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center) {
                Text(current.message, color = MaterialTheme.colorScheme.error)
                TextButton(onClick = { viewModel.load(id) }) { Text("Retry") }
            }
            is SaleDetailState.Ready -> SaleDetailContent(current.sale, current.customer, onCustomerSelected, Modifier.fillMaxSize().padding(insets))
        }
    }
}

@Composable private fun SaleDetailContent(sale: SaleDetailDto, customer: CustomerDto?, onCustomerSelected: (String) -> Unit, modifier: Modifier = Modifier) {
    val paid = salePaidTotal(sale)
    val balance = saleBalance(sale)
    Box(modifier.fillMaxSize(), contentAlignment = Alignment.TopCenter) {
    Column(
        Modifier
            .fillMaxWidth()
            .widthIn(max = 840.dp)
            .verticalScroll(rememberScrollState())
            .padding(18.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.Top) {
            Column(Modifier.weight(1f)) {
                Text(sale.saleNumber.ifBlank { "Sale" }, style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
                Text(customer?.name ?: if (sale.customerId == null) "Walk-in customer" else "Customer #${sale.customerId.take(8)}", style = MaterialTheme.typography.bodyLarge, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Text(formatLagosDateTime(sale.soldAt), style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            PaymentStatusBadge(saleStatus(sale))
        }
        sale.customerId?.let { customerId ->
            TextButton(onClick = { onCustomerSelected(customerId) }) { Text("View customer statement") }
        }
        Card(shape = RoundedCornerShape(22.dp), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer)) {
            Column(Modifier.fillMaxWidth().padding(18.dp), verticalArrangement = Arrangement.spacedBy(5.dp)) {
                Text("Sale total", style = MaterialTheme.typography.titleSmall)
                Text(formatNaira(sale.itemsTotal), style = MaterialTheme.typography.displaySmall, fontWeight = FontWeight.Bold)
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) { Text("Paid"); Text(formatNaira(paid.toPlainString())) }
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) { Text("Balance"); Text(formatNaira(balance.toPlainString()), fontWeight = FontWeight.SemiBold) }
            }
        }
        Text("Items", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
        sale.items.forEach { item -> SaleItemCard(item) }
        Text("Payments", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
        if (sale.payments.isEmpty()) {
            Text("No payment recorded. The full balance is outstanding.", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        sale.payments.forEach { payment -> PaymentCard(payment) }
        if (sale.items.any { it.costPerPack == null }) {
            Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.tertiaryContainer), shape = RoundedCornerShape(16.dp)) {
                Text("One or more sale lines have no cost snapshot. An owner can repair missing costs from the web workspace.", Modifier.padding(14.dp), style = MaterialTheme.typography.bodySmall)
            }
        }
        Text("Sale ${sale.saleNumber} · Recorded ${formatLagosDateTime(sale.soldAt)}", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
    }
}

@Composable private fun SaleItemCard(item: SaleItemDto) {
    Card(shape = RoundedCornerShape(18.dp), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow)) {
        Row(Modifier.fillMaxWidth().padding(14.dp), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.Top) {
            Column(Modifier.weight(1f)) {
                Text(item.productName, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
                Text("${item.quantity} ${item.unit}${if (item.quantity == 1) "" else "s"} · ${formatNaira(item.unitPrice)} each", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            Text(formatNaira(item.lineTotal), style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
        }
    }
}

@Composable private fun PaymentCard(payment: PaymentDto) {
    Card(shape = RoundedCornerShape(18.dp), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow)) {
        Column(Modifier.fillMaxWidth().padding(14.dp), verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text(paymentMethodLabel(payment.method), style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
                Text(formatNaira(payment.amount), style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
            }
            Text(formatLagosDateTime(payment.paidAt), style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            payment.reference?.takeIf { it.isNotBlank() }?.let { Text(it, style = MaterialTheme.typography.bodySmall) }
        }
    }
}
