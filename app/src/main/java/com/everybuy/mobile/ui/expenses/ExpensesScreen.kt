package com.everybuy.mobile.ui.expenses

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.Refresh
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.everybuy.mobile.core.util.formatLagosDateTime
import com.everybuy.mobile.core.util.formatNaira
import com.everybuy.mobile.data.ExpenseDto
import com.everybuy.mobile.ui.reports.currentLagosMonth
import com.everybuy.mobile.ui.reports.formatMonthLabel
import java.math.BigDecimal

@OptIn(ExperimentalMaterial3Api::class)
@Composable fun ExpensesScreen(
    onBack: () -> Unit,
    onSessionExpired: () -> Unit,
    viewModel: ExpensesViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsState()
    var showForm by rememberSaveable { mutableStateOf(false) }
    LaunchedEffect(state) { if (state is ExpensesState.SessionExpired) onSessionExpired() }
    Scaffold(topBar = {
        TopAppBar(
            title = { Column { Text("Expenses"); Text("Shop spending recorded by staff", style = MaterialTheme.typography.labelMedium) } },
            navigationIcon = { IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Rounded.ArrowBack, contentDescription = "Back") } },
            actions = { IconButton(onClick = { viewModel.refresh() }) { Icon(Icons.Rounded.Refresh, contentDescription = "Refresh expenses") } },
        )
    }, floatingActionButton = {
        Button(onClick = { viewModel.clearFormError(); showForm = true }) {
            Icon(Icons.Rounded.Add, contentDescription = null, modifier = Modifier.padding(end = 6.dp))
            Text("Record expense")
        }
    }) { insets ->
        when (val current = state) {
            ExpensesState.Loading -> Column(Modifier.fillMaxSize().padding(insets), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center) {
                CircularProgressIndicator()
                Text("Loading expenses…", Modifier.padding(top = 12.dp))
            }
            ExpensesState.SessionExpired -> Text("Your session has expired.", Modifier.padding(insets).padding(24.dp))
            is ExpensesState.Failed -> Column(Modifier.fillMaxSize().padding(insets).padding(24.dp), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center) {
                Text(current.message, color = MaterialTheme.colorScheme.error)
                TextButton(onClick = { viewModel.refresh() }) { Text("Retry") }
            }
            is ExpensesState.Ready -> ExpensesContent(current, viewModel, Modifier.fillMaxSize().padding(insets))
        }
    }

    if (showForm) {
        val ready = state as? ExpensesState.Ready
        RecordExpenseDialog(
            submitting = ready?.submitting == true,
            error = ready?.formError,
            onDismiss = { if (ready?.submitting != true) { showForm = false; viewModel.clearFormError() } },
            onConfirm = { title, category, amount, notes -> viewModel.record(title, category, amount, notes) },
        )
    }
    val success = (state as? ExpensesState.Ready)?.successMessage
    LaunchedEffect(success) { if (success != null) showForm = false }
}

