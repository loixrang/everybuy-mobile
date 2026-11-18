package com.everybuy.mobile.ui.more

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ReceiptLong
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.History
import androidx.compose.material.icons.rounded.Inventory
import androidx.compose.material.icons.rounded.Security
import androidx.compose.material.icons.rounded.Settings
import androidx.compose.material3.Icon
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.SheetValue
import androidx.compose.material3.Text
import androidx.compose.material3.rememberBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.everybuy.mobile.R
import com.everybuy.mobile.ui.components.GlassModalBottomSheet
import kotlinx.coroutines.launch

private data class MoreEntry(
    val titleRes: Int,
    val bodyRes: Int,
    val icon: androidx.compose.ui.graphics.vector.ImageVector,
    val route: String,
)

@Composable
@OptIn(ExperimentalMaterial3Api::class)
fun MoreBottomSheet(
    userName: String,
    email: String,
    onDismiss: () -> Unit,
    onNavigate: (String) -> Unit,
    onSignOut: () -> Unit,
) {
    val sheetState = rememberBottomSheetState(initialValue = SheetValue.Hidden)
    val scope = rememberCoroutineScope()
    val entries = listOf(
        MoreEntry(R.string.more_expenses, R.string.more_expenses_body, Icons.AutoMirrored.Rounded.ReceiptLong, "expenses"),
        MoreEntry(R.string.more_audit, R.string.more_audit_body, Icons.Rounded.Security, "audit"),
        MoreEntry(R.string.more_movements, R.string.more_movements_body, Icons.Rounded.History, "movements"),
        MoreEntry(R.string.more_stocktakes, R.string.more_stocktakes_body, Icons.Rounded.Inventory, "stocktakes"),
        MoreEntry(R.string.more_settings, R.string.more_settings_body, Icons.Rounded.Settings, "settings"),
    )
    val maxHeight = LocalConfiguration.current.screenHeightDp.dp * 0.92f

    GlassModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
    ) {
        Column(
            Modifier
                .fillMaxWidth()
                .heightIn(max = maxHeight)
                .padding(horizontal = 20.dp),
        ) {
            Row(
                Modifier.fillMaxWidth().padding(bottom = 12.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                Text(
                    stringResource(R.string.more_title),
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.SemiBold,
                )
                androidx.compose.material3.IconButton(onClick = onDismiss) {
                    Icon(Icons.Rounded.Close, contentDescription = stringResource(R.string.action_close))
                }
            }
            Surface(
                shape = MaterialTheme.shapes.medium,
                color = MaterialTheme.colorScheme.surfaceContainerHigh,
                modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp),
            ) {
                Column(Modifier.padding(horizontal = 16.dp, vertical = 12.dp)) {
                    Text(
                        userName,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                    Text(
                        email,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
            }
            LazyColumn(
                modifier = Modifier.weight(1f, fill = false),
                verticalArrangement = Arrangement.spacedBy(4.dp),
            ) {
                items(entries, key = { it.route }) { entry ->
                    Row(
                        Modifier
                            .fillMaxWidth()
                            .heightIn(min = 64.dp)
                            .clickable {
                                scope.launch {
                                    sheetState.hide()
                                    onDismiss()
                                    onNavigate(entry.route)
                                }
                            }
                            .padding(vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(14.dp),
                    ) {
                        Surface(
                            modifier = Modifier.size(48.dp),
                            shape = RoundedCornerShape(16.dp),
                            color = MaterialTheme.colorScheme.secondaryContainer,
                            contentColor = MaterialTheme.colorScheme.onSecondaryContainer,
                        ) {
                            androidx.compose.foundation.layout.Box(contentAlignment = Alignment.Center) {
                                Icon(entry.icon, contentDescription = null, modifier = Modifier.size(24.dp))
                            }
                        }
                        Column(Modifier.weight(1f)) {
                            Text(
                                stringResource(entry.titleRes),
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Medium,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                            )
                            Text(
                                stringResource(entry.bodyRes),
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                            )
                        }
                    }
                }
            }
            OutlinedButton(
                onClick = {
                    scope.launch {
                        sheetState.hide()
                        onDismiss()
                        onSignOut()
                    }
                },
                modifier = Modifier.fillMaxWidth().padding(top = 12.dp, bottom = 8.dp).heightIn(min = 48.dp),
            ) {
                Text(stringResource(R.string.action_sign_out), maxLines = 1)
            }
        }
    }
}
