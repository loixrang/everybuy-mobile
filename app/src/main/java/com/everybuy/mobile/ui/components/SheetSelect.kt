package com.everybuy.mobile.ui.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.ExpandMore
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.SheetValue
import androidx.compose.material3.rememberBottomSheetState
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
import androidx.compose.ui.semantics.Role
import androidx.compose.foundation.selection.selectable
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.paneTitle
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import com.everybuy.mobile.core.util.matchesSearch
import kotlinx.coroutines.launch

data class SheetSelectOption<T>(
    val key: Any,
    val value: T?,
    val label: String,
    val sublabel: String? = null,
    val leadingContent: (@Composable () -> Unit)? = null,
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun <T> SheetSelect(
    label: String,
    placeholder: String,
    options: List<SheetSelectOption<T>>,
    selected: T?,
    onSelect: (T?) -> Unit,
    searchable: Boolean = false,
    enabled: Boolean = true,
    error: String? = null,
    supportingText: String? = null,
) {
    var expanded by rememberSaveable { mutableStateOf(false) }
    var query by rememberSaveable { mutableStateOf("") }
    val sheetState = rememberBottomSheetState(initialValue = SheetValue.Hidden)
    val scope = rememberCoroutineScope()
    val valueLabel = options.firstOrNull { it.value == selected }?.label ?: placeholder
    OutlinedTextField(
        value = valueLabel,
        onValueChange = {},
        modifier = Modifier.fillMaxWidth().clickable(enabled = enabled) { expanded = true },
        label = { Text(label) },
        readOnly = true,
        enabled = enabled,
        singleLine = true,
        isError = error != null,
        supportingText = when {
            error != null -> ({ Text(error) })
            supportingText != null -> ({ Text(supportingText) })
            else -> null
        },
        trailingIcon = {
            IconButton(onClick = { expanded = true }, enabled = enabled) {
                Icon(Icons.Rounded.ExpandMore, contentDescription = "Choose $label")
            }
        },
        shape = MaterialTheme.shapes.large,
    )
    if (expanded) {
        val keyboard = LocalSoftwareKeyboardController.current
        val filtered by remember(options, query) {
            derivedStateOf {
                if (!searchable || query.isBlank()) options
                else options.filter { matchesSearch("${it.label} ${it.sublabel.orEmpty()}", query) }
            }
        }
        GlassModalBottomSheet(
            onDismissRequest = { expanded = false },
            sheetState = sheetState,
        ) {
            val maxHeight = LocalConfiguration.current.screenHeightDp.dp * 0.7f
            Column(
                Modifier
                    .fillMaxWidth()
                    .heightIn(max = maxHeight)
                    .padding(bottom = 16.dp),
            ) {
                SheetHeader(title = label, onClose = {
                    scope.launch {
                        sheetState.hide()
                        expanded = false
                    }
                })
                if (searchable) {
                    ExpressiveSearchField(
                        value = query,
                        onValueChange = { query = it },
                        placeholder = "Search",
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp),
                        onSearch = { keyboard?.hide() },
                    )
                }
                if (filtered.isEmpty()) {
                    Text(
                        text = "No match.",
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        style = MaterialTheme.typography.bodyMedium,
                        modifier = Modifier.padding(horizontal = 20.dp, vertical = 18.dp),
                    )
                } else {
                    LazyColumn(
                        modifier = Modifier.weight(1f, fill = false),
                    ) {
                        itemsIndexed(filtered, key = { _, option -> option.key }) { index, option ->
                            SheetSelectRow(
                                label = option.label,
                                sublabel = option.sublabel,
                                selected = option.value == selected,
                                leadingContent = option.leadingContent,
                                onClick = {
                                    onSelect(option.value)
                                    keyboard?.hide()
                                    scope.launch {
                                        sheetState.hide()
                                        expanded = false
                                    }
                                },
                            )
                            if (index < filtered.lastIndex) HorizontalDivider(
                                modifier = Modifier.padding(start = 16.dp, end = 16.dp),
                                color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f),
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun SheetHeader(
    title: String,
    onClose: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .semantics {
                heading()
                paneTitle = title
            }
            .padding(start = 20.dp, end = 8.dp, top = 8.dp, bottom = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        Text(
            text = title,
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.SemiBold,
            modifier = Modifier.weight(1f),
        )
        IconButton(onClick = onClose) {
            Icon(Icons.Rounded.Close, contentDescription = "Close")
        }
    }
}

@Composable
fun SheetSelectRow(
    label: String,
    sublabel: String? = null,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    leadingContent: (@Composable () -> Unit)? = null,
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .defaultMinSize(minHeight = 52.dp)
            .selectable(selected = selected, role = Role.RadioButton, onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        leadingContent?.invoke()
        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Text(
                text = label,
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = if (selected) FontWeight.Medium else FontWeight.Normal,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            sublabel?.let {
                Text(
                    text = it,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
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
