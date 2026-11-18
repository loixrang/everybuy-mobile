package com.everybuy.mobile.ui.theme

import android.content.res.Configuration
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Assessment
import androidx.compose.material.icons.rounded.Inventory2
import androidx.compose.material.icons.rounded.People
import androidx.compose.material.icons.rounded.ShoppingBag
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Text
import androidx.compose.material3.WideNavigationRail
import androidx.compose.material3.WideNavigationRailItem
import androidx.compose.material3.WideNavigationRailValue
import androidx.compose.material3.rememberWideNavigationRailState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.foundation.layout.WindowInsets
import com.everybuy.mobile.R
import com.everybuy.mobile.ui.components.EverybuyScreen
import com.everybuy.mobile.ui.components.ExpressiveSearchField
import com.everybuy.mobile.ui.components.LocalBottomChromeHeight
import com.everybuy.mobile.ui.components.LocalNavigationRailVisible
import com.everybuy.mobile.ui.components.SectionHeader
import com.everybuy.mobile.ui.components.StatTile
import com.everybuy.mobile.ui.components.TonalBadge
import com.everybuy.mobile.ui.components.TonalCard

private data class PreviewNavItem(
    val labelRes: Int,
    val icon: androidx.compose.ui.graphics.vector.ImageVector,
)

private val previewNavItems = listOf(
    PreviewNavItem(R.string.nav_stock, Icons.Rounded.Inventory2),
    PreviewNavItem(R.string.nav_sales, Icons.Rounded.ShoppingBag),
    PreviewNavItem(R.string.nav_customers, Icons.Rounded.People),
    PreviewNavItem(R.string.nav_overview, Icons.Rounded.Assessment),
)

@Preview(name = "Compact light", widthDp = 400, heightDp = 880, showBackground = true)
@Composable
private fun CompactLightPreview() {
    PreviewApp(dark = false, rail = false, expandedRail = false)
}

@Preview(
    name = "Compact dark",
    widthDp = 400,
    heightDp = 880,
    showBackground = true,
    uiMode = Configuration.UI_MODE_NIGHT_YES,
)
@Composable
private fun CompactDarkPreview() {
    PreviewApp(dark = true, rail = false, expandedRail = false)
}

@Preview(name = "Medium light", widthDp = 800, heightDp = 1000, showBackground = true)
@Composable
private fun MediumLightPreview() {
    PreviewApp(dark = false, rail = true, expandedRail = false)
}

@Preview(
    name = "Medium dark",
    widthDp = 800,
    heightDp = 1000,
    showBackground = true,
    uiMode = Configuration.UI_MODE_NIGHT_YES,
)
@Composable
private fun MediumDarkPreview() {
    PreviewApp(dark = true, rail = true, expandedRail = false)
}

@Preview(name = "Expanded light", widthDp = 1280, heightDp = 900, showBackground = true)
@Composable
private fun ExpandedLightPreview() {
    PreviewApp(dark = false, rail = true, expandedRail = true)
}

