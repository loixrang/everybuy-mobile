package com.everybuy.mobile.ui.reports

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.rounded.Refresh
import androidx.compose.material3.Button
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.NavigateBefore
import androidx.compose.material.icons.automirrored.rounded.NavigateNext
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
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
import com.everybuy.mobile.data.DailyOverview
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
import com.everybuy.mobile.ui.components.LocalAppHazeState
import com.everybuy.mobile.ui.components.glass
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
        subtitle = stringResource(R.string.overview_subtitle),
        onOpenSettings = onOpenSettings,
    ) { padding ->
        Column(Modifier.fillMaxSize()) {
            Button(
                onClick = onRecordSale,
                modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 4.dp),
            ) {
                Icon(Icons.Rounded.Add, contentDescription = null)
                Text(stringResource(R.string.overview_record_sale), modifier = Modifier.padding(start = 8.dp))
            }
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 4.dp)
                    .glass(LocalAppHazeState.current),
                shape = MaterialTheme.shapes.extraLarge,
                color = MaterialTheme.colorScheme.surfaceContainerLow.copy(alpha = 0.96f),
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
            val bottom = padding.calculateBottomPadding() + listBottomPadding(extra = 24.dp)
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
            LazyColumn(
                modifier = modifier,
                contentPadding = PaddingValues(
                    start = 16.dp,
                    end = 16.dp,
                    top = 12.dp,
                    bottom = bottomPadding,
                ),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                item(key = "overview-stat-tiles") {
                    ReportStatTiles(report, onOpenSales, onOpenCustomers, onOpenStock)
                }
                item(key = "recent-sales") {
                    RecentSalesCard(
                        sales = report.recentSales.take(5),
                        onOpenSale = onOpenSale,
                        onRefresh = onRetry,
                        onOpenSales = onOpenSales,
                    )
                }
                item(key = "low-stock-heading") {
                    SectionHeader(
                        title = stringResource(R.string.overview_low_stock_section),
                        supporting = stringResource(R.string.overview_low_stock_supporting),
                    )
                }
                if (report.lowStock.isEmpty()) {
                    item(key = "low-stock-empty") {
                        EmptyPane(title = stringResource(R.string.overview_no_low_stock), body = "")
                    }
                } else {
                    items(report.lowStock, key = { "low-${it.productId}" }) { product ->
                        LowStockRow(
                            name = product.name,
                            packs = product.packsAvailable,
                            onClick = { onOpenProduct(product.productId) },
                        )
                    }
                }
                item(key = "view-inventory") {
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                        androidx.compose.material3.TextButton(onClick = onOpenStock) {
                            Text(stringResource(R.string.overview_view_inventory))
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun ReportStatTiles(
    report: DailyOverview,
    onOpenSales: () -> Unit,
    onOpenCustomers: () -> Unit,
    onOpenStock: () -> Unit,
) {
    BoxWithConstraints(Modifier.fillMaxWidth()) {
        if (maxWidth >= 612.dp) {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    ReportStatTile(report, 0, onOpenSales, onOpenCustomers, onOpenStock, Modifier.weight(1f))
                    ReportStatTile(report, 1, onOpenSales, onOpenCustomers, onOpenStock, Modifier.weight(1f))
                }
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    ReportStatTile(report, 2, onOpenSales, onOpenCustomers, onOpenStock, Modifier.weight(1f))
                    ReportStatTile(report, 3, onOpenSales, onOpenCustomers, onOpenStock, Modifier.weight(1f))
                }
            }
        } else {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                repeat(4) { index ->
                    ReportStatTile(report, index, onOpenSales, onOpenCustomers, onOpenStock, Modifier.fillMaxWidth())
                }
            }
        }
    }
}

