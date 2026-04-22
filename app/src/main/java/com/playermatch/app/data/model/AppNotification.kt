package com.playermatch.app.data.model

data class AppNotification(
    val id: String = "",
    val recipientId: String = "",
    val title: String = "",
    val body: String = "",
    val type: String = "",
    val teamId: String = "",
    val createdAt: Long = 0L,
    val isRead: Boolean = false
) {
    companion object {
        const val TYPE_REQUEST_SENT = "request_sent"
        const val TYPE_REQUEST_ACCEPTED = "request_accepted"
        const val TYPE_REQUEST_REJECTED = "request_rejected"
    }
}
