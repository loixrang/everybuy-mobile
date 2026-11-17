package com.everybuy.mobile.ui.stocktakes

import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

@Composable fun StockTakeStatusBadge(status: String, modifier: Modifier = Modifier) {
    val container = when (status) {
        "Finalized" -> MaterialTheme.colorScheme.primaryContainer
        "Cancelled" -> MaterialTheme.colorScheme.surfaceVariant
        else -> MaterialTheme.colorScheme.tertiaryContainer
    }
    val content = when (status) {
        "Finalized" -> MaterialTheme.colorScheme.onPrimaryContainer
        "Cancelled" -> MaterialTheme.colorScheme.onSurfaceVariant
        else -> MaterialTheme.colorScheme.onTertiaryContainer
    }
    Surface(modifier = modifier, color = container, contentColor = content, shape = RoundedCornerShape(8.dp)) {
        Text(stockTakeStatusLabel(status), style = MaterialTheme.typography.labelMedium, modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp))
    }
}
