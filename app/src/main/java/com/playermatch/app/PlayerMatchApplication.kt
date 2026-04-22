package com.playermatch.app

import android.app.Application
import com.google.firebase.FirebaseApp
import com.playermatch.app.utils.NotificationHelper

class PlayerMatchApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        FirebaseApp.initializeApp(this)
        NotificationHelper.createChannel(this)
    }
}
