package com.playermatch.app.data.repository

import com.google.firebase.firestore.FirebaseFirestore
import com.playermatch.app.data.model.JoinRequest
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await

class JoinRequestRepository {

    private val db = FirebaseFirestore.getInstance()
    private val requests = db.collection("joinRequests")

    // Creates a new request. Idempotency guard prevents duplicate requests.
    suspend fun sendRequest(
        teamId: String,
        senderId: String,
        senderName: String
    ): Result<String> = runCatching {
        val existing = requests
            .whereEqualTo("teamId", teamId)
            .whereEqualTo("senderId", senderId)
            .get().await()
        check(existing.isEmpty) { "You already have a request for this game." }

        val docRef = requests.document()
        docRef.set(
            JoinRequest(
                id = docRef.id,
                teamId = teamId,
                senderId = senderId,
                senderName = senderName
            )
        ).await()
        docRef.id
    }

    // Resets a rejected request back to pending without creating a new document
    suspend fun resendRequest(requestId: String): Result<Unit> = runCatching {
        requests.document(requestId).update(
            mapOf(
                "status" to JoinRequest.STATUS_PENDING,
                "createdAt" to System.currentTimeMillis()
            )
        ).await()
    }

    // Real-time stream of all requests for a team (host uses this)
    fun getRequestsForTeam(teamId: String): Flow<List<JoinRequest>> = callbackFlow {
        val listener = requests
            .whereEqualTo("teamId", teamId)
            .addSnapshotListener { snapshot, error ->
                if (error != null) { close(error); return@addSnapshotListener }
                val sorted = (snapshot?.toObjects(JoinRequest::class.java) ?: emptyList())
                    .sortedBy { it.createdAt }
                trySend(sorted)
            }
        awaitClose { listener.remove() }
    }

    // Real-time stream of the current user's own request for a specific team
    fun getUserRequestForTeam(teamId: String, userId: String): Flow<JoinRequest?> = callbackFlow {
        val listener = requests
            .whereEqualTo("teamId", teamId)
            .whereEqualTo("senderId", userId)
            .addSnapshotListener { snapshot, error ->
                if (error != null) { close(error); return@addSnapshotListener }
                trySend(snapshot?.toObjects(JoinRequest::class.java)?.firstOrNull())
            }
        awaitClose { listener.remove() }
    }

    // Atomically marks the request accepted AND increments the team's filledSlots.
    // The transaction ensures the slot count stays consistent under concurrent requests.
    suspend fun acceptRequest(request: JoinRequest): Result<Unit> = runCatching {
        db.runTransaction { tx ->
            val teamRef = db.collection("teams").document(request.teamId)
            val snap = tx.get(teamRef)
            val filled = snap.getLong("filledSlots")?.toInt() ?: 0
            val total = snap.getLong("totalSlots")?.toInt() ?: 0
            check(filled < total) { "Game is already full." }
            tx.update(requests.document(request.id), "status", JoinRequest.STATUS_ACCEPTED)
            tx.update(teamRef, "filledSlots", filled + 1)
        }.await()
    }

    suspend fun rejectRequest(requestId: String): Result<Unit> = runCatching {
        requests.document(requestId)
            .update("status", JoinRequest.STATUS_REJECTED)
            .await()
    }

    // Player cancels their own pending request
    suspend fun cancelRequest(requestId: String): Result<Unit> = runCatching {
        requests.document(requestId).delete().await()
    }
}
