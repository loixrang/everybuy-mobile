package com.everybuy.mobile.ui.sales

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.ExpandMore
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.everybuy.mobile.core.util.formatNaira
import com.everybuy.mobile.core.util.matchesSearch
import com.everybuy.mobile.data.CustomerListItemDto
import com.everybuy.mobile.ui.components.ExpressiveSearchField
import com.everybuy.mobile.ui.components.GlassModalBottomSheet
import com.everybuy.mobile.ui.components.SheetHeader
import com.everybuy.mobile.ui.theme.LocalWarningColor
import kotlinx.coroutines.launch

private enum class CustomerMode { WALK_IN, CUSTOMER }

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SaleCustomerPicker(
    customers: List<CustomerListItemDto>,
    selectedCustomerId: String?,
    onSelect: (String?) -> Unit,
    onNewCustomer: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val selectedCustomer = customers.firstOrNull { it.id == selectedCustomerId }
    val balance = selectedCustomer?.let { customerBalanceLine(it) }
    var sheetOpen by rememberSaveable { mutableStateOf(false) }
    var query by rememberSaveable { mutableStateOf("") }
    var mode by rememberSaveable { mutableStateOf(CustomerMode.WALK_IN) }
    val scope = rememberCoroutineScope()
    val sheetState = androidx.compose.material3.rememberBottomSheetState(
        initialValue = androidx.compose.material3.SheetValue.Hidden,
    )

    Box(
        modifier
            .fillMaxWidth()
            .clickable(role = Role.Button) {
                mode = if (selectedCustomerId == null) CustomerMode.WALK_IN else CustomerMode.CUSTOMER
                sheetOpen = true
            },
    ) {
        OutlinedTextField(
            value = selectedCustomer?.name ?: if (selectedCustomerId == null) "Walk-in customer" else "Customer added",
            onValueChange = {},
            modifier = Modifier.fillMaxWidth(),
            label = { Text("Customer") },
            enabled = false,
            singleLine = true,
            supportingText = {
                when {
                    selectedCustomer == null && selectedCustomerId == null -> Text("Full payment")
                    balance != null -> Text(balance.text, color = balance.color)
                }
            },
            trailingIcon = { Icon(Icons.Rounded.ExpandMore, contentDescription = null) },
            shape = MaterialTheme.shapes.large,
            colors = OutlinedTextFieldDefaults.colors(
                disabledTextColor = MaterialTheme.colorScheme.onSurface,
                disabledContainerColor = Color.Transparent,
                disabledBorderColor = MaterialTheme.colorScheme.outline,
                disabledLabelColor = MaterialTheme.colorScheme.onSurfaceVariant,
                disabledTrailingIconColor = MaterialTheme.colorScheme.onSurfaceVariant,
                disabledSupportingTextColor = MaterialTheme.colorScheme.onSurfaceVariant,
            ),
        )
    }

    if (sheetOpen) {
        val keyboard = LocalSoftwareKeyboardController.current
        val filtered by remember(customers, query) {
            derivedStateOf { customers.filter { matchesSearch(it.name, query) } }
        }
        GlassModalBottomSheet(
            onDismissRequest = { sheetOpen = false },
            sheetState = sheetState,
        ) {
            Column(
                Modifier
                    .fillMaxWidth()
                    .heightIn(max = LocalConfiguration.current.screenHeightDp.dp * 0.7f)
                    .imePadding()
                    .padding(bottom = 12.dp),
            ) {
                SheetHeader(
                    title = "Who is buying?",
                    onClose = {
                        scope.launch {
                            sheetState.hide()
                            sheetOpen = false
                        }
                    },
                )
                SingleChoiceSegmentedButtonRow(
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 4.dp),
                ) {
                    SegmentedButton(
                        selected = mode == CustomerMode.WALK_IN,
                        onClick = {
                            mode = CustomerMode.WALK_IN
                            keyboard?.hide()
                            scope.launch {
                                sheetState.hide()
                                onSelect(null)
                                sheetOpen = false
                            }
                        },
                        shape = SegmentedButtonDefaults.itemShape(index = 0, count = 2),
                        label = { Text("Walk-in", maxLines = 1) },
                    )
                    SegmentedButton(
                        selected = mode == CustomerMode.CUSTOMER,
                        onClick = { mode = CustomerMode.CUSTOMER },
                        shape = SegmentedButtonDefaults.itemShape(index = 1, count = 2),
                        label = { Text("Customer", maxLines = 1) },
                    )
                }
                if (mode == CustomerMode.WALK_IN) {
                    Text(
                        "Paid in full, no customer attached.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(horizontal = 20.dp, vertical = 16.dp),
                    )
                } else {
                    ExpressiveSearchField(
                        value = query,
                        onValueChange = { query = it },
                        placeholder = "Search customers",
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp),
                        onSearch = { keyboard?.hide() },
                    )
                    if (filtered.isEmpty()) {
                        Text(
                            "No match. Add them as a new customer.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(horizontal = 20.dp, vertical = 16.dp),
                        )
                    } else {
                        LazyColumn(
                            modifier = Modifier.weight(1f, fill = false),
                        ) {
                            itemsIndexed(filtered, key = { _, customer -> customer.id }) { index, customer ->
                                CustomerOptionRow(
                                    customer = customer,
                                    selected = customer.id == selectedCustomerId,
                                    onClick = {
                                        keyboard?.hide()
                                        scope.launch {
                                            sheetState.hide()
                                            onSelect(customer.id)
                                            sheetOpen = false
                                        }
                                    },
                                )
                                if (index < filtered.lastIndex) {
                                    HorizontalDivider(
                                        modifier = Modifier.padding(horizontal = 16.dp),
                                        color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f),
                                    )
                                }
                            }
                        }
                    }
                }
                OutlinedButton(
                    onClick = {
                        keyboard?.hide()
                        scope.launch {
                            sheetState.hide()
                            sheetOpen = false
                            onNewCustomer()
                        }
                    },
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp).heightIn(min = 48.dp),
                ) {
                    Icon(Icons.Rounded.Add, contentDescription = null)
                    Text("New customer", modifier = Modifier.padding(start = 8.dp))
                }
            }
        }
    }
}

