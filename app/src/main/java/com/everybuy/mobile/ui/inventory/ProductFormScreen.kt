package com.everybuy.mobile.ui.inventory

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
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
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.compose.foundation.clickable

private val packNames = listOf("Small Pack", "Twin Pack", "Jumbo Pack", "Wipes", "Pad", "Tissue")

@OptIn(ExperimentalMaterial3Api::class)
@Composable fun ProductFormScreen(id: String?, onBack: () -> Unit, onSaved: (String) -> Unit, onSessionExpired: () -> Unit, viewModel: ProductFormViewModel = hiltViewModel()) {
    val state by viewModel.state.collectAsState()
    LaunchedEffect(id) { viewModel.load(id) }
    LaunchedEffect(state) {
        if (state is ProductFormState.Saved) onSaved((state as ProductFormState.Saved).productId)
        if ((state as? ProductFormState.Failed)?.message?.let { "401" in it || "session has expired" in it.lowercase() } == true || (state as? ProductFormState.Ready)?.sessionExpired == true) onSessionExpired()
    }
    Scaffold(topBar = { TopAppBar(title = { Text(if (id == null) "Add product" else "Edit product") }, navigationIcon = { IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Rounded.ArrowBack, contentDescription = "Back") } }) }) { insets ->
        when (val current = state) {
            ProductFormState.Loading -> Column(Modifier.fillMaxSize().padding(insets), verticalArrangement = Arrangement.Center, horizontalAlignment = Alignment.CenterHorizontally) { CircularProgressIndicator(); Text("Loading product setup…", Modifier.padding(top = 12.dp)) }
            is ProductFormState.Failed -> Column(Modifier.fillMaxSize().padding(insets).padding(24.dp), verticalArrangement = Arrangement.Center, horizontalAlignment = Alignment.CenterHorizontally) {
                Text(current.message, color = MaterialTheme.colorScheme.error)
                TextButton(onClick = { viewModel.load(id) }) { Text("Retry") }
            }
            is ProductFormState.Saved -> Unit
            is ProductFormState.Ready -> ProductFormContent(current, viewModel, id, Modifier.fillMaxSize().padding(insets))
        }
    }
}

