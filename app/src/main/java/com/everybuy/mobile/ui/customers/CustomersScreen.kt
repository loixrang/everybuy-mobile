package com.everybuy.mobile.ui.customers

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.everybuy.mobile.R
import com.everybuy.mobile.core.util.formatNaira
import com.everybuy.mobile.data.CustomerListItemDto
import com.everybuy.mobile.ui.components.EmptyPane
import com.everybuy.mobile.ui.components.ErrorPane
import com.everybuy.mobile.ui.components.EverybuyScreen
import com.everybuy.mobile.ui.components.ExpressiveSearchField
import com.everybuy.mobile.ui.components.LoadingPane
import com.everybuy.mobile.ui.components.SectionHeader
import com.everybuy.mobile.ui.components.StatTile
import com.everybuy.mobile.ui.components.TonalCard
import com.everybuy.mobile.ui.components.listBottomPadding
import java.math.BigDecimal

@OptIn(ExperimentalMaterial3Api::class, ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun CustomersScreen(
    userName: String,
    onOpenCustomer: (String) -> Unit,
    onAddCustomer: () -> Unit,
    onOpenSettings: () -> Unit,
    onSessionExpired: () -> Unit,
    viewModel: CustomersViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsState()
    LaunchedEffect(state) { if (state is CustomersState.SessionExpired) onSessionExpired() }

    EverybuyScreen(
        title = stringResource(R.string.customers_title),
        subtitle = userName,
        onOpenSettings = onOpenSettings,
        onRefresh = viewModel::refresh,
        floatingActionButton = {
            ExtendedFloatingActionButton(
                onClick = onAddCustomer,
                icon = { Icon(Icons.Rounded.Add, contentDescription = null) },
                text = { Text(stringResource(R.string.customers_add)) },
                containerColor = MaterialTheme.colorScheme.primaryContainer,
                contentColor = MaterialTheme.colorScheme.onPrimaryContainer,
            )
        },
    ) { padding ->
        when (val current = state) {
            CustomersState.Loading -> LoadingPane(
                message = stringResource(R.string.customers_loading),
                modifier = Modifier.padding(top = padding.calculateTopPadding()),
            )
            CustomersState.SessionExpired -> Text(
                text = stringResource(R.string.session_expired),
                modifier = Modifier.padding(padding).padding(24.dp),
            )
            is CustomersState.Failed -> ErrorPane(
                message = current.message,
                onRetry = { viewModel.refresh() },
                modifier = Modifier.padding(top = padding.calculateTopPadding()),
            )
            is CustomersState.Ready -> CustomersContent(
                state = current,
                viewModel = viewModel,
                onOpenCustomer = onOpenCustomer,
                padding = padding,
                modifier = Modifier.fillMaxSize(),
            )
        }
    }
}

@Composable
private fun CustomersContent(
    state: CustomersState.Ready,
    viewModel: CustomersViewModel,
    onOpenCustomer: (String) -> Unit,
    padding: PaddingValues,
    modifier: Modifier = Modifier,
) {
    val filtered = state.filtered
    val statusFilters = listOf(
        stringResource(R.string.customers_all) to STATUS_ALL,
        stringResource(R.string.customers_active) to STATUS_ACTIVE,
        stringResource(R.string.customers_inactive) to STATUS_INACTIVE,
    )
    LazyColumn(
        modifier = modifier,
        contentPadding = PaddingValues(
            start = 16.dp,
            end = 16.dp,
            top = padding.calculateTopPadding() + 4.dp,
            bottom = padding.calculateBottomPadding() + listBottomPadding(extra = 96.dp),
        ),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        item {
            ExpressiveSearchField(
                value = state.query,
                onValueChange = viewModel::setQuery,
                placeholder = stringResource(R.string.customers_search),
            )
        }
        item {
            LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                items(statusFilters, key = { it.second }) { (label, value) ->
                    FilterChip(
                        selected = state.status == value,
                        onClick = { viewModel.setStatus(value) },
                        label = { Text(label) },
                        leadingIcon = if (state.status == value) {
                            { Icon(Icons.Rounded.Check, contentDescription = null) }
                        } else null,
                    )
                }
            }
        }
        item { CustomerSummaryTiles(state) }
        item {
            SectionHeader(
                title = pluralStringResource(
                    R.plurals.customers_shown,
                    filtered.size,
                    filtered.size,
                    formatNaira(state.totalOutstanding.toPlainString()),
                ),
            )
        }
        if (filtered.isEmpty()) {
            item {
                EmptyPane(
                    title = if (state.customers.isEmpty()) {
                        stringResource(R.string.customers_empty)
                    } else {
                        stringResource(R.string.customers_no_matches)
                    },
                    body = if (state.customers.isEmpty()) {
                        stringResource(R.string.customers_empty_body)
                    } else {
                        stringResource(R.string.customers_no_matches_body)
                    },
                )
            }
        } else {
            items(filtered, key = { it.id }) { customer ->
                CustomerRowCard(
                    customer = customer,
                    onOpen = { onOpenCustomer(customer.id) },
                    modifier = Modifier.animateItem(),
                )
            }
        }
    }
}

