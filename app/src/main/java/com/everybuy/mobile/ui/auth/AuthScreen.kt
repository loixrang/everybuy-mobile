package com.everybuy.mobile.ui.auth

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Lock
import androidx.compose.material.icons.rounded.ShoppingBag
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.LoadingIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.material3.toPath
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.withTransform
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.graphics.shapes.Morph
import com.everybuy.mobile.R
import com.everybuy.mobile.data.AuthState
import com.everybuy.mobile.navigation.AuthenticatedApp
import com.everybuy.mobile.ui.components.VerticalSpacer
import com.everybuy.mobile.ui.theme.systemAnimationsEnabled

@Composable
fun AuthScreen(
    state: AuthState,
    theme: AppTheme,
    signIn: (String, String) -> Unit,
    verifyTotp: (String) -> Unit,
    retrySession: () -> Unit,
    signOut: () -> Unit,
    setTheme: (AppTheme) -> Unit,
    deepLink: AuthDeepLink? = null,
    onDeepLinkHandled: () -> Unit = {},
) {
    if (deepLink != null) {
        when (deepLink) {
            is AuthDeepLink.ResetPassword -> ResetPasswordScreen(deepLink.token, deepLink.error, onBackToSignIn = onDeepLinkHandled)
            is AuthDeepLink.AcceptInvite -> InviteAcceptScreen(deepLink.token, onDone = onDeepLinkHandled)
        }
        return
    }
    AnimatedContent(
        targetState = state is AuthState.SignedIn,
        transitionSpec = {
            if (systemAnimationsEnabled()) {
                (fadeIn(tween(520)) + scaleIn(initialScale = 0.96f, animationSpec = tween(520))) togetherWith fadeOut(tween(220))
            } else {
                fadeIn(tween(0)) togetherWith fadeOut(tween(0))
            }
        },
        label = "auth_transition",
    ) { signedIn ->
        if (signedIn) {
            val current = state as? AuthState.SignedIn ?: return@AnimatedContent
            AuthenticatedApp(
                user = current.user,
                staff = current.staff,
                theme = theme,
                onSignOut = signOut,
                setTheme = setTheme,
            )
        } else {
            AuthFlow(
                state = state,
                signIn = signIn,
                verifyTotp = verifyTotp,
                retrySession = retrySession,
                signOut = signOut,
            )
        }
    }
}

