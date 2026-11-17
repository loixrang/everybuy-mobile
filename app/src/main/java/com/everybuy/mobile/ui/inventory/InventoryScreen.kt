package com.everybuy.mobile.ui.inventory

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.clickable
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
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
import com.everybuy.mobile.data.AuthUser
import com.everybuy.mobile.data.ProductDto
import com.everybuy.mobile.core.util.formatNaira
import com.everybuy.mobile.ui.auth.AppTheme

@OptIn(ExperimentalMaterial3Api::class)
@Composable fun InventoryScreen(user: AuthUser, theme: AppTheme, onSignOut: () -> Unit, onSessionExpired: () -> Unit, setTheme: (AppTheme) -> Unit, isOwner: Boolean, onProductSelected: (String) -> Unit, onAddProduct: () -> Unit, viewModel: InventoryViewModel = hiltViewModel()) {
    val state by viewModel.state.collectAsState()
    LaunchedEffect(state) { if (state is InventoryState.Unauthorized) onSessionExpired() }
    var search by rememberSaveable { mutableStateOf("") }
    var packFilter by rememberSaveable { mutableStateOf("ALL") }
    var categoryFilter by rememberSaveable { mutableStateOf("ALL") }
    var themeMenuExpanded by remember { mutableStateOf(false) }
    var categoryMenuExpanded by remember { mutableStateOf(false) }
    Scaffold(topBar = {
        TopAppBar(title = { Column { Text("Inventory"); Text(user.name.ifBlank { user.email }, style = MaterialTheme.typography.labelMedium) } }, actions = {
            Box {
                TextButton(onClick = { themeMenuExpanded = true }) { Text("${theme.name.lowercase().replaceFirstChar { it.uppercase() }} theme") }
                DropdownMenu(expanded = themeMenuExpanded, onDismissRequest = { themeMenuExpanded = false }) {
                    AppTheme.entries.forEach { option -> DropdownMenuItem(text = { Text(option.name.lowercase().replaceFirstChar { it.uppercase() }) }, onClick = { themeMenuExpanded = false; setTheme(option) }) }
                }
            }
            TextButton(onClick = onSignOut) { Text("Sign out") }
        })
    }, floatingActionButton = {
        if (isOwner) ExtendedFloatingActionButton(onClick = onAddProduct, icon = { Icon(Icons.Rounded.Add, contentDescription = null) }, text = { Text("Add product") })
    }) { insets ->
        Column(Modifier.fillMaxSize().padding(insets).padding(horizontal = 16.dp)) {
            OutlinedTextField(search, { search = it }, Modifier.fillMaxWidth().padding(top = 8.dp), label = { Text("Search products") }, singleLine = true)
            if (state is InventoryState.Ready) {
                val inventory = (state as InventoryState.Ready).inventory
                LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.padding(vertical = 8.dp)) {
                    items(listOf("All products" to "ALL", "Diapers" to "Small Pack", "Baby wipes" to "Wipes", "Sanitary pads" to "Pad", "Tissue" to "Tissue"), key = { it.second }) { (label, value) ->
                        FilterChip(selected = packFilter == value, onClick = { packFilter = value }, label = { Text(label) })
                    }
                }
                Box {
                    TextButton(onClick = { categoryMenuExpanded = true }) { Text(inventory.categories.firstOrNull { it.id == categoryFilter }?.name ?: "All categories") }
                    DropdownMenu(expanded = categoryMenuExpanded, onDismissRequest = { categoryMenuExpanded = false }) {
                        DropdownMenuItem(text = { Text("All categories") }, onClick = { categoryFilter = "ALL"; categoryMenuExpanded = false })
                        inventory.categories.forEach { category -> DropdownMenuItem(text = { Text(category.name) }, onClick = { categoryFilter = category.id; categoryMenuExpanded = false }) }
                    }
                }
            }
            Spacer(Modifier.height(12.dp))
            when (val current = state) {
                InventoryState.Loading -> Column(Modifier.fillMaxWidth().padding(32.dp), horizontalAlignment = Alignment.CenterHorizontally) { CircularProgressIndicator(); Text("Loading products…", Modifier.padding(top = 12.dp)) }
                is InventoryState.Failed -> Column(Modifier.fillMaxWidth().padding(24.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(current.message, color = MaterialTheme.colorScheme.error)
                    TextButton(onClick = { viewModel.refresh() }) { Text("Retry") }
                }
                InventoryState.Unauthorized -> Text("Your session has expired. Returning to sign in…", modifier = Modifier.padding(24.dp))
                is InventoryState.Ready -> {
                    val inventory = current.inventory
                    val categoryNames = inventory.categories.associate { it.id to it.name }
                    val brandNames = inventory.brands.associate { it.id to it.name }
                    val filtered = inventory.products.filter { p ->
                        (packFilter == "ALL" || p.packName == packFilter) &&
                            (categoryFilter == "ALL" || p.categoryId == categoryFilter) &&
                            "${p.name} ${p.packName} ${p.size.orEmpty()} ${categoryNames[p.categoryId].orEmpty()} ${brandNames[p.brandId].orEmpty()}".contains(search.trim(), ignoreCase = true)
                    }
                    Text("${filtered.size} products", style = MaterialTheme.typography.labelLarge, modifier = Modifier.padding(bottom = 8.dp))
                    if (filtered.isEmpty()) {
                        Text(if (search.isBlank()) "No active products yet." else "No products match your search.", style = MaterialTheme.typography.bodyLarge, modifier = Modifier.padding(20.dp))
                    } else LazyColumn(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        items(filtered, key = { it.id }) { product -> ProductCard(product, brandNames[product.brandId], categoryNames[product.categoryId], { onProductSelected(product.id) }) }
                    }
                }
            }
        }
    }
}

@Composable private fun ProductCard(product: ProductDto, brandName: String?, categoryName: String?, onClick: () -> Unit) {
    Card(modifier = Modifier.fillMaxWidth().clickable(onClick = onClick), shape = RoundedCornerShape(24.dp), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow)) {
        Row(Modifier.fillMaxWidth().padding(16.dp), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Text(product.name, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                Text(listOfNotNull(brandName, categoryName, product.packName.takeIf { it.isNotBlank() }, product.size?.takeIf { it.isNotBlank() }).joinToString(" · "), style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Text("${formatNaira(product.bagPrice)} per bag", style = MaterialTheme.typography.bodyMedium, modifier = Modifier.padding(top = 8.dp))
            }
            Column(horizontalAlignment = Alignment.End) {
                Text(product.stockPacks.toString(), style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
                Text("packs in stock", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
    }
}
