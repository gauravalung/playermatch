package com.playermatch.app.service

import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage
import com.playermatch.app.data.repository.UserRepository
import com.playermatch.app.utils.NotificationHelper
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

class PlayerMatchMessagingService : FirebaseMessagingService() {

    private val userRepo = UserRepository()
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    // Save refreshed token to Firestore so the server can always reach this device
    override fun onNewToken(token: String) {
        super.onNewToken(token)
        val uid = FirebaseAuth.getInstance().currentUser?.uid ?: return
        scope.launch { userRepo.updateFcmToken(uid, token) }
    }

    // Called when the app is in the foreground — show a system notification manually
    override fun onMessageReceived(message: RemoteMessage) {
        super.onMessageReceived(message)
        val title = message.notification?.title ?: message.data["title"] ?: return
        val body = message.notification?.body ?: message.data["body"] ?: return
        NotificationHelper.show(
            applicationContext,
            notifId = System.currentTimeMillis().toInt(),
            title = title,
            body = body
        )
    }
}
