package com.playermatch.app.ui.navigation

import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Groups
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Map
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.google.firebase.auth.FirebaseAuth
import com.playermatch.app.ui.screens.auth.EmailVerificationScreen
import com.playermatch.app.ui.screens.auth.LoginScreen
import com.playermatch.app.ui.screens.auth.RegisterScreen
import com.playermatch.app.ui.screens.chat.ChatScreen
import com.playermatch.app.ui.screens.home.HomeScreen
import com.playermatch.app.ui.screens.map.MapScreen
import com.playermatch.app.ui.screens.profile.ProfileScreen
import com.playermatch.app.ui.screens.team.CreateTeamScreen
import com.playermatch.app.ui.screens.team.TeamDetailScreen
import com.playermatch.app.ui.screens.team.TeamsScreen

private val bottomNavItems = listOf(
    Triple(Screen.Home, "Home", Icons.Filled.Home),
    Triple(Screen.Map, "Map", Icons.Filled.Map),
    Triple(Screen.Teams, "Teams", Icons.Filled.Groups),
    Triple(Screen.Profile, "Profile", Icons.Filled.Person)
)

@Composable
fun NavGraph() {
    val navController = rememberNavController()
    val currentUser = FirebaseAuth.getInstance().currentUser
    val startDestination = when {
        currentUser == null -> Screen.Login.route
        // User registered but hasn't verified email yet — send them to verification screen
        !currentUser.isEmailVerified ->
            Screen.EmailVerification.createRoute(currentUser.email ?: "")
        else -> Screen.Home.route
    }

    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentDestination = navBackStackEntry?.destination

    val showBottomBar = bottomNavItems.any {
        currentDestination?.hierarchy?.any { dest -> dest.route == it.first.route } == true
    }

    Scaffold(
        bottomBar = {
            if (showBottomBar) {
                BottomNavBar(navController = navController)
            }
        }
    ) { innerPadding ->
        NavHost(
            navController = navController,
            startDestination = startDestination,
            modifier = Modifier.padding(innerPadding)
        ) {
            // Auth
            composable(Screen.Login.route) {
                LoginScreen(
                    onLoginSuccess = {
                        navController.navigate(Screen.Home.route) {
                            popUpTo(Screen.Login.route) { inclusive = true }
                        }
                    },
                    onNavigateToRegister = { navController.navigate(Screen.Register.route) },
                    onNavigateToVerification = { email ->
                        navController.navigate(Screen.EmailVerification.createRoute(email))
                    }
                )
            }
            composable(Screen.Register.route) {
                RegisterScreen(
                    onRegisterSuccess = {
                        // After register, always go to email verification before Home
                        val email = FirebaseAuth.getInstance().currentUser?.email ?: ""
                        navController.navigate(Screen.EmailVerification.createRoute(email)) {
                            popUpTo(Screen.Login.route) { inclusive = false }
                        }
                    },
                    onNavigateToLogin = { navController.popBackStack() }
                )
            }
            composable(
                route = Screen.EmailVerification.route,
                arguments = listOf(navArgument("email") { type = NavType.StringType })
            ) { backStackEntry ->
                val encodedEmail = backStackEntry.arguments?.getString("email") ?: ""
                val email = java.net.URLDecoder.decode(encodedEmail, "UTF-8")
                EmailVerificationScreen(
                    email = email,
                    onVerified = {
                        navController.navigate(Screen.Home.route) {
                            popUpTo(Screen.Login.route) { inclusive = true }
                        }
                    },
                    onBackToLogin = {
                        navController.navigate(Screen.Login.route) {
                            popUpTo(0) { inclusive = true }
                        }
                    }
                )
            }

            // Main
            composable(Screen.Home.route) {
                HomeScreen(navController = navController)
            }
            composable(Screen.Map.route) {
                MapScreen(navController = navController)
            }
            composable(Screen.Teams.route) {
                TeamsScreen(navController = navController)
            }
            composable(Screen.Profile.route) {
                ProfileScreen(
                    onLogout = {
                        navController.navigate(Screen.Login.route) {
                            popUpTo(0) { inclusive = true }
                        }
                    }
                )
            }

            // Detail
            composable(Screen.CreateTeam.route) {
                CreateTeamScreen(onTeamCreated = { navController.popBackStack() })
            }
            composable(
                route = Screen.TeamDetail.route,
                arguments = listOf(navArgument("teamId") { type = NavType.StringType })
            ) { backStackEntry ->
                TeamDetailScreen(
                    teamId = backStackEntry.arguments?.getString("teamId") ?: "",
                    navController = navController
                )
            }
            composable(
                route = Screen.Chat.route,
                arguments = listOf(
                    navArgument("chatId") { type = NavType.StringType },
                    navArgument("otherUserId") { type = NavType.StringType },
                    navArgument("otherUserName") { type = NavType.StringType }
                )
            ) { backStackEntry ->
                ChatScreen(
                    chatId = backStackEntry.arguments?.getString("chatId") ?: "",
                    otherUserId = backStackEntry.arguments?.getString("otherUserId") ?: "",
                    otherUserName = backStackEntry.arguments?.getString("otherUserName") ?: "",
                    onBack = { navController.popBackStack() }
                )
            }
        }
    }
}

@Composable
private fun BottomNavBar(navController: NavHostController) {
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentDestination = navBackStackEntry?.destination

    NavigationBar {
        bottomNavItems.forEach { (screen, label, icon) ->
            NavigationBarItem(
                icon = { Icon(icon, contentDescription = label) },
                label = { Text(label) },
                selected = currentDestination?.hierarchy?.any { it.route == screen.route } == true,
                onClick = {
                    navController.navigate(screen.route) {
                        popUpTo(navController.graph.findStartDestination().id) { saveState = true }
                        launchSingleTop = true
                        restoreState = true
                    }
                }
            )
        }
    }
}