@Preview(
    name = "Expanded dark",
    widthDp = 1280,
    heightDp = 900,
    showBackground = true,
    uiMode = Configuration.UI_MODE_NIGHT_YES,
)
@Composable
private fun ExpandedDarkPreview() {
    PreviewApp(dark = true, rail = true, expandedRail = true)
}

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
private fun PreviewApp(dark: Boolean, rail: Boolean, expandedRail: Boolean) {
    EverybuyTheme(colors = if (dark) GreenDarkScheme else GreenLightScheme) {
        CompositionLocalProvider(
            LocalBottomChromeHeight provides if (rail) 0.dp else 80.dp,
            LocalNavigationRailVisible provides rail,
        ) {
            Row(
                Modifier
                    .fillMaxSize()
                    .background(MaterialTheme.colorScheme.background),
            ) {
                if (rail) {
                    WideNavigationRail(
                        state = rememberWideNavigationRailState(
                            initialValue = if (expandedRail) {
                                WideNavigationRailValue.Expanded
                            } else {
                                WideNavigationRailValue.Collapsed
                            },
                        ),
                        windowInsets = WindowInsets(0),
                        colors = androidx.compose.material3.WideNavigationRailDefaults.colors(
                            containerColor = MaterialTheme.colorScheme.surfaceContainerLow,
                        ),
                    ) {
                        previewNavItems.forEachIndexed { index, item ->
                            val label = stringResource(item.labelRes)
                            WideNavigationRailItem(
                                selected = index == 0,
                                onClick = {},
                                icon = { Icon(item.icon, contentDescription = label) },
                                label = { Text(label, maxLines = 1, overflow = TextOverflow.Ellipsis) },
                                railExpanded = expandedRail,
                            )
                        }
                    }
                }
                Box(Modifier.weight(1f)) {
                    EverybuyScreen(
                        title = stringResource(R.string.inventory_title),
                        subtitle = stringResource(R.string.preview_sample_customer),
                        onOpenSettings = {},
                    ) { padding ->
                        PreviewBody(padding)
                    }
                    if (!rail) {
                        NavigationBar(
                            modifier = Modifier
                                .align(Alignment.BottomCenter)
                                .fillMaxWidth(),
                        ) {
                            previewNavItems.forEachIndexed { index, item ->
                                val label = stringResource(item.labelRes)
                                NavigationBarItem(
                                    selected = index == 0,
                                    onClick = {},
                                    icon = { Icon(item.icon, contentDescription = label) },
                                    alwaysShowLabel = true,
                                    label = {
                                        Text(
                                            text = label,
                                            maxLines = 1,
                                            softWrap = false,
                                            overflow = TextOverflow.Visible,
                                            style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp),
                                        )
                                    },
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun PreviewBody(padding: PaddingValues) {
    LazyVerticalGrid(
        columns = GridCells.Adaptive(minSize = 320.dp),
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(
            start = 16.dp,
            end = 16.dp,
            top = padding.calculateTopPadding() + 4.dp,
            bottom = padding.calculateBottomPadding() + 104.dp,
        ),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        item(span = { GridItemSpan(maxLineSpan) }) {
            ExpressiveSearchField(
                value = "",
                onValueChange = {},
                placeholder = stringResource(R.string.inventory_search),
            )
        }
        item(span = { GridItemSpan(maxLineSpan) }) {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                FilterChip(
                    selected = false,
                    onClick = {},
                    label = { Text(stringResource(R.string.inventory_all_categories)) },
                )
            }
        }
        item(span = { GridItemSpan(maxLineSpan) }) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                StatTile(
                    label = stringResource(R.string.overview_sales_today),
                    value = "₦148,500",
                    supporting = pluralStringResource(R.plurals.overview_sales_count, 12, 12),
                    modifier = Modifier.weight(1f),
                )
                StatTile(
                    label = stringResource(R.string.overview_low_stock_stat),
                    value = "4",
                    supporting = stringResource(R.string.overview_low_stock_detail),
                    modifier = Modifier.weight(1f),
                    containerColor = MaterialTheme.colorScheme.tertiaryContainer,
                    contentColor = MaterialTheme.colorScheme.onTertiaryContainer,
                )
            }
        }
        item(span = { GridItemSpan(maxLineSpan) }) {
            SectionHeader(
                title = pluralStringResource(R.plurals.inventory_products_count, 4, 4),
                supporting = stringResource(R.string.inventory_subtitle),
            )
        }
        items(4) { index ->
            TonalCard(modifier = Modifier.fillMaxWidth()) {
                androidx.compose.foundation.layout.Column(
                    Modifier.fillMaxWidth().padding(18.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    Row(
                        Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.Top,
                    ) {
                        androidx.compose.foundation.layout.Column(Modifier.weight(1f)) {
                            Text(
                                text = stringResource(R.string.preview_sample_product) + " ${index + 1}",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                            )
                            Text(
                                text = stringResource(R.string.preview_sample_brand),
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                        TonalBadge(
                            text = "18 ${stringResource(R.string.inventory_packs_in_stock)}",
                        )
                    }
                    Text(
                        text = "₦4,800",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold,
                    )
                }
            }
        }
    }
}
