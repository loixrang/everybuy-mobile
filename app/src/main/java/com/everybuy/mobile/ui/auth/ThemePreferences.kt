package com.everybuy.mobile.ui.auth

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

enum class AppTheme { SYSTEM, LIGHT, DARK, AMOLED }
private val Context.themeStore by preferencesDataStore("everybuy_preferences")
@Singleton class ThemePreferences @Inject constructor(@ApplicationContext context: Context) {
    private val store = context.themeStore
    val theme = store.data.map { prefs -> runCatching { AppTheme.valueOf(prefs[stringPreferencesKey("theme")] ?: "SYSTEM") }.getOrDefault(AppTheme.SYSTEM) }
    suspend fun set(theme: AppTheme) { store.edit { it[stringPreferencesKey("theme")] = theme.name } }
}