@Composable
private fun ReportStatTile(
    report: DailyOverview,
    index: Int,
    onOpenSales: () -> Unit,
    onOpenCustomers: () -> Unit,
    onOpenStock: () -> Unit,
    modifier: Modifier,
) {
    when (index) {
        0 -> StatTile(
            label = stringResource(R.string.overview_sales_today),
            value = formatNaira(report.salesToday.total),
            supporting = pluralStringResource(R.plurals.overview_sales_count, report.salesToday.count, report.salesToday.count),
            containerColor = MaterialTheme.colorScheme.secondaryContainer,
            contentColor = MaterialTheme.colorScheme.onSecondaryContainer,
            onClick = onOpenSales,
            modifier = modifier,
        )
        1 -> StatTile(
            label = stringResource(R.string.overview_payments_received),
            value = formatNaira(report.paymentsReceived),
            supporting = stringResource(R.string.overview_payments_detail),
            containerColor = MaterialTheme.colorScheme.tertiaryContainer,
            contentColor = MaterialTheme.colorScheme.onTertiaryContainer,
            onClick = onOpenSales,
            modifier = modifier,
        )
        2 -> {
            val outstanding = positive(report.customerBalances)
            StatTile(
                label = stringResource(R.string.overview_customer_balances),
                value = formatNaira(report.customerBalances),
                supporting = stringResource(R.string.overview_balance_detail),
                containerColor = if (outstanding) MaterialTheme.colorScheme.errorContainer else MaterialTheme.colorScheme.surfaceContainerHigh,
                contentColor = if (outstanding) MaterialTheme.colorScheme.onErrorContainer else MaterialTheme.colorScheme.onSurface,
                onClick = onOpenCustomers,
                modifier = modifier,
            )
        }
        else -> {
            val low = report.lowStock.isNotEmpty()
            StatTile(
                label = stringResource(R.string.overview_low_stock_stat),
                value = report.lowStock.size.toString(),
                supporting = stringResource(R.string.overview_low_stock_detail),
                containerColor = if (low) MaterialTheme.colorScheme.errorContainer else MaterialTheme.colorScheme.tertiaryContainer,
                contentColor = if (low) MaterialTheme.colorScheme.onErrorContainer else MaterialTheme.colorScheme.onTertiaryContainer,
                onClick = onOpenStock,
                modifier = modifier,
            )
        }
    }
}

@Composable
private fun RecentSalesCard(
    sales: List<OverviewRecentSale>,
    onOpenSale: (String) -> Unit,
    onRefresh: () -> Unit,
    onOpenSales: () -> Unit,
) {
    TonalCard(Modifier.fillMaxWidth()) {
        Column(Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 12.dp)) {
            SectionHeader(
                title = stringResource(R.string.overview_recent_sales),
                supporting = stringResource(R.string.overview_recent_sales_supporting),
                action = {
                    androidx.compose.material3.FilledTonalIconButton(onClick = onRefresh) {
                        Icon(Icons.Rounded.Refresh, contentDescription = stringResource(R.string.action_refresh))
                    }
                },
            )
            if (sales.isEmpty()) {
                EmptyPane(title = stringResource(R.string.overview_no_sales), body = "")
            } else {
                sales.forEachIndexed { index, sale ->
                    if (index > 0) androidx.compose.material3.HorizontalDivider()
                    Row(
                        Modifier
                            .fillMaxWidth()
                            .clickable { onOpenSale(sale.id) }
                            .padding(vertical = 12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                    ) {
                        Column(Modifier.weight(1f)) {
                            Text(
                                sale.reference,
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Medium,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                            )
                            Text(
                                "${sale.customer?.name ?: stringResource(R.string.sales_walk_in)} · ${formatLagosDateTime(sale.soldAt)}",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                maxLines = 2,
                                overflow = TextOverflow.Ellipsis,
                            )
                        }
                        Column(horizontalAlignment = Alignment.End) {
                            Text(
                                formatNaira(sale.itemsTotal),
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.SemiBold,
                                maxLines = 1,
                                softWrap = false,
                            )
                            Text(
                                stringResource(R.string.overview_balance_amount, formatNaira(sale.balance)),
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                maxLines = 1,
                                softWrap = false,
                            )
                        }
                    }
                }
            }
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                androidx.compose.material3.TextButton(onClick = onOpenSales) {
                    Text(stringResource(R.string.overview_all_sales))
                }
            }
        }
    }
}

