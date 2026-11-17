package com.everybuy.mobile.ui.settings

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.Refresh
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
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
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.everybuy.mobile.data.ShopSettingsDto
import com.everybuy.mobile.data.UpdateShopSettingsInput

@OptIn(ExperimentalMaterial3Api::class)
@Composable fun SettingsScreen(
    userName: String,
    isOwner: Boolean,
    onBack: () -> Unit,
    onOpenTeam: () -> Unit,
    onOpenCatalog: () -> Unit,
    onOpenSuppliers: () -> Unit,
    onSessionExpired: () -> Unit,
    viewModel: SettingsViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsState()
    var displayName by rememberSaveable(userName) { mutableStateOf(userName) }
    var pendingName by remember { mutableStateOf<String?>(null) }
    var showNameDialog by rememberSaveable { mutableStateOf(false) }
    var showShopDialog by rememberSaveable { mutableStateOf(false) }

    LaunchedEffect(state) { if (state is SettingsState.SessionExpired) onSessionExpired() }
    val ready = state as? SettingsState.Ready
    LaunchedEffect(ready?.profileSaved) {
        if (ready?.profileSaved == true) {
            pendingName?.let { displayName = it }
            pendingName = null
            showNameDialog = false
        }
    }
    LaunchedEffect(ready?.shopSaved) { if (ready?.shopSaved == true) showShopDialog = false }

    Scaffold(topBar = {
        TopAppBar(
            title = { Column { Text("Settings"); Text("Your account, the shop and team access", style = MaterialTheme.typography.labelMedium) } },
            navigationIcon = { IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Rounded.ArrowBack, contentDescription = "Back") } },
            actions = { IconButton(onClick = { viewModel.refresh() }) { Icon(Icons.Rounded.Refresh, contentDescription = "Refresh settings") } },
        )
    }) { insets ->
        when (val current = state) {
            SettingsState.Loading -> Column(Modifier.fillMaxSize().padding(insets), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center) {
                CircularProgressIndicator()
                Text("Loading settings…", Modifier.padding(top = 12.dp))
            }
            SettingsState.SessionExpired -> Text("Your session has expired.", Modifier.padding(insets).padding(24.dp))
            is SettingsState.Failed -> Column(Modifier.fillMaxSize().padding(insets).padding(24.dp), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center) {
                Text(current.message, color = MaterialTheme.colorScheme.error)
                TextButton(onClick = { viewModel.refresh() }) { Text("Retry") }
            }
            is SettingsState.Ready -> SettingsContent(
                state = current,
                displayName = displayName,
                isOwner = isOwner,
                onEditName = { viewModel.clearProfileFeedback(); showNameDialog = true },
                onEditShop = { viewModel.clearShopFeedback(); showShopDialog = true },
                onOpenTeam = onOpenTeam,
                onOpenCatalog = onOpenCatalog,
                onOpenSuppliers = onOpenSuppliers,
                onDismissFeedback = { viewModel.clearProfileFeedback(); viewModel.clearShopFeedback() },
                modifier = Modifier.fillMaxSize().padding(insets),
            )
        }
    }

    if (showNameDialog && ready != null) {
        NameDialog(
            currentName = displayName,
            saving = ready.savingProfile,
            error = ready.profileError,
            onDismiss = { if (!ready.savingProfile) { showNameDialog = false; viewModel.clearProfileFeedback() } },
            onConfirm = { name -> pendingName = name.trim(); viewModel.updateName(name) },
        )
    }
    val shop = ready?.shop
    if (showShopDialog && shop != null) {
        ShopDialog(
            shop = shop,
            saving = ready.savingShop,
            error = ready.shopError,
            onDismiss = { if (!ready.savingShop) { showShopDialog = false; viewModel.clearShopFeedback() } },
            onConfirm = { input -> viewModel.saveShop(shop.id, input) },
        )
    }
}

