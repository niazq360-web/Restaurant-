package com.example.ui.auth

import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import android.util.Log
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Fill
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.credentials.CredentialManager
import androidx.credentials.CustomCredential
import androidx.credentials.GetCredentialRequest
import androidx.credentials.exceptions.GetCredentialCancellationException
import com.example.R
import com.example.ui.theme.CleanOutline
import com.example.ui.theme.TextPrimary
import com.google.android.libraries.identity.googleid.GetSignInWithGoogleOption
import com.google.android.libraries.identity.googleid.GoogleIdTokenCredential
import com.google.android.libraries.identity.googleid.GoogleIdTokenCredential.Companion.TYPE_GOOGLE_ID_TOKEN_CREDENTIAL
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch

private const val TAG = "GoogleSignIn"

fun Context.findActivity(): Activity? {
    var current = this
    while (current is ContextWrapper) {
        if (current is Activity) return current
        current = current.baseContext
    }
    return null
}

fun launchGoogleSignIn(
    context: Context,
    scope: CoroutineScope,
    authViewModel: AuthViewModel,
    customDisplayName: String? = null,
    onLoadingChange: (Boolean) -> Unit = {},
    onSuccess: () -> Unit = {},
    onError: (String) -> Unit = {}
) {
    val activity = context.findActivity()
    if (activity == null) {
        Log.e(TAG, "Activity context not found for CredentialManager")
        onError("Activity context not found.")
        return
    }

    val clientId = try {
        context.getString(R.string.default_web_client_id)
    } catch (e: Exception) {
        Log.e(TAG, "Error resolving default_web_client_id: ${e.message}")
        ""
    }

    if (clientId.isBlank()) {
        Log.e(TAG, "default_web_client_id is blank")
        onError("Google Client ID is missing. Please check configuration.")
        return
    }

    onLoadingChange(true)
    val credentialManager = CredentialManager.create(activity)
    val signInOption = GetSignInWithGoogleOption.Builder(serverClientId = clientId).build()
    val request = GetCredentialRequest.Builder()
        .addCredentialOption(signInOption)
        .build()

    scope.launch {
        try {
            val result = credentialManager.getCredential(activity, request)
            val credential = result.credential
            if (credential is CustomCredential && credential.type == TYPE_GOOGLE_ID_TOKEN_CREDENTIAL) {
                val googleIdToken = GoogleIdTokenCredential.createFrom(credential.data).idToken
                authViewModel.signInWithGoogle(googleIdToken, customDisplayName) {
                    onLoadingChange(false)
                    onSuccess()
                }
            } else {
                onLoadingChange(false)
                Log.e(TAG, "Unexpected credential type: ${credential.type}")
                onError("Unexpected credential format received.")
            }
        } catch (e: GetCredentialCancellationException) {
            Log.w(TAG, "Google Sign-In cancelled or dismissed: ${e.message}")
            onLoadingChange(false)
        } catch (e: Exception) {
            Log.e(TAG, "Google Sign-In failed", e)
            onLoadingChange(false)
            onError(e.localizedMessage ?: "Google Sign-In failed.")
        }
    }
}

