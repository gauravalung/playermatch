package com.playermatch.app.data.model

data class User(
    val uid: String = "",
    val name: String = "",
    val email: String = "",
    val sport: String = "",          // e.g. "cricket", "football", "badminton"
    val latitude: Double = 0.0,
    val longitude: Double = 0.0,
    val fcmToken: String = "",
    val createdAt: Long = System.currentTimeMillis()
)
