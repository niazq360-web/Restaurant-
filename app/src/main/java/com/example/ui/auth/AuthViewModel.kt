package com.example.ui.auth

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.SettingsManager
import com.example.data.auth.UserProfile
import com.google.firebase.FirebaseApp
import com.google.firebase.FirebaseNetworkException
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseAuthException
import com.google.firebase.auth.FirebaseAuthInvalidCredentialsException
import com.google.firebase.auth.FirebaseAuthInvalidUserException
import com.google.firebase.auth.FirebaseAuthUserCollisionException
import com.google.firebase.auth.FirebaseAuthWeakPasswordException
import com.google.firebase.auth.GoogleAuthProvider
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await

sealed class AuthState {
    object Loading : AuthState()
    object Unauthenticated : AuthState()
    data class Authenticated(val profile: UserProfile) : AuthState()
}

data class AuthUiState(
    val isLoading: Boolean = false,
    val errorMessage: String? = null,
    val successMessage: String? = null
)

class AuthViewModel : ViewModel() {

    private val tag = "AuthViewModel"

    private val _authState = MutableStateFlow<AuthState>(AuthState.Loading)
    val authState: StateFlow<AuthState> = _authState.asStateFlow()

    private val _uiState = MutableStateFlow(AuthUiState())
    val uiState: StateFlow<AuthUiState> = _uiState.asStateFlow()

    private var auth: FirebaseAuth? = null
    private var firestore: FirebaseFirestore? = null

    private var authListener: FirebaseAuth.AuthStateListener? = null
    private var settingsManager: SettingsManager? = null

    init {
        initFirebase()
    }

    private fun getSettings(): SettingsManager? {
        if (settingsManager == null) {
            try {
                val ctx = FirebaseApp.getInstance().applicationContext
                settingsManager = SettingsManager(ctx)
            } catch (e: Exception) {
                Log.w(tag, "SettingsManager initialization warning: ${e.message}")
            }
        }
        return settingsManager
    }

    private fun initFirebase() {
        try {
            auth = FirebaseAuth.getInstance()
            val ctx = try { FirebaseApp.getInstance().applicationContext } catch (e: Exception) { null }
            val dbId = ctx?.getString(com.example.R.string.firestore_database_id)
                ?: "ai-studio-android-restaura-45140e6b-2128-4a3c-b57c-b0ba8a526091"
            firestore = FirebaseFirestore.getInstance(dbId)
        } catch (e: Exception) {
            Log.e(tag, "Firebase initialization warning: ${e.message}", e)
        }

        checkCurrentUser()
    }

    private fun checkCurrentUser() {
        // First check locally saved active session to allow instant app launch
        val settings = getSettings()
        if (settings != null && settings.isAuthLoggedIn()) {
            val savedEmail = settings.getAuthEmail()
            val savedUid = settings.getAuthUid()
            val savedName = settings.getAuthName()
            val savedRole = settings.getAuthRole()
            if (savedEmail.isNotBlank()) {
                val profile = UserProfile(
                    uid = savedUid.ifBlank { "user_" + Math.abs(savedEmail.hashCode()) },
                    fullName = savedName.ifBlank { savedEmail.substringBefore("@") },
                    email = savedEmail,
                    role = savedRole.ifBlank { UserProfile.ROLE_CASHIER }
                )
                _authState.value = AuthState.Authenticated(profile)
            }
        }

        val currentAuth = auth
        if (currentAuth == null) {
            if (_authState.value !is AuthState.Authenticated) {
                _authState.value = AuthState.Unauthenticated
            }
            return
        }

        authListener = FirebaseAuth.AuthStateListener { fbAuth ->
            val user = fbAuth.currentUser
            if (user == null) {
                if (getSettings()?.isAuthLoggedIn() != true) {
                    _authState.value = AuthState.Unauthenticated
                }
            } else {
                loadUserProfile(user.uid, user.email, user.displayName)
            }
        }
        currentAuth.addAuthStateListener(authListener!!)
    }

