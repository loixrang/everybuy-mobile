package com.everybuy.mobile.ui.theme

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.shape.CornerBasedShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ColorScheme
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.MaterialExpressiveTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.MotionScheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.everybuy.mobile.R
import com.everybuy.mobile.ui.auth.AppTheme

private val Poppins = FontFamily(
    Font(R.font.poppins_regular, FontWeight.Normal),
    Font(R.font.poppins_semibold, FontWeight.SemiBold),
    Font(R.font.poppins_bold, FontWeight.Bold),
)

/**
 * Material 3 Expressive shape scale. Everything is deliberately rounder than the
 * baseline Material scale so containers read as soft, tactile "expressive" surfaces.
 */
val EverybuyShapes = Shapes(
    extraSmall = RoundedCornerShape(10.dp),
    small = RoundedCornerShape(16.dp),
    medium = RoundedCornerShape(22.dp),
    large = RoundedCornerShape(28.dp),
    extraLarge = RoundedCornerShape(36.dp),
    largeIncreased = RoundedCornerShape(32.dp),
    extraLargeIncreased = RoundedCornerShape(42.dp),
    extraExtraLarge = RoundedCornerShape(48.dp),
)

private fun TextStyle.poppins(): TextStyle = copy(fontFamily = Poppins)

private val BaseTypography = Typography()

/** Expressive type scale: Poppins for every role plus the emphasized variants. */
val EverybuyTypography: Typography = BaseTypography.copy(
    displayLarge = BaseTypography.displayLarge.poppins(),
    displayMedium = BaseTypography.displayMedium.poppins(),
    displaySmall = BaseTypography.displaySmall.poppins(),
    headlineLarge = BaseTypography.headlineLarge.poppins(),
    headlineMedium = BaseTypography.headlineMedium.poppins(),
    headlineSmall = BaseTypography.headlineSmall.poppins(),
    titleLarge = BaseTypography.titleLarge.poppins(),
    titleMedium = BaseTypography.titleMedium.poppins(),
    titleSmall = BaseTypography.titleSmall.poppins(),
    bodyLarge = BaseTypography.bodyLarge.poppins(),
    bodyMedium = BaseTypography.bodyMedium.poppins(),
    bodySmall = BaseTypography.bodySmall.poppins(),
    labelLarge = BaseTypography.labelLarge.poppins(),
    labelMedium = BaseTypography.labelMedium.poppins(),
    labelSmall = BaseTypography.labelSmall.poppins(),
    displayLargeEmphasized = BaseTypography.displayLargeEmphasized.poppins(),
    displayMediumEmphasized = BaseTypography.displayMediumEmphasized.poppins(),
    displaySmallEmphasized = BaseTypography.displaySmallEmphasized.poppins(),
    headlineLargeEmphasized = BaseTypography.headlineLargeEmphasized.poppins(),
    headlineMediumEmphasized = BaseTypography.headlineMediumEmphasized.poppins(),
    headlineSmallEmphasized = BaseTypography.headlineSmallEmphasized.poppins(),
    titleLargeEmphasized = BaseTypography.titleLargeEmphasized.poppins(),
    titleMediumEmphasized = BaseTypography.titleMediumEmphasized.poppins(),
    titleSmallEmphasized = BaseTypography.titleSmallEmphasized.poppins(),
    bodyLargeEmphasized = BaseTypography.bodyLargeEmphasized.poppins(),
    bodyMediumEmphasized = BaseTypography.bodyMediumEmphasized.poppins(),
    bodySmallEmphasized = BaseTypography.bodySmallEmphasized.poppins(),
    labelLargeEmphasized = BaseTypography.labelLargeEmphasized.poppins(),
    labelMediumEmphasized = BaseTypography.labelMediumEmphasized.poppins(),
    labelSmallEmphasized = BaseTypography.labelSmallEmphasized.poppins(),
)

