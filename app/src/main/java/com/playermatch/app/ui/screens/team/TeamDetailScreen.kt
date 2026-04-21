package com.playermatch.app.ui.screens.team

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.navigation.NavController

// Full implementation in STEP 6 (Join Request System)
@Composable
fun TeamDetailScreen(teamId: String, navController: NavController) {
    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Text("Team Detail ($teamId) — implemented in Step 6")
    }
}
