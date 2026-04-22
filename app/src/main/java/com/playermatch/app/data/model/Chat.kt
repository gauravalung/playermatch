package com.playermatch.app.data.model

data class Chat(
    val id: String = "",                                   // deterministic: min(uid1,uid2)_max(uid1,uid2)
    val participantIds: List<String> = emptyList(),
    val participantNames: Map<String, String> = emptyMap(), // uid → display name
    val lastMessage: String = "",
    val lastMessageTime: Long = 0L
)
