package com.everybuy.mobile.ui.auth

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel

@Composable fun ForgotPasswordScreen(onBack: () -> Unit, viewModel: ForgotPasswordViewModel = hiltViewModel()) {
    val state by viewModel.state.collectAsState()
    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(24.dp), verticalArrangement = Arrangement.Center) {
        Text("Reset your password", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
        Text(
            "Enter the email address on your Everybuy account and we will send a reset link. The link expires after one hour.",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(top = 8.dp),
        )
        val sentTo = state.sentTo
        if (sentTo != null) {
            Spacer(Modifier.height(20.dp))
            Card(shape = RoundedCornerShape(16.dp), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.secondaryContainer), modifier = Modifier.fillMaxWidth()) {
                Text(
                    "If an account exists for $sentTo, a reset link is on its way. Check the inbox and spam folder.",
                    style = MaterialTheme.typography.bodySmall,
                    modifier = Modifier.padding(14.dp),
                )
            }
            Spacer(Modifier.height(12.dp))
            Button(onClick = { viewModel.reset() }, modifier = Modifier.fillMaxWidth()) { Text("Use a different email") }
        } else {
            Spacer(Modifier.height(20.dp))
            OutlinedTextField(
                state.email,
                viewModel::setEmail,
                Modifier.fillMaxWidth(),
                label = { Text("Email address") },
                singleLine = true,
                enabled = !state.sending,
            )
            state.error?.let { Text(it, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall, modifier = Modifier.padding(top = 8.dp)) }
            Button(onClick = { viewModel.submit() }, modifier = Modifier.fillMaxWidth().padding(top = 16.dp), enabled = !state.sending && state.email.isNotBlank()) {
                if (state.sending) CircularProgressIndicator(Modifier.height(20.dp)) else Text("Send reset link")
            }
        }
        TextButton(onClick = onBack, modifier = Modifier.padding(top = 8.dp)) { Text("Back to sign in") }
    }
}

