package com.everybuy.mobile.ui.reports

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
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
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
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
import com.everybuy.mobile.data.AuthUser
import com.everybuy.mobile.data.MonthlyClosingStock
import com.everybuy.mobile.data.MonthlyProductSold
import com.everybuy.mobile.data.MonthlyReportDto
import com.everybuy.mobile.data.OverviewRecentSale
import com.everybuy.mobile.data.ProfitReportDto
import com.everybuy.mobile.ui.auth.AppTheme
import java.math.BigDecimal

private enum class OverviewTab(val label: String) { TODAY("Today"), MONTHLY("Monthly"), PROFIT("Profit") }

@OptIn(ExperimentalMaterial3Api::class)
@Composable fun OverviewScreen(
    user: AuthUser,
    theme: AppTheme,
    onSignOut: () -> Unit,
    onSessionExpired: () -> Unit,
    setTheme: (AppTheme) -> Unit,
    onOpenSale: (String) -> Unit = {},
    onOpenProduct: (String) -> Unit = {},
    onRecordSale: () -> Unit = {},
    onOpenExpenses: () -> Unit = {},
    onOpenAudit: () -> Unit = {},
    onOpenSettings: () -> Unit = {},
    viewModel: OverviewViewModel = hiltViewModel(),
    monthlyViewModel: MonthlyReportViewModel = hiltViewModel(),
    profitViewModel: ProfitReportViewModel = hiltViewModel(),
) {
    val dailyState by viewModel.state.collectAsState()
    val monthlyState by monthlyViewModel.state.collectAsState()
    val profitState by profitViewModel.state.collectAsState()
    var moreMenu by remember { mutableStateOf(false) }
    var tab by rememberSaveable { mutableStateOf(OverviewTab.TODAY) }
    var month by rememberSaveable { mutableStateOf(currentLagosMonth()) }
    LaunchedEffect(dailyState) { if ((dailyState as? OverviewState.Failed)?.sessionExpired == true) onSessionExpired() }
    LaunchedEffect(monthlyState) { if (monthlyState is MonthlyReportState.SessionExpired) onSessionExpired() }
    LaunchedEffect(profitState) { if (profitState is ProfitReportState.SessionExpired) onSessionExpired() }
    LaunchedEffect(tab, month) {
        when (tab) {
            OverviewTab.MONTHLY -> monthlyViewModel.load(month)
            OverviewTab.PROFIT -> profitViewModel.load(month)
            OverviewTab.TODAY -> Unit
        }
    }
    Scaffold(topBar = {
        TopAppBar(title = { Column { Text("Overview"); Text(user.name.ifBlank { user.email }, style = MaterialTheme.typography.labelMedium) } }, actions = {
            Box {
                TextButton(onClick = { moreMenu = true }) { Text("More") }
                DropdownMenu(expanded = moreMenu, onDismissRequest = { moreMenu = false }) {
                    DropdownMenuItem(text = { Text("Expenses") }, onClick = { moreMenu = false; onOpenExpenses() })
                    DropdownMenuItem(text = { Text("Audit log") }, onClick = { moreMenu = false; onOpenAudit() })
                    DropdownMenuItem(text = { Text("Settings") }, onClick = { moreMenu = false; onOpenSettings() })
                    AppTheme.entries.forEach { option ->
                        DropdownMenuItem(
                            text = { Text("${option.name.lowercase().replaceFirstChar { it.uppercase() }} theme") },
                            onClick = { moreMenu = false; setTheme(option) },
                        )
                    }
                }
            }
            TextButton(onClick = onSignOut) { Text("Sign out") }
        })
    }) { inset ->
        Column(Modifier.fillMaxSize().padding(inset)) {
            TabRow(selectedTabIndex = tab.ordinal) {
                OverviewTab.entries.forEach { option ->
                    Tab(selected = tab == option, onClick = { tab = option }, text = { Text(option.label) })
                }
            }
            when (tab) {
                OverviewTab.TODAY -> TodayPanel(dailyState, viewModel, onOpenSale, onOpenProduct, onRecordSale, Modifier.fillMaxSize())
                OverviewTab.MONTHLY -> MonthlyPanel(monthlyState, month, { month = it }, { monthlyViewModel.load(month) }, Modifier.fillMaxSize())
                OverviewTab.PROFIT -> ProfitPanel(profitState, month, { month = it }, { profitViewModel.load(month) }, onOpenSale, Modifier.fillMaxSize())
            }
        }
    }
}

