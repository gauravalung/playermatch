package com.playermatch.app.data.repository

import com.google.firebase.firestore.FirebaseFirestore
import com.playermatch.app.data.model.User
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await

class UserRepository {

    private val db = FirebaseFirestore.getInstance()
    private val users = db.collection("users")

    suspend fun createUser(user: User): Result<Unit> = runCatching {
        users.document(user.uid).set(user).await()
    }

    suspend fun getUser(uid: String): Result<User?> = runCatching {
        users.document(uid).get().await().toObject(User::class.java)
    }

    suspend fun updateUser(user: User): Result<Unit> = runCatching {
        users.document(user.uid).set(user).await()
    }

    // Real-time stream of the user document
    fun getUserFlow(uid: String): Flow<User?> = callbackFlow {
        val listener = users.document(uid).addSnapshotListener { snapshot, error ->
            if (error != null) { close(error); return@addSnapshotListener }
            trySend(snapshot?.toObject(User::class.java))
        }
        awaitClose { listener.remove() }
    }

    // Lightweight location update — avoids overwriting other fields
    suspend fun updateLocation(uid: String, latitude: Double, longitude: Double): Result<Unit> =
        runCatching {
            users.document(uid)
                .update("latitude", latitude, "longitude", longitude)
                .await()
        }

    suspend fun updateFcmToken(uid: String, token: String): Result<Unit> = runCatching {
        users.document(uid).update("fcmToken", token).await()
    }
}
