package com.everybuy.mobile.ui.settings

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.Logout
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.FilledTonalButton
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
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.everybuy.mobile.R
import com.everybuy.mobile.data.ShopSettingsDto
import com.everybuy.mobile.data.UpdateShopSettingsInput
import com.everybuy.mobile.ui.auth.AppTheme
import com.everybuy.mobile.ui.components.ConnectedButtonGroup
import com.everybuy.mobile.ui.components.EmptyPane
import com.everybuy.mobile.ui.components.ErrorPane
import com.everybuy.mobile.ui.components.EverybuyScreen
import com.everybuy.mobile.ui.components.LoadingPane
import com.everybuy.mobile.ui.components.SectionHeader
import com.everybuy.mobile.ui.components.TonalCard
import com.everybuy.mobile.ui.components.listBottomPadding

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun SettingsScreen(
    userName: String,
    userEmail: String,
    role: String,
    theme: AppTheme,
    onThemeChange: (AppTheme) -> Unit,
    isOwner: Boolean,
    onBack: () -> Unit,
    onOpenTeam: () -> Unit,
    onOpenCatalog: () -> Unit,
    onOpenSuppliers: () -> Unit,
    onSignOut: () -> Unit,
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

    val themeLabels = AppTheme.entries.map { option ->
        when (option) {
            AppTheme.SYSTEM -> stringResource(R.string.theme_system)
            AppTheme.LIGHT -> stringResource(R.string.theme_light)
            AppTheme.DARK -> stringResource(R.string.theme_dark)
            AppTheme.AMOLED -> stringResource(R.string.theme_amoled)
        }
    }

    EverybuyScreen(
        title = stringResource(R.string.settings_title),
        subtitle = userName,
        onOpenSettings = null,
        onBack = onBack,
        onRefresh = { viewModel.refresh() },
    ) { padding ->
        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.TopCenter) {
            LazyColumn(
                modifier = Modifier
                    .fillMaxWidth()
                    .fillMaxHeight()
                    .widthIn(max = 840.dp),
                contentPadding = PaddingValues(
                    start = 16.dp,
                    end = 16.dp,
                    top = padding.calculateTopPadding(),
                    bottom = padding.calculateBottomPadding() + listBottomPadding(),
                ),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
            item {
                SectionHeader(
                    title = stringResource(R.string.settings_theme),
                    supporting = stringResource(R.string.settings_theme_body),
                    modifier = Modifier.padding(top = 8.dp),
                )
            }
            item {
                ConnectedButtonGroup(
                    options = AppTheme.entries.toList(),
                    selected = theme,
                    onSelect = onThemeChange,
                    label = { themeLabels[it.ordinal] },
                )
            }
            item {
                SectionHeader(
                    title = stringResource(R.string.settings_account),
                    supporting = stringResource(R.string.settings_account_body),
                    modifier = Modifier.padding(top = 8.dp),
                )
            }
            item {
                TonalCard(modifier = Modifier.fillMaxWidth()) {
                    Column(Modifier.fillMaxWidth().padding(18.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        LabeledValue(stringResource(R.string.settings_name), displayName.ifBlank { "—" })
                        LabeledValue(stringResource(R.string.settings_email), userEmail)
                        LabeledValue(stringResource(R.string.settings_role), role)
                        Row(
                            Modifier.fillMaxWidth().padding(top = 4.dp),
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            FilledTonalButton(onClick = { viewModel.clearProfileFeedback(); showNameDialog = true }) {
                                Text(stringResource(R.string.settings_edit_name))
                            }
                            Button(
                                onClick = onSignOut,
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = MaterialTheme.colorScheme.errorContainer,
                                    contentColor = MaterialTheme.colorScheme.onErrorContainer,
                                ),
                            ) {
                                Icon(Icons.AutoMirrored.Rounded.Logout, contentDescription = null, modifier = Modifier.padding(end = 6.dp))
                                Text(stringResource(R.string.action_sign_out))
                            }
                        }
                        Text(
                            stringResource(R.string.settings_sign_out_body),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
            }

            when (val current = state) {
                SettingsState.Loading -> item { LoadingPane(stringResource(R.string.settings_loading)) }
                SettingsState.SessionExpired -> item { ErrorPane(stringResource(R.string.session_expired), null) }
                is SettingsState.Failed -> item { ErrorPane(current.message, { viewModel.refresh() }) }
                is SettingsState.Ready -> {
                    item {
                        SectionHeader(
                            title = stringResource(R.string.settings_profile),
                            supporting = stringResource(R.string.settings_profile_body),
                            modifier = Modifier.padding(top = 8.dp),
                        )
                    }
                    if (current.profileSaved) item { FeedbackCard("Your name was updated.", false, { viewModel.clearProfileFeedback() }) }
                    if (current.profileError != null) item { FeedbackCard(current.profileError, true, { viewModel.clearProfileFeedback() }) }
                    if (current.shopSaved) item { FeedbackCard("Shop details saved.", false, { viewModel.clearShopFeedback() }) }
                    if (current.shopError != null) item { FeedbackCard(current.shopError, true, { viewModel.clearShopFeedback() }) }

                    if (current.shopMissing) {
                        item {
                            TonalCard(Modifier.fillMaxWidth()) {
                                Column(Modifier.fillMaxWidth().padding(18.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                    Text(stringResource(R.string.settings_shop), style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                                    Text(
                                        "Shop details are not available yet. Ask an administrator to set them up on the web app.",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    )
                                }
                            }
                        }
                    } else {
                        val shop = current.shop
                        item {
                            TonalCard(Modifier.fillMaxWidth()) {
                                Column(Modifier.fillMaxWidth().padding(18.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                                        Column(Modifier.weight(1f)) {
                                            Text(stringResource(R.string.settings_shop), style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                                            Text(
                                                stringResource(if (isOwner) R.string.settings_shop_body_owner else R.string.settings_shop_body_staff),
                                                style = MaterialTheme.typography.bodySmall,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                            )
                                        }
                                        if (isOwner && shop != null) {
                                            FilledTonalButton(onClick = { viewModel.clearShopFeedback(); showShopDialog = true }, enabled = !current.savingShop) {
                                                Text(stringResource(R.string.settings_edit_name))
                                            }
                                        }
                                    }
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
                    }

                    if (isOwner) {
                        item { SectionHeader(title = stringResource(R.string.settings_manage), modifier = Modifier.padding(top = 8.dp)) }
                        item { NavigationCard(stringResource(R.string.settings_team), stringResource(R.string.settings_team_body), onOpenTeam) }
                        item { NavigationCard(stringResource(R.string.settings_catalog), stringResource(R.string.settings_catalog_body), onOpenCatalog) }
                        item { NavigationCard(stringResource(R.string.settings_suppliers), stringResource(R.string.settings_suppliers_body), onOpenSuppliers) }
                    }
                }
            }
        }
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

@Composable
private fun NavigationCard(title: String, supporting: String, onClick: () -> Unit) {
    TonalCard(Modifier.fillMaxWidth(), onClick = onClick) {
        Column(Modifier.fillMaxWidth().padding(18.dp)) {
            Text(title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
            Text(
                supporting,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(top = 2.dp),
            )
        }
    }
}

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
private fun NameDialog(currentName: String, saving: Boolean, error: String?, onDismiss: () -> Unit, onConfirm: (String) -> Unit) {
    var name by rememberSaveable { mutableStateOf(currentName) }
    AlertDialog(
        onDismissRequest = onDismiss,
        shape = MaterialTheme.shapes.extraLarge,
        containerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
        title = { Text(stringResource(R.string.settings_name)) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(
                    value = name,
                    onValueChange = { if (it.length <= PROFILE_NAME_MAX) name = it },
                    modifier = Modifier.fillMaxWidth(),
                    label = { Text(stringResource(R.string.settings_name)) },
                    singleLine = true,
                    shape = MaterialTheme.shapes.large,
                )
                if (error != null) Text(error, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall)
            }
        },
        confirmButton = {
            Button(onClick = { onConfirm(name) }, enabled = !saving && name.isNotBlank()) {
                if (saving) LoadingIndicator(modifier = Modifier.padding(2.dp)) else Text("Save name")
            }
        },
        dismissButton = { TextButton(onClick = onDismiss, enabled = !saving) { Text("Cancel") } },
    )
}

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
private fun ShopDialog(
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
        shape = MaterialTheme.shapes.extraLarge,
        containerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
        title = { Text("Edit shop details") },
        text = {
            Column(Modifier.verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(name, { if (it.length <= SHOP_NAME_MAX) name = it }, Modifier.fillMaxWidth(), label = { Text("Shop name") }, singleLine = true, shape = MaterialTheme.shapes.large)
                OutlinedTextField(address, { address = it }, Modifier.fillMaxWidth(), label = { Text("Address") }, minLines = 2, maxLines = 3, shape = MaterialTheme.shapes.large)
                OutlinedTextField(phone, { phone = it }, Modifier.fillMaxWidth(), label = { Text("Phone") }, singleLine = true, shape = MaterialTheme.shapes.large)
                OutlinedTextField(currency, { currency = it }, Modifier.fillMaxWidth(), label = { Text("Currency code") }, singleLine = true, shape = MaterialTheme.shapes.large)
                OutlinedTextField(timezone, { timezone = it }, Modifier.fillMaxWidth(), label = { Text("Timezone") }, singleLine = true, shape = MaterialTheme.shapes.large)
                OutlinedTextField(receiptFooter, { receiptFooter = it }, Modifier.fillMaxWidth(), label = { Text("Receipt footer") }, minLines = 2, maxLines = 4, shape = MaterialTheme.shapes.large)
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
                if (saving) LoadingIndicator(modifier = Modifier.padding(2.dp)) else Text("Save shop")
            }
        },
        dismissButton = { TextButton(onClick = onDismiss, enabled = !saving) { Text("Cancel") } },
    )
}
