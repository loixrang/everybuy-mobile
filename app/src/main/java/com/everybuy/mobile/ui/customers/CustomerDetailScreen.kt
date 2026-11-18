package com.everybuy.mobile.ui.customers

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.Edit
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.RadioButton
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
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.everybuy.mobile.core.util.formatLagosDate
import com.everybuy.mobile.core.util.formatLagosDateTime
import com.everybuy.mobile.core.util.formatNaira
import com.everybuy.mobile.data.CustomerStatementDto
import com.everybuy.mobile.data.PaymentDto
import com.everybuy.mobile.data.StatementSaleDto
import com.everybuy.mobile.ui.sales.paymentMethodLabel
import java.math.BigDecimal

@OptIn(ExperimentalMaterial3Api::class)
@Composable fun CustomerDetailScreen(
    id: String,
    onBack: () -> Unit,
    onEdit: () -> Unit,
    onOpenSale: (String) -> Unit,
    onSessionExpired: () -> Unit,
    viewModel: CustomerDetailViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsState()
    LaunchedEffect(id) { viewModel.load(id) }
    LaunchedEffect(state) { if (state is CustomerDetailState.SessionExpired) onSessionExpired() }
    Scaffold(topBar = {
        TopAppBar(
            title = { Text("Customer statement") },
            navigationIcon = { IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Rounded.ArrowBack, contentDescription = "Back") } },
            actions = { IconButton(onClick = onEdit) { Icon(Icons.Rounded.Edit, contentDescription = "Edit customer") } },
        )
    }) { insets ->
        when (val current = state) {
            CustomerDetailState.Loading -> Column(Modifier.fillMaxSize().padding(insets), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center) {
                CircularProgressIndicator()
                Text("Loading statement…", Modifier.padding(top = 12.dp))
            }
            CustomerDetailState.SessionExpired -> Text("Your session has expired.", Modifier.padding(insets).padding(24.dp))
            CustomerDetailState.NotFound -> Column(Modifier.fillMaxSize().padding(insets).padding(24.dp), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center) {
                Text("Customer not found", style = MaterialTheme.typography.titleLarge)
                TextButton(onClick = onBack) { Text("Back to customers") }
            }
            is CustomerDetailState.Failed -> Column(Modifier.fillMaxSize().padding(insets).padding(24.dp), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center) {
                Text(current.message, color = MaterialTheme.colorScheme.error)
                TextButton(onClick = { viewModel.load(id) }) { Text("Retry") }
            }
            is CustomerDetailState.Ready -> CustomerDetailContent(current, viewModel, onOpenSale, Modifier.fillMaxSize().padding(insets))
        }
    }
}

