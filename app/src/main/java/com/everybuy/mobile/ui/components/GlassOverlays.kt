package com.everybuy.mobile.ui.components

import android.os.Build
import android.os.Handler
import android.os.Looper
import android.database.ContentObserver
import android.provider.Settings
import android.view.WindowManager
import android.view.WindowManager.LayoutParams
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.SheetState
import androidx.compose.material3.SheetValue
import androidx.compose.material3.rememberBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.DialogProperties
import androidx.compose.ui.window.DialogWindowProvider
import kotlinx.coroutines.launch
import androidx.compose.runtime.rememberCoroutineScope
import java.util.function.Consumer
import com.everybuy.mobile.ui.theme.systemAnimationsEnabled

private const val GLASS_ALPHA = 0.82f
private const val SOLID_ALPHA = 0.98f
private const val GLASS_SCRIM_ALPHA = 0.15f
private const val SOLID_SCRIM_ALPHA = 0.45f
private const val BLUR_RADIUS_DP = 24
private val SheetShape = RoundedCornerShape(topStart = 16.dp, topEnd = 16.dp)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GlassModalBottomSheet(
    onDismissRequest: () -> Unit,
    modifier: Modifier = Modifier,
    sheetState: SheetState = rememberBottomSheetState(initialValue = SheetValue.Hidden),
    content: @Composable ColumnScope.() -> Unit,
) {
    val scheme = MaterialTheme.colorScheme
    val crossWindowBlurEnabled = rememberCrossWindowBlurEnabled()
    val glassEnabled = crossWindowBlurEnabled && rememberSystemAnimationsEnabled()
    val scope = rememberCoroutineScope()
    val borderColor = scheme.outline.copy(alpha = if (scheme.surface.luminance() < 0.5f) 0.1f else 0.08f)
    ModalBottomSheet(
        onDismissRequest = {
            scope.launch {
                sheetState.hide()
                if (!sheetState.isVisible) onDismissRequest()
            }
        },
        modifier = modifier.border(1.dp, borderColor, SheetShape),
        sheetState = sheetState,
        shape = SheetShape,
        containerColor = scheme.surfaceContainer.copy(alpha = if (glassEnabled) GLASS_ALPHA else SOLID_ALPHA),
        contentColor = scheme.onSurface,
        tonalElevation = 8.dp,
        scrimColor = scheme.scrim.copy(alpha = if (glassEnabled) GLASS_SCRIM_ALPHA else SOLID_SCRIM_ALPHA),
        dragHandle = { androidx.compose.material3.BottomSheetDefaults.DragHandle() },
    ) {
        WindowBlurEffect()
        content()
    }
}

@Composable
fun GlassAlertDialog(
    onDismissRequest: () -> Unit,
    confirmButton: @Composable () -> Unit,
    modifier: Modifier = Modifier,
    dismissButton: @Composable (() -> Unit)? = null,
    icon: @Composable (() -> Unit)? = null,
    title: @Composable (() -> Unit)? = null,
    text: @Composable (() -> Unit)? = null,
    shape: Shape = MaterialTheme.shapes.extraLarge,
    containerColor: Color? = null,
    properties: DialogProperties = DialogProperties(),
) {
    val scheme = MaterialTheme.colorScheme
    val crossWindowBlurEnabled = rememberCrossWindowBlurEnabled()
    val glassEnabled = crossWindowBlurEnabled && rememberSystemAnimationsEnabled()
    val borderColor = scheme.outline.copy(alpha = if (scheme.surface.luminance() < 0.5f) 0.1f else 0.08f)
    AlertDialog(
        onDismissRequest = onDismissRequest,
        modifier = modifier.border(1.dp, borderColor, shape),
        confirmButton = {
            WindowBlurEffect(dialog = true)
            confirmButton()
        },
        dismissButton = dismissButton,
        icon = icon,
        title = title,
        text = text,
        shape = shape,
        containerColor = (containerColor ?: scheme.surfaceContainer).copy(alpha = if (glassEnabled) GLASS_ALPHA else SOLID_ALPHA),
        iconContentColor = scheme.primary,
        titleContentColor = scheme.onSurface,
        textContentColor = scheme.onSurfaceVariant,
        tonalElevation = 8.dp,
        properties = properties,
    )
}

