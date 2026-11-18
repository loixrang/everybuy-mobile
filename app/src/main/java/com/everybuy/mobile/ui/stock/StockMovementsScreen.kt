package com.everybuy.mobile.ui.stock

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material3.Button
import com.everybuy.mobile.ui.components.ExpressiveLoadingIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SheetValue
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import com.everybuy.mobile.R
import com.everybuy.mobile.core.util.formatLagosDateTime
import com.everybuy.mobile.core.util.matchesSearch
import com.everybuy.mobile.data.ProductDto
import com.everybuy.mobile.data.StockMovementDto
import com.everybuy.mobile.ui.components.EmptyPane
import com.everybuy.mobile.ui.components.ErrorPane
import com.everybuy.mobile.ui.components.EverybuyScreen
import com.everybuy.mobile.ui.components.ExpressiveSearchField
import com.everybuy.mobile.ui.components.LoadingPane
import com.everybuy.mobile.ui.components.SectionHeader
import com.everybuy.mobile.ui.components.TonalBadge
import com.everybuy.mobile.ui.components.TonalCard
import com.everybuy.mobile.ui.components.GlassModalBottomSheet
import com.everybuy.mobile.ui.components.SheetSelect
import com.everybuy.mobile.ui.components.SheetSelectOption
import kotlinx.coroutines.launch

private data class MovementFilter(val labelRes: Int, val value: String)

private val movementTypes = listOf(
    MovementFilter(R.string.history_filter_all, "ALL"),
    MovementFilter(R.string.history_filter_opening, "Opening"),
    MovementFilter(R.string.history_filter_receipts, "Receipt"),
    MovementFilter(R.string.history_filter_sales, "Sale"),
    MovementFilter(R.string.history_filter_adjustments, "Adjustment"),
)

