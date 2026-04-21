package com.playermatch.app

import android.app.Application
import com.google.firebase.FirebaseApp

class PlayerMatchApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        FirebaseApp.initializeApp(this)
    }
}
