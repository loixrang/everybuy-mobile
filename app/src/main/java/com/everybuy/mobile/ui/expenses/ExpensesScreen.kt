package com.everybuy.mobile.ui.expenses

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.FilterChip
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
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.everybuy.mobile.R
import com.everybuy.mobile.core.util.formatLagosDateTime
import com.everybuy.mobile.core.util.formatNaira
import com.everybuy.mobile.data.ExpenseDto
import com.everybuy.mobile.ui.components.EmptyPane
import com.everybuy.mobile.ui.components.ErrorPane
import com.everybuy.mobile.ui.components.EverybuyScreen
import com.everybuy.mobile.ui.components.ExpressiveSearchField
import com.everybuy.mobile.ui.components.LoadingPane
import com.everybuy.mobile.ui.components.SectionHeader
import com.everybuy.mobile.ui.components.StatTile
import com.everybuy.mobile.ui.components.TonalCard
import com.everybuy.mobile.ui.reports.currentLagosMonth
import com.everybuy.mobile.ui.reports.formatMonthLabel
import java.math.BigDecimal

@OptIn(ExperimentalMaterial3Api::class, ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun ExpensesScreen(
    onBack: () -> Unit,
    onSessionExpired: () -> Unit,
    viewModel: ExpensesViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsState()
    var showForm by rememberSaveable { mutableStateOf(false) }
    LaunchedEffect(state) { if (state is ExpensesState.SessionExpired) onSessionExpired() }

    EverybuyScreen(
        title = stringResource(R.string.expenses_title),
        subtitle = stringResource(R.string.expenses_subtitle),
        onOpenSettings = null,
        onBack = onBack,
        onRefresh = viewModel::refresh,
        floatingActionButton = {
            ExtendedFloatingActionButton(
                onClick = {
                    viewModel.clearFormError()
                    showForm = true
                },
                icon = { Icon(Icons.Rounded.Add, contentDescription = null) },
                text = { Text(stringResource(R.string.expenses_record)) },
                containerColor = MaterialTheme.colorScheme.primaryContainer,
                contentColor = MaterialTheme.colorScheme.onPrimaryContainer,
            )
        },
    ) { padding ->
        when (val current = state) {
            ExpensesState.Loading -> LoadingPane(
                message = stringResource(R.string.expenses_loading),
                modifier = Modifier.padding(top = padding.calculateTopPadding()),
            )
            ExpensesState.SessionExpired -> Text(
                text = stringResource(R.string.session_expired),
                modifier = Modifier.padding(padding).padding(24.dp),
            )
            is ExpensesState.Failed -> ErrorPane(
                message = current.message,
                onRetry = { viewModel.refresh() },
                modifier = Modifier.padding(top = padding.calculateTopPadding()),
            )
            is ExpensesState.Ready -> ExpensesContent(
                state = current,
                viewModel = viewModel,
                padding = padding,
                modifier = Modifier.fillMaxSize(),
            )
        }
    }

    if (showForm) {
        val ready = state as? ExpensesState.Ready
        RecordExpenseDialog(
            submitting = ready?.submitting == true,
            error = ready?.formError,
            onDismiss = {
                if (ready?.submitting != true) {
                    showForm = false
                    viewModel.clearFormError()
                }
            },
            onConfirm = { title, category, amount, notes ->
                viewModel.record(title, category, amount, notes)
            },
        )
    }
    val success = (state as? ExpensesState.Ready)?.successMessage
    LaunchedEffect(success) { if (success != null) showForm = false }
}

@Composable
private fun ExpensesContent(
    state: ExpensesState.Ready,
    viewModel: ExpensesViewModel,
    padding: PaddingValues,
    modifier: Modifier = Modifier,
) {
    val thisMonth = currentLagosMonth()
    val filtered = state.filtered
    LazyVerticalGrid(
        columns = GridCells.Adaptive(minSize = 320.dp),
        modifier = modifier,
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
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                StatTile(
                    label = stringResource(R.string.expenses_this_month),
                    value = formatNaira(expenseTotal(state.expenses, thisMonth).toPlainString()),
                    supporting = formatMonthLabel(thisMonth),
                    modifier = Modifier.weight(1f),
                )
                StatTile(
                    label = stringResource(R.string.expenses_all_recorded),
                    value = formatNaira(expenseTotal(state.expenses).toPlainString()),
                    supporting = stringResource(R.string.expenses_all_recorded_body),
                    modifier = Modifier.weight(1f),
                    containerColor = MaterialTheme.colorScheme.tertiaryContainer,
                    contentColor = MaterialTheme.colorScheme.onTertiaryContainer,
                )
            }
        }
        item(span = { GridItemSpan(maxLineSpan) }) {
            StatTile(
                label = stringResource(R.string.expenses_entries),
                value = state.expenses.size.toString(),
                supporting = pluralStringResource(
                    R.plurals.expenses_entry_count,
                    state.expenses.size,
                    state.expenses.size,
                ),
                modifier = Modifier.fillMaxWidth(),
                containerColor = MaterialTheme.colorScheme.secondaryContainer,
                contentColor = MaterialTheme.colorScheme.onSecondaryContainer,
            )
        }
        state.successMessage?.let { message ->
            item(span = { GridItemSpan(maxLineSpan) }) {
                TonalCard(
                    modifier = Modifier.fillMaxWidth(),
                    containerColor = MaterialTheme.colorScheme.secondaryContainer,
                    contentColor = MaterialTheme.colorScheme.onSecondaryContainer,
                ) {
                    Row(
                        Modifier.fillMaxWidth().padding(14.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Text(
                            text = message,
                            style = MaterialTheme.typography.bodySmall,
                            modifier = Modifier.weight(1f),
                        )
                        TextButton(onClick = { viewModel.dismissSuccess() }) {
                            Text(stringResource(R.string.action_dismiss))
                        }
                    }
                }
            }
        }
        item(span = { GridItemSpan(maxLineSpan) }) {
            ExpressiveSearchField(
                value = state.query,
                onValueChange = viewModel::setQuery,
                placeholder = stringResource(R.string.expenses_search),
            )
        }
        item(span = { GridItemSpan(maxLineSpan) }) {
            SectionHeader(
                title = if (filtered.isEmpty()) {
                    stringResource(R.string.expenses_entries)
                } else {
                    pluralStringResource(
                        R.plurals.expenses_entry_count,
                        filtered.size,
                        filtered.size,
                    )
                },
            )
        }
        if (filtered.isEmpty()) {
            item(span = { GridItemSpan(maxLineSpan) }) {
                EmptyPane(
                    title = if (state.expenses.isEmpty()) {
                        stringResource(R.string.expenses_empty)
                    } else {
                        stringResource(R.string.expenses_no_matches)
                    },
                    body = if (state.expenses.isEmpty()) {
                        stringResource(R.string.expenses_empty_body)
                    } else {
                        stringResource(R.string.expenses_no_matches_body)
                    },
                )
            }
        } else {
            items(filtered, key = { it.id }) { expense ->
                ExpenseCard(expense, Modifier.animateItem())
            }
        }
    }
}