@Composable
fun GoogleSignInButton(
    authViewModel: AuthViewModel,
    modifier: Modifier = Modifier,
    buttonText: String = "Sign in with Google",
    isPrimary: Boolean = true,
    onSuccess: () -> Unit = {}
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var isSigningIn by remember { mutableStateOf(false) }

    if (isPrimary) {
        Button(
            onClick = {
                if (isSigningIn) return@Button
                launchGoogleSignIn(
                    context = context,
                    scope = scope,
                    authViewModel = authViewModel,
                    onLoadingChange = { isSigningIn = it },
                    onSuccess = onSuccess
                )
            },
            enabled = !isSigningIn,
            shape = RoundedCornerShape(14.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = Color.White,
                contentColor = TextPrimary
            ),
            elevation = ButtonDefaults.buttonElevation(
                defaultElevation = 3.dp,
                pressedElevation = 1.dp
            ),
            border = BorderStroke(1.2.dp, CleanOutline),
            modifier = modifier
                .fillMaxWidth()
                .height(54.dp)
                .testTag("google_sign_in_button")
        ) {
            if (isSigningIn) {
                CircularProgressIndicator(
                    modifier = Modifier.size(20.dp),
                    color = TextPrimary,
                    strokeWidth = 2.dp
                )
                Spacer(modifier = Modifier.width(10.dp))
                Text(
                    text = "Authenticating with Google...",
                    fontSize = 15.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = TextPrimary
                )
            } else {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Center
                ) {
                    GoogleLogoIcon(modifier = Modifier.size(22.dp))
                    Spacer(modifier = Modifier.width(12.dp))
                    Text(
                        text = buttonText,
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold,
                        color = TextPrimary
                    )
                }
            }
        }
    } else {
        OutlinedButton(
            onClick = {
                if (isSigningIn) return@OutlinedButton
                launchGoogleSignIn(
                    context = context,
                    scope = scope,
                    authViewModel = authViewModel,
                    onLoadingChange = { isSigningIn = it },
                    onSuccess = onSuccess
                )
            },
            enabled = !isSigningIn,
            shape = RoundedCornerShape(12.dp),
            colors = ButtonDefaults.outlinedButtonColors(
                containerColor = Color.White,
                contentColor = TextPrimary
            ),
            border = BorderStroke(1.2.dp, CleanOutline),
            modifier = modifier
                .fillMaxWidth()
                .height(52.dp)
                .testTag("google_sign_in_button")
        ) {
            if (isSigningIn) {
                CircularProgressIndicator(
                    modifier = Modifier.size(20.dp),
                    color = TextPrimary,
                    strokeWidth = 2.dp
                )
                Spacer(modifier = Modifier.width(10.dp))
                Text(
                    text = "Connecting to Google...",
                    fontSize = 15.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = TextPrimary
                )
            } else {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Center
                ) {
                    GoogleLogoIcon(modifier = Modifier.size(22.dp))
                    Spacer(modifier = Modifier.width(12.dp))
                    Text(
                        text = buttonText,
                        fontSize = 15.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = TextPrimary
                    )
                }
            }
        }
    }
}

@Composable
fun GoogleLogoIcon(modifier: Modifier = Modifier) {
    Canvas(modifier = modifier) {
        val sizePx = size.minDimension
        val strokeW = sizePx * 0.18f
        val center = Offset(size.width / 2f, size.height / 2f)
        val radius = (sizePx - strokeW) / 2f

        // Draw colorful Google "G" arcs
        // Blue arc (top-right to right)
        drawArc(
            color = Color(0xFF4285F4),
            startAngle = -45f,
            sweepAngle = 90f,
            useCenter = false,
            topLeft = Offset(center.x - radius, center.y - radius),
            size = Size(radius * 2f, radius * 2f),
            style = Stroke(width = strokeW)
        )
        // Green arc (bottom-right)
        drawArc(
            color = Color(0xFF34A853),
            startAngle = 45f,
            sweepAngle = 90f,
            useCenter = false,
            topLeft = Offset(center.x - radius, center.y - radius),
            size = Size(radius * 2f, radius * 2f),
            style = Stroke(width = strokeW)
        )
        // Yellow arc (bottom-left to top-left)
        drawArc(
            color = Color(0xFFFBBC05),
            startAngle = 135f,
            sweepAngle = 90f,
            useCenter = false,
            topLeft = Offset(center.x - radius, center.y - radius),
            size = Size(radius * 2f, radius * 2f),
            style = Stroke(width = strokeW)
        )
        // Red arc (top-left to top-right)
        drawArc(
            color = Color(0xFFEA4335),
            startAngle = 225f,
            sweepAngle = 90f,
            useCenter = false,
            topLeft = Offset(center.x - radius, center.y - radius),
            size = Size(radius * 2f, radius * 2f),
            style = Stroke(width = strokeW)
        )

        // Center bar of G in Blue
        val barPath = Path().apply {
            moveTo(center.x, center.y - strokeW / 2f)
            lineTo(center.x + radius + strokeW / 2f, center.y - strokeW / 2f)
            lineTo(center.x + radius + strokeW / 2f, center.y + strokeW / 2f)
            lineTo(center.x, center.y + strokeW / 2f)
            close()
        }
        drawPath(barPath, color = Color(0xFF4285F4), style = Fill)
    }
}
