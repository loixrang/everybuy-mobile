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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Assessment
import androidx.compose.material.icons.rounded.Inventory2
import androidx.compose.material.icons.rounded.MoreHoriz
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
    PreviewNavItem(R.string.nav_more, Icons.Rounded.MoreHoriz),
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

@Preview(name = "Compact 1.3x", widthDp = 360, heightDp = 800, fontScale = 1.3f, showBackground = true)
@Composable
private fun CompactLargeFontPreview() {
    PreviewApp(dark = false, rail = false, expandedRail = false)
}

@Preview(name = "Landscape compact", widthDp = 800, heightDp = 360, showBackground = true)
@Composable
private fun LandscapeCompactPreview() {
    PreviewApp(dark = true, rail = true, expandedRail = false)
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
                                            overflow = TextOverflow.Clip,
                                            style = MaterialTheme.typography.labelSmall,
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
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(
            start = 16.dp,
            end = 16.dp,
            top = padding.calculateTopPadding() + 4.dp,
            bottom = padding.calculateBottomPadding() + 104.dp,
        ),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        item {
            ExpressiveSearchField(
                value = "",
                onValueChange = {},
                placeholder = stringResource(R.string.inventory_search),
            )
        }
        item {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                FilterChip(
                    selected = false,
                    onClick = {},
                    label = { Text(stringResource(R.string.inventory_all_categories)) },
                )
            }
        }
        item {
            StatTile(
                label = stringResource(R.string.overview_sales_today),
                value = "₦987,654,321.50",
                supporting = pluralStringResource(R.plurals.overview_sales_count, 12, 12),
                modifier = Modifier.fillMaxWidth(),
            )
        }
        item {
            StatTile(
                label = stringResource(R.string.overview_low_stock_stat),
                value = "4",
                supporting = stringResource(R.string.overview_low_stock_detail),
                modifier = Modifier.fillMaxWidth(),
                containerColor = MaterialTheme.colorScheme.tertiaryContainer,
                contentColor = MaterialTheme.colorScheme.onTertiaryContainer,
            )
        }
        item {
            SectionHeader(
                title = pluralStringResource(R.plurals.inventory_products_count, 4, 4),
                supporting = stringResource(R.string.inventory_subtitle),
            )
        }
        items(4) { index ->
            TonalCard(modifier = Modifier.fillMaxWidth()) {
                Row(
                    Modifier.fillMaxWidth().padding(16.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    androidx.compose.foundation.layout.Column(Modifier.weight(1f)) {
                        Text(
                            text = if (index == 0) stringResource(R.string.preview_long_product_name) else stringResource(R.string.preview_sample_product),
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Medium,
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis,
                        )
                        Text(
                            text = stringResource(R.string.preview_sample_brand),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis,
                        )
                        Text(
                            text = "₦987,654,321.50",
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.Medium,
                            maxLines = 1,
                            softWrap = false,
                        )
                    }
                    TonalBadge(text = "18 packs")
                }
            }
        }
        item {
            TonalCard(Modifier.fillMaxWidth()) {
                Row(
                    Modifier.fillMaxWidth().padding(16.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    androidx.compose.foundation.layout.Column(Modifier.weight(1f)) {
                        Text(
                            stringResource(R.string.preview_long_customer_name),
                            style = MaterialTheme.typography.titleMedium,
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis,
                        )
                        Text(
                            "Customer · 18 Nov, 08:35",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 2,
                        )
                    }
                    Text(
                        "₦987,654,321.50",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold,
                        maxLines = 1,
                        softWrap = false,
                    )
                }
            }
        }
    }
}