@Composable
private fun LowStockRow(name: String, packs: Int, onClick: () -> Unit) {
    val isEmpty = packs == 0
    TonalCard(onClick = onClick, modifier = Modifier.fillMaxWidth()) {
        Row(
            Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Text(
                name,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Medium,
                modifier = Modifier.weight(1f),
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
            )
            Surface(
                shape = MaterialTheme.shapes.small,
                color = if (isEmpty) MaterialTheme.colorScheme.errorContainer else MaterialTheme.colorScheme.tertiaryContainer,
                contentColor = if (isEmpty) MaterialTheme.colorScheme.onErrorContainer else MaterialTheme.colorScheme.onTertiaryContainer,
            ) {
                Text(
                    pluralStringResource(R.plurals.overview_pack_count, packs, packs),
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 1,
                    softWrap = false,
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                )
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
    val outstanding = positive(report.outstandingDebtAtMonthEnd)
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        StatTile(stringResource(R.string.overview_monthly_sales), formatNaira(report.salesTotal), "", Modifier.fillMaxWidth())
        StatTile(stringResource(R.string.overview_money_received), formatNaira(report.moneyReceived), "", Modifier.fillMaxWidth())
        StatTile(
            label = stringResource(R.string.overview_outstanding_debt),
            value = formatNaira(report.outstandingDebtAtMonthEnd),
            supporting = "",
            containerColor = if (outstanding) MaterialTheme.colorScheme.errorContainer else MaterialTheme.colorScheme.secondaryContainer,
            contentColor = if (outstanding) MaterialTheme.colorScheme.onErrorContainer else MaterialTheme.colorScheme.onSecondaryContainer,
            modifier = Modifier.fillMaxWidth(),
        )
        StatTile(stringResource(R.string.overview_products_sold), report.productsSold.size.toString(), "", Modifier.fillMaxWidth())
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
    val grossNegative = negative(report.grossProfit)
    val netNegative = negative(report.netProfit)
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        StatTile(stringResource(R.string.overview_revenue), formatNaira(report.revenue.orEmpty()), "", Modifier.fillMaxWidth())
        StatTile(stringResource(R.string.overview_cogs), formatNaira(report.cogs.orEmpty()), "", Modifier.fillMaxWidth())
        StatTile(
            label = stringResource(R.string.overview_gross_profit),
            value = formatNaira(report.grossProfit.orEmpty()),
            supporting = "",
            containerColor = if (grossNegative) MaterialTheme.colorScheme.errorContainer else MaterialTheme.colorScheme.tertiaryContainer,
            contentColor = if (grossNegative) MaterialTheme.colorScheme.onErrorContainer else MaterialTheme.colorScheme.onTertiaryContainer,
            modifier = Modifier.fillMaxWidth(),
        )
        StatTile(
            label = stringResource(R.string.overview_net_profit),
            value = formatNaira(report.netProfit.orEmpty()),
            supporting = "",
            containerColor = if (netNegative) MaterialTheme.colorScheme.errorContainer else MaterialTheme.colorScheme.primaryContainer,
            contentColor = if (netNegative) MaterialTheme.colorScheme.onErrorContainer else MaterialTheme.colorScheme.onPrimaryContainer,
            modifier = Modifier.fillMaxWidth(),
        )
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
            fontWeight = FontWeight.SemiBold,
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
                fontWeight = FontWeight.SemiBold,
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

private fun positive(value: String?): Boolean = (value?.toBigDecimalOrNull() ?: BigDecimal.ZERO) > BigDecimal.ZERO

private fun negative(value: String?): Boolean = (value?.toBigDecimalOrNull() ?: BigDecimal.ZERO) < BigDecimal.ZERO
