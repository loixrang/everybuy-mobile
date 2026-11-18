package com.everybuy.mobile.ui.components

import android.os.Build
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.AccountCircle
import androidx.compose.material.icons.rounded.DarkMode
import androidx.compose.material.icons.rounded.Refresh
import androidx.compose.material.icons.rounded.ShoppingBag
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.MaterialShapes
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.TopAppBarScrollBehavior
import androidx.compose.material3.rememberTopAppBarState
import androidx.compose.material3.toPath
import androidx.compose.runtime.Composable
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.drawscope.withTransform
import androidx.compose.foundation.Canvas
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.input.nestedscroll.nestedScroll
import com.everybuy.mobile.R
import androidx.graphics.shapes.Morph
import dev.chrisbanes.haze.HazeState
import dev.chrisbanes.haze.HazeStyle
import dev.chrisbanes.haze.hazeEffect
import dev.chrisbanes.haze.hazeSource
import dev.chrisbanes.haze.materials.ExperimentalHazeMaterialsApi
import dev.chrisbanes.haze.materials.HazeMaterials

/** Haze state shared by the app chrome and the currently visible screen. */
val LocalAppHazeState = compositionLocalOf<HazeState?> { null }

/** Height reserved by the compact bottom navigation bar, or 0.dp when a rail is used. */
val LocalBottomChromeHeight = compositionLocalOf { 0.dp }

/** True when the window is medium/expanded and the shell is showing a navigation rail. */
val LocalNavigationRailVisible = compositionLocalOf { false }
val LocalCycleTheme = compositionLocalOf<(() -> Unit)?> { null }
val LocalOpenSettings = compositionLocalOf<(() -> Unit)?> { null }

@OptIn(ExperimentalHazeMaterialsApi::class)
@Composable
fun rememberGlassStyle(): HazeStyle = HazeMaterials.thick(
    MaterialTheme.colorScheme.surface.copy(alpha = 0.92f),
)

/**
 * A blurred, opaque-enough glass surface. Below API 31 Haze automatically falls back
 * to the solid tint, so content stays readable on older devices.
 */
@OptIn(ExperimentalHazeMaterialsApi::class)
@Composable
fun Modifier.glass(state: HazeState?): Modifier {
    if (state == null) return this
    return this.hazeEffect(state = state, style = rememberGlassStyle())
}

/** Blur enabled only where the platform can do it efficiently (API 31+). */
fun hazeBlurEnabled(): Boolean = Build.VERSION.SDK_INT >= Build.VERSION_CODES.S

/** Bottom padding that clears the bottom navigation bar, the FAB and the inset. */
@Composable
fun listBottomPadding(extra: Dp = 24.dp): Dp {
    val navInset = WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding()
    return navInset + LocalBottomChromeHeight.current + extra
}