@Composable
private fun CustomerOptionRow(
    customer: CustomerListItemDto,
    selected: Boolean,
    onClick: () -> Unit,
) {
    val balance = customerBalanceLine(customer)
    Row(
        Modifier
            .fillMaxWidth()
            .defaultMinSize(minHeight = 60.dp)
            .selectable(selected = selected, role = Role.RadioButton, onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Surface(
            modifier = Modifier.size(38.dp),
            shape = CircleShape,
            color = MaterialTheme.colorScheme.primaryContainer,
            contentColor = MaterialTheme.colorScheme.onPrimaryContainer,
        ) {
            Box(contentAlignment = Alignment.Center) {
                Text(
                    customerInitials(customer.name),
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.SemiBold,
                )
            }
        }
        Column(Modifier.weight(1f)) {
            Text(
                customer.name,
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = if (selected) FontWeight.Medium else FontWeight.Normal,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            balance?.let {
                Text(
                    it.text,
                    style = MaterialTheme.typography.bodySmall,
                    color = it.color,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        }
        if (selected) Icon(
            Icons.Rounded.Check,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.primary,
            modifier = Modifier.size(24.dp),
        )
    }
}

private data class CustomerBalanceLine(val text: String, val color: androidx.compose.ui.graphics.Color)

@Composable
private fun customerBalanceLine(customer: CustomerListItemDto): CustomerBalanceLine? {
    val balance = customer.balance.toBigDecimalOrNull() ?: return null
    return if (balance > java.math.BigDecimal.ZERO) {
        CustomerBalanceLine("Owes ${formatNaira(customer.balance)}", LocalWarningColor.current)
    } else {
        CustomerBalanceLine("No balance", MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

private fun customerInitials(name: String): String = name
    .trim()
    .split(Regex("\\s+"))
    .filter(String::isNotBlank)
    .take(2)
    .mapNotNull { it.firstOrNull()?.uppercaseChar() }
    .joinToString("")