@Composable private fun ProductFormContent(state: ProductFormState.Ready, viewModel: ProductFormViewModel, id: String?, modifier: Modifier) {
    val d = state.draft
    Column(modifier.verticalScroll(rememberScrollState()).padding(horizontal = 20.dp, vertical = 16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Text(if (state.editing) "Update product details" else "Define the pack and bag units before receiving stock.", style = MaterialTheme.typography.bodyLarge, color = MaterialTheme.colorScheme.onSurfaceVariant)
        state.formError?.let { Text(it, color = MaterialTheme.colorScheme.error) }
        ProductEntry("Product name", d.name, state.errors["name"], { value -> viewModel.updateDraft { it.copy(name = value) } })
        ProductChoice("Brand", state.brands.map { it.id to it.name }, d.brandId, state.errors["brandId"], { value -> viewModel.updateDraft { it.copy(brandId = value) } })
        ProductChoice("Category", state.categories.map { it.id to it.name }, d.categoryId, state.errors["categoryId"], { value -> viewModel.updateDraft { it.copy(categoryId = value) } })
        ProductChoice("Pack type", packNames.map { it to it }, d.packName, state.errors["packName"], { value -> viewModel.updateDraft { it.copy(packName = value) } })
        ProductEntry("Size (optional)", d.size, null, { value -> viewModel.updateDraft { it.copy(size = value) } })
        ProductEntry("Packs per bag", d.packsPerBag, state.errors["packsPerBag"], { value -> viewModel.updateDraft { it.copy(packsPerBag = value) } }, KeyboardType.Number)
        ProductEntry("Pieces per pack (optional)", d.piecesPerPack, state.errors["piecesPerPack"], { value -> viewModel.updateDraft { it.copy(piecesPerPack = value) } }, KeyboardType.Number)
        Text("Selling prices", style = MaterialTheme.typography.titleLarge, modifier = Modifier.padding(top = 8.dp))
        ProductEntry("Pack price (₦)", d.retailPrice, state.errors["retailPrice"], { value -> viewModel.updateDraft { it.copy(retailPrice = value) } }, KeyboardType.Decimal)
        ProductEntry("Minimum pack price (₦)", d.minRetailPrice, state.errors["minRetailPrice"], { value -> viewModel.updateDraft { it.copy(minRetailPrice = value) } }, KeyboardType.Decimal)
        ProductEntry("Bag price (₦)", d.bagPrice, state.errors["bagPrice"], { value -> viewModel.updateDraft { it.copy(bagPrice = value) } }, KeyboardType.Decimal)
        ProductEntry("Minimum bag price (₦)", d.minBagPrice, state.errors["minBagPrice"], { value -> viewModel.updateDraft { it.copy(minBagPrice = value) } }, KeyboardType.Decimal)
        Text("Half-bag pricing is optional and requires an even pack count.", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        ProductEntry("Half-bag price (₦)", d.halfBagPrice, state.errors["halfBagPrice"], { value -> viewModel.updateDraft { it.copy(halfBagPrice = value) } }, KeyboardType.Decimal)
        ProductEntry("Minimum half-bag price (₦)", d.minHalfBagPrice, state.errors["minHalfBagPrice"], { value -> viewModel.updateDraft { it.copy(minHalfBagPrice = value) } }, KeyboardType.Decimal)
        Text("Acquisition cost", style = MaterialTheme.typography.titleLarge, modifier = Modifier.padding(top = 8.dp))
        ProductEntry("Cost per pack (₦, optional)", d.costPerPack, state.errors["costPerPack"], { value -> viewModel.updateDraft { it.copy(costPerPack = value) } }, KeyboardType.Decimal)
        if (state.editing) Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween) {
            Text("Product active", style = MaterialTheme.typography.titleMedium)
            Switch(checked = d.active, onCheckedChange = { checked -> viewModel.updateDraft { it.copy(active = checked) } })
        }
        Spacer(Modifier.height(8.dp))
        Button(onClick = { viewModel.save(id) }, modifier = Modifier.fillMaxWidth(), enabled = !state.saving && state.categories.isNotEmpty() && state.brands.isNotEmpty()) {
            if (state.saving) CircularProgressIndicator(Modifier.height(18.dp)) else Text(if (state.editing) "Save changes" else "Create product")
        }
        if (state.categories.isEmpty() || state.brands.isEmpty()) Text("An active brand and category are required. Check the catalog and retry.", color = MaterialTheme.colorScheme.error)
        TextButton(onClick = onBack, modifier = Modifier.align(Alignment.CenterHorizontally)) { Text("Cancel") }
    }
}

@Composable private fun ProductEntry(label: String, value: String, error: String?, onValue: (String) -> Unit, keyboardType: KeyboardType = KeyboardType.Text) {
    OutlinedTextField(
        value = value,
        onValueChange = onValue,
        modifier = Modifier.fillMaxWidth(),
        label = { Text(label) },
        singleLine = true,
        isError = error != null,
        supportingText = if (error == null) null else { { Text(error) } },
        keyboardOptions = KeyboardOptions(keyboardType = keyboardType),
    )
}

@Composable private fun ProductChoice(label: String, options: List<Pair<String, String>>, selected: String, error: String?, onSelected: (String) -> Unit) {
    var expanded by remember { mutableStateOf(false) }
    val value = options.firstOrNull { it.first == selected }?.second ?: "Choose $label"
    androidx.compose.material3.Box {
        OutlinedTextField(value = value, onValueChange = {}, modifier = Modifier.fillMaxWidth().clickable { expanded = true }, label = { Text(label) }, readOnly = true, singleLine = true, isError = error != null, supportingText = if (error == null) null else { { Text(error) } })
        DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
            options.forEach { (id, name) -> DropdownMenuItem(text = { Text(name) }, onClick = { onSelected(id); expanded = false }) }
        }
    }
}