@OptIn(ExperimentalMaterial3ExpressiveApi::class, ExperimentalMaterial3Api::class)
@Composable
fun EverybuyScreen(
    title: String,
    subtitle: String?,
    onOpenSettings: (() -> Unit)?,
    modifier: Modifier = Modifier,
    onBack: (() -> Unit)? = null,
    onRefresh: (() -> Unit)? = null,
    actions: @Composable RowScope.() -> Unit = {},
    floatingActionButton: (@Composable () -> Unit)? = null,
    scrollBehavior: TopAppBarScrollBehavior = TopAppBarDefaults.exitUntilCollapsedScrollBehavior(rememberTopAppBarState()),
    content: @Composable (PaddingValues) -> Unit,
) {
    val haze = LocalAppHazeState.current
    val openSettings = onOpenSettings ?: LocalOpenSettings.current
    Scaffold(
        modifier = modifier.fillMaxWidth().nestedScroll(scrollBehavior.nestedScrollConnection),
        containerColor = MaterialTheme.colorScheme.background,
        contentWindowInsets = WindowInsets(0),
        topBar = {
            TopAppBar(
                title = {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        CookieBrandMark()
                        Text(
                            text = stringResource(R.string.app_name),
                            maxLines = 1,
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.onSurface,
                        )
                    }
                },
                navigationIcon = {
                    if (onBack != null) {
                        IconButton(onClick = onBack) {
                            Icon(Icons.AutoMirrored.Rounded.ArrowBack, contentDescription = stringResource(R.string.action_back))
                        }
                    }
                },
                actions = {
                    if (onRefresh != null) {
                        IconButton(onClick = onRefresh) {
                            Icon(Icons.Rounded.Refresh, contentDescription = stringResource(R.string.action_refresh))
                        }
                    }
                    actions()
                    LocalCycleTheme.current?.let { cycleTheme ->
                        IconButton(onClick = cycleTheme) {
                            Icon(Icons.Rounded.DarkMode, contentDescription = stringResource(R.string.action_cycle_theme))
                        }
                    }
                    if (openSettings != null) {
                        IconButton(onClick = openSettings) {
                            Icon(Icons.Rounded.AccountCircle, contentDescription = stringResource(R.string.action_open_settings))
                        }
                    }
                },
                scrollBehavior = scrollBehavior,
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Color.Transparent,
                    scrolledContainerColor = Color.Transparent,
                    titleContentColor = MaterialTheme.colorScheme.onSurface,
                    navigationIconContentColor = MaterialTheme.colorScheme.onSurface,
                    actionIconContentColor = MaterialTheme.colorScheme.onSurface,
                ),
                modifier = Modifier.glass(haze),
            )
        },
        floatingActionButton = {
            if (floatingActionButton != null) {
                androidx.compose.foundation.layout.Box(
                    Modifier.padding(bottom = listBottomPadding(extra = 16.dp)),
                ) { floatingActionButton() }
            }
        },
    ) { padding ->
        Column(
            Modifier.fillMaxSize().then(if (haze != null) Modifier.hazeSource(haze) else Modifier),
        ) {
            androidx.compose.foundation.layout.Box(
                Modifier.fillMaxWidth().weight(1f).padding(top = padding.calculateTopPadding()),
                contentAlignment = Alignment.TopCenter,
            ) {
                Column(Modifier.fillMaxHeight().widthIn(max = 840.dp).fillMaxWidth()) {
                    Column(
                        Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp),
                        verticalArrangement = Arrangement.spacedBy(2.dp),
                    ) {
                        Text(
                            text = title,
                            style = MaterialTheme.typography.headlineMedium,
                            fontWeight = FontWeight.SemiBold,
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis,
                            color = MaterialTheme.colorScheme.onSurface,
                        )
                        subtitle?.let {
                            Text(
                                text = it,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                maxLines = 2,
                                overflow = TextOverflow.Ellipsis,
                            )
                        }
                    }
                    androidx.compose.foundation.layout.Box(
                        Modifier.fillMaxWidth().weight(1f),
                        contentAlignment = Alignment.TopCenter,
                    ) { content(PaddingValues(0.dp)) }
                }
            }
        }
    }
}

@Composable
@OptIn(ExperimentalMaterial3ExpressiveApi::class)
private fun CookieBrandMark() {
    val morph = remember { Morph(MaterialShapes.Cookie9Sided, MaterialShapes.Cookie12Sided) }
    val containerColor = MaterialTheme.colorScheme.primaryContainer
    val contentColor = MaterialTheme.colorScheme.onPrimaryContainer
    Box(Modifier.size(32.dp), contentAlignment = Alignment.Center) {
        Canvas(Modifier.fillMaxSize()) {
            val path = morph.toPath(0.5f)
            withTransform({
                translate(size.width / 2f, size.height / 2f)
                scale(size.minDimension / 2f, size.minDimension / 2f, pivot = Offset.Zero)
            }) {
                drawPath(path, containerColor)
            }
        }
        Icon(
            Icons.Rounded.ShoppingBag,
            contentDescription = null,
            tint = contentColor,
            modifier = Modifier.size(16.dp),
        )
    }
}

@Composable
fun VerticalSpacer(height: Dp) {
    androidx.compose.foundation.layout.Spacer(Modifier.height(height))
}

@Composable
fun ActionRow(content: @Composable RowScope.() -> Unit) {
    androidx.compose.foundation.layout.Row(
        Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically,
        content = content,
    )
}