/** Green seed-derived fallback used below API 31 and for the AMOLED variant. */
internal val GreenLightScheme: ColorScheme = lightColorScheme(
    primary = Color(0xFF2F6B4F),
    onPrimary = Color(0xFFFFFFFF),
    primaryContainer = Color(0xFFB4F1CE),
    onPrimaryContainer = Color(0xFF002114),
    inversePrimary = Color(0xFF98D5B2),
    secondary = Color(0xFF4E6355),
    onSecondary = Color(0xFFFFFFFF),
    secondaryContainer = Color(0xFFD0E8D7),
    onSecondaryContainer = Color(0xFF0B1F14),
    tertiary = Color(0xFF3A6470),
    onTertiary = Color(0xFFFFFFFF),
    tertiaryContainer = Color(0xFFBEEAF8),
    onTertiaryContainer = Color(0xFF001F26),
    error = Color(0xFFBA1A1A),
    onError = Color(0xFFFFFFFF),
    errorContainer = Color(0xFFFFDAD6),
    onErrorContainer = Color(0xFF410002),
    background = Color(0xFFF6FBF4),
    onBackground = Color(0xFF181D19),
    surface = Color(0xFFF6FBF4),
    onSurface = Color(0xFF181D19),
    surfaceVariant = Color(0xFFDCE5DB),
    onSurfaceVariant = Color(0xFF404942),
    outline = Color(0xFF707972),
    outlineVariant = Color(0xFFBFC9C0),
    inverseSurface = Color(0xFF2C322D),
    inverseOnSurface = Color(0xFFEDF2EB),
    surfaceDim = Color(0xFFD6DBD5),
    surfaceBright = Color(0xFFF6FBF4),
    surfaceContainerLowest = Color(0xFFFFFFFF),
    surfaceContainerLow = Color(0xFFF0F5EF),
    surfaceContainer = Color(0xFFEAEFE8),
    surfaceContainerHigh = Color(0xFFE4EAE3),
    surfaceContainerHighest = Color(0xFFDEE4DD),
    scrim = Color(0xFF000000),
)

internal val GreenDarkScheme: ColorScheme = darkColorScheme(
    primary = Color(0xFF98D5B2),
    onPrimary = Color(0xFF003825),
    primaryContainer = Color(0xFF155239),
    onPrimaryContainer = Color(0xFFB4F1CE),
    inversePrimary = Color(0xFF2F6B4F),
    secondary = Color(0xFFB4CCBB),
    onSecondary = Color(0xFF203528),
    secondaryContainer = Color(0xFF364B3E),
    onSecondaryContainer = Color(0xFFD0E8D7),
    tertiary = Color(0xFFA2CDDB),
    onTertiary = Color(0xFF023640),
    tertiaryContainer = Color(0xFF214B56),
    onTertiaryContainer = Color(0xFFBEEAF8),
    error = Color(0xFFFFB4AB),
    onError = Color(0xFF690005),
    errorContainer = Color(0xFF93000A),
    onErrorContainer = Color(0xFFFFDAD6),
    background = Color(0xFF101410),
    onBackground = Color(0xFFE0E4DE),
    surface = Color(0xFF101410),
    onSurface = Color(0xFFE0E4DE),
    surfaceVariant = Color(0xFF404942),
    onSurfaceVariant = Color(0xFFBFC9C0),
    outline = Color(0xFF8A938B),
    outlineVariant = Color(0xFF404942),
    inverseSurface = Color(0xFFE0E4DE),
    inverseOnSurface = Color(0xFF2C322D),
    surfaceDim = Color(0xFF101410),
    surfaceBright = Color(0xFF363B36),
    surfaceContainerLowest = Color(0xFF0B0F0B),
    surfaceContainerLow = Color(0xFF181D19),
    surfaceContainer = Color(0xFF1C211C),
    surfaceContainerHigh = Color(0xFF262B26),
    surfaceContainerHighest = Color(0xFF313631),
    scrim = Color(0xFF000000),
)

/** True-black variant: background and surface are pure #000000. */
private val AmoledScheme: ColorScheme = GreenDarkScheme.copy(
    background = Color.Black,
    surface = Color.Black,
    surfaceDim = Color.Black,
    surfaceContainerLowest = Color.Black,
    surfaceContainerLow = Color(0xFF090C09),
    surfaceContainer = Color(0xFF101410),
    surfaceContainerHigh = Color(0xFF181D18),
    surfaceContainerHighest = Color(0xFF202620),
)

@Composable
fun EverybuyTheme(theme: AppTheme, content: @Composable () -> Unit) {
    val context = LocalContext.current
    val systemDark = isSystemInDarkTheme()
    val dark = when (theme) {
        AppTheme.SYSTEM -> systemDark
        AppTheme.LIGHT -> false
        AppTheme.DARK, AppTheme.AMOLED -> true
    }
    val colors = when {
        theme == AppTheme.AMOLED -> AmoledScheme
        theme == AppTheme.LIGHT -> if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) dynamicLightColorScheme(context) else GreenLightScheme
        theme == AppTheme.DARK -> if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) dynamicDarkColorScheme(context) else GreenDarkScheme
        else -> if (dark) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) dynamicDarkColorScheme(context) else GreenDarkScheme
        } else {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) dynamicLightColorScheme(context) else GreenLightScheme
        }
    }
    EverybuyTheme(colors = colors, content = content)
}

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun EverybuyTheme(
    colors: ColorScheme,
    content: @Composable () -> Unit,
) {
    MaterialExpressiveTheme(
        colorScheme = colors,
        motionScheme = MotionScheme.expressive(),
        shapes = EverybuyShapes,
        typography = EverybuyTypography,
        content = content,
    )
}

/** True when the system asks apps to remove animations (Settings > Remove animations). */
fun systemAnimationsEnabled(): Boolean = android.animation.ValueAnimator.areAnimatorsEnabled()
