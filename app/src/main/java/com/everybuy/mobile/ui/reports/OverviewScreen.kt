package com.everybuy.mobile.ui.reports

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.NavigateBefore
import androidx.compose.material.icons.automirrored.rounded.NavigateNext
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.FilledTonalIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.PrimaryTabRow
import androidx.compose.material3.Surface
import androidx.compose.material3.Tab
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.everybuy.mobile.R
import com.everybuy.mobile.core.util.formatLagosDateTime
import com.everybuy.mobile.core.util.formatNaira
import com.everybuy.mobile.data.AuthUser
import com.everybuy.mobile.data.MonthlyClosingStock
import com.everybuy.mobile.data.MonthlyProductSold
import com.everybuy.mobile.data.MonthlyReportDto
import com.everybuy.mobile.data.OverviewRecentSale
import com.everybuy.mobile.data.ProfitReportDto
import com.everybuy.mobile.ui.components.EmptyPane
import com.everybuy.mobile.ui.components.ErrorPane
import com.everybuy.mobile.ui.components.EverybuyScreen
import com.everybuy.mobile.ui.components.LoadingPane
import com.everybuy.mobile.ui.components.SectionHeader
import com.everybuy.mobile.ui.components.StatTile
import com.everybuy.mobile.ui.components.TonalCard
import com.everybuy.mobile.ui.components.listBottomPadding
import java.math.BigDecimal

private enum class OverviewTab(val labelRes: Int) {
    TODAY(R.string.overview_today),
    MONTHLY(R.string.overview_monthly),
    PROFIT(R.string.overview_profit),
}

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun OverviewScreen(
    user: AuthUser,
    onOpenSettings: () -> Unit,
    onOpenSale: (String) -> Unit = {},
    onOpenProduct: (String) -> Unit = {},
    onOpenSales: () -> Unit = {},
    onOpenCustomers: () -> Unit = {},
    onOpenStock: () -> Unit = {},
    onRecordSale: () -> Unit = {},
    onSessionExpired: () -> Unit,
    viewModel: OverviewViewModel = hiltViewModel(),
    monthlyViewModel: MonthlyReportViewModel = hiltViewModel(),
    profitViewModel: ProfitReportViewModel = hiltViewModel(),
) {
    val dailyState by viewModel.state.collectAsState()
    val monthlyState by monthlyViewModel.state.collectAsState()
    val profitState by profitViewModel.state.collectAsState()
    var tab by rememberSaveable { mutableStateOf(OverviewTab.TODAY) }
    var month by rememberSaveable { mutableStateOf(currentLagosMonth()) }
    LaunchedEffect(dailyState) {
        if ((dailyState as? OverviewState.Failed)?.sessionExpired == true) onSessionExpired()
    }
    LaunchedEffect(monthlyState) { if (monthlyState is MonthlyReportState.SessionExpired) onSessionExpired() }
    LaunchedEffect(profitState) { if (profitState is ProfitReportState.SessionExpired) onSessionExpired() }
    LaunchedEffect(tab, month) {
        when (tab) {
            OverviewTab.MONTHLY -> monthlyViewModel.load(month)
            OverviewTab.PROFIT -> profitViewModel.load(month)
            OverviewTab.TODAY -> Unit
        }
    }
    EverybuyScreen(
        title = stringResource(R.string.overview_title),
        subtitle = user.name.ifBlank { user.email },
        onOpenSettings = onOpenSettings,
        floatingActionButton = {
            ExtendedFloatingActionButton(
                onClick = onRecordSale,
                icon = { Icon(Icons.Rounded.Add, contentDescription = null) },
                text = { Text(stringResource(R.string.overview_record_sale)) },
                containerColor = MaterialTheme.colorScheme.primaryContainer,
                contentColor = MaterialTheme.colorScheme.onPrimaryContainer,
            )
        },
    ) { padding ->
        Column(Modifier.fillMaxSize()) {
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(start = 16.dp, end = 16.dp, top = padding.calculateTopPadding()),
                shape = MaterialTheme.shapes.extraLarge,
                color = MaterialTheme.colorScheme.surfaceContainerLow,
            ) {
                PrimaryTabRow(
                    selectedTabIndex = tab.ordinal,
                    containerColor = MaterialTheme.colorScheme.surfaceContainerLow,
                    contentColor = MaterialTheme.colorScheme.onSurface,
                    divider = {},
                ) {
                    OverviewTab.entries.forEach { option ->
                        Tab(
                            selected = tab == option,
                            onClick = { tab = option },
                            text = {
                                Text(
                                    text = stringResource(option.labelRes),
                                    maxLines = 1,
                                    style = MaterialTheme.typography.labelLarge,
                                )
                            },
                        )
                    }
                }
            }
            val bottom = padding.calculateBottomPadding() + listBottomPadding(extra = 104.dp)
            when (tab) {
                OverviewTab.TODAY -> TodayPanel(
                    state = dailyState,
                    onRetry = { viewModel.refresh() },
                    onOpenSale = onOpenSale,
                    onOpenProduct = onOpenProduct,
                    onOpenSales = onOpenSales,
                    onOpenCustomers = onOpenCustomers,
                    onOpenStock = onOpenStock,
                    bottomPadding = bottom,
                    modifier = Modifier.fillMaxSize(),
                )
                OverviewTab.MONTHLY -> MonthlyPanel(
                    state = monthlyState,
                    month = month,
                    onMonthChange = { month = it },
                    onRetry = { monthlyViewModel.load(month) },
                    bottomPadding = bottom,
                    modifier = Modifier.fillMaxSize(),
                )
                OverviewTab.PROFIT -> ProfitPanel(
                    state = profitState,
                    month = month,
                    onMonthChange = { month = it },
                    onRetry = { profitViewModel.load(month) },
                    onOpenSale = onOpenSale,
                    bottomPadding = bottom,
                    modifier = Modifier.fillMaxSize(),
                )
            }
        }
    }
}