@Composable
private fun ExpenseCard(expense: ExpenseDto, modifier: Modifier = Modifier) {
    TonalCard(modifier = modifier.fillMaxWidth()) {
        Column(
            Modifier.fillMaxWidth().padding(18.dp),
            verticalArrangement = Arrangement.spacedBy(5.dp),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalAlignment = Alignment.Top,
            ) {
                Column(Modifier.weight(1f)) {
                    Text(
                        text = expense.title,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis,
                    )
                    Text(
                        text = expense.category,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                Text(
                    text = formatNaira(expense.amount),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                )
            }
            expense.notes?.takeIf { it.isNotBlank() }?.let {
                Text(it, style = MaterialTheme.typography.bodySmall)
            }
            Text(
                text = formatLagosDateTime(expense.spentAt),
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
private fun RecordExpenseDialog(
    submitting: Boolean,
    error: String?,
    onDismiss: () -> Unit,
    onConfirm: (title: String, category: String, amount: String, notes: String) -> Unit,
) {
    var title by rememberSaveable { mutableStateOf("") }
    var category by rememberSaveable { mutableStateOf("") }
    var amount by rememberSaveable { mutableStateOf("") }
    var notes by rememberSaveable { mutableStateOf("") }
    AlertDialog(
        onDismissRequest = onDismiss,
        shape = MaterialTheme.shapes.extraLarge,
        containerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
        title = { Text(stringResource(R.string.expenses_record)) },
        text = {
            Column(
                modifier = Modifier.verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                OutlinedTextField(
                    value = title,
                    onValueChange = { title = it.take(EXPENSE_TITLE_MAX) },
                    modifier = Modifier.fillMaxWidth(),
                    label = { Text(stringResource(R.string.expenses_title_field)) },
                    singleLine = true,
                    shape = MaterialTheme.shapes.large,
                )
                OutlinedTextField(
                    value = category,
                    onValueChange = { category = it.take(EXPENSE_CATEGORY_MAX) },
                    modifier = Modifier.fillMaxWidth(),
                    label = { Text(stringResource(R.string.expenses_category)) },
                    singleLine = true,
                    shape = MaterialTheme.shapes.large,
                )
                LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    items(suggestedExpenseCategories) { suggestion ->
                        FilterChip(
                            selected = category == suggestion,
                            onClick = { category = suggestion },
                            label = { Text(suggestion) },
                        )
                    }
                }
                OutlinedTextField(
                    value = amount,
                    onValueChange = { value -> amount = value.filter { it.isDigit() || it == '.' } },
                    modifier = Modifier.fillMaxWidth(),
                    label = { Text(stringResource(R.string.expenses_amount)) },
                    singleLine = true,
                    shape = MaterialTheme.shapes.large,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                )
                OutlinedTextField(
                    value = notes,
                    onValueChange = { notes = it.take(EXPENSE_NOTES_MAX) },
                    modifier = Modifier.fillMaxWidth(),
                    label = { Text(stringResource(R.string.expenses_notes)) },
                    minLines = 2,
                    maxLines = 4,
                    shape = MaterialTheme.shapes.large,
                )
                if (error != null) {
                    Text(error, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall)
                }
            }
        },
        confirmButton = {
            Button(
                onClick = { onConfirm(title, category, amount, notes) },
                enabled = !submitting &&
                    title.isNotBlank() &&
                    category.isNotBlank() &&
                    (amount.toBigDecimalOrNull() ?: BigDecimal.ZERO) > BigDecimal.ZERO,
            ) {
                if (submitting) {
                    LoadingIndicator(modifier = Modifier.padding(2.dp))
                } else {
                    Text(stringResource(R.string.expenses_save))
                }
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss, enabled = !submitting) {
                Text(stringResource(R.string.action_cancel))
            }
        },
    )
}