@Composable private fun TodayPanel(
    state: OverviewState,
    viewModel: OverviewViewModel,
    onOpenSale: (String) -> Unit,
    onOpenProduct: (String) -> Unit,
    onRecordSale: () -> Unit,
    modifier: Modifier = Modifier,
) {
    when (state) {
        OverviewState.Loading -> Loading("Loading today’s overview…", modifier)
        is OverviewState.Failed -> ReportFailure(state.message, state.forbidden, { viewModel.refresh() }, modifier)
        is OverviewState.Ready -> LazyColumn(modifier.padding(horizontal = 16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            item {
                Row(Modifier.fillMaxWidth().padding(top = 10.dp), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                    Text("Today at a glance", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
                    Button(onClick = onRecordSale) { Text("Record sale") }
                }
            }
            item {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        ReportSummaryCard("Sales today", formatNaira(state.report.salesToday.total), "${state.report.salesToday.count} sales", Modifier.weight(1f))
                        ReportSummaryCard("Payments received", formatNaira(state.report.paymentsReceived), "Includes repayments", Modifier.weight(1f))
                    }
                    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        ReportSummaryCard("Customer balances", formatNaira(state.report.customerBalances), "Outstanding", Modifier.weight(1f), warning = positive(state.report.customerBalances))
                        ReportSummaryCard("Low stock", state.report.lowStock.size.toString(), "10 packs or fewer", Modifier.weight(1f), warning = state.report.lowStock.isNotEmpty())
                    }
                }
            }
            item { SectionTitle("Low stock") }
            if (state.report.lowStock.isEmpty()) item { MutedText("Every product is above the low-stock threshold.") }
            else items(state.report.lowStock, key = { it.productId }) { item ->
                CompactCard(item.name, "${item.packsAvailable} packs available") { onOpenProduct(item.productId) }
            }
            item { SectionTitle("Recent sales") }
            if (state.report.recentSales.isEmpty()) item { MutedText("No sales recorded today.") }
            else items(state.report.recentSales, key = { it.id }) { sale -> RecentSaleCard(sale) { onOpenSale(sale.id) } }
            item { Spacer(Modifier.height(12.dp)) }
        }
    }
}

@Composable private fun MonthlyPanel(
    state: MonthlyReportState,
    month: String,
    onMonthChange: (String) -> Unit,
    onRetry: () -> Unit,
    modifier: Modifier = Modifier,
) {
    LazyColumn(modifier.padding(horizontal = 16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        item { MonthHeader(month, onMonthChange) }
        item { MutedText("Money received includes payments for earlier sales, and outstanding debt counts only what was still unpaid at month end.") }
        when (state) {
            MonthlyReportState.Loading -> item { Box(Modifier.fillMaxWidth().padding(32.dp), contentAlignment = Alignment.Center) { CircularProgressIndicator() } }
            MonthlyReportState.SessionExpired -> item { ReportFailure("Your session has expired.", true, onRetry) }
            is MonthlyReportState.Failed -> item { ReportFailure(state.message, false, onRetry) }
            is MonthlyReportState.Ready -> {
                val report = state.report
                item { MonthlySummary(report) }
                item { SectionTitle("Product sales") }
                item { MutedText("Bags and packs sold during ${formatMonthLabel(report.month.ifBlank { month })}.") }
                if (report.productsSold.isEmpty()) item { MutedText("No products were sold in this month.") }
                else items(report.productsSold, key = { it.productId }) { product -> ProductSoldCard(product) }
                item { SectionTitle("Closing stock") }
                item { MutedText("Pack balance at the end of the month, not current stock.") }
                if (report.closingStock.isEmpty()) item { MutedText("No products existed at the end of this month.") }
                else items(report.closingStock, key = { it.productId }) { product -> ClosingStockCard(product) }
            }
        }
        item { Spacer(Modifier.height(12.dp)) }
    }
}

@Composable private fun ProfitPanel(
    state: ProfitReportState,
    month: String,
    onMonthChange: (String) -> Unit,
    onRetry: () -> Unit,
    onOpenSale: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    LazyColumn(modifier.padding(horizontal = 16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        item { MonthHeader(month, onMonthChange) }
        item { MutedText("Revenue is what customers were charged. Cost of goods uses the recorded cost per pack at the time of each sale, and expenses are dated in this month.") }
        when (state) {
            ProfitReportState.Loading -> item { Box(Modifier.fillMaxWidth().padding(32.dp), contentAlignment = Alignment.Center) { CircularProgressIndicator() } }
            ProfitReportState.SessionExpired -> item { ReportFailure("Your session has expired.", true, onRetry) }
            is ProfitReportState.Failed -> item { ReportFailure(state.message, false, onRetry) }
            is ProfitReportState.Ready -> {
                val report = state.report
                if (report.missingCostCount > 0) {
                    item { MissingCostWarning(report, onOpenSale) }
                } else {
                    item { ProfitSummary(report) }
                    item { ProfitBreakdown(report) }
                }
            }
        }
        item { Spacer(Modifier.height(12.dp)) }
    }
}

@Composable private fun MonthHeader(month: String, onMonthChange: (String) -> Unit) {
    Row(Modifier.fillMaxWidth().padding(top = 10.dp), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            TextButton(onClick = { onMonthChange(shiftMonth(month, -1)) }) { Text("‹ Previous") }
            Text(formatMonthLabel(month), style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold, modifier = Modifier.padding(horizontal = 4.dp))
            TextButton(onClick = { onMonthChange(shiftMonth(month, 1)) }) { Text("Next ›") }
        }
        if (month != currentLagosMonth()) TextButton(onClick = { onMonthChange(currentLagosMonth()) }) { Text("This month") }
    }
}

@Composable private fun MonthlySummary(report: MonthlyReportDto) {
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            ReportSummaryCard("Sales", formatNaira(report.salesTotal), "Recorded this month", Modifier.weight(1f))
            ReportSummaryCard("Money received", formatNaira(report.moneyReceived), "Payments this month", Modifier.weight(1f))
        }
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            ReportSummaryCard("Outstanding debt", formatNaira(report.outstandingDebtAtMonthEnd), "At month end", Modifier.weight(1f), warning = positive(report.outstandingDebtAtMonthEnd))
            ReportSummaryCard("Products sold", report.productsSold.size.toString(), "Product lines", Modifier.weight(1f))
        }
    }
}

