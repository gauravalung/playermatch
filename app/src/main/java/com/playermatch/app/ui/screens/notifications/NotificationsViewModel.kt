package com.playermatch.app.ui.screens.notifications

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.playermatch.app.data.model.AppNotification
import com.playermatch.app.data.repository.AuthRepository
import com.playermatch.app.data.repository.NotificationRepository
import com.playermatch.app.utils.NotificationHelper
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class NotificationsViewModel(app: Application) : AndroidViewModel(app) {

    private val notifRepo = NotificationRepository()
    private val authRepo = AuthRepository()

    val currentUserId: String? = authRepo.currentUserId

    private val _notifications = MutableStateFlow<List<AppNotification>>(emptyList())
    val notifications: StateFlow<List<AppNotification>> = _notifications.asStateFlow()

    val unreadCount: StateFlow<Int> get() = _unreadCount
    private val _unreadCount = MutableStateFlow(0)

    // IDs of notifications that have already triggered a system notification this session
    private val shownIds = mutableSetOf<String>()

    init {
        val uid = currentUserId ?: return
        viewModelScope.launch {
            notifRepo.getNotificationsFlow(uid).collect { notifs ->
                // Show system notification for each newly arrived unread item
                notifs.filter { !it.isRead && it.id !in shownIds }.forEach { n ->
                    NotificationHelper.show(
                        getApplication(),
                        notifId = n.id.hashCode(),
                        title = n.title,
                        body = n.body
                    )
                }
                shownIds.addAll(notifs.map { it.id })
                _notifications.value = notifs
                _unreadCount.value = notifs.count { !it.isRead }
            }
        }
    }

    fun markAllAsRead() {
        val uid = currentUserId ?: return
        viewModelScope.launch { notifRepo.markAllAsRead(uid) }
    }
}
