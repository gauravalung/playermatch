package com.playermatch.app.data.repository

import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import com.playermatch.app.data.model.Team
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await

class TeamRepository {

    private val db = FirebaseFirestore.getInstance()
    private val teamsCol = db.collection("teams")

    // Returns the generated document ID on success
    suspend fun createTeam(team: Team): Result<String> = runCatching {
        val docRef = teamsCol.document()
        val withId = team.copy(id = docRef.id)
        docRef.set(withId).await()
        docRef.id
    }

    suspend fun getTeam(teamId: String): Result<Team?> = runCatching {
        teamsCol.document(teamId).get().await().toObject(Team::class.java)
    }

    fun getAllTeamsFlow(): Flow<List<Team>> = callbackFlow {
        val listener = teamsCol
            .orderBy("createdAt", Query.Direction.DESCENDING)
            .addSnapshotListener { snapshot, error ->
                if (error != null) { close(error); return@addSnapshotListener }
                trySend(snapshot?.toObjects(Team::class.java) ?: emptyList())
            }
        awaitClose { listener.remove() }
    }

    fun getTeamsByHostFlow(hostId: String): Flow<List<Team>> = callbackFlow {
        val listener = teamsCol
            .whereEqualTo("hostId", hostId)
            .orderBy("createdAt", Query.Direction.DESCENDING)
            .addSnapshotListener { snapshot, error ->
                if (error != null) { close(error); return@addSnapshotListener }
                trySend(snapshot?.toObjects(Team::class.java) ?: emptyList())
            }
        awaitClose { listener.remove() }
    }

    // Partial update — only increments filledSlots, preserves all other fields
    suspend fun incrementFilledSlots(teamId: String): Result<Unit> = runCatching {
        db.runTransaction { tx ->
            val ref = teamsCol.document(teamId)
            val snap = tx.get(ref)
            val current = snap.getLong("filledSlots")?.toInt() ?: 0
            tx.update(ref, "filledSlots", current + 1)
        }.await()
    }
}