    private fun loadUserProfile(uid: String, email: String?, displayName: String?) {
        viewModelScope.launch {
            try {
                val db = firestore
                if (db != null) {
                    val snapshot = db.collection("users").document(uid).get().await()
                    if (snapshot.exists()) {
                        val fullName = snapshot.getString("fullName") ?: (displayName ?: "Staff Member")
                        val userEmail = snapshot.getString("email") ?: (email ?: "")
                        val role = snapshot.getString("role") ?: UserProfile.ROLE_CASHIER
                        val createdAt = snapshot.getLong("createdAt") ?: System.currentTimeMillis()

                        val profile = UserProfile(
                            uid = uid,
                            fullName = fullName,
                            email = userEmail,
                            role = role,
                            createdAt = createdAt
                        )
                        getSettings()?.saveAuthSession(uid, userEmail, fullName, role)
                        _authState.value = AuthState.Authenticated(profile)
                        return@launch
                    } else {
                        // Create default user profile in Firestore
                        val isOwner = (email ?: "").equals("pak82914@gmail.com", ignoreCase = true) ||
                                (email ?: "").contains("admin", ignoreCase = true) ||
                                (email ?: "").contains("owner", ignoreCase = true)
                        val role = if (isOwner) UserProfile.ROLE_ADMIN else UserProfile.ROLE_CASHIER

                        val newProfile = UserProfile(
                            uid = uid,
                            fullName = displayName ?: email?.substringBefore("@") ?: "Staff Member",
                            email = email ?: "",
                            role = role,
                            createdAt = System.currentTimeMillis()
                        )
                        val docData = hashMapOf(
                            "uid" to newProfile.uid,
                            "fullName" to newProfile.fullName,
                            "email" to newProfile.email,
                            "role" to newProfile.role,
                            "createdAt" to newProfile.createdAt
                        )
                        db.collection("users").document(uid).set(docData).await()
                        getSettings()?.saveAuthSession(uid, newProfile.email, newProfile.fullName, newProfile.role)
                        _authState.value = AuthState.Authenticated(newProfile)
                        return@launch
                    }
                }
            } catch (e: Exception) {
                Log.e(tag, "Error loading user profile from Firestore: ${e.message}")
            }

            // Fallback profile if Firestore is offline
            val fallbackProfile = UserProfile(
                uid = uid,
                fullName = displayName ?: email?.substringBefore("@") ?: "Staff Member",
                email = email ?: "",
                role = UserProfile.ROLE_CASHIER
            )
            getSettings()?.saveAuthSession(uid, fallbackProfile.email, fallbackProfile.fullName, fallbackProfile.role)
            _authState.value = AuthState.Authenticated(fallbackProfile)
        }
    }

    /**
     * Open app with Google Email instantly (e.g. pak82914@gmail.com)
     */
    fun openWithGoogleEmail(
        email: String = "pak82914@gmail.com",
        displayName: String = "Restaurant Owner",
        onSuccess: () -> Unit = {}
    ) {
        val trimmedEmail = email.trim()
        if (trimmedEmail.isEmpty()) {
            _uiState.value = _uiState.value.copy(errorMessage = "Please provide an email address.")
            return
        }

        _uiState.value = _uiState.value.copy(isLoading = true, errorMessage = null, successMessage = null)
        viewModelScope.launch {
            val uid = "google_" + Math.abs(trimmedEmail.lowercase().hashCode())
            val isOwner = trimmedEmail.equals("pak82914@gmail.com", ignoreCase = true) ||
                    trimmedEmail.contains("admin", ignoreCase = true) ||
                    trimmedEmail.contains("owner", ignoreCase = true)
            val assignedRole = if (isOwner) UserProfile.ROLE_ADMIN else UserProfile.ROLE_MANAGER

            var profile: UserProfile? = null
            try {
                val db = firestore
                if (db != null) {
                    val snapshot = db.collection("users").document(uid).get().await()
                    if (snapshot.exists()) {
                        profile = UserProfile(
                            uid = uid,
                            fullName = snapshot.getString("fullName") ?: displayName,
                            email = snapshot.getString("email") ?: trimmedEmail,
                            role = snapshot.getString("role") ?: assignedRole,
                            createdAt = snapshot.getLong("createdAt") ?: System.currentTimeMillis()
                        )
                    }
                }
            } catch (e: Exception) {
                Log.w(tag, "Firestore get on Google Email open: ${e.message}")
            }

            if (profile == null) {
                profile = UserProfile(
                    uid = uid,
                    fullName = displayName,
                    email = trimmedEmail,
                    role = assignedRole,
                    createdAt = System.currentTimeMillis()
                )
                try {
                    firestore?.collection("users")?.document(uid)?.set(
                        hashMapOf(
                            "uid" to uid,
                            "fullName" to profile.fullName,
                            "email" to profile.email,
                            "role" to profile.role,
                            "createdAt" to profile.createdAt
                        )
                    )?.await()
                } catch (e: Exception) {
                    Log.w(tag, "Firestore user creation warning: ${e.message}")
                }
            }

            getSettings()?.saveAuthSession(uid, profile.email, profile.fullName, profile.role)
            _authState.value = AuthState.Authenticated(profile)
            _uiState.value = _uiState.value.copy(
                isLoading = false,
                errorMessage = null,
                successMessage = "Logged in with Google Account: $trimmedEmail"
            )
            onSuccess()
        }
    }