@Composable
private fun TodayPanel(
    state: OverviewState,
    onRetry: () -> Unit,
    onOpenSale: (String) -> Unit,
    onOpenProduct: (String) -> Unit,
    onOpenSales: () -> Unit,
    onOpenCustomers: () -> Unit,
    onOpenStock: () -> Unit,
    bottomPadding: androidx.compose.ui.unit.Dp,
    modifier: Modifier = Modifier,
) {
    when (state) {
        OverviewState.Loading -> LoadingPane(stringResource(R.string.overview_loading), modifier)
        is OverviewState.Failed -> ErrorPane(state.message, if (state.forbidden) null else onRetry, modifier)
        is OverviewState.Ready -> {
            val report = state.report
            LazyVerticalGrid(
                columns = GridCells.Adaptive(minSize = 168.dp),
                modifier = modifier,
                contentPadding = PaddingValues(
                    start = 16.dp,
                    end = 16.dp,
                    top = 12.dp,
                    bottom = bottomPadding,
                ),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                item(span = { GridItemSpan(maxLineSpan) }) {
                    SectionHeader(title = stringResource(R.string.overview_at_a_glance))
                }
                item {
                    StatTile(
                        label = stringResource(R.string.overview_sales_today),
                        value = formatNaira(report.salesToday.total),
                        supporting = pluralStringResource(
                            R.plurals.overview_sales_count,
                            report.salesToday.count,
                            report.salesToday.count,
                        ),
                        containerColor = MaterialTheme.colorScheme.secondaryContainer,
                        contentColor = MaterialTheme.colorScheme.onSecondaryContainer,
                        onClick = onOpenSales,
                        modifier = Modifier.fillMaxWidth(),
                    )
                }
                item {
                    StatTile(
                        label = stringResource(R.string.overview_payments_received),
                        value = formatNaira(report.paymentsReceived),
                        supporting = stringResource(R.string.overview_payments_detail),
                        containerColor = MaterialTheme.colorScheme.tertiaryContainer,
                        contentColor = MaterialTheme.colorScheme.onTertiaryContainer,
                        onClick = onOpenSales,
                        modifier = Modifier.fillMaxWidth(),
                    )
                }
                item {
                    val outstanding = positive(report.customerBalances)
                    StatTile(
                        label = stringResource(R.string.overview_customer_balances),
                        value = formatNaira(report.customerBalances),
                        supporting = stringResource(R.string.overview_balance_detail),
                        containerColor = if (outstanding) MaterialTheme.colorScheme.errorContainer else MaterialTheme.colorScheme.surfaceContainerHigh,
                        contentColor = if (outstanding) MaterialTheme.colorScheme.onErrorContainer else MaterialTheme.colorScheme.onSurface,
                        onClick = onOpenCustomers,
                        modifier = Modifier.fillMaxWidth(),
                    )
                }
                item {
                    val low = report.lowStock.isNotEmpty()
                    StatTile(
                        label = stringResource(R.string.overview_low_stock_stat),
                        value = report.lowStock.size.toString(),
                        supporting = stringResource(R.string.overview_low_stock_detail),
                        containerColor = if (low) MaterialTheme.colorScheme.errorContainer else MaterialTheme.colorScheme.tertiaryContainer,
                        contentColor = if (low) MaterialTheme.colorScheme.onErrorContainer else MaterialTheme.colorScheme.onTertiaryContainer,
                        onClick = onOpenStock,
                        modifier = Modifier.fillMaxWidth(),
                    )
                }
                item(span = { GridItemSpan(maxLineSpan) }) {
                    SectionHeader(title = stringResource(R.string.overview_low_stock_section))
                }
                if (report.lowStock.isEmpty()) {
                    item(span = { GridItemSpan(maxLineSpan) }) {
                        EmptyPane(title = stringResource(R.string.overview_no_low_stock), body = "")
                    }
                } else {
                    items(report.lowStock, key = { "low-${it.productId}" }) { item ->
                        TonalCard(onClick = { onOpenProduct(item.productId) }, modifier = Modifier.fillMaxWidth()) {
                            Row(
                                Modifier.fillMaxWidth().padding(16.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically,
                            ) {
                                Text(
                                    text = item.name,
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.SemiBold,
                                    modifier = Modifier.weight(1f),
                                    maxLines = 2,
                                    overflow = TextOverflow.Ellipsis,
                                )
                                Text(
                                    text = "${item.packsAvailable} packs",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.error,
                                    fontWeight = FontWeight.SemiBold,
                                )
                            }
                        }
                    }
                }
                item(span = { GridItemSpan(maxLineSpan) }) {
                    SectionHeader(title = stringResource(R.string.overview_recent_sales))
                }
                if (report.recentSales.isEmpty()) {
                    item(span = { GridItemSpan(maxLineSpan) }) {
                        EmptyPane(title = stringResource(R.string.overview_no_sales), body = "")
                    }
                } else {
                    items(report.recentSales, key = { "sale-${it.id}" }) { sale ->
                        RecentSaleCard(sale) { onOpenSale(sale.id) }
                    }
                }
            }
        }
    }
}

@Composable
private fun MonthlyPanel(
    state: MonthlyReportState,
    month: String,
    onMonthChange: (String) -> Unit,
    onRetry: () -> Unit,
    bottomPadding: androidx.compose.ui.unit.Dp,
    modifier: Modifier = Modifier,
) {
    LazyColumn(
        modifier = modifier,
        contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 12.dp, bottom = bottomPadding),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        item { MonthHeader(month, onMonthChange) }
        when (state) {
            MonthlyReportState.Loading -> item { LoadingPane(stringResource(R.string.loading)) }
            MonthlyReportState.SessionExpired -> item { ErrorPane(stringResource(R.string.session_expired), onRetry) }
            is MonthlyReportState.Failed -> item { ErrorPane(state.message, onRetry) }
            is MonthlyReportState.Ready -> {
                val report = state.report
                item { MonthlySummary(report) }
                item { SectionHeader(title = stringResource(R.string.overview_product_sales)) }
                if (report.productsSold.isEmpty()) item { EmptyPane(title = stringResource(R.string.overview_no_product_sales), body = "") }
                else items(report.productsSold, key = { it.productId }) { product -> ProductSoldCard(product) }
                item { SectionHeader(title = stringResource(R.string.overview_closing_stock)) }
                if (report.closingStock.isEmpty()) item { EmptyPane(title = stringResource(R.string.overview_no_closing_stock), body = "") }
                else items(report.closingStock, key = { it.productId }) { product -> ClosingStockCard(product) }
            }
        }
    }
}