@OptIn(ExperimentalMaterial3Api::class, ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun StockMovementsScreen(
    isOwner: Boolean,
    onBack: () -> Unit,
    onOpenSettings: () -> Unit,
    onSessionExpired: () -> Unit,
    onProductSelected: (String) -> Unit,
    onMovementSelected: (String) -> Unit,
    initialProductId: String? = null,
    viewModel: StockMovementsViewModel = hiltViewModel(),
    adjustmentViewModel: StockAdjustmentViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsState()
    var query by rememberSaveable { mutableStateOf("") }
    var type by rememberSaveable { mutableStateOf("ALL") }
    var productFilter by rememberSaveable(initialProductId) {
        mutableStateOf(initialProductId?.takeIf { it.isNotBlank() } ?: "ALL")
    }
    var adjustmentOpen by remember { mutableStateOf(false) }
    var adjustmentProductId by rememberSaveable { mutableStateOf("") }
    val adjustmentState by adjustmentViewModel.state.collectAsState()
    LaunchedEffect(state) { if (state is StockMovementsState.SessionExpired) onSessionExpired() }
    LaunchedEffect(adjustmentState) {
        when (adjustmentState) {
            StockAdjustmentState.SessionExpired -> onSessionExpired()
            StockAdjustmentState.Saved -> {
                adjustmentOpen = false
                adjustmentViewModel.reset()
                viewModel.refresh()
            }
            else -> Unit
        }
    }

    EverybuyScreen(
        title = stringResource(R.string.history_title),
        subtitle = stringResource(R.string.history_subtitle),
        onOpenSettings = onOpenSettings,
        onBack = onBack,
        onRefresh = viewModel::refresh,
    ) { padding ->
        when (val current = state) {
            StockMovementsState.Loading -> LoadingPane(
                message = stringResource(R.string.history_loading),
                modifier = Modifier.padding(top = padding.calculateTopPadding()),
            )
            StockMovementsState.SessionExpired -> Text(
                text = stringResource(R.string.session_expired),
                modifier = Modifier.padding(padding).padding(24.dp),
            )
            is StockMovementsState.Failed -> ErrorPane(
                message = current.message,
                onRetry = { viewModel.refresh() },
                modifier = Modifier.padding(top = padding.calculateTopPadding()),
            )
            is StockMovementsState.Ready -> {
                val products = current.products.associateBy { it.id }
                val filtered by remember(current.movements, current.products, query, type, productFilter) {
                    derivedStateOf {
                        current.movements.filter { movement ->
                            val product = products[movement.productId]
                            matchesSearch(
                                listOf(
                                    movement.id,
                                    movement.productId,
                                    product?.name.orEmpty(),
                                    product?.size.orEmpty(),
                                    product?.packName.orEmpty(),
                                    product?.categoryId.orEmpty(),
                                    product?.brandId.orEmpty(),
                                    movement.type,
                                    movement.changePacks.toString(),
                                    movement.supplierId?.toString().orEmpty(),
                                    movement.saleItemId.orEmpty(),
                                    movement.requestId.orEmpty(),
                                    movement.costPerPack.orEmpty(),
                                    movement.note.orEmpty(),
                                    movement.createdBy,
                                    movement.createdAt,
                                ).joinToString(" "),
                                query,
                            ) &&
                                (type == "ALL" || movement.type == type) &&
                                (productFilter == "ALL" || movement.productId == productFilter)
                        }
                    }
                }
                Column(
                    Modifier
                        .fillMaxSize()
                        .padding(top = padding.calculateTopPadding()),
                ) {
                    if (isOwner) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 16.dp, vertical = 4.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                        ) {
                            Box(Modifier.weight(1f)) {
                                SheetSelect(
                                    label = stringResource(R.string.history_choose_product),
                                    placeholder = stringResource(R.string.history_choose_product),
                                    options = current.products.map { product ->
                                        SheetSelectOption(product.id, product.id, product.name)
                                    },
                                    selected = adjustmentProductId.takeIf(String::isNotBlank),
                                    onSelect = { productId -> adjustmentProductId = productId.orEmpty() },
                                    searchable = current.products.size >= 8,
                                )
                            }
                            Button(
                                onClick = {
                                    adjustmentViewModel.reset()
                                    adjustmentOpen = true
                                },
                                enabled = adjustmentProductId.isNotBlank(),
                            ) { Text(stringResource(R.string.history_adjust_stock)) }
                        }
                    }
                    ExpressiveSearchField(
                        value = query,
                        onValueChange = {
                            query = it
                        },
                        placeholder = stringResource(R.string.history_search),
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp),
                    )
                    LazyRow(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        contentPadding = PaddingValues(horizontal = 16.dp),
                        modifier = Modifier.padding(vertical = 8.dp),
                    ) {
                        items(movementTypes, key = { it.value }) { option ->
                            FilterChip(
                                selected = type == option.value,
                                onClick = { type = option.value },
                                label = { Text(stringResource(option.labelRes)) },
                                leadingIcon = if (type == option.value) {
                                    { Icon(Icons.Rounded.Check, contentDescription = null) }
                                } else null,
                            )
                        }
                    }
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        SheetSelect(
                            label = stringResource(R.string.history_all_products),
                            placeholder = stringResource(R.string.history_all_products),
                            options = listOf(
                                SheetSelectOption("all", "ALL", stringResource(R.string.history_all_products)),
                            ) + current.products.map { product ->
                                SheetSelectOption(product.id, product.id, product.name)
                            },
                            selected = productFilter,
                            onSelect = { productFilter = it ?: "ALL" },
                            searchable = current.products.size + 1 >= 8,
                        )
                        Text(
                            text = pluralStringResource(R.plurals.history_count, filtered.size, filtered.size),
                            style = MaterialTheme.typography.labelLarge,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                    if (filtered.isEmpty()) {
                        EmptyPane(
                            title = if (query.isBlank() && type == "ALL" && productFilter == "ALL") {
                                stringResource(R.string.history_empty)
                            } else {
                                stringResource(R.string.history_no_matches)
                            },
                            body = stringResource(R.string.history_search),
                        )
                    } else {
                        LazyColumn(
                            modifier = Modifier.weight(1f),
                            contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 8.dp, bottom = 24.dp),
                            verticalArrangement = Arrangement.spacedBy(10.dp),
                        ) {
                            items(filtered, key = { it.id }) { movement ->
                                MovementCard(
                                    item = movement,
                                    productName = products[movement.productId]?.name
                                        ?: stringResource(R.string.history_unknown_product),
                                    onProduct = { onProductSelected(movement.productId) },
                                    onOpen = { onMovementSelected(movement.id) },
                                    modifier = Modifier.animateItem(),
                                )
                            }
                            if (current.movements.size < current.total) {
                                item {
                                    Column(
                                        horizontalAlignment = Alignment.CenterHorizontally,
                                        modifier = Modifier.fillMaxWidth().padding(vertical = 12.dp),
                                    ) {
                                        current.moreError?.let {
                                            Text(it, color = MaterialTheme.colorScheme.error)
                                        }
                                        Button(
                                            onClick = { viewModel.loadMore() },
                                            enabled = !current.loadingMore,
                                        ) {
                                            if (current.loadingMore) {
                                                ExpressiveLoadingIndicator(Modifier.height(16.dp))
                                            } else {
                                                Text(stringResource(R.string.history_load_more))
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
                if (adjustmentOpen && isOwner) {
                    val product = current.products.firstOrNull { it.id == adjustmentProductId }
                    if (product != null) {
                        StockAdjustmentSheet(
                            product = product,
                            state = adjustmentState,
                            viewModel = adjustmentViewModel,
                            onDismiss = { adjustmentOpen = false },
                        )
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun StockAdjustmentSheet(
    product: ProductDto,
    state: StockAdjustmentState,
    viewModel: StockAdjustmentViewModel,
    onDismiss: () -> Unit,
) {
    var direction by rememberSaveable(product.id) { mutableStateOf("Add") }
    var quantity by rememberSaveable(product.id) { mutableStateOf("") }
    var note by rememberSaveable(product.id) { mutableStateOf("") }
    val sheetState = rememberBottomSheetState(
        initialValue = SheetValue.Hidden,
        confirmValueChange = { state !is StockAdjustmentState.Saving || it != SheetValue.Hidden },
    )
    val scope = rememberCoroutineScope()
    val closeSheet: () -> Unit = {
        scope.launch {
            sheetState.hide()
            if (!sheetState.isVisible) onDismiss()
        }
    }
    GlassModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
    ) {
        Column(
            Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp)
                .padding(bottom = 28.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Text(
                text = stringResource(R.string.history_adjust_stock),
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.SemiBold,
            )
            Text(
                text = pluralStringResource(
                    R.plurals.history_product_available,
                    product.stockPacks,
                    product.name,
                    product.stockPacks,
                ),
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                FilterChip(
                    selected = direction == "Add",
                    onClick = { direction = "Add" },
                    label = { Text(stringResource(R.string.history_adjust_add)) },
                )
                FilterChip(
                    selected = direction == "Sub",
                    onClick = { direction = "Sub" },
                    label = { Text(stringResource(R.string.history_adjust_remove)) },
                )
            }
            OutlinedTextField(
                value = quantity,
                onValueChange = { quantity = it },
                modifier = Modifier.fillMaxWidth(),
                label = { Text(stringResource(R.string.history_adjust_quantity)) },
                singleLine = true,
                shape = MaterialTheme.shapes.large,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
            )
            OutlinedTextField(
                value = note,
                onValueChange = { note = it.take(500) },
                modifier = Modifier.fillMaxWidth(),
                label = { Text(stringResource(R.string.history_adjust_reason)) },
                supportingText = {
                    Text(stringResource(R.string.history_adjust_note_support, note.length, 500))
                },
                minLines = 3,
                maxLines = 5,
                shape = MaterialTheme.shapes.large,
            )
            val count = quantity.toIntOrNull()
            if (count != null && count > 0) {
                val projected = product.stockPacks.toLong() +
                    if (direction == "Add") count.toLong() else -count.toLong()
                val projectedQuantity = projected
                    .coerceAtLeast(0L)
                    .coerceAtMost(Int.MAX_VALUE.toLong())
                    .toInt()
                Text(
                    text = pluralStringResource(
                        R.plurals.history_adjust_projected,
                        projectedQuantity,
                        projected,
                    ),
                    color = if (projected < 0) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            if (state is StockAdjustmentState.Failed) {
                Text(state.message, color = MaterialTheme.colorScheme.error)
            }
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                TextButton(
                    onClick = closeSheet,
                    enabled = state !is StockAdjustmentState.Saving,
                    modifier = Modifier.weight(1f),
                ) { Text(stringResource(R.string.action_cancel)) }
                Button(
                    onClick = { viewModel.submit(product, quantity, direction, note) },
                    enabled = state !is StockAdjustmentState.Saving,
                    modifier = Modifier.weight(1f),
                ) {
                    if (state is StockAdjustmentState.Saving) {
                        ExpressiveLoadingIndicator(Modifier.height(18.dp))
                    } else {
                        Text(stringResource(R.string.history_adjust_save))
                    }
                }
            }
        }
    }
}

@Composable
private fun MovementCard(
    item: StockMovementDto,
    productName: String,
    onProduct: () -> Unit,
    onOpen: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val positive = item.changePacks > 0
    val title = when (item.type) {
        "Sale" -> stringResource(R.string.history_type_sale)
        "Receipt" -> stringResource(R.string.history_type_receipt)
        "Opening" -> stringResource(R.string.history_type_opening)
        "Adjustment" -> if (item.note?.contains("stocktake", ignoreCase = true) == true) {
            stringResource(R.string.history_type_stocktake)
        } else {
            stringResource(R.string.history_type_adjustment)
        }
        else -> item.type
    }
    TonalCard(modifier = modifier.fillMaxWidth(), onClick = onOpen) {
        Column(
            Modifier.fillMaxWidth().padding(18.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = formatLagosDateTime(item.createdAt),
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.weight(1f),
                )
                TonalBadge(
                    text = "${if (positive) "+" else ""}${item.changePacks} packs",
                    containerColor = if (positive) {
                        MaterialTheme.colorScheme.primaryContainer
                    } else {
                        MaterialTheme.colorScheme.surfaceContainerHighest
                    },
                    contentColor = if (positive) {
                        MaterialTheme.colorScheme.onPrimaryContainer
                    } else {
                        MaterialTheme.colorScheme.onSurfaceVariant
                    },
                )
            }
            TextButton(onClick = onProduct, modifier = Modifier.align(Alignment.Start)) {
                Text(productName)
            }
            Text(title, style = MaterialTheme.typography.bodyMedium)
            val ref = item.note
                ?.takeIf { it.startsWith("Ref:") || it.startsWith("ST-") || it.startsWith("RC-") || it.startsWith("SL-") }
                ?.removePrefix("Ref: ")
                ?: if (item.type == "Sale" && item.saleItemId != null) {
                    "SL-${item.saleItemId.take(6).uppercase()}"
                } else {
                    item.requestId?.let { "RC-${it.take(6).uppercase()}" }
                }
            if (!ref.isNullOrBlank()) {
                Text(ref, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
    }
}
