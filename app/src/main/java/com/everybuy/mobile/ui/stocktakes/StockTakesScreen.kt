package com.everybuy.mobile.ui.stocktakes

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.LoadingIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
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
import androidx.hilt.navigation.compose.hiltViewModel
import com.everybuy.mobile.R
import com.everybuy.mobile.core.util.formatLagosDateTime
import com.everybuy.mobile.data.StockTakeDto
import com.everybuy.mobile.ui.components.EmptyPane
import com.everybuy.mobile.ui.components.ErrorPane
import com.everybuy.mobile.ui.components.EverybuyScreen
import com.everybuy.mobile.ui.components.LoadingPane
import com.everybuy.mobile.ui.components.TonalCard
import com.everybuy.mobile.ui.components.listBottomPadding

@OptIn(ExperimentalMaterial3Api::class, ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun StockTakesScreen(
    onOpenStockTake: (String) -> Unit,
    onBack: () -> Unit,
    onOpenSettings: () -> Unit,
    onSessionExpired: () -> Unit,
    viewModel: StockTakesViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsState()
    var createOpen by rememberSaveable { mutableStateOf(false) }
    LaunchedEffect(state) { if (state is StockTakesState.SessionExpired) onSessionExpired() }
    LaunchedEffect((state as? StockTakesState.Ready)?.createdId) {
        val createdId = (state as? StockTakesState.Ready)?.createdId
        if (createdId != null) {
            createOpen = false
            viewModel.clearCreated()
            onOpenStockTake(createdId)
        }
    }

    EverybuyScreen(
        title = stringResource(R.string.counts_title),
        subtitle = stringResource(R.string.counts_subtitle),
        onOpenSettings = onOpenSettings,
        onBack = onBack,
        onRefresh = viewModel::refresh,
        floatingActionButton = {
            ExtendedFloatingActionButton(
                onClick = { createOpen = true },
                icon = { Icon(Icons.Rounded.Add, contentDescription = null) },
                text = { Text(stringResource(R.string.counts_new)) },
                containerColor = MaterialTheme.colorScheme.primaryContainer,
                contentColor = MaterialTheme.colorScheme.onPrimaryContainer,
            )
        },
    ) { padding ->
        when (val current = state) {
            StockTakesState.Loading -> LoadingPane(
                message = stringResource(R.string.counts_loading),
                modifier = Modifier.padding(top = padding.calculateTopPadding()),
            )
            StockTakesState.SessionExpired -> Text(
                text = stringResource(R.string.session_expired),
                modifier = Modifier.padding(padding).padding(24.dp),
            )
            is StockTakesState.Failed -> ErrorPane(
                message = current.message,
                onRetry = { viewModel.refresh() },
                modifier = Modifier.padding(top = padding.calculateTopPadding()),
            )
            is StockTakesState.Ready -> StockTakeList(
                state = current,
                onOpen = onOpenStockTake,
                onPage = { viewModel.load(it) },
                onStart = { createOpen = true },
                padding = padding,
                modifier = Modifier.fillMaxSize(),
            )
        }
    }

    if (createOpen) {
        val current = state as? StockTakesState.Ready
        CreateStockTakeDialog(
            creating = current?.creating == true,
            error = current?.createError,
            onDismiss = {
                if (current?.creating != true) {
                    createOpen = false
                    viewModel.clearCreateError()
                }
            },
            onNameChanged = { viewModel.clearCreateError() },
            onCreate = { viewModel.create(it) },
        )
    }
}

@Composable
private fun StockTakeList(
    state: StockTakesState.Ready,
    onOpen: (String) -> Unit,
    onPage: (Int) -> Unit,
    onStart: () -> Unit,
    padding: PaddingValues,
    modifier: Modifier = Modifier,
) {
    val totalPages = maxOf(1, (state.total + STOCK_TAKE_PAGE_SIZE - 1) / STOCK_TAKE_PAGE_SIZE)
    LazyVerticalGrid(
        columns = GridCells.Adaptive(minSize = 320.dp),
        modifier = modifier,
        contentPadding = PaddingValues(
            start = 16.dp,
            end = 16.dp,
            top = padding.calculateTopPadding() + 4.dp,
            bottom = padding.calculateBottomPadding() + listBottomPadding(extra = 104.dp),
        ),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        if (state.stockTakes.isEmpty()) {
            item(span = { GridItemSpan(maxLineSpan) }) {
                EmptyPane(
                    title = stringResource(R.string.counts_empty),
                    body = stringResource(R.string.counts_empty_body),
                    action = {
                        Button(onClick = onStart) { Text(stringResource(R.string.counts_new)) }
                    },
                )
            }
        } else {
            items(state.stockTakes, key = { it.id }) { stockTake ->
                StockTakeCard(
                    stockTake = stockTake,
                    onOpen = { onOpen(stockTake.id) },
                    modifier = Modifier.animateItem(),
                )
            }
            item(span = { GridItemSpan(maxLineSpan) }) {
                Row(
                    modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    TextButton(
                        onClick = { onPage(state.page - 1) },
                        enabled = state.page > 1,
                    ) { Text(stringResource(R.string.action_previous)) }
                    Text(
                        text = pluralStringResource(
                            R.plurals.counts_total,
                            state.total,
                            state.page,
                            totalPages,
                            state.total,
                        ),
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    TextButton(
                        onClick = { onPage(state.page + 1) },
                        enabled = state.page < totalPages,
                    ) { Text(stringResource(R.string.action_next)) }
                }
            }
        }
    }
}

@Composable
private fun StockTakeCard(
    stockTake: StockTakeDto,
    onOpen: () -> Unit,
    modifier: Modifier = Modifier,
) {
    TonalCard(modifier = modifier.fillMaxWidth(), onClick = onOpen) {
        Column(
            Modifier.fillMaxWidth().padding(18.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalAlignment = Alignment.Top,
            ) {
                Column(Modifier.weight(1f)) {
                    Text(
                        text = stockTake.name,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis,
                    )
                    Text(
                        text = stockTake.reference,
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                StockTakeStatusBadge(stockTake.status)
            }
            Text(
                text = stockTake.createdAt?.let {
                    stringResource(R.string.counts_started, formatLagosDateTime(it))
                } ?: stringResource(R.string.counts_start_unavailable),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
private fun CreateStockTakeDialog(
    creating: Boolean,
    error: String?,
    onDismiss: () -> Unit,
    onNameChanged: () -> Unit,
    onCreate: (String) -> Unit,
) {
    var name by rememberSaveable { mutableStateOf("") }
    var localError by remember { mutableStateOf<String?>(null) }
    AlertDialog(
        onDismissRequest = { if (!creating) onDismiss() },
        shape = MaterialTheme.shapes.extraLarge,
        containerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
        title = { Text(stringResource(R.string.counts_new)) },
        text = {
            Column(
                modifier = Modifier.verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                Text(
                    text = stringResource(R.string.counts_dialog_body),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                OutlinedTextField(
                    value = name,
                    onValueChange = { value ->
                        name = value.take(MAX_STOCK_TAKE_NAME_LENGTH)
                        localError = null
                        onNameChanged()
                    },
                    modifier = Modifier.fillMaxWidth(),
                    label = { Text(stringResource(R.string.counts_name)) },
                    placeholder = { Text(stringResource(R.string.counts_name_placeholder)) },
                    singleLine = true,
                    shape = MaterialTheme.shapes.large,
                    isError = localError != null || error != null,
                    supportingText = {
                        Text(
                            localError ?: error ?: stringResource(
                                R.string.counts_name_counter,
                                name.length,
                                MAX_STOCK_TAKE_NAME_LENGTH,
                            ),
                        )
                    },
                )
            }
        },
        confirmButton = {
            Button(
                enabled = !creating,
                onClick = {
                    val message = validateStockTakeName(name)
                    if (message != null) {
                        localError = message
                    } else {
                        localError = null
                        onCreate(name)
                    }
                },
            ) {
                if (creating) {
                    LoadingIndicator(modifier = Modifier.padding(2.dp))
                } else {
                    Text(stringResource(R.string.counts_new))
                }
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss, enabled = !creating) {
                Text(stringResource(R.string.action_cancel))
            }
        },
    )
}