@Composable fun ResetPasswordScreen(
    token: String?,
    linkError: String?,
    onBackToSignIn: () -> Unit,
    viewModel: ResetPasswordViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsState()
    val invalidLink = token.isNullOrBlank() || linkError != null
    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(24.dp), verticalArrangement = Arrangement.Center) {
        if (state.done) {
            Text("Password updated", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
            Text(
                "Sign in with your new password. Other sessions were signed out.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(top = 8.dp),
            )
            Button(onClick = onBackToSignIn, modifier = Modifier.fillMaxWidth().padding(top = 20.dp)) { Text("Back to sign in") }
            return@Column
        }
        if (invalidLink) {
            Text("Reset link not valid", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
            Text(
                if (linkError == "INVALID_TOKEN") "This reset link is invalid or has expired."
                else "This page needs a reset token from the email link.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(top = 8.dp),
            )
            Text(
                "Reset links expire after one hour and can be used once.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.error,
                modifier = Modifier.padding(top = 12.dp),
            )
            Button(onClick = onBackToSignIn, modifier = Modifier.fillMaxWidth().padding(top = 20.dp)) { Text("Request a new link") }
            return@Column
        }
        Text("Choose a new password", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
        Text(
            "Use between $PASSWORD_MIN and $PASSWORD_MAX characters.",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(top = 8.dp),
        )
        Spacer(Modifier.height(20.dp))
        OutlinedTextField(
            state.password,
            viewModel::setPassword,
            Modifier.fillMaxWidth(),
            label = { Text("New password") },
            singleLine = true,
            enabled = !state.saving,
            visualTransformation = PasswordVisualTransformation(),
        )
        Spacer(Modifier.height(10.dp))
        OutlinedTextField(
            state.confirmation,
            viewModel::setConfirmation,
            Modifier.fillMaxWidth(),
            label = { Text("Confirm new password") },
            singleLine = true,
            enabled = !state.saving,
            visualTransformation = PasswordVisualTransformation(),
        )
        state.error?.let { Text(it, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall, modifier = Modifier.padding(top = 8.dp)) }
        Button(onClick = { token?.let(viewModel::submit) }, modifier = Modifier.fillMaxWidth().padding(top = 16.dp), enabled = !state.saving && state.password.isNotBlank() && state.confirmation.isNotBlank()) {
            if (state.saving) CircularProgressIndicator(Modifier.height(20.dp)) else Text("Update password")
        }
        TextButton(onClick = onBackToSignIn, modifier = Modifier.padding(top = 8.dp)) { Text("Back to sign in") }
    }
}

@Composable fun InviteAcceptScreen(token: String?, onDone: () -> Unit, viewModel: InviteAcceptViewModel = hiltViewModel()) {
    val state by viewModel.state.collectAsState()
    LaunchedEffect(token) { token?.takeIf { it.isNotBlank() }?.let(viewModel::load) }
    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(24.dp), verticalArrangement = Arrangement.Center) {
        if (token.isNullOrBlank()) {
            Text("Invitation not available", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
            Text(
                "This invitation link is missing its token. Open the full link from your invitation email, or ask an owner for a new invitation.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(top = 8.dp),
            )
            Button(onClick = onDone, modifier = Modifier.fillMaxWidth().padding(top = 20.dp)) { Text("Back to sign in") }
            return@Column
        }
        when (val current = state) {
            InviteAcceptState.Loading -> {
                CircularProgressIndicator()
                Text("Checking your invitation…", Modifier.padding(top = 12.dp))
            }
            is InviteAcceptState.Invalid -> {
                Text("Invitation not available", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
                Text(current.message, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.padding(top = 8.dp))
                Button(onClick = onDone, modifier = Modifier.fillMaxWidth().padding(top = 20.dp)) { Text("Back to sign in") }
            }
            is InviteAcceptState.Failed -> {
                Text("Could not load invitation", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
                Text(current.message, color = MaterialTheme.colorScheme.error, modifier = Modifier.padding(top = 8.dp))
                Row(Modifier.padding(top = 16.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Button(onClick = { viewModel.retry() }) { Text("Retry") }
                    TextButton(onClick = onDone) { Text("Back to sign in") }
                }
            }
            is InviteAcceptState.Done -> {
                Text("Welcome to Everybuy", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
                Text("Your staff account for ${current.email} is ready. Sign in with the password you just chose.", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.padding(top = 8.dp))
                Button(onClick = onDone, modifier = Modifier.fillMaxWidth().padding(top = 20.dp)) { Text("Back to sign in") }
            }
            is InviteAcceptState.Ready -> {
                Text("Join Everybuy", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
                Text(
                    "${current.preview.name.ifBlank { "You" }} (${current.preview.email}) were invited to join the shop as staff.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(top = 8.dp),
                )
                Spacer(Modifier.height(16.dp))
                OutlinedTextField(
                    current.password,
                    viewModel::setPassword,
                    Modifier.fillMaxWidth(),
                    label = { Text("Choose a password") },
                    supportingText = { Text("$PASSWORD_MIN–$PASSWORD_MAX characters") },
                    singleLine = true,
                    enabled = !current.accepting,
                    visualTransformation = PasswordVisualTransformation(),
                )
                Spacer(Modifier.height(10.dp))
                OutlinedTextField(
                    current.confirmation,
                    viewModel::setConfirmation,
                    Modifier.fillMaxWidth(),
                    label = { Text("Confirm password") },
                    singleLine = true,
                    enabled = !current.accepting,
                    visualTransformation = PasswordVisualTransformation(),
                )
                current.error?.let { Text(it, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall, modifier = Modifier.padding(top = 8.dp)) }
                Button(
                    onClick = { token?.let(viewModel::accept) },
                    modifier = Modifier.fillMaxWidth().padding(top = 16.dp),
                    enabled = !current.accepting && current.password.isNotBlank() && current.confirmation.isNotBlank(),
                ) {
                    if (current.accepting) CircularProgressIndicator(Modifier.height(20.dp)) else Text("Create account")
                }
                TextButton(onClick = onDone, modifier = Modifier.padding(top = 4.dp)) { Text("Back to sign in") }
            }
        }
    }
}