@Composable
private fun ProfitPanel(
    state: ProfitReportState,
    month: String,
    onMonthChange: (String) -> Unit,
    onRetry: () -> Unit,
    onOpenSale: (String) -> Unit,
    bottomPadding: androidx.compose.ui.unit.Dp,
    modifier: Modifier = Modifier,
) {
    LazyColumn(
        modifier = modifier,
        contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 12.dp, bottom = bottomPadding),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        item { MonthHeader(month, onMonthChange) }
        when (state) {
            ProfitReportState.Loading -> item { LoadingPane(stringResource(R.string.loading)) }
            ProfitReportState.SessionExpired -> item { ErrorPane(stringResource(R.string.session_expired), onRetry) }
            is ProfitReportState.Failed -> item { ErrorPane(state.message, onRetry) }
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
    }
}

@Composable
private fun MonthHeader(month: String, onMonthChange: (String) -> Unit) {
    Row(
        Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
            FilledTonalIconButton(onClick = { onMonthChange(shiftMonth(month, -1)) }) {
                Icon(Icons.AutoMirrored.Rounded.NavigateBefore, contentDescription = stringResource(R.string.overview_previous_month))
            }
            Text(
                text = formatMonthLabel(month),
                style = MaterialTheme.typography.titleMediumEmphasized,
                fontWeight = FontWeight.SemiBold,
            )
            FilledTonalIconButton(onClick = { onMonthChange(shiftMonth(month, 1)) }) {
                Icon(Icons.AutoMirrored.Rounded.NavigateNext, contentDescription = stringResource(R.string.overview_next_month))
            }
        }
        if (month != currentLagosMonth()) {
            FilledTonalButton(onClick = { onMonthChange(currentLagosMonth()) }) {
                Text(stringResource(R.string.overview_this_month))
            }
        }
    }
}

