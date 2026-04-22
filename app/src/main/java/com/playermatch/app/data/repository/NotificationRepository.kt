package com.playermatch.app.data.repository

import com.google.firebase.firestore.FirebaseFirestore
import com.playermatch.app.data.model.AppNotification
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await

class NotificationRepository {

    private val db = FirebaseFirestore.getInstance()
    private val notifCol = db.collection("notifications")

    suspend fun createNotification(notification: AppNotification): Result<Unit> = runCatching {
        val docRef = notifCol.document()
        docRef.set(notification.copy(id = docRef.id)).await()
    }

    fun getNotificationsFlow(userId: String): Flow<List<AppNotification>> = callbackFlow {
        val listener = notifCol
            .whereEqualTo("recipientId", userId)
            .addSnapshotListener { snapshot, error ->
                if (error != null) { close(error); return@addSnapshotListener }
                val sorted = (snapshot?.toObjects(AppNotification::class.java) ?: emptyList())
                    .sortedByDescending { it.createdAt }
                trySend(sorted)
            }
        awaitClose { listener.remove() }
    }

    suspend fun markAsRead(notificationId: String): Result<Unit> = runCatching {
        notifCol.document(notificationId).update("isRead", true).await()
    }

    suspend fun markAllAsRead(userId: String): Result<Unit> = runCatching {
        val unread = notifCol
            .whereEqualTo("recipientId", userId)
            .whereEqualTo("isRead", false)
            .get().await()
        if (unread.isEmpty) return@runCatching
        val batch = db.batch()
        unread.documents.forEach { batch.update(it.reference, "isRead", true) }
        batch.commit().await()
    }
}
