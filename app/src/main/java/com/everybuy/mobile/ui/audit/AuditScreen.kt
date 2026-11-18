package com.everybuy.mobile.ui.audit

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
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.everybuy.mobile.R
import com.everybuy.mobile.core.util.formatLagosDateTime
import com.everybuy.mobile.data.AuditEntryDto
import com.everybuy.mobile.ui.components.EmptyPane
import com.everybuy.mobile.ui.components.ErrorPane
import com.everybuy.mobile.ui.components.EverybuyScreen
import com.everybuy.mobile.ui.components.ExpressiveSearchField
import com.everybuy.mobile.ui.components.LoadingPane
import com.everybuy.mobile.ui.components.SectionHeader
import com.everybuy.mobile.ui.components.TonalCard

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AuditScreen(
    onBack: () -> Unit,
    onSessionExpired: () -> Unit,
    viewModel: AuditViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsState()
    LaunchedEffect(state) { if (state is AuditState.SessionExpired) onSessionExpired() }

    EverybuyScreen(
        title = stringResource(R.string.audit_title),
        subtitle = stringResource(R.string.audit_subtitle),
        onOpenSettings = null,
        onBack = onBack,
        onRefresh = viewModel::refresh,
    ) { padding ->
        when (val current = state) {
            AuditState.Loading -> LoadingPane(
                message = stringResource(R.string.audit_loading),
                modifier = Modifier.padding(top = padding.calculateTopPadding()),
            )
            AuditState.SessionExpired -> Text(
                text = stringResource(R.string.session_expired),
                modifier = Modifier.padding(padding).padding(24.dp),
            )
            is AuditState.Failed -> ErrorPane(
                message = current.message,
                onRetry = { viewModel.refresh() },
                modifier = Modifier.padding(top = padding.calculateTopPadding()),
            )
            is AuditState.Ready -> AuditContent(
                state = current,
                viewModel = viewModel,
                padding = padding,
                modifier = Modifier.fillMaxSize(),
            )
        }
    }
}

@Composable
private fun AuditContent(
    state: AuditState.Ready,
    viewModel: AuditViewModel,
    padding: PaddingValues,
    modifier: Modifier = Modifier,
) {
    val filtered = state.filtered
    LazyVerticalGrid(
        columns = GridCells.Adaptive(minSize = 320.dp),
        modifier = modifier,
        contentPadding = PaddingValues(
            start = 16.dp,
            end = 16.dp,
            top = padding.calculateTopPadding() + 4.dp,
            bottom = padding.calculateBottomPadding() + 24.dp,
        ),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        item(span = { GridItemSpan(maxLineSpan) }) {
            ExpressiveSearchField(
                value = state.query,
                onValueChange = viewModel::setQuery,
                placeholder = stringResource(R.string.audit_search),
            )
        }
        item(span = { GridItemSpan(maxLineSpan) }) {
            SectionHeader(title = stringResource(R.string.audit_title))
        }
        if (filtered.isEmpty()) {
            item(span = { GridItemSpan(maxLineSpan) }) {
                EmptyPane(
                    title = if (state.entries.isEmpty()) {
                        stringResource(R.string.audit_empty)
                    } else {
                        stringResource(R.string.audit_no_matches)
                    },
                    body = if (state.entries.isEmpty()) {
                        stringResource(R.string.audit_empty_body)
                    } else {
                        stringResource(R.string.audit_no_matches_body)
                    },
                )
            }
        } else {
            items(filtered, key = { "${it.entityId}-${it.at}-${it.action}" }) { entry ->
                AuditCard(entry, Modifier.animateItem())
            }
        }
    }
}

@Composable
private fun AuditCard(entry: AuditEntryDto, modifier: Modifier = Modifier) {
    TonalCard(modifier = modifier.fillMaxWidth()) {
        Column(
            Modifier.fillMaxWidth().padding(18.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalAlignment = Alignment.Top,
            ) {
                Text(
                    text = "${entry.action} · ${entry.entity}",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f),
                )
                Text(
                    text = formatLagosDateTime(entry.at),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            Text(entry.actor, style = MaterialTheme.typography.bodyMedium)
            Text(
                text = auditDetailSummary(entry),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Text(
                text = stringResource(R.string.audit_entity_id, entry.entityId),
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}