@Composable
private fun MonthlySummary(report: MonthlyReportDto) {
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            StatTile(
                label = stringResource(R.string.overview_monthly_sales),
                value = formatNaira(report.salesTotal),
                supporting = "",
                modifier = Modifier.weight(1f),
            )
            StatTile(
                label = stringResource(R.string.overview_money_received),
                value = formatNaira(report.moneyReceived),
                supporting = "",
                modifier = Modifier.weight(1f),
            )
        }
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            val outstanding = positive(report.outstandingDebtAtMonthEnd)
            StatTile(
                label = stringResource(R.string.overview_outstanding_debt),
                value = formatNaira(report.outstandingDebtAtMonthEnd),
                supporting = "",
                containerColor = if (outstanding) MaterialTheme.colorScheme.errorContainer else MaterialTheme.colorScheme.secondaryContainer,
                contentColor = if (outstanding) MaterialTheme.colorScheme.onErrorContainer else MaterialTheme.colorScheme.onSecondaryContainer,
                modifier = Modifier.weight(1f),
            )
            StatTile(
                label = stringResource(R.string.overview_products_sold),
                value = report.productsSold.size.toString(),
                supporting = "",
                modifier = Modifier.weight(1f),
            )
        }
    }
}

@Composable
private fun ProductSoldCard(product: MonthlyProductSold) {
    TonalCard(Modifier.fillMaxWidth()) {
        Row(
            Modifier.fillMaxWidth().padding(16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(Modifier.weight(1f)) {
                Text(product.productName, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
                Text(
                    "${product.bagsSold} bags · ${product.retailPacksSold} retail packs",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            Text(
                "${product.totalPacksSold} packs",
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.SemiBold,
            )
        }
    }
}

@Composable
private fun ClosingStockCard(product: MonthlyClosingStock) {
    TonalCard(Modifier.fillMaxWidth()) {
        Row(
            Modifier.fillMaxWidth().padding(16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                product.productName,
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.SemiBold,
                modifier = Modifier.weight(1f),
            )
            Text(
                "${product.closingPacks} packs",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@Composable
private fun ProfitSummary(report: ProfitReportDto) {
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            StatTile(
                label = stringResource(R.string.overview_revenue),
                value = formatNaira(report.revenue.orEmpty()),
                supporting = "",
                modifier = Modifier.weight(1f),
            )
            StatTile(
                label = stringResource(R.string.overview_cogs),
                value = formatNaira(report.cogs.orEmpty()),
                supporting = "",
                modifier = Modifier.weight(1f),
            )
        }
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            val grossNegative = negative(report.grossProfit)
            StatTile(
                label = stringResource(R.string.overview_gross_profit),
                value = formatNaira(report.grossProfit.orEmpty()),
                supporting = "",
                containerColor = if (grossNegative) MaterialTheme.colorScheme.errorContainer else MaterialTheme.colorScheme.tertiaryContainer,
                contentColor = if (grossNegative) MaterialTheme.colorScheme.onErrorContainer else MaterialTheme.colorScheme.onTertiaryContainer,
                modifier = Modifier.weight(1f),
            )
            val netNegative = negative(report.netProfit)
            StatTile(
                label = stringResource(R.string.overview_net_profit),
                value = formatNaira(report.netProfit.orEmpty()),
                supporting = "",
                containerColor = if (netNegative) MaterialTheme.colorScheme.errorContainer else MaterialTheme.colorScheme.primaryContainer,
                contentColor = if (netNegative) MaterialTheme.colorScheme.onErrorContainer else MaterialTheme.colorScheme.onPrimaryContainer,
                modifier = Modifier.weight(1f),
            )
        }
    }
}

@Composable
private fun ProfitBreakdown(report: ProfitReportDto) {
    TonalCard(Modifier.fillMaxWidth()) {
        Column(
            Modifier.fillMaxWidth().padding(18.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Text("How net profit is calculated", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
            ProfitRow("Revenue", report.revenue.orEmpty())
            ProfitRow("Less cost of goods sold", report.cogs.orEmpty(), negative = true)
            ProfitRow("Gross profit", report.grossProfit.orEmpty(), strong = true)
            ProfitRow("Less expenses", report.expenses, negative = true)
            ProfitRow("Net profit", report.netProfit.orEmpty(), strong = true)
        }
    }
}

@Composable
private fun ProfitRow(label: String, value: String, negative: Boolean = false, strong: Boolean = false) {
    val amount = value.toBigDecimalOrNull() ?: BigDecimal.ZERO
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
        Text(
            label,
            style = if (strong) MaterialTheme.typography.bodyMedium else MaterialTheme.typography.bodySmall,
            color = if (strong) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Text(
            (if (negative) "− " else "") + formatNaira(amount.abs().toPlainString()),
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = if (strong) FontWeight.Bold else FontWeight.SemiBold,
            color = if (amount < BigDecimal.ZERO) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurface,
        )
    }
}

@Composable
private fun MissingCostWarning(report: ProfitReportDto, onOpenSale: (String) -> Unit) {
    Card(
        Modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.large,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.tertiaryContainer),
    ) {
        Column(Modifier.fillMaxWidth().padding(18.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Text(
                "Profit cannot be calculated",
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold,
            )
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
                    Text(
                        "${item.productName} · ${item.quantity} line units / ${item.quantity * item.packsPerUnit} packs",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(start = 12.dp),
                    )
                }
            }
        }
    }
}

@Composable
private fun RecentSaleCard(sale: OverviewRecentSale, onOpen: () -> Unit) {
    TonalCard(
        modifier = Modifier.fillMaxWidth(),
        onClick = onOpen,
    ) {
        Row(
            Modifier.fillMaxWidth().padding(16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(Modifier.weight(1f)) {
                Text(sale.reference, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                Text(sale.customer?.name ?: stringResource(R.string.sales_walk_in), style = MaterialTheme.typography.bodyMedium)
                Text(
                    formatLagosDateTime(sale.soldAt),
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            Column(horizontalAlignment = Alignment.End) {
                Text(
                    formatNaira(sale.itemsTotal),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                )
                if (positive(sale.balance)) {
                    Text(
                        "Due ${formatNaira(sale.balance)}",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.error,
                    )
                }
            }
        }
    }
}

private fun positive(value: String?): Boolean = (value?.toBigDecimalOrNull() ?: BigDecimal.ZERO) > BigDecimal.ZERO

private fun negative(value: String?): Boolean = (value?.toBigDecimalOrNull() ?: BigDecimal.ZERO) < BigDecimal.ZERO
