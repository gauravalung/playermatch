package com.playermatch.app.data.repository

import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import kotlinx.coroutines.tasks.await

class AuthRepository {

    private val auth = FirebaseAuth.getInstance()

    val currentUser: FirebaseUser? get() = auth.currentUser
    val currentUserId: String? get() = auth.currentUser?.uid
    val isEmailVerified: Boolean get() = auth.currentUser?.isEmailVerified == true

    suspend fun login(email: String, password: String): Result<FirebaseUser> = runCatching {
        auth.signInWithEmailAndPassword(email, password).await().user
            ?: error("Login returned no user")
    }

    suspend fun register(email: String, password: String): Result<FirebaseUser> = runCatching {
        auth.createUserWithEmailAndPassword(email, password).await().user
            ?: error("Registration returned no user")
    }

    // Sends the verification link to the currently signed-in user's email
    suspend fun sendVerificationEmail(): Result<Unit> = runCatching {
        auth.currentUser?.sendEmailVerification()?.await()
            ?: error("No signed-in user to send verification to")
    }

    // Reloads the Firebase user object so isEmailVerified reflects the latest state
    suspend fun reloadUser(): Result<Unit> = runCatching {
        auth.currentUser?.reload()?.await()
            ?: error("No signed-in user to reload")
    }

    fun logout() = auth.signOut()
}