@Composable
private fun rememberCrossWindowBlurEnabled(): Boolean {
    val context = LocalContext.current
    val windowManager = remember(context) { context.getSystemService(WindowManager::class.java) }
    var enabled by remember(windowManager) {
        mutableStateOf(
            Build.VERSION.SDK_INT >= Build.VERSION_CODES.S &&
                windowManager?.isCrossWindowBlurEnabled == true,
        )
    }
    DisposableEffect(windowManager) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S && windowManager != null) {
            val listener = Consumer<Boolean> { isEnabled -> enabled = isEnabled }
            windowManager.addCrossWindowBlurEnabledListener(context.mainExecutor, listener)
            onDispose { windowManager.removeCrossWindowBlurEnabledListener(listener) }
        } else {
            onDispose {}
        }
    }
    return enabled
}

@Composable
private fun rememberSystemAnimationsEnabled(): Boolean {
    val context = LocalContext.current
    var enabled by remember(context) { mutableStateOf(systemAnimationsEnabled()) }
    DisposableEffect(context) {
        val observer = object : ContentObserver(Handler(Looper.getMainLooper())) {
            override fun onChange(selfChange: Boolean) {
                enabled = systemAnimationsEnabled()
            }
        }
        context.contentResolver.registerContentObserver(
            Settings.Global.getUriFor(Settings.Global.ANIMATOR_DURATION_SCALE),
            false,
            observer,
        )
        onDispose { context.contentResolver.unregisterContentObserver(observer) }
    }
    return enabled
}

@Composable
private fun WindowBlurEffect(dialog: Boolean = false) {
    val view = LocalView.current
    val window = (view.parent as? DialogWindowProvider)?.window ?: return
    val context = LocalContext.current
    val density = LocalDensity.current
    val windowManager = remember(context) { context.getSystemService(WindowManager::class.java) }
    val animationsEnabled = rememberSystemAnimationsEnabled()
    var blurEnabled by remember(windowManager) {
        mutableStateOf(
            Build.VERSION.SDK_INT >= Build.VERSION_CODES.S &&
                windowManager?.isCrossWindowBlurEnabled == true,
        )
    }
    DisposableEffect(windowManager, window) {
        val listener = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S && windowManager != null) {
            Consumer<Boolean> { enabled -> blurEnabled = enabled }.also {
                windowManager.addCrossWindowBlurEnabledListener(context.mainExecutor, it)
            }
        } else null
        onDispose {
            if (listener != null && windowManager != null && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                windowManager.removeCrossWindowBlurEnabledListener(listener)
            }
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                window.setBackgroundBlurRadius(0)
            }
            window.clearFlags(LayoutParams.FLAG_BLUR_BEHIND)
        }
    }
    val shouldBlur = Build.VERSION.SDK_INT >= Build.VERSION_CODES.S &&
        blurEnabled &&
        animationsEnabled
    SideEffect {
        if (shouldBlur) {
            window.addFlags(LayoutParams.FLAG_BLUR_BEHIND)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                window.setBackgroundBlurRadius(with(density) { BLUR_RADIUS_DP.dp.roundToPx() })
            }
        } else {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                window.setBackgroundBlurRadius(0)
            }
            window.clearFlags(LayoutParams.FLAG_BLUR_BEHIND)
        }
        if (dialog) {
            window.addFlags(LayoutParams.FLAG_DIM_BEHIND)
            window.setDimAmount(if (shouldBlur) GLASS_SCRIM_ALPHA else SOLID_SCRIM_ALPHA)
        } else {
            window.clearFlags(LayoutParams.FLAG_DIM_BEHIND)
        }
    }
}

private fun Color.luminance(): Float {
    fun channel(value: Float): Float =
        if (value <= 0.04045f) value / 12.92f else ((value + 0.055f) / 1.055f).toDouble().let { Math.pow(it, 2.4).toFloat() }
    return 0.2126f * channel(red) + 0.7152f * channel(green) + 0.0722f * channel(blue)
}