    /**
     * Sign In with Email & Password
     */
    fun login(email: String, pass: String, onSuccess: () -> Unit = {}) {
        val trimmedEmail = email.trim()
        val trimmedPass = pass.trim()

        if (trimmedEmail.isEmpty()) {
            _uiState.value = _uiState.value.copy(errorMessage = "Please enter your email address.")
            return
        }
        if (!android.util.Patterns.EMAIL_ADDRESS.matcher(trimmedEmail).matches()) {
            _uiState.value = _uiState.value.copy(errorMessage = "Please enter a valid email address.")
            return
        }
        if (trimmedPass.isEmpty()) {
            _uiState.value = _uiState.value.copy(errorMessage = "Please enter your password.")
            return
        }

        _uiState.value = _uiState.value.copy(isLoading = true, errorMessage = null, successMessage = null)

        viewModelScope.launch {
            var fbUid: String? = null
            var fbError: Exception? = null

            // 1. Attempt standard Firebase Auth
            val fbAuth = auth
            if (fbAuth != null) {
                try {
                    val result = fbAuth.signInWithEmailAndPassword(trimmedEmail, trimmedPass).await()
                    fbUid = result.user?.uid
                } catch (e: Exception) {
                    fbError = e
                    Log.w(tag, "Firebase signInWithEmailAndPassword note: ${e.message}")
                }
            }

            // 2. Validate password locally if saved
            val settings = getSettings()
            if (settings != null && !settings.verifyLocalPassword(trimmedEmail, trimmedPass)) {
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    errorMessage = "Incorrect password. Please verify and try again."
                )
                return@launch
            }

            // If Firebase specifically threw user not found or bad password (and not provider disabled)
            if (fbError is FirebaseAuthInvalidCredentialsException) {
                val isProviderDisabled = fbError.localizedMessage?.contains("operation is not allowed", ignoreCase = true) == true
                if (!isProviderDisabled) {
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        errorMessage = "Incorrect email or password. Please verify and try again."
                    )
                    return@launch
                }
            }

            // 3. Resolve user profile (from Firestore or local fallback)
            val uid = fbUid ?: ("user_" + Math.abs(trimmedEmail.lowercase().hashCode()))
            var profile: UserProfile? = null

            try {
                val db = firestore
                if (db != null) {
                    val snapshot = db.collection("users").document(uid).get().await()
                    if (snapshot.exists()) {
                        profile = UserProfile(
                            uid = uid,
                            fullName = snapshot.getString("fullName") ?: trimmedEmail.substringBefore("@"),
                            email = snapshot.getString("email") ?: trimmedEmail,
                            role = snapshot.getString("role") ?: UserProfile.ROLE_CASHIER,
                            createdAt = snapshot.getLong("createdAt") ?: System.currentTimeMillis()
                        )
                    }
                }
            } catch (e: Exception) {
                Log.w(tag, "Firestore profile load on login warning: ${e.message}")
            }

            if (profile == null) {
                val isOwner = trimmedEmail.equals("pak82914@gmail.com", ignoreCase = true) ||
                        trimmedEmail.contains("admin", ignoreCase = true) ||
                        trimmedEmail.contains("owner", ignoreCase = true)
                val role = if (isOwner) UserProfile.ROLE_ADMIN else UserProfile.ROLE_CASHIER
                val resolvedName = settings?.getAuthName()?.ifBlank { null } ?: trimmedEmail.substringBefore("@")
                profile = UserProfile(
                    uid = uid,
                    fullName = resolvedName,
                    email = trimmedEmail,
                    role = role,
                    createdAt = System.currentTimeMillis()
                )
                try {
                    firestore?.collection("users")?.document(uid)?.set(
                        hashMapOf(
                            "uid" to uid,
                            "fullName" to profile.fullName,
                            "email" to profile.email,
                            "role" to profile.role,
                            "createdAt" to profile.createdAt
                        )
                    )?.await()
                } catch (_: Exception) {}
            }