@Composable
private fun AuthFlow(
    state: AuthState,
    signIn: (String, String) -> Unit,
    verifyTotp: (String) -> Unit,
    retrySession: () -> Unit,
    signOut: () -> Unit,
) {
    when (state) {
        AuthState.Loading -> ConnectingScreen()
        else -> SignInScreen(
            state = state,
            signIn = signIn,
            verifyTotp = verifyTotp,
            retrySession = retrySession,
            signOut = signOut,
        )
    }
}

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
private fun ConnectingScreen() {
    val colors = MaterialTheme.colorScheme
    Box(
        Modifier
            .fillMaxSize()
            .background(Brush.verticalGradient(listOf(colors.primaryContainer, colors.surface))),
    ) {
        Column(
            Modifier
                .fillMaxSize()
                .windowInsetsPadding(WindowInsets.safeDrawing)
                .padding(horizontal = 32.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Spacer(Modifier.weight(1f))
            BrandMark()
            Text(
                text = stringResource(R.string.auth_title),
                style = MaterialTheme.typography.displaySmallEmphasized,
                fontWeight = FontWeight.SemiBold,
                color = colors.onPrimaryContainer,
                modifier = Modifier.padding(top = 24.dp),
                textAlign = TextAlign.Center,
            )
            Text(
                text = stringResource(R.string.splash_tagline),
                style = MaterialTheme.typography.bodyLarge,
                color = colors.onPrimaryContainer.copy(alpha = 0.82f),
                modifier = Modifier.padding(top = 8.dp),
                textAlign = TextAlign.Center,
            )
            LoadingIndicator(modifier = Modifier.padding(top = 32.dp))
            Spacer(Modifier.weight(1f))
            Text(
                text = stringResource(R.string.splash_connecting),
                style = MaterialTheme.typography.labelLarge,
                color = colors.onPrimaryContainer,
                modifier = Modifier.navigationBarsPadding().padding(bottom = 16.dp),
                textAlign = TextAlign.Center,
            )
        }
    }
}

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
private fun BrandMark() {
    val progress = if (systemAnimationsEnabled()) {
        val transition = rememberInfiniteTransition(label = "brand_mark")
        val animated by transition.animateFloat(
            initialValue = 0f,
            targetValue = 1f,
            animationSpec = infiniteRepeatable(
                animation = tween(durationMillis = 4200, easing = LinearEasing),
                repeatMode = RepeatMode.Reverse,
            ),
            label = "brand_morph",
        )
        animated
    } else {
        0.5f
    }
    val morph = androidx.compose.runtime.remember {
        Morph(
            start = androidx.compose.material3.MaterialShapes.Cookie9Sided,
            end = androidx.compose.material3.MaterialShapes.Cookie12Sided,
        )
    }
    val shapeColor = MaterialTheme.colorScheme.primary
    Box(Modifier.size(104.dp), contentAlignment = Alignment.Center) {
        Canvas(Modifier.fillMaxSize()) {
            val path = morph.toPath(progress)
            withTransform({
                translate(size.width / 2f, size.height / 2f)
                scale(size.minDimension / 2f, size.minDimension / 2f, pivot = Offset.Zero)
            }) {
                drawPath(path, color = shapeColor)
            }
        }
        Icon(
            imageVector = Icons.Rounded.ShoppingBag,
            contentDescription = stringResource(R.string.splash_brand_mark),
            tint = MaterialTheme.colorScheme.onPrimary,
            modifier = Modifier.size(46.dp),
        )
    }
}

@Composable
private fun SignInScreen(
    state: AuthState,
    signIn: (String, String) -> Unit,
    verifyTotp: (String) -> Unit,
    retrySession: () -> Unit,
    signOut: () -> Unit,
) {
    var email by rememberSaveable { mutableStateOf("") }
    var password by rememberSaveable { mutableStateOf("") }
    var code by rememberSaveable { mutableStateOf("") }
    var showForgot by rememberSaveable { mutableStateOf(false) }
    if (showForgot) {
        ForgotPasswordScreen(onBack = { showForgot = false })
        return
    }
    val colors = MaterialTheme.colorScheme
    Box(
        Modifier
            .fillMaxSize()
            .background(Brush.verticalGradient(listOf(colors.surfaceContainerLow, colors.surface))),
    ) {
        Column(
            Modifier
                .fillMaxSize()
                .windowInsetsPadding(WindowInsets.safeDrawing)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 24.dp, vertical = 32.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
        ) {
            Surface(shape = CircleShape, color = colors.primaryContainer, contentColor = colors.onPrimaryContainer) {
                Icon(
                    Icons.Rounded.ShoppingBag,
                    contentDescription = null,
                    modifier = Modifier.padding(16.dp).size(36.dp),
                )
            }
            Text(
                text = stringResource(R.string.auth_title),
                style = MaterialTheme.typography.displaySmallEmphasized,
                fontWeight = FontWeight.SemiBold,
                modifier = Modifier.padding(top = 16.dp),
            )
            Text(
                text = stringResource(R.string.splash_tagline),
                style = MaterialTheme.typography.bodyMedium,
                color = colors.onSurfaceVariant,
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(top = 4.dp),
            )
            VerticalSpacer(24.dp)
            Surface(
                modifier = Modifier.fillMaxWidth(),
                shape = MaterialTheme.shapes.extraLarge,
                color = colors.surfaceContainerLow,
            ) {
                Column(Modifier.fillMaxWidth().padding(20.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    when (state) {
                        AuthState.SignedOut, is AuthState.Error -> {
                            val error = state as? AuthState.Error
                            if (error?.retrySession == true) {
                                Text(error.message, color = colors.error, style = MaterialTheme.typography.bodyMedium)
                                FilledTonalButton(onClick = retrySession, modifier = Modifier.fillMaxWidth()) {
                                    Text(stringResource(R.string.auth_retry_session))
                                }
                            } else if (error?.twoFactor == true) {
                                Text(error.message, color = colors.error, style = MaterialTheme.typography.bodyMedium)
                                Text(stringResource(R.string.auth_two_step_body), style = MaterialTheme.typography.bodyMedium)
                                OutlinedTextField(
                                    value = code,
                                    onValueChange = { code = it },
                                    modifier = Modifier.fillMaxWidth(),
                                    label = { Text(stringResource(R.string.auth_totp_label)) },
                                    singleLine = true,
                                )
                                Button(
                                    onClick = { verifyTotp(code) },
                                    modifier = Modifier.fillMaxWidth(),
                                    enabled = code.isNotBlank(),
                                ) { Text(stringResource(R.string.auth_verify)) }
                            } else {
                                Text(
                                    text = stringResource(R.string.auth_subtitle),
                                    style = MaterialTheme.typography.titleLargeEmphasized,
                                )
                                if (error != null) {
                                    Text(error.message, color = colors.error, style = MaterialTheme.typography.bodyMedium)
                                }
                                OutlinedTextField(
                                    value = email,
                                    onValueChange = { email = it },
                                    modifier = Modifier.fillMaxWidth(),
                                    label = { Text(stringResource(R.string.auth_email)) },
                                    singleLine = true,
                                    shape = MaterialTheme.shapes.large,
                                )
                                OutlinedTextField(
                                    value = password,
                                    onValueChange = { password = it },
                                    modifier = Modifier.fillMaxWidth(),
                                    label = { Text(stringResource(R.string.auth_password)) },
                                    singleLine = true,
                                    shape = MaterialTheme.shapes.large,
                                    visualTransformation = PasswordVisualTransformation(),
                                )
                                Button(
                                    onClick = { signIn(email, password) },
                                    modifier = Modifier.fillMaxWidth(),
                                    enabled = email.isNotBlank() && password.isNotBlank(),
                                ) { Text(stringResource(R.string.auth_sign_in)) }
                                TextButton(
                                    onClick = { showForgot = true },
                                    modifier = Modifier.align(Alignment.CenterHorizontally),
                                ) { Text(stringResource(R.string.auth_forgot_password)) }
                            }
                        }
                        AuthState.TwoFactor -> {
                            Text(
                                stringResource(R.string.auth_two_step_title),
                                style = MaterialTheme.typography.titleLargeEmphasized,
                            )
                            Text(stringResource(R.string.auth_two_step_body), style = MaterialTheme.typography.bodyMedium)
                            OutlinedTextField(
                                value = code,
                                onValueChange = { code = it },
                                modifier = Modifier.fillMaxWidth(),
                                label = { Text(stringResource(R.string.auth_totp_label)) },
                                singleLine = true,
                            )
                            Button(
                                onClick = { verifyTotp(code) },
                                modifier = Modifier.fillMaxWidth(),
                                enabled = code.isNotBlank(),
                            ) { Text(stringResource(R.string.auth_verify)) }
                        }
                        AuthState.NoStaffAccess -> {
                            Text(
                                stringResource(R.string.auth_staff_required),
                                style = MaterialTheme.typography.titleLargeEmphasized,
                            )
                            Text(
                                stringResource(R.string.auth_staff_body),
                                color = colors.onSurfaceVariant,
                                style = MaterialTheme.typography.bodyMedium,
                            )
                            Button(onClick = signOut, modifier = Modifier.fillMaxWidth()) {
                                Text(stringResource(R.string.auth_sign_out))
                            }
                        }
                        AuthState.Loading, is AuthState.SignedIn -> Unit
                    }
                }
            }
            Row(
                Modifier.padding(top = 20.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp),
            ) {
                Icon(Icons.Rounded.Lock, contentDescription = null, tint = colors.onSurfaceVariant, modifier = Modifier.size(16.dp))
                Text(
                    text = stringResource(R.string.splash_connecting),
                    style = MaterialTheme.typography.labelMedium,
                    color = colors.onSurfaceVariant,
                )
            }
        }
    }
}
