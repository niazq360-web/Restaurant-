package com.example.ui.auth

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
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.AccountCircle
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.PersonAdd
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
import com.example.ui.theme.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RegisterScreen(
    authViewModel: AuthViewModel,
    onNavigateBack: () -> Unit,
    onRegisterSuccess: () -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val focusManager = LocalFocusManager.current
    val uiState by authViewModel.uiState.collectAsState()
    var isRegistering by remember { mutableStateOf(false) }

    var fullName by remember { mutableStateOf("") }
    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var confirmPassword by remember { mutableStateOf("") }
    var passwordVisible by remember { mutableStateOf(false) }
    var confirmPasswordVisible by remember { mutableStateOf(false) }

    Scaffold(
        containerColor = CleanBackground,
        topBar = {
            TopAppBar(
                title = { Text("Staff Registration", fontWeight = FontWeight.Bold, color = BrandPrimary) },
                navigationIcon = {
                    IconButton(
                        onClick = {
                            authViewModel.clearMessages()
                            onNavigateBack()
                        },
                        modifier = Modifier.testTag("register_back_button")
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back",
                            tint = BrandPrimary
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = CleanBackground)
            )
        }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .verticalScroll(rememberScrollState()),
            contentAlignment = Alignment.TopCenter
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .widthIn(max = 480.dp)
                    .padding(horizontal = 24.dp, vertical = 16.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Header badge
                Box(
                    modifier = Modifier
                        .size(72.dp)
                        .clip(CircleShape)
                        .background(BrandSecondaryContainer)
                        .border(2.dp, BrandSecondary.copy(alpha = 0.3f), CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Filled.PersonAdd,
                        contentDescription = "New Account",
                        tint = BrandOnSecondaryContainer,
                        modifier = Modifier.size(36.dp)
                    )
                }

                Spacer(modifier = Modifier.height(12.dp))

                Text(
                    text = "Create Staff Account",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = BrandPrimary
                )

                Text(
                    text = "Enter details to create your secure Firebase account",
                    style = MaterialTheme.typography.bodyMedium,
                    color = TextSecondary,
                    modifier = Modifier.padding(top = 4.dp, bottom = 20.dp)
                )

                // Registration Card
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
                        // 1-Click Google Account Sign Up (Instant Open)
                        Button(
                            onClick = {
                                focusManager.clearFocus()
                                authViewModel.openWithGoogleEmail("pak82914@gmail.com", fullName.ifBlank { "Restaurant Owner" }, onRegisterSuccess)
                            },
                            enabled = !uiState.isLoading,
                            shape = RoundedCornerShape(14.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(52.dp)
                                .testTag("google_email_register_button"),
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
                                text = "Quick Setup with Google (pak82914@gmail.com)",
                                fontSize = 15.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }

                        Spacer(modifier = Modifier.height(10.dp))

                        // Standard Google Account Picker
                        GoogleSignInButton(
                            authViewModel = authViewModel,
                            buttonText = "Sign up with other Google Account",
                            isPrimary = false,
                            onSuccess = onRegisterSuccess
                        )

                        Spacer(modifier = Modifier.height(18.dp))

                        // Divider
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            HorizontalDivider(modifier = Modifier.weight(1f), color = CleanOutline)
                            Text(
                                text = "OR USE EMAIL & PASSWORD",
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
                                            buttonText = "Sign up with Google Instead",
                                            onSuccess = onRegisterSuccess
                                        )
                                    }
                                }
                            }
                        }

                        // Full Name Field
                        Text(
                            text = "Full Name",
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.SemiBold,
                            color = TextPrimary,
                            modifier = Modifier.padding(bottom = 6.dp)
                        )
                        OutlinedTextField(
                            value = fullName,
                            onValueChange = {
                                fullName = it
                                if (uiState.errorMessage != null) authViewModel.clearMessages()
                            },
                            placeholder = { Text("e.g. John Doe", color = TextMuted) },
                            textStyle = TextStyle(color = TextPrimary, fontSize = 15.sp, fontWeight = FontWeight.Normal),
                            leadingIcon = {
                                Icon(Icons.Filled.Person, contentDescription = "Name", tint = TextSecondary)
                            },
                            singleLine = true,
                            keyboardOptions = KeyboardOptions(
                                keyboardType = KeyboardType.Text,
                                imeAction = ImeAction.Next
                            ),
                            keyboardActions = KeyboardActions(
                                onNext = { focusManager.moveFocus(FocusDirection.Down) }
                            ),
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("register_name_input"),
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

                        Spacer(modifier = Modifier.height(16.dp))

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
                                .testTag("register_email_input"),
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

                        Spacer(modifier = Modifier.height(16.dp))

                        // Password Field
                        Text(
                            text = "Password (min 6 characters)",
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.SemiBold,
                            color = TextPrimary,
                            modifier = Modifier.padding(bottom = 6.dp)
                        )
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
                                IconButton(onClick = { passwordVisible = !passwordVisible }) {
                                    Icon(
                                        imageVector = if (passwordVisible) Icons.Filled.Visibility else Icons.Filled.VisibilityOff,
                                        contentDescription = if (passwordVisible) "Hide" else "Show",
                                        tint = TextSecondary
                                    )
                                }
                            },
                            visualTransformation = if (passwordVisible) VisualTransformation.None else PasswordVisualTransformation(),
                            singleLine = true,
                            keyboardOptions = KeyboardOptions(
                                keyboardType = KeyboardType.Password,
                                imeAction = ImeAction.Next
                            ),
                            keyboardActions = KeyboardActions(
                                onNext = { focusManager.moveFocus(FocusDirection.Down) }
                            ),
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("register_password_input"),
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

                        Spacer(modifier = Modifier.height(16.dp))

                        // Confirm Password Field
                        Text(
                            text = "Confirm Password",
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.SemiBold,
                            color = TextPrimary,
                            modifier = Modifier.padding(bottom = 6.dp)
                        )
                        OutlinedTextField(
                            value = confirmPassword,
                            onValueChange = {
                                confirmPassword = it
                                if (uiState.errorMessage != null) authViewModel.clearMessages()
                            },
                            placeholder = { Text("••••••••", color = TextMuted) },
                            textStyle = TextStyle(color = TextPrimary, fontSize = 15.sp, fontWeight = FontWeight.Normal),
                            leadingIcon = {
                                Icon(Icons.Filled.Lock, contentDescription = "Confirm Password", tint = TextSecondary)
                            },
                            trailingIcon = {
                                IconButton(onClick = { confirmPasswordVisible = !confirmPasswordVisible }) {
                                    Icon(
                                        imageVector = if (confirmPasswordVisible) Icons.Filled.Visibility else Icons.Filled.VisibilityOff,
                                        contentDescription = if (confirmPasswordVisible) "Hide" else "Show",
                                        tint = TextSecondary
                                    )
                                }
                            },
                            visualTransformation = if (confirmPasswordVisible) VisualTransformation.None else PasswordVisualTransformation(),
                            singleLine = true,
                            keyboardOptions = KeyboardOptions(
                                keyboardType = KeyboardType.Password,
                                imeAction = ImeAction.Done
                            ),
                            keyboardActions = KeyboardActions(
                                onDone = {
                                    focusManager.clearFocus()
                                    authViewModel.register(
                                        fullName = fullName,
                                        email = email,
                                        pass = password,
                                        confirmPass = confirmPassword,
                                        onSuccess = onRegisterSuccess
                                    )
                                }
                            ),
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("register_confirm_password_input"),
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

                        Spacer(modifier = Modifier.height(20.dp))

                        // Role Notice Card
                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = BrandSecondaryContainer.copy(alpha = 0.5f),
                            border = androidx.compose.foundation.BorderStroke(1.dp, BrandSecondary.copy(alpha = 0.3f)),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier.padding(12.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    imageVector = Icons.Filled.Info,
                                    contentDescription = null,
                                    tint = BrandOnSecondaryContainer,
                                    modifier = Modifier.size(20.dp)
                                )
                                Spacer(modifier = Modifier.width(10.dp))
                                Column {
                                    Text(
                                        text = "Role: pak82914@gmail.com gets Owner/Admin",
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 12.sp,
                                        color = BrandOnSecondaryContainer
                                    )
                                    Text(
                                        text = "Other staff emails receive Cashier or Manager privileges automatically.",
                                        fontSize = 11.sp,
                                        color = BrandOnSecondaryContainer.copy(alpha = 0.85f)
                                    )
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(24.dp))

                        // Submit Button
                        Button(
                            onClick = {
                                focusManager.clearFocus()
                                authViewModel.register(
                                    fullName = fullName,
                                    email = email,
                                    pass = password,
                                    confirmPass = confirmPassword,
                                    onSuccess = onRegisterSuccess
                                )
                            },
                            enabled = !uiState.isLoading,
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(52.dp)
                                .testTag("register_button"),
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
                                Text("Creating Account...", fontWeight = FontWeight.SemiBold)
                            } else {
                                Text(
                                    text = "Create Account",
                                    fontSize = 16.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(20.dp))

                // Back to login
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Center
                ) {
                    Text(
                        text = "Already have an account?",
                        style = MaterialTheme.typography.bodyMedium,
                        color = TextSecondary
                    )
                    TextButton(
                        onClick = {
                            authViewModel.clearMessages()
                            onNavigateBack()
                        },
                        modifier = Modifier.testTag("back_to_login_button")
                    ) {
                        Text(
                            text = "Sign In",
                            fontWeight = FontWeight.Bold,
                            color = BrandSecondary
                        )
                    }
                }
            }
        }
    }
}
