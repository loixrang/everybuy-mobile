package com.everybuy.mobile.ui.more

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.History
import androidx.compose.material.icons.rounded.Inventory
import androidx.compose.material.icons.automirrored.rounded.ReceiptLong
import androidx.compose.material.icons.rounded.Security
import androidx.compose.material.icons.rounded.Settings
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.everybuy.mobile.R
import com.everybuy.mobile.ui.components.EverybuyScreen
import com.everybuy.mobile.ui.components.TonalCard
import com.everybuy.mobile.ui.components.listBottomPadding

private data class MoreEntry(
    val titleRes: Int,
    val bodyRes: Int,
    val icon: ImageVector,
    val onClick: () -> Unit,
)

@Composable
fun MoreScreen(
    userName: String,
    onOpenHistory: () -> Unit,
    onOpenCounts: () -> Unit,
    onOpenExpenses: () -> Unit,
    onOpenAudit: () -> Unit,
    onOpenSettings: () -> Unit,
) {
    val entries = listOf(
        MoreEntry(R.string.more_history, R.string.more_history_body, Icons.Rounded.History, onOpenHistory),
        MoreEntry(R.string.more_counts, R.string.more_counts_body, Icons.Rounded.Inventory, onOpenCounts),
        MoreEntry(R.string.more_expenses, R.string.more_expenses_body, Icons.AutoMirrored.Rounded.ReceiptLong, onOpenExpenses),
        MoreEntry(R.string.more_audit, R.string.more_audit_body, Icons.Rounded.Security, onOpenAudit),
        MoreEntry(R.string.more_settings, R.string.more_settings_body, Icons.Rounded.Settings, onOpenSettings),
    )
    EverybuyScreen(
        title = stringResource(R.string.more_title),
        subtitle = userName,
        onOpenSettings = onOpenSettings,
    ) { padding ->
        LazyVerticalGrid(
            columns = GridCells.Adaptive(minSize = 320.dp),
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(
                start = 16.dp,
                end = 16.dp,
                top = padding.calculateTopPadding() + 8.dp,
                bottom = padding.calculateBottomPadding() + listBottomPadding(),
            ),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            items(entries, key = { it.titleRes }) { entry ->
                TonalCard(onClick = entry.onClick, modifier = Modifier.fillMaxWidth()) {
                    Row(
                        Modifier.fillMaxWidth().padding(16.dp),
                        horizontalArrangement = Arrangement.spacedBy(16.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Surface(
                            shape = CircleShape,
                            color = MaterialTheme.colorScheme.primaryContainer,
                            contentColor = MaterialTheme.colorScheme.onPrimaryContainer,
                        ) {
                            Icon(
                                imageVector = entry.icon,
                                contentDescription = null,
                                modifier = Modifier.padding(10.dp).size(24.dp),
                            )
                        }
                        Column(Modifier.weight(1f)) {
                            Text(
                                text = stringResource(entry.titleRes),
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.SemiBold,
                            )
                            Text(
                                text = stringResource(entry.bodyRes),
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                    }
                }
            }
        }
    }
}