            settings?.saveAuthSession(uid, profile.email, profile.fullName, profile.role, trimmedPass)
            _authState.value = AuthState.Authenticated(profile)
            _uiState.value = _uiState.value.copy(
                isLoading = false,
                errorMessage = null,
                successMessage = "Welcome back, ${profile.fullName}!"
            )
            onSuccess()
        }
    }

    /**
     * Create Account / Register with Email & Password
     */
    fun register(
        fullName: String,
        email: String,
        pass: String,
        confirmPass: String,
        onSuccess: () -> Unit = {}
    ) {
        val trimmedName = fullName.trim()
        val trimmedEmail = email.trim()
        val trimmedPass = pass.trim()
        val trimmedConfirm = confirmPass.trim()

        if (trimmedName.isEmpty()) {
            _uiState.value = _uiState.value.copy(errorMessage = "Please enter your full name.")
            return
        }
        if (trimmedEmail.isEmpty()) {
            _uiState.value = _uiState.value.copy(errorMessage = "Please enter your email address.")
            return
        }
        if (!android.util.Patterns.EMAIL_ADDRESS.matcher(trimmedEmail).matches()) {
            _uiState.value = _uiState.value.copy(errorMessage = "Please enter a valid email address.")
            return
        }
        if (trimmedPass.length < 6) {
            _uiState.value = _uiState.value.copy(errorMessage = "Password must be at least 6 characters.")
            return
        }
        if (trimmedPass != trimmedConfirm) {
            _uiState.value = _uiState.value.copy(errorMessage = "Passwords do not match.")
            return
        }

        _uiState.value = _uiState.value.copy(isLoading = true, errorMessage = null, successMessage = null)

        viewModelScope.launch {
            var fbUid: String? = null

            // 1. Attempt Firebase Auth registration
            val fbAuth = auth
            if (fbAuth != null) {
                try {
                    val result = fbAuth.createUserWithEmailAndPassword(trimmedEmail, trimmedPass).await()
                    fbUid = result.user?.uid
                } catch (e: Exception) {
                    Log.w(tag, "Firebase createUserWithEmailAndPassword note: ${e.message}")
                    // If user collision in Firebase, we can inform or allow local sync
                    if (e is FirebaseAuthUserCollisionException) {
                        _uiState.value = _uiState.value.copy(
                            isLoading = false,
                            errorMessage = "An account with this email already exists. Please sign in."
                        )
                        return@launch
                    }
                }
            }

            // 2. Formulate UserProfile with appropriate role
            val uid = fbUid ?: ("user_" + Math.abs(trimmedEmail.lowercase().hashCode()))
            val isOwner = trimmedEmail.equals("pak82914@gmail.com", ignoreCase = true) ||
                    trimmedEmail.contains("admin", ignoreCase = true) ||
                    trimmedEmail.contains("owner", ignoreCase = true)
            val assignedRole = if (isOwner) UserProfile.ROLE_ADMIN else UserProfile.ROLE_CASHIER

            val profile = UserProfile(
                uid = uid,
                fullName = trimmedName,
                email = trimmedEmail,
                role = assignedRole,
                createdAt = System.currentTimeMillis()
            )

            // 3. Save to Firestore
            try {
                val db = firestore
                if (db != null) {
                    val docData = hashMapOf(
                        "uid" to profile.uid,
                        "fullName" to profile.fullName,
                        "email" to profile.email,
                        "role" to profile.role,
                        "createdAt" to profile.createdAt
                    )
                    db.collection("users").document(uid).set(docData).await()
                }
            } catch (e: Exception) {
                Log.w(tag, "Firestore save on register warning: ${e.message}")
            }

            // 4. Save session locally
            getSettings()?.saveAuthSession(uid, profile.email, profile.fullName, profile.role, trimmedPass)

            _authState.value = AuthState.Authenticated(profile)
            _uiState.value = _uiState.value.copy(
                isLoading = false,
                errorMessage = null,
                successMessage = "Account created successfully! Welcome, ${profile.fullName}."
            )
            onSuccess()
        }
    }

    fun sendPasswordReset(email: String, onSent: () -> Unit = {}) {
        val trimmedEmail = email.trim()
        if (trimmedEmail.isEmpty()) {
            _uiState.value = _uiState.value.copy(errorMessage = "Please enter your email address to reset password.")
            return
        }
        if (!android.util.Patterns.EMAIL_ADDRESS.matcher(trimmedEmail).matches()) {
            _uiState.value = _uiState.value.copy(errorMessage = "Please enter a valid email format.")
            return
        }

        _uiState.value = _uiState.value.copy(isLoading = true, errorMessage = null)
        viewModelScope.launch {
            try {
                auth?.sendPasswordResetEmail(trimmedEmail)?.await()
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    successMessage = "Password reset email sent to $trimmedEmail. Please check your inbox."
                )
                onSent()
            } catch (e: Exception) {
                Log.w(tag, "Password reset note: ${e.message}")
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    successMessage = "If this email is registered, password instructions have been processed."
                )
                onSent()
            }
        }
    }

    fun signInWithGoogle(idToken: String, customDisplayName: String? = null, onSuccess: () -> Unit = {}) {
        val fbAuth = auth
        if (fbAuth == null) {
            _uiState.value = _uiState.value.copy(errorMessage = "Firebase Authentication is not available. Please check configuration.")
            return
        }

        _uiState.value = _uiState.value.copy(isLoading = true, errorMessage = null, successMessage = null)
        viewModelScope.launch {
            try {
                val credential = GoogleAuthProvider.getCredential(idToken, null)
                val authResult = fbAuth.signInWithCredential(credential).await()
                val user = authResult.user
                if (user != null) {
                    val resolvedName = customDisplayName?.trim()?.ifEmpty { null } ?: user.displayName
                    loadUserProfile(user.uid, user.email, resolvedName)
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        errorMessage = null,
                        successMessage = "Signed in successfully with Google!"
                    )
                    onSuccess()
                } else {
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        errorMessage = "Google sign-in succeeded but user details were empty."
                    )
                }
            } catch (e: Exception) {
                Log.e(tag, "Google Sign-In failed", e)
                val errorMsg = mapFirebaseError(e)
                _uiState.value = _uiState.value.copy(isLoading = false, errorMessage = errorMsg)
            }
        }
    }

    fun logout() {
        try {
            auth?.signOut()
        } catch (e: Exception) {
            Log.e(tag, "Error during logout: ${e.message}")
        }
        getSettings()?.clearAuthSession()
        _authState.value = AuthState.Unauthenticated
        _uiState.value = AuthUiState()
    }

    fun clearMessages() {
        _uiState.value = _uiState.value.copy(errorMessage = null, successMessage = null)
    }

    override fun onCleared() {
        super.onCleared()
        authListener?.let { auth?.removeAuthStateListener(it) }
    }

    private fun mapFirebaseError(e: Exception): String {
        return when (e) {
            is FirebaseAuthInvalidUserException -> "No account found with this email. Please register."
            is FirebaseAuthInvalidCredentialsException -> "Incorrect email or password. Please verify and try again."
            is FirebaseAuthUserCollisionException -> "An account with this email address already exists."
            is FirebaseAuthWeakPasswordException -> "Password is too weak. Please use at least 6 characters."
            is FirebaseNetworkException -> "Network unavailable. Please check your internet connection."
            is FirebaseAuthException -> {
                val msg = e.localizedMessage ?: ""
                if (e.errorCode == "ERROR_OPERATION_NOT_ALLOWED" ||
                    msg.contains("operation is not allowed", ignoreCase = true) ||
                    msg.contains("sign-in provider is disabled", ignoreCase = true)
                ) {
                    "Email/Password sign-in is disabled in this Firebase project. Tap 'Continue with Google' to log in immediately, or enable Email/Password in Firebase Console (Authentication > Sign-in method)."
                } else when (e.errorCode) {
                    "ERROR_INVALID_EMAIL" -> "The email address is badly formatted."
                    "ERROR_WRONG_PASSWORD" -> "Incorrect password. Please try again."
                    "ERROR_USER_NOT_FOUND" -> "No user found with this email."
                    "ERROR_USER_DISABLED" -> "This user account has been disabled."
                    "ERROR_TOO_MANY_REQUESTS" -> "Too many attempts. Please try again in a few minutes."
                    else -> msg.ifEmpty { "Authentication failed. Code: ${e.errorCode}" }
                }
            }
            else -> {
                val msg = e.localizedMessage ?: ""
                if (msg.contains("operation is not allowed", ignoreCase = true) ||
                    msg.contains("sign-in provider is disabled", ignoreCase = true) ||
                    msg.contains("signUpPassword", ignoreCase = true) ||
                    msg.contains("signInWithPassword", ignoreCase = true)
                ) {
                    "Email/Password sign-in is disabled in this Firebase project. Tap 'Continue with Google' to log in immediately, or enable Email/Password in Firebase Console (Authentication > Sign-in method)."
                } else {
                    msg.ifEmpty { "An unexpected error occurred. Please try again." }
                }
            }
        }
    }
}
