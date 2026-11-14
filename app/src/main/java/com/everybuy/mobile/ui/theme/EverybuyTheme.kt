package com.everybuy.mobile.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import com.everybuy.mobile.ui.auth.AppTheme
import com.everybuy.mobile.R

private val Poppins = FontFamily(
    Font(R.font.poppins_regular, FontWeight.Normal),
    Font(R.font.poppins_semibold, FontWeight.SemiBold),
    Font(R.font.poppins_bold, FontWeight.Bold),
)
private val BaseTypography = Typography()
private val EverybuyTypography = Typography(
    displayLarge = BaseTypography.displayLarge.copy(fontFamily = Poppins),
    displayMedium = BaseTypography.displayMedium.copy(fontFamily = Poppins),
    displaySmall = BaseTypography.displaySmall.copy(fontFamily = Poppins),
    headlineLarge = BaseTypography.headlineLarge.copy(fontFamily = Poppins),
    headlineMedium = BaseTypography.headlineMedium.copy(fontFamily = Poppins),
    headlineSmall = BaseTypography.headlineSmall.copy(fontFamily = Poppins),
    titleLarge = BaseTypography.titleLarge.copy(fontFamily = Poppins),
    titleMedium = BaseTypography.titleMedium.copy(fontFamily = Poppins),
    titleSmall = BaseTypography.titleSmall.copy(fontFamily = Poppins),
    bodyLarge = BaseTypography.bodyLarge.copy(fontFamily = Poppins),
    bodyMedium = BaseTypography.bodyMedium.copy(fontFamily = Poppins),
    bodySmall = BaseTypography.bodySmall.copy(fontFamily = Poppins),
    labelLarge = BaseTypography.labelLarge.copy(fontFamily = Poppins),
    labelMedium = BaseTypography.labelMedium.copy(fontFamily = Poppins),
    labelSmall = BaseTypography.labelSmall.copy(fontFamily = Poppins),
)

@Composable fun EverybuyTheme(theme: AppTheme, content: @Composable () -> Unit) {
    val context = LocalContext.current
    val dark = when (theme) { AppTheme.SYSTEM -> isSystemInDarkTheme(); AppTheme.LIGHT -> false; AppTheme.DARK, AppTheme.AMOLED -> true }
    val colors = when (theme) {
        AppTheme.SYSTEM -> if (android.os.Build.VERSION.SDK_INT >= 31) { if (dark) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context) } else if (dark) darkColorScheme() else lightColorScheme()
        AppTheme.LIGHT -> if (android.os.Build.VERSION.SDK_INT >= 31) dynamicLightColorScheme(context) else lightColorScheme()
        AppTheme.DARK -> if (android.os.Build.VERSION.SDK_INT >= 31) dynamicDarkColorScheme(context) else darkColorScheme()
        AppTheme.AMOLED -> darkColorScheme(background = Color.Black, surface = Color.Black, surfaceVariant = Color(0xFF171717), surfaceContainer = Color(0xFF101010), surfaceContainerLow = Color(0xFF080808), surfaceContainerHigh = Color(0xFF202020))
    }
    MaterialTheme(colorScheme = colors, typography = EverybuyTypography, content = content)
}
