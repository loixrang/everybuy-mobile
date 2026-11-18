package com.everybuy.mobile.ui.customers

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable fun CustomerFormScreen(
    id: String?,
    onBack: () -> Unit,
    onSaved: (String) -> Unit,
    onSessionExpired: () -> Unit,
    viewModel: CustomerFormViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsState()
    LaunchedEffect(id) { viewModel.load(id) }
    LaunchedEffect(state) {
        when (val current = state) {
            is CustomerFormState.Saved -> onSaved(current.customerId)
            is CustomerFormState.Ready -> if (current.sessionExpired) onSessionExpired()
            else -> Unit
        }
    }
    Scaffold(topBar = {
        TopAppBar(
            title = { Text(if (id == null) "Add customer" else "Edit customer") },
            navigationIcon = { IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Rounded.ArrowBack, contentDescription = "Back") } },
        )
    }) { insets ->
        when (val current = state) {
            CustomerFormState.Loading -> Column(Modifier.fillMaxSize().padding(insets), verticalArrangement = Arrangement.Center, horizontalAlignment = Alignment.CenterHorizontally) {
                CircularProgressIndicator()
                Text("Loading customer…", Modifier.padding(top = 12.dp))
            }
            is CustomerFormState.Failed -> Column(Modifier.fillMaxSize().padding(insets).padding(24.dp), verticalArrangement = Arrangement.Center, horizontalAlignment = Alignment.CenterHorizontally) {
                Text(current.message, color = MaterialTheme.colorScheme.error)
                TextButton(onClick = { viewModel.load(id) }) { Text("Retry") }
            }
            is CustomerFormState.Saved -> Unit
            is CustomerFormState.Ready -> CustomerFormContent(current, id, viewModel, onBack, Modifier.fillMaxSize().padding(insets))
        }
    }
}

@Composable private fun CustomerFormContent(
    state: CustomerFormState.Ready,
    id: String?,
    viewModel: CustomerFormViewModel,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val draft = state.draft
    Box(modifier.fillMaxSize(), contentAlignment = Alignment.TopCenter) {
    Column(
        Modifier
            .fillMaxWidth()
            .widthIn(max = 840.dp)
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 20.dp, vertical = 16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Text(
            if (state.editing) "Update the customer’s contact details and status." else "Customers keep credit sales and later repayments linked to the right person.",
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        state.formError?.let { Text(it, color = MaterialTheme.colorScheme.error) }

        OutlinedTextField(
            draft.name,
            { value -> viewModel.updateDraft { it.copy(name = value.take(CUSTOMER_NAME_MAX + 1)) } },
            Modifier.fillMaxWidth(),
            label = { Text("Full name") },
            singleLine = true,
            isError = state.errors["name"] != null,
            supportingText = state.errors["name"]?.let { { Text(it) } },
        )
        OutlinedTextField(
            draft.phone,
            { value -> viewModel.updateDraft { it.copy(phone = value.take(CUSTOMER_PHONE_MAX + 1)) } },
            Modifier.fillMaxWidth(),
            label = { Text("Phone number (optional)") },
            singleLine = true,
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
            isError = state.errors["phone"] != null,
            supportingText = state.errors["phone"]?.let { { Text(it) } },
        )
        OutlinedTextField(
            draft.address,
            { value -> viewModel.updateDraft { it.copy(address = value.take(CUSTOMER_ADDRESS_MAX + 1)) } },
            Modifier.fillMaxWidth(),
            label = { Text("Address (optional)") },
            minLines = 1,
            maxLines = 3,
            isError = state.errors["address"] != null,
            supportingText = state.errors["address"]?.let { { Text(it) } },
        )
        OutlinedTextField(
            draft.notes,
            { value -> viewModel.updateDraft { it.copy(notes = value.take(CUSTOMER_NOTES_MAX + 1)) } },
            Modifier.fillMaxWidth(),
            label = { Text("Notes (optional)") },
            placeholder = { Text("Credit terms, contact preferences…") },
            minLines = 2,
            maxLines = 4,
            isError = state.errors["notes"] != null,
            supportingText = state.errors["notes"]?.let { { Text(it) } },
        )
        if (state.editing) {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Text("Active customer", style = MaterialTheme.typography.titleSmall)
                    Text("Inactive customers cannot be selected for new credit sales.", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                Switch(checked = draft.active, onCheckedChange = { value -> viewModel.updateDraft { it.copy(active = value) } })
            }
        }
        Button(onClick = { viewModel.save(id) }, modifier = Modifier.fillMaxWidth(), enabled = !state.saving) {
            if (state.saving) CircularProgressIndicator(Modifier.height(18.dp)) else Text(if (state.editing) "Save changes" else "Add customer")
        }
        TextButton(onClick = onBack, modifier = Modifier.align(Alignment.CenterHorizontally)) { Text("Cancel") }
    }
    }
}
