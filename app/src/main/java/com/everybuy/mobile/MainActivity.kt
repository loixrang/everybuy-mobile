package com.everybuy.mobile

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.runtime.SideEffect
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.core.view.WindowInsetsControllerCompat
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import dagger.hilt.android.AndroidEntryPoint
import androidx.hilt.navigation.compose.hiltViewModel
import com.everybuy.mobile.ui.auth.AuthDeepLink
import com.everybuy.mobile.ui.auth.AuthScreen
import com.everybuy.mobile.ui.auth.AuthViewModel
import com.everybuy.mobile.ui.auth.AppTheme
import com.everybuy.mobile.ui.auth.parseAuthDeepLink
import com.everybuy.mobile.ui.theme.EverybuyTheme

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    private var deepLink by mutableStateOf<AuthDeepLink?>(null)

    override fun onCreate(savedInstanceState: Bundle?) {
        installSplashScreen()
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        deepLink = parseAuthDeepLink(intent?.dataString)
        setContent {
            val viewModel: AuthViewModel = hiltViewModel()
            val state by viewModel.state.collectAsState()
            val theme by viewModel.theme.collectAsState(initial = AppTheme.SYSTEM)
            val dark = when (theme) {
                AppTheme.SYSTEM -> isSystemInDarkTheme()
                AppTheme.LIGHT -> false
                AppTheme.DARK, AppTheme.AMOLED -> true
            }
            SideEffect {
                WindowInsetsControllerCompat(window, window.decorView).apply {
                    isAppearanceLightStatusBars = !dark
                    isAppearanceLightNavigationBars = !dark
                }
            }
            EverybuyTheme(theme = theme) {
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
}
