package com.playermatch.app.data.model

data class JoinRequest(
    val id: String = "",
    val teamId: String = "",
    val senderId: String = "",
    val senderName: String = "",
    val status: String = STATUS_PENDING,   // "pending" | "accepted" | "rejected"
    val createdAt: Long = System.currentTimeMillis()
) {
    companion object {
        const val STATUS_PENDING = "pending"
        const val STATUS_ACCEPTED = "accepted"
        const val STATUS_REJECTED = "rejected"
    }
}
