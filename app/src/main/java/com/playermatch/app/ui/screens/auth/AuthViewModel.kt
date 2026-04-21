package com.playermatch.app.ui.screens.auth

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.playermatch.app.data.model.User
import com.playermatch.app.data.repository.AuthRepository
import com.playermatch.app.data.repository.UserRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

sealed class AuthUiState {
    object Idle : AuthUiState()
    object Loading : AuthUiState()
    object Success : AuthUiState()                    // login verified OK → go to Home
    object VerificationEmailSent : AuthUiState()      // register OK → go to EmailVerification screen
    object EmailNotVerified : AuthUiState()           // login OK but email not verified yet
    data class Error(val message: String) : AuthUiState()
}

class AuthViewModel : ViewModel() {

    private val authRepo = AuthRepository()
    private val userRepo = UserRepository()

    private val _uiState = MutableStateFlow<AuthUiState>(AuthUiState.Idle)
    val uiState: StateFlow<AuthUiState> = _uiState.asStateFlow()

    // ── Login ────────────────────────────────────────────────────────────────

    fun login(email: String, password: String) {
        if (email.isBlank() || password.isBlank()) {
            _uiState.value = AuthUiState.Error("Email and password cannot be empty")
            return
        }
        viewModelScope.launch {
            _uiState.value = AuthUiState.Loading
            authRepo.login(email, password)
                .onSuccess { firebaseUser ->
                    if (firebaseUser.isEmailVerified) {
                        _uiState.value = AuthUiState.Success
                    } else {
                        // Stay signed in but block access until verified
                        _uiState.value = AuthUiState.EmailNotVerified
                    }
                }
                .onFailure { _uiState.value = AuthUiState.Error(friendlyMessage(it)) }
        }
    }

    // ── Register ─────────────────────────────────────────────────────────────

    fun register(email: String, password: String, name: String, sport: String) {
        if (email.isBlank() || password.isBlank() || name.isBlank() || sport.isBlank()) {
            _uiState.value = AuthUiState.Error("Please fill in all fields")
            return
        }
        viewModelScope.launch {
            _uiState.value = AuthUiState.Loading
            authRepo.register(email, password)
                .onSuccess { firebaseUser ->
                    // 1. Create Firestore profile
                    val user = User(
                        uid = firebaseUser.uid,
                        name = name.trim(),
                        email = email.trim(),
                        sport = sport
                    )
                    userRepo.createUser(user)
                        .onSuccess {
                            // 2. Send verification email
                            authRepo.sendVerificationEmail()
                            _uiState.value = AuthUiState.VerificationEmailSent
                        }
                        .onFailure { _uiState.value = AuthUiState.Error(friendlyMessage(it)) }
                }
                .onFailure { _uiState.value = AuthUiState.Error(friendlyMessage(it)) }
        }
    }

    // ── Email Verification ────────────────────────────────────────────────────

    // Called from EmailVerificationScreen after user clicks link; reloads Firebase token
    fun checkEmailVerification() {
        viewModelScope.launch {
            _uiState.value = AuthUiState.Loading
            authRepo.reloadUser()
                .onSuccess {
                    if (authRepo.isEmailVerified) {
                        _uiState.value = AuthUiState.Success
                    } else {
                        _uiState.value = AuthUiState.EmailNotVerified
                    }
                }
                .onFailure { _uiState.value = AuthUiState.Error(friendlyMessage(it)) }
        }
    }

    // Re-sends the verification email (rate-limited by Firebase to a few per hour)
    fun resendVerificationEmail() {
        viewModelScope.launch {
            _uiState.value = AuthUiState.Loading
            authRepo.sendVerificationEmail()
                .onSuccess { _uiState.value = AuthUiState.VerificationEmailSent }
                .onFailure { _uiState.value = AuthUiState.Error(friendlyMessage(it)) }
        }
    }

    // ── Helpers ───────────────────────────────────────────────────────────────

    fun resetState() {
        _uiState.value = AuthUiState.Idle
    }

    private fun friendlyMessage(error: Throwable): String {
        val msg = error.message ?: return "Something went wrong. Please try again."
        return when {
            msg.contains("INVALID_EMAIL", true) -> "Invalid email address"
            msg.contains("WRONG_PASSWORD", true) ||
            msg.contains("INVALID_CREDENTIAL", true) -> "Invalid email or password"
            msg.contains("USER_NOT_FOUND", true) -> "No account found with this email"
            msg.contains("EMAIL_ALREADY_IN_USE", true) -> "Email is already registered"
            msg.contains("WEAK_PASSWORD", true) -> "Password must be at least 6 characters"
            msg.contains("NETWORK_ERROR", true) ||
            msg.contains("network", true) -> "Network error — check your connection"
            msg.contains("TOO_MANY_REQUESTS", true) -> "Too many attempts. Try again later."
            else -> msg
        }
    }
}
