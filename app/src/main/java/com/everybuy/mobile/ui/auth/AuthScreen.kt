package com.everybuy.mobile.ui.auth

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import com.everybuy.mobile.data.AuthState
import com.everybuy.mobile.navigation.AuthenticatedApp

@Composable fun AuthScreen(state: AuthState, theme: AppTheme, signIn: (String, String) -> Unit, verifyTotp: (String) -> Unit, retrySession: () -> Unit, signOut: () -> Unit, setTheme: (AppTheme) -> Unit) {
    if (state is AuthState.SignedIn) {
        AuthenticatedApp(user = state.user, staff = state.staff, theme = theme, onSignOut = signOut, setTheme = setTheme)
        return
    }
    var email by rememberSaveable { mutableStateOf("") }
    var password by rememberSaveable { mutableStateOf("") }
    var code by rememberSaveable { mutableStateOf("") }
    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(24.dp), verticalArrangement = Arrangement.Center) {
        Text("Everybuy", style = MaterialTheme.typography.headlineLarge)
        Spacer(Modifier.height(8.dp))
        when (state) {
            AuthState.Loading -> { CircularProgressIndicator(); Text("Connecting securely…", Modifier.padding(top = 12.dp)) }
            AuthState.SignedOut, is AuthState.Error -> {
                val error = state as? AuthState.Error
                if (error?.retrySession == true) {
                    Text(error.message, color = MaterialTheme.colorScheme.error)
                    TextButton(onClick = retrySession) { Text("Retry session") }
                } else if (error?.twoFactor == true) {
                    Text(error.message, color = MaterialTheme.colorScheme.error)
                    Text("Enter the code from your authenticator app.", modifier = Modifier.padding(vertical = 12.dp))
                    OutlinedTextField(code, { code = it }, Modifier.fillMaxWidth(), label = { Text("Authenticator code") }, singleLine = true)
                    Button(onClick = { verifyTotp(code) }, Modifier.fillMaxWidth().padding(top = 16.dp), enabled = code.isNotBlank()) { Text("Verify") }
                } else {
                Text("Sign in to your business", style = MaterialTheme.typography.titleLarge, modifier = Modifier.padding(top = 8.dp, bottom = 22.dp))
                if (error != null) Text(error.message, color = MaterialTheme.colorScheme.error, modifier = Modifier.padding(bottom = 12.dp))
                OutlinedTextField(email, { email = it }, Modifier.fillMaxWidth(), label = { Text("Email") }, singleLine = true)
                Spacer(Modifier.height(12.dp))
                OutlinedTextField(password, { password = it }, Modifier.fillMaxWidth(), label = { Text("Password") }, singleLine = true, visualTransformation = PasswordVisualTransformation())
                Button(onClick = { signIn(email, password) }, Modifier.fillMaxWidth().padding(top = 20.dp), enabled = email.isNotBlank() && password.isNotBlank()) { Text("Sign in") }
                }
            }
            AuthState.TwoFactor -> {
                Text("Two-step verification", style = MaterialTheme.typography.titleLarge, modifier = Modifier.padding(vertical = 12.dp))
                Text("Enter the code from your authenticator app.", modifier = Modifier.padding(bottom = 12.dp))
                OutlinedTextField(code, { code = it }, Modifier.fillMaxWidth(), label = { Text("Authenticator code") }, singleLine = true)
                Button(onClick = { verifyTotp(code) }, Modifier.fillMaxWidth().padding(top = 16.dp), enabled = code.isNotBlank()) { Text("Verify") }
            }
            AuthState.NoStaffAccess -> {
                Text("Staff access required", style = MaterialTheme.typography.titleLarge, modifier = Modifier.padding(vertical = 12.dp))
                Text("This account is signed in, but it does not have an active Everybuy staff profile.", color = MaterialTheme.colorScheme.onSurfaceVariant)
                Button(onClick = signOut, Modifier.fillMaxWidth().padding(top = 16.dp)) { Text("Sign out") }
            }
            is AuthState.SignedIn -> Unit
        }
        Spacer(Modifier.height(28.dp))
        Text("Appearance", style = MaterialTheme.typography.titleMedium)
        androidx.compose.foundation.layout.Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(4.dp)) {
            AppTheme.entries.forEach { choice -> TextButton(onClick = { setTheme(choice) }) { Text(choice.name.lowercase().replaceFirstChar { it.uppercase() }) } }
        }
    }
}
