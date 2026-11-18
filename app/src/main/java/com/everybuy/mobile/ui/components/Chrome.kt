package com.everybuy.mobile.ui.components

import android.os.Build
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.Refresh
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LargeFlexibleTopAppBar
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.TopAppBarScrollBehavior
import androidx.compose.material3.rememberTopAppBarState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.everybuy.mobile.R
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
    compactHeader: Boolean = false,
    onBack: (() -> Unit)? = null,
    onRefresh: (() -> Unit)? = null,
    actions: @Composable RowScope.() -> Unit = {},
    floatingActionButton: (@Composable () -> Unit)? = null,
    scrollBehavior: TopAppBarScrollBehavior = TopAppBarDefaults.exitUntilCollapsedScrollBehavior(rememberTopAppBarState()),
    content: @Composable (PaddingValues) -> Unit,
) {
    val haze = LocalAppHazeState.current
    Scaffold(
        modifier = modifier.fillMaxWidth(),
        containerColor = MaterialTheme.colorScheme.background,
        contentWindowInsets = WindowInsets(0),
        topBar = {
            if (compactHeader) {
                TopAppBar(
                    title = {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            androidx.compose.foundation.layout.Column(Modifier.weight(1f)) {
                                Text(
                                    text = title,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis,
                                    style = MaterialTheme.typography.titleLargeEmphasized,
                                    fontWeight = FontWeight.SemiBold,
                                    color = MaterialTheme.colorScheme.onSurface,
                                )
                                if (subtitle != null) {
                                    Text(
                                        text = subtitle,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis,
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    )
                                }
                            }
                            if (onOpenSettings != null) {
                                AccountAvatarButton(onClick = onOpenSettings, name = subtitle.orEmpty())
                            }
                        }
                    },
                    navigationIcon = {
                        if (onBack != null) {
                            IconButton(onClick = onBack) {
                                Icon(
                                    Icons.AutoMirrored.Rounded.ArrowBack,
                                    contentDescription = stringResource(R.string.action_back),
                                )
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
            } else {
                LargeFlexibleTopAppBar(
                    title = {
                        Text(
                            text = title,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            style = MaterialTheme.typography.headlineMedium,
                            fontWeight = FontWeight.SemiBold,
                        )
                    },
                    subtitle = subtitle?.let {
                        {
                            Text(
                                text = it,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                    },
                    navigationIcon = {
                        if (onBack != null) {
                            IconButton(onClick = onBack) {
                                Icon(
                                    Icons.AutoMirrored.Rounded.ArrowBack,
                                    contentDescription = stringResource(R.string.action_back),
                                )
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
                        if (onOpenSettings != null) {
                            AccountAvatarButton(onClick = onOpenSettings, name = subtitle.orEmpty())
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
            }
        },
        floatingActionButton = {
            if (floatingActionButton != null) {
                androidx.compose.foundation.layout.Box(
                    Modifier.padding(bottom = listBottomPadding(extra = 16.dp)),
                ) { floatingActionButton() }
            }
        },
    ) { padding ->
        androidx.compose.foundation.layout.Box(
            Modifier
                .fillMaxSize()
                .then(if (haze != null) Modifier.hazeSource(haze) else Modifier),
            contentAlignment = Alignment.TopCenter,
        ) {
            androidx.compose.foundation.layout.Box(
                Modifier
                    .fillMaxHeight()
                    .widthIn(max = 840.dp)
                    .fillMaxWidth(),
            ) {
                content(padding)
            }
        }
    }
}

@Composable
fun AccountAvatarButton(onClick: () -> Unit, name: String = "") {
    val description = stringResource(R.string.action_open_settings)
    Surface(
        onClick = onClick,
        shape = CircleShape,
        color = MaterialTheme.colorScheme.primaryContainer,
        contentColor = MaterialTheme.colorScheme.onPrimaryContainer,
        modifier = Modifier
            .padding(end = 8.dp)
            .size(48.dp)
            .semantics { contentDescription = description },
    ) {
        androidx.compose.foundation.layout.Box(contentAlignment = Alignment.Center) {
            Text(
                text = name.trim().firstOrNull()?.uppercase() ?: "E",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
            )
        }
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
