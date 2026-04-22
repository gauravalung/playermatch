package com.playermatch.app.data.repository

import com.google.firebase.firestore.FirebaseFirestore
import com.playermatch.app.data.model.Chat
import com.playermatch.app.data.model.Message
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await

class ChatRepository {

    private val db = FirebaseFirestore.getInstance()
    private val chatsCol = db.collection("chats")

    // Deterministic ID — both participants always arrive at the same string
    fun getChatId(uid1: String, uid2: String): String =
        "${minOf(uid1, uid2)}_${maxOf(uid1, uid2)}"

    // Creates the chat document if it doesn't already exist (idempotent)
    suspend fun getOrCreateChat(
        chatId: String,
        uid1: String, name1: String,
        uid2: String, name2: String
    ): Result<Unit> = runCatching {
        val ref = chatsCol.document(chatId)
        if (!ref.get().await().exists()) {
            ref.set(
                Chat(
                    id = chatId,
                    participantIds = listOf(uid1, uid2),
                    participantNames = mapOf(uid1 to name1, uid2 to name2)
                )
            ).await()
        }
    }

    // Sends a message and updates the last-message preview on the chat document
    suspend fun sendMessage(
        chatId: String,
        senderId: String,
        senderName: String,
        text: String
    ): Result<Unit> = runCatching {
        val messagesCol = chatsCol.document(chatId).collection("messages")
        val docRef = messagesCol.document()
        val now = System.currentTimeMillis()
        docRef.set(
            Message(
                id = docRef.id,
                senderId = senderId,
                senderName = senderName,
                text = text,
                timestamp = now
            )
        ).await()
        // Keep last-message preview in sync for future chat-list screen
        chatsCol.document(chatId).update(
            mapOf("lastMessage" to text, "lastMessageTime" to now)
        ).await()
    }

    // Real-time message stream — sorted client-side to avoid index requirements
    fun getMessagesFlow(chatId: String): Flow<List<Message>> = callbackFlow {
        val listener = chatsCol.document(chatId)
            .collection("messages")
            .addSnapshotListener { snapshot, error ->
                if (error != null) { close(error); return@addSnapshotListener }
                val sorted = (snapshot?.toObjects(Message::class.java) ?: emptyList())
                    .sortedBy { it.timestamp }
                trySend(sorted)
            }
        awaitClose { listener.remove() }
    }

    // All chats a user participates in (for a future chat-list screen)
    fun getUserChatsFlow(uid: String): Flow<List<Chat>> = callbackFlow {
        val listener = chatsCol
            .whereArrayContains("participantIds", uid)
            .addSnapshotListener { snapshot, error ->
                if (error != null) { close(error); return@addSnapshotListener }
                val sorted = (snapshot?.toObjects(Chat::class.java) ?: emptyList())
                    .sortedByDescending { it.lastMessageTime }
                trySend(sorted)
            }
        awaitClose { listener.remove() }
    }
}