@Composable private fun ExpensesContent(state: ExpensesState.Ready, viewModel: ExpensesViewModel, modifier: Modifier = Modifier) {
    val thisMonth = currentLagosMonth()
    Column(modifier.padding(horizontal = 16.dp)) {
        Column(Modifier.padding(top = 8.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                SummaryCard("This month", formatNaira(expenseTotal(state.expenses, thisMonth).toPlainString()), formatMonthLabel(thisMonth), Modifier.weight(1f))
                SummaryCard("All recorded", formatNaira(expenseTotal(state.expenses).toPlainString()), "Every recorded expense", Modifier.weight(1f))
            }
            SummaryCard("Entries", state.expenses.size.toString(), if (state.expenses.size == 1) "1 expense" else "${state.expenses.size} expenses", Modifier.fillMaxWidth())
        }
        state.successMessage?.let { message ->
            Card(Modifier.fillMaxWidth().padding(top = 10.dp), shape = RoundedCornerShape(16.dp), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.secondaryContainer)) {
                Row(Modifier.fillMaxWidth().padding(14.dp), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                    Text(message, style = MaterialTheme.typography.bodySmall, modifier = Modifier.weight(1f))
                    TextButton(onClick = { viewModel.dismissSuccess() }) { Text("Dismiss") }
                }
            }
        }
        OutlinedTextField(
            state.query,
            viewModel::setQuery,
            Modifier.fillMaxWidth().padding(top = 10.dp),
            label = { Text("Search title, category or notes") },
            singleLine = true,
        )
        if (state.filtered.isEmpty()) {
            Column(Modifier.fillMaxSize().padding(24.dp), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center) {
                Text(if (state.expenses.isEmpty()) "No expenses recorded" else "No matching expenses", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                Text(
                    if (state.expenses.isEmpty()) "Record shop spending so the profit report stays accurate." else "Try a different search term.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(top = 6.dp),
                )
            }
        } else {
            LazyColumn(Modifier.weight(1f).padding(top = 8.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                items(state.filtered, key = { it.id }) { expense -> ExpenseCard(expense) }
            }
        }
    }
}

@Composable private fun ExpenseCard(expense: ExpenseDto) {
    Card(Modifier.fillMaxWidth(), shape = RoundedCornerShape(20.dp), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow)) {
        Column(Modifier.fillMaxWidth().padding(16.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.Top) {
                Column(Modifier.weight(1f)) {
                    Text(expense.title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                    Text(expense.category, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                Text(formatNaira(expense.amount), style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            }
            expense.notes?.takeIf { it.isNotBlank() }?.let { Text(it, style = MaterialTheme.typography.bodySmall) }
            Text(formatLagosDateTime(expense.spentAt), style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

@Composable private fun SummaryCard(label: String, value: String, detail: String, modifier: Modifier = Modifier) {
    Card(modifier, shape = RoundedCornerShape(20.dp), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow)) {
        Column(Modifier.fillMaxWidth().padding(14.dp)) {
            Text(label, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Text(value, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold, modifier = Modifier.padding(top = 4.dp))
            Text(detail, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.padding(top = 2.dp))
        }
    }
}

@Composable private fun RecordExpenseDialog(
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
        title = { Text("Record expense") },
        text = {
            Column(Modifier.verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                OutlinedTextField(title, { title = it.take(EXPENSE_TITLE_MAX) }, Modifier.fillMaxWidth(), label = { Text("What was it for?") }, singleLine = true)
                OutlinedTextField(category, { category = it.take(EXPENSE_CATEGORY_MAX) }, Modifier.fillMaxWidth(), label = { Text("Category") }, singleLine = true)
                LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    items(suggestedExpenseCategories) { suggestion ->
                        FilterChip(selected = category == suggestion, onClick = { category = suggestion }, label = { Text(suggestion) })
                    }
                }
                OutlinedTextField(
                    amount,
                    { value -> amount = value.filter { it.isDigit() || it == '.' } },
                    Modifier.fillMaxWidth(),
                    label = { Text("Amount (₦)") },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                )
                OutlinedTextField(notes, { notes = it.take(EXPENSE_NOTES_MAX) }, Modifier.fillMaxWidth(), label = { Text("Notes (optional)") }, minLines = 2, maxLines = 4)
                if (error != null) Text(error, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall)
            }
        },
        confirmButton = {
            Button(
                onClick = { onConfirm(title, category, amount, notes) },
                enabled = !submitting && title.isNotBlank() && category.isNotBlank() && (amount.toBigDecimalOrNull() ?: BigDecimal.ZERO) > BigDecimal.ZERO,
            ) {
                if (submitting) CircularProgressIndicator(Modifier.height(18.dp)) else Text("Save expense")
            }
        },
        dismissButton = { TextButton(onClick = onDismiss, enabled = !submitting) { Text("Cancel") } },
    )
}
