package com.everybuy.mobile.ui.sales

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
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
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import com.everybuy.mobile.R
import com.everybuy.mobile.core.util.formatLagosDateTime
import com.everybuy.mobile.core.util.formatNaira
import com.everybuy.mobile.data.SalesListRowDto
import com.everybuy.mobile.data.SalesSummaryDto
import com.everybuy.mobile.ui.components.EmptyPane
import com.everybuy.mobile.ui.components.ErrorPane
import com.everybuy.mobile.ui.components.EverybuyScreen
import com.everybuy.mobile.ui.components.ExpressiveSearchField
import com.everybuy.mobile.ui.components.LoadingPane
import com.everybuy.mobile.ui.components.StatTile
import com.everybuy.mobile.ui.components.TonalBadge
import com.everybuy.mobile.ui.components.TonalCard
import com.everybuy.mobile.ui.components.listBottomPadding
import com.everybuy.mobile.ui.components.rememberGlassStyle
import dev.chrisbanes.haze.hazeEffect

private val statusFilters = listOf(
    R.string.sales_filter_all to null,
    R.string.sales_filter_paid to "PAID",
    R.string.sales_filter_part_paid to "PARTIAL",
    R.string.sales_filter_unpaid to "UNPAID",
)

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun SalesScreen(
    userName: String,
    onOpenSale: (String) -> Unit,
    onRecordSale: () -> Unit,
    onOpenSettings: () -> Unit,
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
    EverybuyScreen(
        title = stringResource(R.string.sales_title),
        subtitle = userName,
        onOpenSettings = onOpenSettings,
        onRefresh = { viewModel.refresh() },
        floatingActionButton = {
            ExtendedFloatingActionButton(
                onClick = onRecordSale,
                icon = { Icon(Icons.Rounded.Add, contentDescription = null) },
                text = { Text(stringResource(R.string.sales_record_sale)) },
                containerColor = MaterialTheme.colorScheme.primaryContainer,
                contentColor = MaterialTheme.colorScheme.onPrimaryContainer,
            )
        },
    ) { padding ->
        when (val current = state) {
            SalesState.Loading -> LoadingPane(
                stringResource(R.string.sales_loading),
                Modifier.padding(top = padding.calculateTopPadding()),
            )
            SalesState.SessionExpired -> ErrorPane(
                stringResource(R.string.session_expired),
                null,
                Modifier.padding(top = padding.calculateTopPadding()),
            )
            is SalesState.Failed -> ErrorPane(
                current.message,
                { viewModel.refresh() },
                Modifier.padding(top = padding.calculateTopPadding()),
            )
            is SalesState.Ready -> {
                LazyVerticalGrid(
                    columns = GridCells.Adaptive(minSize = 340.dp),
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(
                        start = 16.dp,
                        end = 16.dp,
                        top = padding.calculateTopPadding(),
                        bottom = padding.calculateBottomPadding() + listBottomPadding(extra = 104.dp),
                    ),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    item(span = { GridItemSpan(maxLineSpan) }) {
                        SummaryCards(current.summary, current.summaryError)
                    }
                    item(span = { GridItemSpan(maxLineSpan) }) {
                        ExpressiveSearchField(
                            value = query,
                            onValueChange = { query = it },
                            placeholder = stringResource(R.string.sales_search),
                        )
                    }
                    item(span = { GridItemSpan(maxLineSpan) }) {
                        LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            items(statusFilters, key = { it.first }) { (labelRes, value) ->
                                val label = stringResource(labelRes)
                                FilterChip(
                                    selected = status == value,
                                    onClick = { status = value },
                                    label = { Text(label) },
                                    leadingIcon = if (status == value) {
                                        { Icon(Icons.Rounded.Check, contentDescription = null) }
                                    } else null,
                                )
                            }
                        }
                    }
                    item(span = { GridItemSpan(maxLineSpan) }) {
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                            androidx.compose.material3.OutlinedTextField(
                                value = from,
                                onValueChange = { from = it },
                                modifier = Modifier.weight(1f),
                                label = { Text(stringResource(R.string.sales_from)) },
                                placeholder = { Text(stringResource(R.string.sales_date_hint)) },
                                singleLine = true,
                                isError = dateError != null,
                                shape = MaterialTheme.shapes.large,
                            )
                            androidx.compose.material3.OutlinedTextField(
                                value = to,
                                onValueChange = { to = it },
                                modifier = Modifier.weight(1f),
                                label = { Text(stringResource(R.string.sales_to)) },
                                placeholder = { Text(stringResource(R.string.sales_date_hint)) },
                                singleLine = true,
                                isError = dateError != null,
                                shape = MaterialTheme.shapes.large,
                            )
                        }
                    }
                    dateError?.let { error ->
                        item(span = { GridItemSpan(maxLineSpan) }) {
                            Text(error, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall)
                        }
                    }
                    item(span = { GridItemSpan(maxLineSpan) }) {
                        Row(
                            Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.End),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            FilledTonalButton(onClick = {
                                query = ""
                                status = null
                                from = ""
                                to = ""
                                dateError = null
                                viewModel.applyFilters(SalesFilters())
                            }) { Text(stringResource(R.string.action_clear)) }
                            Button(onClick = {
                                dateError = validateDateFilters(from, to)
                                if (dateError == null) {
                                    viewModel.applyFilters(
                                        SalesFilters(
                                            query = query.trim(),
                                            paymentStatus = status,
                                            from = from.trim().ifBlank { null },
                                            to = to.trim().ifBlank { null },
                                        ),
                                    )
                                }
                            }) { Text(stringResource(R.string.action_apply_filters)) }
                        }
                    }
                    item(span = { GridItemSpan(maxLineSpan) }) {
                        Text(
                            text = pluralStringResource(R.plurals.sales_count, current.total, current.total),
                            style = MaterialTheme.typography.labelLarge,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                    if (current.rows.isEmpty()) {
                        item(span = { GridItemSpan(maxLineSpan) }) {
                            EmptyPane(
                                title = stringResource(R.string.sales_no_results),
                                body = stringResource(R.string.sales_no_results_body),
                            )
                        }
                    } else {
                        items(current.rows, key = { it.id }) { row ->
                            SaleRowCard(row) { onOpenSale(row.id) }
                        }
                        item(span = { GridItemSpan(maxLineSpan) }) {
                            Row(
                                Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically,
                            ) {
                                FilledTonalButton(
                                    onClick = { viewModel.loadPage(current.page - 1) },
                                    enabled = current.page > 1,
                                ) { Text(stringResource(R.string.action_previous)) }
                                Text(
                                    stringResource(R.string.sales_page, current.page, current.totalPages),
                                    style = MaterialTheme.typography.labelMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                )
                                FilledTonalButton(
                                    onClick = { viewModel.loadPage(current.page + 1) },
                                    enabled = current.page < current.totalPages,
                                ) { Text(stringResource(R.string.action_next)) }
                            }
                        }
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

@Composable
private fun SummaryCards(summary: SalesSummaryDto?, error: String?) {
    if (summary == null) {
        Text(
            text = error ?: "Sales totals are unavailable.",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        return
    }
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            StatTile(stringResource(R.string.sales_summary_sales), formatNaira(summary.salesTotal), "", Modifier.weight(1f))
            StatTile(stringResource(R.string.sales_summary_received), formatNaira(summary.amountPaid), "", Modifier.weight(1f))
        }
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            StatTile(
                label = stringResource(R.string.sales_summary_outstanding),
                value = formatNaira(summary.outstanding),
                supporting = "",
                modifier = Modifier.weight(1f),
                containerColor = if ((summary.outstanding.toBigDecimalOrNull() ?: java.math.BigDecimal.ZERO) > java.math.BigDecimal.ZERO) {
                    MaterialTheme.colorScheme.errorContainer
                } else {
                    MaterialTheme.colorScheme.secondaryContainer
                },
                contentColor = if ((summary.outstanding.toBigDecimalOrNull() ?: java.math.BigDecimal.ZERO) > java.math.BigDecimal.ZERO) {
                    MaterialTheme.colorScheme.onErrorContainer
                } else {
                    MaterialTheme.colorScheme.onSecondaryContainer
                },
            )
            StatTile(stringResource(R.string.sales_summary_count), summary.salesCount.toString(), "", Modifier.weight(1f))
        }
    }
}

@Composable
private fun SaleRowCard(row: SalesListRowDto, onOpen: () -> Unit) {
    TonalCard(Modifier.fillMaxWidth(), onClick = onOpen) {
        Column(Modifier.fillMaxWidth().padding(16.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.Top) {
                Column(Modifier.weight(1f)) {
                    Text(
                        row.reference.ifBlank { "Sale" },
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                    Text(
                        row.customer?.name ?: stringResource(R.string.sales_walk_in),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                PaymentStatusBadge(row.status)
            }
            Text(
                formatLagosDateTime(row.createdAt),
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text(stringResource(R.string.sales_total, formatNaira(row.itemsTotal)), style = MaterialTheme.typography.bodyMedium)
                Text(
                    stringResource(R.string.sales_balance, formatNaira(row.balance)),
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Medium,
                )
            }
        }
    }
}

@Composable
fun PaymentStatusBadge(status: String) {
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
    TonalBadge(
        text = paymentStatusLabel(normalised),
        containerColor = container,
        contentColor = content,
    )
}
