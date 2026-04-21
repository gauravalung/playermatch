package com.playermatch.app.data.repository

import com.google.firebase.firestore.FirebaseFirestore
import com.playermatch.app.data.model.Team
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await

class TeamRepository {

    private val db = FirebaseFirestore.getInstance()
    private val teamsCol = db.collection("teams")

    suspend fun createTeam(team: Team): Result<String> = runCatching {
        val docRef = teamsCol.document()
        docRef.set(team.copy(id = docRef.id)).await()
        docRef.id
    }

    suspend fun getTeam(teamId: String): Result<Team?> = runCatching {
        teamsCol.document(teamId).get().await().toObject(Team::class.java)
    }

    // Real-time single-document listener — TeamDetailScreen uses this so the
    // slot count updates live when the host accepts requests.
    fun getTeamFlow(teamId: String): Flow<Team?> = callbackFlow {
        val listener = teamsCol.document(teamId)
            .addSnapshotListener { snapshot, error ->
                if (error != null) { close(error); return@addSnapshotListener }
                trySend(snapshot?.toObject(Team::class.java))
            }
        awaitClose { listener.remove() }
    }

    // NOTE: orderBy removed from both collection flows to avoid requiring a
    // Firestore composite index during development.  Sorting is done client-side.
    fun getAllTeamsFlow(): Flow<List<Team>> = callbackFlow {
        val listener = teamsCol
            .addSnapshotListener { snapshot, error ->
                if (error != null) { close(error); return@addSnapshotListener }
                val sorted = (snapshot?.toObjects(Team::class.java) ?: emptyList())
                    .sortedByDescending { it.createdAt }
                trySend(sorted)
            }
        awaitClose { listener.remove() }
    }

    fun getTeamsByHostFlow(hostId: String): Flow<List<Team>> = callbackFlow {
        val listener = teamsCol
            .whereEqualTo("hostId", hostId)
            .addSnapshotListener { snapshot, error ->
                if (error != null) { close(error); return@addSnapshotListener }
                val sorted = (snapshot?.toObjects(Team::class.java) ?: emptyList())
                    .sortedByDescending { it.createdAt }
                trySend(sorted)
            }
        awaitClose { listener.remove() }
    }

    suspend fun incrementFilledSlots(teamId: String): Result<Unit> = runCatching {
        db.runTransaction { tx ->
            val ref = teamsCol.document(teamId)
            val snap = tx.get(ref)
            tx.update(ref, "filledSlots", (snap.getLong("filledSlots")?.toInt() ?: 0) + 1)
        }.await()
    }
}