@Composable
private fun CustomerRowCard(
    customer: CustomerListItemDto,
    onOpen: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val balance = customer.balance.toBigDecimalOrNull() ?: BigDecimal.ZERO
    TonalCard(modifier = modifier.fillMaxWidth(), onClick = onOpen) {
        Column(Modifier.fillMaxWidth().padding(18.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalAlignment = Alignment.Top,
            ) {
                Column(Modifier.weight(1f)) {
                    Text(
                        text = customer.name,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis,
                    )
                    Text(
                        text = customer.phone?.takeIf { it.isNotBlank() } ?: stringResource(R.string.customers_no_phone),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                CustomerStatusBadge(customer.active)
            }
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    stringResource(R.string.customers_statement),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Text(
                    text = if (balance > BigDecimal.ZERO) formatNaira(customer.balance) else stringResource(R.string.customers_no_balance),
                    style = MaterialTheme.typography.titleSmall,
                    color = if (balance > BigDecimal.ZERO) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurfaceVariant,
                    fontWeight = if (balance > BigDecimal.ZERO) FontWeight.SemiBold else FontWeight.Medium,
                    maxLines = 1,
                    softWrap = false,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        }
    }
}

@Composable
private fun CustomerSummaryTiles(state: CustomersState.Ready) {
    BoxWithConstraints(Modifier.fillMaxWidth()) {
        val activeTile: @Composable (Modifier) -> Unit = { tileModifier ->
            StatTile(
                label = stringResource(R.string.customers_active),
                value = stringResource(R.string.customers_active_count, state.activeCount, state.customers.size),
                supporting = stringResource(R.string.customers_title),
                modifier = tileModifier,
            )
        }
        val balanceTile: @Composable (Modifier) -> Unit = { tileModifier ->
            StatTile(
                label = stringResource(R.string.customers_owed),
                value = formatNaira(state.totalOutstanding.toPlainString()),
                supporting = stringResource(R.string.overview_balance_detail),
                modifier = tileModifier,
                containerColor = MaterialTheme.colorScheme.tertiaryContainer,
                contentColor = MaterialTheme.colorScheme.onTertiaryContainer,
            )
        }
        if (maxWidth >= 612.dp) {
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                activeTile(Modifier.weight(1f))
                balanceTile(Modifier.weight(1f))
            }
        } else {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                activeTile(Modifier.fillMaxWidth())
                balanceTile(Modifier.fillMaxWidth())
            }
        }
    }
}

@Composable
fun CustomerStatusBadge(active: Boolean) {
    val container = if (active) MaterialTheme.colorScheme.secondaryContainer else MaterialTheme.colorScheme.surfaceContainerHighest
    val content = if (active) MaterialTheme.colorScheme.onSecondaryContainer else MaterialTheme.colorScheme.onSurfaceVariant
    Surface(color = container, contentColor = content, shape = MaterialTheme.shapes.small) {
        Text(
            text = stringResource(if (active) R.string.customers_active else R.string.customers_inactive),
            style = MaterialTheme.typography.labelMedium,
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
        )
    }
}
