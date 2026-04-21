package com.playermatch.app.data.model

data class Team(
    val id: String = "",
    val hostId: String = "",
    val hostName: String = "",
    val sport: String = "",
    val locationName: String = "",   // human-readable address / turf name
    val latitude: Double = 0.0,
    val longitude: Double = 0.0,
    val scheduledTime: Long = 0L,    // epoch millis
    val totalSlots: Int = 0,
    val filledSlots: Int = 0,
    val description: String = "",
    val createdAt: Long = System.currentTimeMillis()
)