@Composable private fun ProductSoldCard(product: MonthlyProductSold) {
    Card(Modifier.fillMaxWidth(), shape = RoundedCornerShape(20.dp), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow)) {
        Row(Modifier.fillMaxWidth().padding(14.dp), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Text(product.productName, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
                Text("${product.bagsSold} bags · ${product.retailPacksSold} retail packs", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            Text("${product.totalPacksSold} packs", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
        }
    }
}

@Composable private fun ClosingStockCard(product: MonthlyClosingStock) {
    Card(Modifier.fillMaxWidth(), shape = RoundedCornerShape(20.dp), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow)) {
        Row(Modifier.fillMaxWidth().padding(14.dp), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
            Text(product.productName, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold, modifier = Modifier.weight(1f))
            Text("${product.closingPacks} packs", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

@Composable private fun ProfitSummary(report: ProfitReportDto) {
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            ReportSummaryCard("Revenue", formatNaira(report.revenue.orEmpty()), "Total sales value", Modifier.weight(1f))
            ReportSummaryCard("Cost of goods", formatNaira(report.cogs.orEmpty()), "Recorded pack costs", Modifier.weight(1f))
        }
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            ReportSummaryCard("Gross profit", formatNaira(report.grossProfit.orEmpty()), "Revenue minus cost", Modifier.weight(1f), warning = negative(report.grossProfit))
            ReportSummaryCard("Net profit", formatNaira(report.netProfit.orEmpty()), "After expenses", Modifier.weight(1f), warning = negative(report.netProfit))
        }
    }
}

@Composable private fun ProfitBreakdown(report: ProfitReportDto) {
    Card(Modifier.fillMaxWidth(), shape = RoundedCornerShape(20.dp), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow)) {
        Column(Modifier.fillMaxWidth().padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text("How net profit is calculated", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
            ProfitRow("Revenue", report.revenue.orEmpty())
            ProfitRow("Less cost of goods sold", report.cogs.orEmpty(), negative = true)
            ProfitRow("Gross profit", report.grossProfit.orEmpty(), strong = true)
            ProfitRow("Less expenses", report.expenses, negative = true)
            ProfitRow("Net profit", report.netProfit.orEmpty(), strong = true)
        }
    }
}

@Composable private fun ProfitRow(label: String, value: String, negative: Boolean = false, strong: Boolean = false) {
    val amount = value.toBigDecimalOrNull() ?: BigDecimal.ZERO
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
        Text(label, style = if (strong) MaterialTheme.typography.bodyMedium else MaterialTheme.typography.bodySmall, color = if (strong) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurfaceVariant)
        Text(
            (if (negative) "− " else "") + formatNaira(amount.abs().toPlainString()),
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = if (strong) FontWeight.Bold else FontWeight.SemiBold,
            color = if (amount < BigDecimal.ZERO) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurface,
        )
    }
}

@Composable private fun MissingCostWarning(report: ProfitReportDto, onOpenSale: (String) -> Unit) {
    Card(Modifier.fillMaxWidth(), shape = RoundedCornerShape(18.dp), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.tertiaryContainer)) {
        Column(Modifier.fillMaxWidth().padding(16.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Text("Profit cannot be calculated", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
            Text(
                "${report.missingCostCount} sale item cost snapshot${if (report.missingCostCount == 1) " is" else "s are"} missing. Profit figures stay hidden until an owner confirms the historical acquisition costs.",
                style = MaterialTheme.typography.bodySmall,
            )
            Text("Expenses recorded: ${formatNaira(report.expenses)}", style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.Medium)
            report.missingCostSales.forEach { sale ->
                Text(
                    "${sale.saleNumber.ifBlank { "#${sale.saleId.take(8)}" }} · ${formatLagosDateTime(sale.soldAt)}",
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.padding(top = 4.dp).clickable { onOpenSale(sale.saleId) },
                )
                sale.items.forEach { item ->
                    Text("${item.productName} · ${item.quantity} line units / ${item.quantity * item.packsPerUnit} packs", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.padding(start = 12.dp))
                }
            }
        }
    }
}

@Composable private fun ReportSummaryCard(label: String, value: String, detail: String, modifier: Modifier = Modifier, warning: Boolean = false) {
    Card(modifier, shape = RoundedCornerShape(22.dp), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.secondaryContainer)) {
        Column(Modifier.fillMaxWidth().padding(14.dp)) {
            Text(label, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSecondaryContainer)
            Text(
                value,
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                color = if (warning) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSecondaryContainer,
                modifier = Modifier.padding(top = 6.dp),
            )
            Text(detail, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSecondaryContainer, modifier = Modifier.padding(top = 2.dp))
        }
    }
}

@Composable private fun SectionTitle(title: String) {
    Text(title, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.SemiBold, modifier = Modifier.padding(top = 6.dp))
}

@Composable private fun MutedText(text: String) {
    Text(text, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
}

@Composable private fun Loading(message: String, modifier: Modifier = Modifier) {
    Column(modifier, horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center) {
        CircularProgressIndicator()
        Text(message, Modifier.padding(top = 12.dp))
    }
}

@Composable private fun ReportFailure(message: String, forbidden: Boolean, onRetry: () -> Unit, modifier: Modifier = Modifier) {
    Column(modifier.padding(24.dp), verticalArrangement = Arrangement.Center, horizontalAlignment = Alignment.CenterHorizontally) {
        Text(if (forbidden) "Reports unavailable" else "Couldn’t load report", style = MaterialTheme.typography.titleLarge)
        Text(message, Modifier.padding(top = 8.dp), color = MaterialTheme.colorScheme.onSurfaceVariant)
        if (!forbidden) TextButton(onClick = onRetry) { Text("Retry") }
    }
}

@Composable private fun CompactCard(title: String, supporting: String, onOpen: () -> Unit) {
    Card(Modifier.fillMaxWidth().clickable(onClick = onOpen), shape = RoundedCornerShape(22.dp), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow)) {
        Row(Modifier.fillMaxWidth().padding(16.dp), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
            Text(title, style = MaterialTheme.typography.titleMedium, modifier = Modifier.weight(1f))
            Text(supporting, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

@Composable private fun RecentSaleCard(sale: OverviewRecentSale, onOpen: () -> Unit) {
    Card(Modifier.fillMaxWidth().clickable(onClick = onOpen), shape = RoundedCornerShape(22.dp), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow)) {
        Row(Modifier.fillMaxWidth().padding(16.dp), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Text(sale.reference, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                Text(sale.customer?.name ?: "Walk-in customer", style = MaterialTheme.typography.bodyMedium)
                Text(formatLagosDateTime(sale.soldAt), style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            Column(horizontalAlignment = Alignment.End) {
                Text(formatNaira(sale.itemsTotal), style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                if (positive(sale.balance)) Text("Due ${formatNaira(sale.balance)}", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.error)
            }
        }
    }
}

private fun positive(value: String?): Boolean = (value?.toBigDecimalOrNull() ?: BigDecimal.ZERO) > BigDecimal.ZERO

private fun negative(value: String?): Boolean = (value?.toBigDecimalOrNull() ?: BigDecimal.ZERO) < BigDecimal.ZERO
