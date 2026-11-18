package com.everybuy.mobile.ui.inventory

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
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
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
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
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import com.everybuy.mobile.R
import com.everybuy.mobile.core.util.formatNaira
import com.everybuy.mobile.data.AuthUser
import com.everybuy.mobile.data.ProductDto
import com.everybuy.mobile.ui.components.EmptyPane
import com.everybuy.mobile.ui.components.ErrorPane
import com.everybuy.mobile.ui.components.EverybuyScreen
import com.everybuy.mobile.ui.components.ExpressiveSearchField
import com.everybuy.mobile.ui.components.LoadingPane
import com.everybuy.mobile.ui.components.SectionHeader
import com.everybuy.mobile.ui.components.TonalBadge
import com.everybuy.mobile.ui.components.TonalCard
import com.everybuy.mobile.ui.components.listBottomPadding

private const val LOW_STOCK_THRESHOLD = 10

@OptIn(ExperimentalMaterial3Api::class, ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun InventoryScreen(
    user: AuthUser,
    isOwner: Boolean,
    onOpenSettings: () -> Unit,
    onSessionExpired: () -> Unit,
    onProductSelected: (String) -> Unit,
    onAddProduct: () -> Unit,
    viewModel: InventoryViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsState()
    LaunchedEffect(state) { if (state is InventoryState.Unauthorized) onSessionExpired() }
    var search by rememberSaveable { mutableStateOf("") }
    var categoryFilter by rememberSaveable { mutableStateOf("ALL") }

    EverybuyScreen(
        title = stringResource(R.string.inventory_title),
        subtitle = user.name.ifBlank { user.email },
        onOpenSettings = onOpenSettings,
        compactHeader = true,
        floatingActionButton = if (isOwner) {
            {
                ExtendedFloatingActionButton(
                    onClick = onAddProduct,
                    icon = { Icon(Icons.Rounded.Add, contentDescription = null) },
                    text = { Text(stringResource(R.string.inventory_add_product)) },
                    containerColor = MaterialTheme.colorScheme.primaryContainer,
                    contentColor = MaterialTheme.colorScheme.onPrimaryContainer,
                )
            }
        } else null,
    ) { padding ->
        when (val current = state) {
            InventoryState.Loading -> LoadingPane(
                message = stringResource(R.string.inventory_loading),
                modifier = Modifier.padding(top = padding.calculateTopPadding()),
            )
            is InventoryState.Failed -> ErrorPane(
                message = current.message,
                onRetry = { viewModel.refresh() },
                modifier = Modifier.padding(top = padding.calculateTopPadding()),
            )
            InventoryState.Unauthorized -> Text(
                text = stringResource(R.string.session_expired),
                modifier = Modifier.padding(padding).padding(24.dp),
            )
            is InventoryState.Ready -> {
                val inventory = current.inventory
                val categoryNames = inventory.categories.associate { it.id to it.name }
                val brandNames = inventory.brands.associate { it.id to it.name }
                val filtered = inventory.products.filter { product ->
                    (categoryFilter == "ALL" || product.categoryId == categoryFilter) &&
                        "${product.name} ${product.packName} ${product.size.orEmpty()} ${categoryNames[product.categoryId].orEmpty()} ${brandNames[product.brandId].orEmpty()}"
                            .contains(search.trim(), ignoreCase = true)
                }
                LazyVerticalGrid(
                    columns = GridCells.Adaptive(minSize = 320.dp),
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(
                        start = 16.dp,
                        end = 16.dp,
                        top = padding.calculateTopPadding() + 4.dp,
                        bottom = padding.calculateBottomPadding() + listBottomPadding(
                            extra = 16.dp,
                        ),
                    ),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    item(span = { GridItemSpan(maxLineSpan) }) {
                        ExpressiveSearchField(
                            value = search,
                            onValueChange = { search = it },
                            placeholder = stringResource(R.string.inventory_search),
                        )
                    }
                    if (inventory.categories.isNotEmpty()) {
                        item(span = { GridItemSpan(maxLineSpan) }) {
                            LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                item {
                                    FilterChip(
                                        selected = categoryFilter == "ALL",
                                        onClick = { categoryFilter = "ALL" },
                                        label = { Text(stringResource(R.string.inventory_all_categories)) },
                                        leadingIcon = if (categoryFilter == "ALL") {
                                            { Icon(Icons.Rounded.Check, contentDescription = null) }
                                        } else null,
                                    )
                                }
                                items(inventory.categories, key = { it.id }) { category ->
                                    FilterChip(
                                        selected = categoryFilter == category.id,
                                        onClick = { categoryFilter = category.id },
                                        label = { Text(category.name) },
                                        leadingIcon = if (categoryFilter == category.id) {
                                            { Icon(Icons.Rounded.Check, contentDescription = null) }
                                        } else null,
                                    )
                                }
                            }
                        }
                    }
                    item(span = { GridItemSpan(maxLineSpan) }) {
                        SectionHeader(
                            title = pluralStringResource(R.plurals.inventory_products_count, filtered.size, filtered.size),
                            supporting = if (search.isBlank() && categoryFilter == "ALL") {
                                stringResource(R.string.inventory_subtitle)
                            } else null,
                        )
                    }
                    if (filtered.isEmpty()) {
                        item(span = { GridItemSpan(maxLineSpan) }) {
                            EmptyPane(
                                title = if (search.isBlank()) stringResource(R.string.inventory_empty) else stringResource(R.string.inventory_no_matches),
                                body = stringResource(R.string.inventory_search),
                            )
                        }
                    } else {
                        items(filtered, key = { it.id }) { product ->
                            ProductCard(
                                product = product,
                                brandName = brandNames[product.brandId],
                                categoryName = categoryNames[product.categoryId],
                                onClick = { onProductSelected(product.id) },
                                modifier = Modifier.animateItem(),
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun ProductCard(
    product: ProductDto,
    brandName: String?,
    categoryName: String?,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val lowStock = product.stockPacks <= LOW_STOCK_THRESHOLD
    val metadata = listOfNotNull(
        brandName,
        categoryName,
        product.packName.takeIf { it.isNotBlank() },
        product.size?.takeIf { it.isNotBlank() },
    ).joinToString(" · ")
    TonalCard(
        modifier = modifier.fillMaxWidth(),
        containerColor = MaterialTheme.colorScheme.surfaceContainerLow,
        onClick = onClick,
    ) {
        Column(Modifier.fillMaxWidth().padding(18.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                Column(Modifier.weight(1f)) {
                    Text(
                        text = product.name,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis,
                    )
                    if (metadata.isNotBlank()) {
                        Text(
                            text = metadata,
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis,
                        )
                    }
                }
                TonalBadge(
                    text = stringResource(R.string.inventory_packs_in_stock).let { label -> "${product.stockPacks} $label" },
                    containerColor = if (lowStock) MaterialTheme.colorScheme.errorContainer else MaterialTheme.colorScheme.secondaryContainer,
                    contentColor = if (lowStock) MaterialTheme.colorScheme.onErrorContainer else MaterialTheme.colorScheme.onSecondaryContainer,
                )
            }
            Spacer(Modifier.height(2.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = stringResource(R.string.inventory_price_per_bag, formatNaira(product.bagPrice)),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                )
                if (lowStock) {
                    TonalBadge(
                        text = stringResource(R.string.inventory_low_stock),
                        containerColor = MaterialTheme.colorScheme.errorContainer,
                        contentColor = MaterialTheme.colorScheme.onErrorContainer,
                    )
                }
            }
        }
    }
}
