package com.playermatch.app.ui.navigation

sealed class Screen(val route: String) {
    // Auth
    object Login : Screen("login")
    object Register : Screen("register")

    // Main bottom-nav destinations
    object Home : Screen("home")
    object Map : Screen("map")
    object Teams : Screen("teams")
    object Profile : Screen("profile")

    // Detail screens (with arguments)
    object CreateTeam : Screen("create_team")

    object TeamDetail : Screen("team_detail/{teamId}") {
        fun createRoute(teamId: String) = "team_detail/$teamId"
    }

    object Chat : Screen("chat/{chatId}/{otherUserId}/{otherUserName}") {
        fun createRoute(chatId: String, otherUserId: String, otherUserName: String) =
            "chat/$chatId/$otherUserId/$otherUserName"
    }

    object EmailVerification : Screen("email_verification/{email}") {
        fun createRoute(email: String) = "email_verification/${email.encodeForRoute()}"
    }

    object PlayerDetail : Screen("player_detail/{playerId}") {
        fun createRoute(playerId: String) = "player_detail/$playerId"
    }
}

// Encode special characters (e.g. '@', '.') so they survive NavController routing
private fun String.encodeForRoute() =
    java.net.URLEncoder.encode(this, "UTF-8")
