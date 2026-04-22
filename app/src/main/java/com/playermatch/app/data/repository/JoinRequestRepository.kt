package com.playermatch.app.data.repository

import com.google.firebase.firestore.FirebaseFirestore
import com.playermatch.app.data.model.AppNotification
import com.playermatch.app.data.model.JoinRequest
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await

class JoinRequestRepository {

    private val db = FirebaseFirestore.getInstance()
    private val requests = db.collection("joinRequests")
    private val notifRepo = NotificationRepository()

    // Creates a new request. Idempotency guard prevents duplicate requests.
    // hostId + sportName are used to notify the host; failures are silently ignored.
    suspend fun sendRequest(
        teamId: String,
        senderId: String,
        senderName: String,
        hostId: String,
        sportName: String
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

        runCatching {
            notifRepo.createNotification(
                AppNotification(
                    recipientId = hostId,
                    title = "New Join Request",
                    body = "$senderName wants to join your $sportName game",
                    type = AppNotification.TYPE_REQUEST_SENT,
                    teamId = teamId,
                    createdAt = System.currentTimeMillis()
                )
            )
        }

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
    // Notifies the player (best-effort — failure does not roll back the accept).
    suspend fun acceptRequest(request: JoinRequest, hostName: String): Result<Unit> = runCatching {
        db.runTransaction { tx ->
            val teamRef = db.collection("teams").document(request.teamId)
            val snap = tx.get(teamRef)
            val filled = snap.getLong("filledSlots")?.toInt() ?: 0
            val total = snap.getLong("totalSlots")?.toInt() ?: 0
            check(filled < total) { "Game is already full." }
            tx.update(requests.document(request.id), "status", JoinRequest.STATUS_ACCEPTED)
            tx.update(teamRef, "filledSlots", filled + 1)
        }.await()

        runCatching {
            notifRepo.createNotification(
                AppNotification(
                    recipientId = request.senderId,
                    title = "Request Accepted!",
                    body = "$hostName accepted your join request. See you on the field!",
                    type = AppNotification.TYPE_REQUEST_ACCEPTED,
                    teamId = request.teamId,
                    createdAt = System.currentTimeMillis()
                )
            )
        }
    }

    // Marks request rejected and notifies the player (best-effort).
    suspend fun rejectRequest(request: JoinRequest, hostName: String): Result<Unit> = runCatching {
        requests.document(request.id)
            .update("status", JoinRequest.STATUS_REJECTED)
            .await()

        runCatching {
            notifRepo.createNotification(
                AppNotification(
                    recipientId = request.senderId,
                    title = "Request Not Accepted",
                    body = "$hostName couldn't add you this time. Try again or find another game!",
                    type = AppNotification.TYPE_REQUEST_REJECTED,
                    teamId = request.teamId,
                    createdAt = System.currentTimeMillis()
                )
            )
        }
    }

    // Player cancels their own pending request
    suspend fun cancelRequest(requestId: String): Result<Unit> = runCatching {
        requests.document(requestId).delete().await()
    }
}
