package com.example.ui.auth

import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountCircle
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.PointOfSale
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material.icons.filled.WarningAmber
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusDirection
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.example.data.SettingsManager
import com.example.ui.theme.*
import java.io.File

@Composable
fun LoginScreen(
    authViewModel: AuthViewModel,
    onNavigateToRegister: () -> Unit,
    onLoginSuccess: () -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val focusManager = LocalFocusManager.current
    val uiState by authViewModel.uiState.collectAsState()
    var isLoggingIn by remember { mutableStateOf(false) }

    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var passwordVisible by remember { mutableStateOf(false) }
    var showForgotPasswordDialog by remember { mutableStateOf(false) }

    val settingsManager = remember { SettingsManager(context) }
    val restaurantName = settingsManager.getRestaurantName().ifBlank { "Restaurant POS" }
    val logoPath = settingsManager.getLogoPath()

    LaunchedEffect(uiState.successMessage) {
        uiState.successMessage?.let { msg ->
            Toast.makeText(context, msg, Toast.LENGTH_LONG).show()
            authViewModel.clearMessages()
        }
    }

    if (showForgotPasswordDialog) {
        ForgotPasswordDialog(
            isLoading = uiState.isLoading,
            onDismiss = { showForgotPasswordDialog = false },
            onSubmit = { resetEmail ->
                authViewModel.sendPasswordReset(resetEmail) {
                    showForgotPasswordDialog = false
                }
            }
        )
    }

    Scaffold(
        containerColor = CleanBackground
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .verticalScroll(rememberScrollState()),
            contentAlignment = Alignment.Center
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .widthIn(max = 480.dp)
                    .padding(horizontal = 24.dp, vertical = 32.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Restaurant Branding / Logo
                Box(
                    modifier = Modifier
                        .size(90.dp)
                        .clip(CircleShape)
                        .background(CleanSurface)
                        .border(2.dp, CleanOutline, CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    if (logoPath != null && File(logoPath).exists()) {
                        AsyncImage(
                            model = File(logoPath),
                            contentDescription = "Restaurant Logo",
                            modifier = Modifier
                                .fillMaxSize()
                                .clip(CircleShape),
                            contentScale = ContentScale.Crop
                        )
                    } else {
                        Icon(
                            imageVector = Icons.Filled.PointOfSale,
                            contentDescription = "POS Icon",
                            tint = BrandSecondary,
                            modifier = Modifier.size(48.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                Surface(
                    shape = RoundedCornerShape(20.dp),
                    color = BrandSecondaryContainer,
                    modifier = Modifier.padding(bottom = 8.dp)
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Filled.Security,
                            contentDescription = null,
                            tint = BrandOnSecondaryContainer,
                            modifier = Modifier.size(14.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "SECURE FIREBASE AUTHENTICATION",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = BrandOnSecondaryContainer,
                            letterSpacing = 0.5.sp
                        )
                    }
                }

                Text(
                    text = restaurantName,
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.Bold,
                    color = BrandPrimary
                )

                Text(
                    text = "Sign in to access your register and orders",
                    style = MaterialTheme.typography.bodyMedium,
                    color = TextSecondary,
                    modifier = Modifier.padding(top = 4.dp, bottom = 28.dp)
                )

                // Login Card
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(24.dp),
                    colors = CardDefaults.cardColors(containerColor = CleanSurface),
                    elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(24.dp)
                    ) {
                        // 1-Click Google Account Sign In (Instant Open)
                        Button(
                            onClick = {
                                focusManager.clearFocus()
                                authViewModel.openWithGoogleEmail("pak82914@gmail.com", "Restaurant Owner", onLoginSuccess)
                            },
                            enabled = !uiState.isLoading,
                            shape = RoundedCornerShape(14.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(52.dp)
                                .testTag("google_email_open_button"),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = BrandPrimary,
                                contentColor = Color.White
                            )
                        ) {
                            Icon(
                                imageVector = Icons.Filled.AccountCircle,
                                contentDescription = null,
                                modifier = Modifier.size(24.dp)
                            )
                            Spacer(modifier = Modifier.width(10.dp))
                            Text(
                                text = "Open with Google (pak82914@gmail.com)",
                                fontSize = 15.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }

                        Spacer(modifier = Modifier.height(10.dp))

                        // Standard Google Account Picker
                        GoogleSignInButton(
                            authViewModel = authViewModel,
                            buttonText = "Sign in with other Google Account",
                            isPrimary = false,
                            onSuccess = onLoginSuccess
                        )

                        Spacer(modifier = Modifier.height(18.dp))

                        // Divider
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            HorizontalDivider(modifier = Modifier.weight(1f), color = CleanOutline)
                            Text(
                                text = "OR SIGN IN WITH EMAIL",
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Bold,
                                color = TextMuted,
                                modifier = Modifier.padding(horizontal = 12.dp)
                            )
                            HorizontalDivider(modifier = Modifier.weight(1f), color = CleanOutline)
                        }

                        Spacer(modifier = Modifier.height(18.dp))

                        // Error Alert Banner
                        if (uiState.errorMessage != null) {
                            Surface(
                                shape = RoundedCornerShape(12.dp),
                                color = ErrorRed.copy(alpha = 0.1f),
                                border = androidx.compose.foundation.BorderStroke(1.dp, ErrorRed.copy(alpha = 0.3f)),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(bottom = 16.dp)
                            ) {
                                Column(modifier = Modifier.padding(12.dp)) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Icon(
                                            imageVector = Icons.Filled.WarningAmber,
                                            contentDescription = "Error",
                                            tint = ErrorRed,
                                            modifier = Modifier.size(20.dp)
                                        )
                                        Spacer(modifier = Modifier.width(10.dp))
                                        Text(
                                            text = uiState.errorMessage ?: "",
                                            color = ErrorRed,
                                            style = MaterialTheme.typography.bodySmall,
                                            fontWeight = FontWeight.Medium
                                        )
                                    }
                                    val isProviderIssue = (uiState.errorMessage ?: "").contains("disabled", ignoreCase = true) ||
                                            (uiState.errorMessage ?: "").contains("operation is not allowed", ignoreCase = true)
                                    if (isProviderIssue) {
                                        Spacer(modifier = Modifier.height(10.dp))
                                        GoogleSignInButton(
                                            authViewModel = authViewModel,
                                            buttonText = "Sign in with Google Instead",
                                            onSuccess = onLoginSuccess
                                        )
                                    }
                                }
                            }
                        }

                        // Email Field
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(bottom = 6.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "Email Address",
                                style = MaterialTheme.typography.labelMedium,
                                fontWeight = FontWeight.SemiBold,
                                color = TextPrimary
                            )
                            Text(
                                text = "Fill pak82914@gmail.com",
                                style = MaterialTheme.typography.labelSmall,
                                color = BrandSecondary,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier
                                    .clickable {
                                        email = "pak82914@gmail.com"
                                        if (uiState.errorMessage != null) authViewModel.clearMessages()
                                    }
                                    .padding(vertical = 2.dp, horizontal = 4.dp)
                            )
                        }
                        OutlinedTextField(
                            value = email,
                            onValueChange = {
                                email = it
                                if (uiState.errorMessage != null) authViewModel.clearMessages()
                            },
                            placeholder = { Text("e.g. pak82914@gmail.com", color = TextMuted) },
                            textStyle = TextStyle(color = TextPrimary, fontSize = 15.sp, fontWeight = FontWeight.Normal),
                            leadingIcon = {
                                Icon(Icons.Filled.Email, contentDescription = "Email", tint = TextSecondary)
                            },
                            singleLine = true,
                            keyboardOptions = KeyboardOptions(
                                keyboardType = KeyboardType.Email,
                                imeAction = ImeAction.Next
                            ),
                            keyboardActions = KeyboardActions(
                                onNext = { focusManager.moveFocus(FocusDirection.Down) }
                            ),
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("login_email_input"),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedTextColor = TextPrimary,
                                unfocusedTextColor = TextPrimary,
                                cursorColor = BrandSecondary,
                                focusedBorderColor = BrandSecondary,
                                unfocusedBorderColor = CleanOutline,
                                focusedContainerColor = CleanSurface,
                                unfocusedContainerColor = CleanSurface,
                                focusedPlaceholderColor = TextMuted,
                                unfocusedPlaceholderColor = TextMuted,
                                focusedLeadingIconColor = BrandSecondary,
                                unfocusedLeadingIconColor = TextSecondary
                            )
                        )

                        Spacer(modifier = Modifier.height(18.dp))

                        // Password Field
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "Password",
                                style = MaterialTheme.typography.labelMedium,
                                fontWeight = FontWeight.SemiBold,
                                color = TextPrimary
                            )
                            TextButton(
                                onClick = {
                                    authViewModel.clearMessages()
                                    showForgotPasswordDialog = true
                                },
                                contentPadding = PaddingValues(0.dp),
                                modifier = Modifier.testTag("forgot_password_button")
                            ) {
                                Text(
                                    text = "Forgot Password?",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = BrandSecondary,
                                    fontWeight = FontWeight.SemiBold
                                )
                            }
                        }

                        OutlinedTextField(
                            value = password,
                            onValueChange = {
                                password = it
                                if (uiState.errorMessage != null) authViewModel.clearMessages()
                            },
                            placeholder = { Text("••••••••", color = TextMuted) },
                            textStyle = TextStyle(color = TextPrimary, fontSize = 15.sp, fontWeight = FontWeight.Normal),
                            leadingIcon = {
                                Icon(Icons.Filled.Lock, contentDescription = "Password", tint = TextSecondary)
                            },
                            trailingIcon = {
                                IconButton(
                                    onClick = { passwordVisible = !passwordVisible },
                                    modifier = Modifier.testTag("login_password_toggle")
                                ) {
                                    Icon(
                                        imageVector = if (passwordVisible) Icons.Filled.Visibility else Icons.Filled.VisibilityOff,
                                        contentDescription = if (passwordVisible) "Hide password" else "Show password",
                                        tint = TextSecondary
                                    )
                                }
                            },
                            visualTransformation = if (passwordVisible) VisualTransformation.None else PasswordVisualTransformation(),
                            singleLine = true,
                            keyboardOptions = KeyboardOptions(
                                keyboardType = KeyboardType.Password,
                                imeAction = ImeAction.Done
                            ),
                            keyboardActions = KeyboardActions(
                                onDone = {
                                    focusManager.clearFocus()
                                    authViewModel.login(email, password, onLoginSuccess)
                                }
                            ),
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("login_password_input"),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedTextColor = TextPrimary,
                                unfocusedTextColor = TextPrimary,
                                cursorColor = BrandSecondary,
                                focusedBorderColor = BrandSecondary,
                                unfocusedBorderColor = CleanOutline,
                                focusedContainerColor = CleanSurface,
                                unfocusedContainerColor = CleanSurface,
                                focusedPlaceholderColor = TextMuted,
                                unfocusedPlaceholderColor = TextMuted,
                                focusedLeadingIconColor = BrandSecondary,
                                unfocusedLeadingIconColor = TextSecondary,
                                focusedTrailingIconColor = BrandSecondary,
                                unfocusedTrailingIconColor = TextSecondary
                            )
                        )

                        Spacer(modifier = Modifier.height(24.dp))

                        // Sign In Button
                        Button(
                            onClick = {
                                focusManager.clearFocus()
                                authViewModel.login(email, password, onLoginSuccess)
                            },
                            enabled = !uiState.isLoading && !isLoggingIn,
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(52.dp)
                                .testTag("login_button"),
                            shape = RoundedCornerShape(12.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = BrandSecondary,
                                contentColor = Color.White
                            )
                        ) {
                            if (uiState.isLoading) {
                                CircularProgressIndicator(
                                    color = Color.White,
                                    modifier = Modifier.size(22.dp),
                                    strokeWidth = 2.5.dp
                                )
                                Spacer(modifier = Modifier.width(10.dp))
                                Text("Signing In...", fontWeight = FontWeight.SemiBold)
                            } else {
                                Text(
                                    text = "Sign In with Email",
                                    fontSize = 16.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(20.dp))

                        // Role note
                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = CleanSurfaceVariant,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(
                                text = "💡 Tip: Sign in instantly with your Google account, or use Email & Password. New staff accounts start with Cashier role.",
                                style = MaterialTheme.typography.bodySmall,
                                color = TextSecondary,
                                modifier = Modifier.padding(12.dp)
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(24.dp))

                // Create Account Button
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Center
                ) {
                    Text(
                        text = "Don't have an account?",
                        style = MaterialTheme.typography.bodyMedium,
                        color = TextSecondary
                    )
                    TextButton(
                        onClick = {
                            authViewModel.clearMessages()
                            onNavigateToRegister()
                        },
                        modifier = Modifier.testTag("register_navigate_button")
                    ) {
                        Text(
                            text = "Create Account",
                            fontWeight = FontWeight.Bold,
                            color = BrandSecondary
                        )
                    }
                }
            }
        }
    }
}