@Composable private fun SettingsContent(
    state: SettingsState.Ready,
    displayName: String,
    isOwner: Boolean,
    onEditName: () -> Unit,
    onEditShop: () -> Unit,
    onOpenTeam: () -> Unit,
    onOpenCatalog: () -> Unit,
    onOpenSuppliers: () -> Unit,
    onDismissFeedback: () -> Unit,
    modifier: Modifier = Modifier,
) {
    LazyColumn(modifier.padding(horizontal = 16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        item {
            SectionCard(
                title = "Profile",
                subtitle = "Read from your Everybuy account.",
                action = { TextButton(onClick = onEditName, enabled = !state.savingProfile) { Text("Edit") } },
            ) {
                LabeledValue("Display name", displayName.ifBlank { "—" })
                Text("Only your display name can be changed here.", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
        if (state.profileSaved) item { FeedbackCard("Your name was updated.", false, onDismissFeedback) }
        if (state.profileError != null) item { FeedbackCard(state.profileError, true, onDismissFeedback) }
        if (state.shopSaved) item { FeedbackCard("Shop details saved.", false, onDismissFeedback) }
        if (state.shopError != null) item { FeedbackCard(state.shopError, true, onDismissFeedback) }

        if (state.shopMissing) {
            item {
                SectionCard(title = "Shop details", subtitle = "No shop settings row exists yet.") {
                    Text(
                        "Shop details are not available yet. Ask an administrator to set them up on the web app.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
        } else {
            val shop = state.shop
            item {
                SectionCard(
                    title = "Shop details",
                    subtitle = if (isOwner) "Shown on receipts and reports. Only owners can change these." else "Only owners can change the shop details.",
                    action = if (isOwner && shop != null) {
                        { TextButton(onClick = onEditShop, enabled = !state.savingShop) { Text("Edit") } }
                    } else null,
                ) {
                    if (shop == null) {
                        Text("Shop details are unavailable.", style = MaterialTheme.typography.bodySmall)
                    } else {
                        LabeledValue("Name", shopValue(shop.name))
                        LabeledValue("Address", shopValue(shop.address))
                        LabeledValue("Phone", shopValue(shop.phone))
                        LabeledValue("Currency", shopValue(shop.currency))
                        LabeledValue("Timezone", shopValue(shop.timezone))
                        LabeledValue("Receipt footer", shopValue(shop.receiptFooter))
                    }
                }
            }
        }

        if (isOwner) {
            item { Text("Manage", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.SemiBold, modifier = Modifier.padding(top = 6.dp)) }
            item { NavigationCard("Team", "Roles, access and pending invitations", onOpenTeam) }
            item { NavigationCard("Categories & brands", "Organise products used in the inventory", onOpenCatalog) }
            item { NavigationCard("Suppliers", "Contacts used when receiving stock", onOpenSuppliers) }
        }
    }
}

@Composable private fun NavigationCard(title: String, supporting: String, onClick: () -> Unit) {
    Card(Modifier.fillMaxWidth().clickable(onClick = onClick), shape = RoundedCornerShape(20.dp), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow)) {
        Column(Modifier.fillMaxWidth().padding(16.dp)) {
            Text(title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
            Text(supporting, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.padding(top = 2.dp))
        }
    }
}

@Composable private fun NameDialog(currentName: String, saving: Boolean, error: String?, onDismiss: () -> Unit, onConfirm: (String) -> Unit) {
    var name by rememberSaveable { mutableStateOf(currentName) }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Display name") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(name, { if (it.length <= PROFILE_NAME_MAX) name = it }, Modifier.fillMaxWidth(), label = { Text("Your name") }, singleLine = true)
                if (error != null) Text(error, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall)
            }
        },
        confirmButton = {
            Button(onClick = { onConfirm(name) }, enabled = !saving && name.isNotBlank()) {
                if (saving) CircularProgressIndicator(Modifier.padding(2.dp)) else Text("Save name")
            }
        },
        dismissButton = { TextButton(onClick = onDismiss, enabled = !saving) { Text("Cancel") } },
    )
}

@Composable private fun ShopDialog(
    shop: ShopSettingsDto,
    saving: Boolean,
    error: String?,
    onDismiss: () -> Unit,
    onConfirm: (UpdateShopSettingsInput) -> Unit,
) {
    var name by remember(shop.id) { mutableStateOf(shop.name) }
    var address by remember(shop.id) { mutableStateOf(shop.address.orEmpty()) }
    var phone by remember(shop.id) { mutableStateOf(shop.phone.orEmpty()) }
    var currency by remember(shop.id) { mutableStateOf(shop.currency) }
    var timezone by remember(shop.id) { mutableStateOf(shop.timezone) }
    var receiptFooter by remember(shop.id) { mutableStateOf(shop.receiptFooter.orEmpty()) }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Edit shop details") },
        text = {
            Column(Modifier.verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(name, { if (it.length <= SHOP_NAME_MAX) name = it }, Modifier.fillMaxWidth(), label = { Text("Shop name") }, singleLine = true)
                OutlinedTextField(address, { address = it }, Modifier.fillMaxWidth(), label = { Text("Address") }, minLines = 2, maxLines = 3)
                OutlinedTextField(phone, { phone = it }, Modifier.fillMaxWidth(), label = { Text("Phone") }, singleLine = true)
                OutlinedTextField(currency, { currency = it }, Modifier.fillMaxWidth(), label = { Text("Currency code") }, singleLine = true)
                OutlinedTextField(timezone, { timezone = it }, Modifier.fillMaxWidth(), label = { Text("Timezone") }, singleLine = true)
                OutlinedTextField(receiptFooter, { receiptFooter = it }, Modifier.fillMaxWidth(), label = { Text("Receipt footer") }, minLines = 2, maxLines = 4)
                if (error != null) Text(error, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall)
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    onConfirm(
                        UpdateShopSettingsInput(
                            name = name,
                            address = address.trim().ifEmpty { null },
                            phone = phone.trim().ifEmpty { null },
                            currency = currency,
                            timezone = timezone,
                            receiptFooter = receiptFooter.trim().ifEmpty { null },
                        ),
                    )
                },
                enabled = !saving && name.isNotBlank() && currency.isNotBlank(),
            ) {
                if (saving) CircularProgressIndicator(Modifier.padding(2.dp)) else Text("Save shop")
            }
        },
        dismissButton = { TextButton(onClick = onDismiss, enabled = !saving) { Text("Cancel") } },
    )
}
