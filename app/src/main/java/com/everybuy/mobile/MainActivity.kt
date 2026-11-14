package com.everybuy.mobile

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.SideEffect
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.core.view.WindowInsetsControllerCompat
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import dagger.hilt.android.AndroidEntryPoint
import androidx.hilt.navigation.compose.hiltViewModel
import com.everybuy.mobile.ui.auth.AuthScreen
import com.everybuy.mobile.ui.auth.AuthViewModel
import com.everybuy.mobile.ui.auth.AppTheme
import com.everybuy.mobile.ui.theme.EverybuyTheme

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        installSplashScreen()
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
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
            EverybuyTheme(theme = theme) { AuthScreen(state, theme, viewModel::signIn, viewModel::verifyTotp, { viewModel.restore() }, { viewModel.signOut() }, { viewModel.setTheme(it) }) }
        }
    }
}