@Composable private fun CustomerDetailContent(
    state: CustomerDetailState.Ready,
    viewModel: CustomerDetailViewModel,
    onOpenSale: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    val statement = state.statement
    val outstanding = outstandingStatementSales(statement)
    val payments = customerPaymentHistory(statement)
    var showRepayment by rememberSaveable { mutableStateOf(false) }
    var initialSaleId by remember { mutableStateOf<String?>(null) }
    LaunchedEffect(state.success) { if (state.success != null) showRepayment = false }
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
                Text(statement.customer.name, style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
                Text(statement.customer.phone?.takeIf { it.isNotBlank() } ?: "No phone number on record", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            CustomerStatusBadge(statement.customer.active)
        }

        state.success?.let { success ->
            Card(shape = RoundedCornerShape(16.dp), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.secondaryContainer)) {
                Column(Modifier.fillMaxWidth().padding(14.dp), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                    Text("Repayment recorded", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                    Text(
                        "${formatNaira(success.amount)} applied to sale ${saleShortReference(success.saleId)}. The balances below are current.",
                        style = MaterialTheme.typography.bodySmall,
                    )
                    TextButton(onClick = { viewModel.dismissSuccess() }) { Text("Dismiss") }
                }
            }
        }

        Card(shape = RoundedCornerShape(22.dp), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer)) {
            Column(Modifier.fillMaxWidth().padding(18.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Text("Balance owed", style = MaterialTheme.typography.titleSmall)
                Text(formatNaira(statement.totals.outstandingDebt), style = MaterialTheme.typography.displaySmall, fontWeight = FontWeight.Bold)
                Text(
                    if (outstanding.isEmpty()) "All sales are fully paid" else "${outstanding.size} outstanding sale${if (outstanding.size == 1) "" else "s"}",
                    style = MaterialTheme.typography.bodySmall,
                )
                Row(Modifier.fillMaxWidth().padding(top = 4.dp), horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                    Column(Modifier.weight(1f)) {
                        Text("Total sales", style = MaterialTheme.typography.labelSmall)
                        Text(formatNaira(statement.totals.salesTotal), style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
                    }
                    Column(Modifier.weight(1f)) {
                        Text("Money received", style = MaterialTheme.typography.labelSmall)
                        Text(formatNaira(statement.totals.moneyReceived), style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
                    }
                }
            }
        }

        Button(
            onClick = { viewModel.clearRepaymentError(); initialSaleId = null; showRepayment = true },
            modifier = Modifier.fillMaxWidth(),
            enabled = outstanding.isNotEmpty() && !state.submitting,
        ) { Text("Record repayment") }

        Text("Outstanding sales", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
        if (outstanding.isEmpty()) {
            Text("Every sale recorded for this customer is fully paid.", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        outstanding.forEach { sale ->
            OutstandingSaleCard(sale, onOpenSale = { onOpenSale(sale.id) }, onRepay = {
                viewModel.clearRepaymentError()
                initialSaleId = sale.id
                showRepayment = true
            })
        }

        Text("Payments", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
        if (payments.isEmpty()) {
            Text("No payments recorded for this customer yet.", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        payments.forEach { PaymentRow(it) }

        Text("Customer details", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
        DetailCard(statement)
        Text("Last purchase ${statement.sales.firstOrNull()?.let { formatLagosDate(it.soldAt) } ?: "—"}", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
    }

    if (showRepayment) {
        RepaymentDialog(
            sales = outstanding,
            initialSaleId = initialSaleId,
            submitting = state.submitting,
            error = state.repaymentError,
            onDismiss = { if (!state.submitting) { showRepayment = false; viewModel.clearRepaymentError() } },
            onConfirm = { saleId, amount -> viewModel.repay(saleId, amount) },
        )
    }
}

@Composable private fun OutstandingSaleCard(sale: StatementSaleDto, onOpenSale: () -> Unit, onRepay: () -> Unit) {
    Card(shape = RoundedCornerShape(18.dp), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow)) {
        Column(Modifier.fillMaxWidth().clickable(onClick = onOpenSale).padding(14.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.Top) {
                Column(Modifier.weight(1f)) {
                    Text("Sale ${saleShortReference(sale.id)}", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
                    Text(formatLagosDateTime(sale.soldAt), style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                Text("${formatNaira(sale.balanceDue)} owed", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold, color = MaterialTheme.colorScheme.error)
            }
            Row(Modifier.fillMaxWidth().padding(top = 2.dp), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                Text("Total ${formatNaira(sale.totalAmount)} · paid ${formatNaira(sale.paidAmount)}", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                TextButton(onClick = onRepay) { Text("Record repayment") }
            }
        }
    }
}

@Composable private fun PaymentRow(payment: PaymentDto) {
    Card(shape = RoundedCornerShape(18.dp), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow)) {
        Row(Modifier.fillMaxWidth().padding(14.dp), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.Top) {
            Column(Modifier.weight(1f)) {
                Text(formatNaira(payment.amount), style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
                Text(
                    "${paymentMethodLabel(payment.method)} · sale ${saleShortReference(payment.saleId)}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                payment.reference?.takeIf { it.isNotBlank() }?.let { Text(it, style = MaterialTheme.typography.bodySmall) }
            }
            Text(formatLagosDate(payment.paidAt), style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

@Composable private fun DetailCard(statement: CustomerStatementDto) {
    val customer = statement.customer
    Card(shape = RoundedCornerShape(18.dp), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow)) {
        Column(Modifier.fillMaxWidth().padding(14.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            DetailRow("Phone", customer.phone?.takeIf { it.isNotBlank() } ?: "—")
            DetailRow("Address", customer.address?.takeIf { it.isNotBlank() } ?: "—")
            DetailRow("Added", customer.createdAt.takeIf { it.isNotBlank() }?.let(::formatLagosDate) ?: "—")
            DetailRow("Status", if (customer.active) "Active" else "Inactive")
            customer.notes?.takeIf { it.isNotBlank() }?.let {
                Text("Notes", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.padding(top = 4.dp))
                Text(it, style = MaterialTheme.typography.bodySmall)
            }
        }
    }
}

@Composable private fun DetailRow(label: String, value: String) {
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
        Text(label, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Text(value, style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.Medium, modifier = Modifier.padding(start = 12.dp))
    }
}

@Composable private fun RepaymentDialog(
    sales: List<StatementSaleDto>,
    initialSaleId: String?,
    submitting: Boolean,
    error: String?,
    onDismiss: () -> Unit,
    onConfirm: (saleId: String, amount: String) -> Unit,
) {
    var selectedSaleId by remember(sales, initialSaleId) {
        mutableStateOf(initialSaleId ?: sales.firstOrNull()?.id.orEmpty())
    }
    var amount by remember { mutableStateOf("") }
    val selected = sales.firstOrNull { it.id == selectedSaleId }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Record repayment") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text("Choose the sale receiving this repayment.", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                sales.forEach { sale ->
                    Row(
                        Modifier.fillMaxWidth().clickable { selectedSaleId = sale.id }.padding(vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        RadioButton(selected = sale.id == selectedSaleId, onClick = { selectedSaleId = sale.id })
                        Column(Modifier.weight(1f)) {
                            Text("Sale ${saleShortReference(sale.id)}", style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Medium)
                            Text("${formatNaira(sale.balanceDue)} owed · ${formatNaira(sale.totalAmount)} total", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }
                }
                OutlinedTextField(
                    amount,
                    { value -> amount = value.filter { it.isDigit() || it == '.' } },
                    Modifier.fillMaxWidth(),
                    label = { Text("Amount (₦)") },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    supportingText = {
                        Text(
                            selected?.let { "Up to ${formatNaira(it.balanceDue)} outstanding" } ?: "Select a sale",
                        )
                    },
                )
                error?.let { Text(it, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall) }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val sale = selected
                    if (sale != null) onConfirm(sale.id, amount)
                },
                enabled = !submitting && selected != null && amount.isNotBlank() && (amount.toBigDecimalOrNull() ?: BigDecimal.ZERO) > BigDecimal.ZERO,
            ) {
                if (submitting) CircularProgressIndicator(Modifier.height(18.dp)) else Text("Save repayment")
            }
        },
        dismissButton = { TextButton(onClick = onDismiss, enabled = !submitting) { Text("Cancel") } },
    )
}
