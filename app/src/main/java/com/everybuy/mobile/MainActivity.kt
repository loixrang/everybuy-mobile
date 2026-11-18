package com.everybuy.mobile

import android.content.Intent
import android.graphics.Color
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsControllerCompat
import androidx.hilt.navigation.compose.hiltViewModel
import com.everybuy.mobile.ui.auth.AppTheme
import com.everybuy.mobile.ui.auth.AuthDeepLink
import com.everybuy.mobile.ui.auth.AuthScreen
import com.everybuy.mobile.ui.auth.AuthViewModel
import com.everybuy.mobile.ui.auth.parseAuthDeepLink
import com.everybuy.mobile.ui.theme.EverybuyTheme
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    private var deepLink by mutableStateOf<AuthDeepLink?>(null)

    override fun onCreate(savedInstanceState: Bundle?) {
        installSplashScreen()
        super.onCreate(savedInstanceState)
        enableEdgeToEdge(
            statusBarStyle = SystemBarStyle.auto(Color.TRANSPARENT, Color.TRANSPARENT),
            navigationBarStyle = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                SystemBarStyle.auto(Color.TRANSPARENT, Color.TRANSPARENT)
            } else {
                SystemBarStyle.auto(LIGHT_SCRIM, DARK_SCRIM)
            },
        )
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            // Keep the gesture/navigation area truly transparent so content draws edge-to-edge.
            window.isNavigationBarContrastEnforced = false
        }
        WindowCompat.setDecorFitsSystemWindows(window, false)
        deepLink = parseAuthDeepLink(intent?.dataString)
        setContent {
            val viewModel: AuthViewModel = hiltViewModel()
            val state by viewModel.state.collectAsState()
            val theme by viewModel.theme.collectAsState(initial = AppTheme.SYSTEM)
            EverybuyTheme(theme = theme) {
                SystemBars(dark = theme.isDark())
                AuthScreen(
                    state = state,
                    theme = theme,
                    signIn = viewModel::signIn,
                    verifyTotp = viewModel::verifyTotp,
                    retrySession = { viewModel.restore() },
                    signOut = { viewModel.signOut() },
                    setTheme = { viewModel.setTheme(it) },
                    deepLink = deepLink,
                    onDeepLinkHandled = {
                        deepLink = null
                        intent?.data = null
                    },
                )
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        deepLink = parseAuthDeepLink(intent.dataString)
    }

    @Composable
    private fun SystemBars(dark: Boolean) {
        SideEffect {
            val controller = WindowInsetsControllerCompat(window, window.decorView)
            controller.isAppearanceLightStatusBars = !dark
            controller.isAppearanceLightNavigationBars = !dark
        }
    }

    private companion object {
        const val LIGHT_SCRIM = 0xE6FFFFFF.toInt()
        const val DARK_SCRIM = 0x801B1B1B.toInt()
    }
}

@Composable
private fun AppTheme.isDark(): Boolean = when (this) {
    AppTheme.SYSTEM -> isSystemInDarkTheme()
    AppTheme.LIGHT -> false
    AppTheme.DARK, AppTheme.AMOLED -> true
}
